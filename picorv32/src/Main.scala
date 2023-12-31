package rvcosim.picorv32

import chisel3.stage.{ChiselGeneratorAnnotation, PrintFullStackTraceAnnotation}
import firrtl.stage.FirrtlCircuitAnnotation
import firrtl.AnnotationSeq

object RunPicorv32 extends App {

  val resources = os.resource()
  val runDir = os.pwd / "run"
  os.remove.all(runDir)
  val elaborateDir = runDir / "elaborate"
  os.makeDir.all(elaborateDir)
  val rtlDir = runDir / "rtl"
  os.makeDir.all(rtlDir)
  val emulatorDir = runDir / "emulator"
  os.makeDir.all(emulatorDir)
  val emulatorCSrc = emulatorDir / "src"
  os.makeDir.all(emulatorCSrc)
  val emulatorCHeader = emulatorDir / "include"
  os.makeDir.all(emulatorCHeader)
  val emulatorBuildDir = emulatorDir / "build"
  os.makeDir.all(emulatorBuildDir)
  val swDir = runDir / "sw"
  os.makeDir.all(swDir)
  val releaseDir = os.pwd / "release"
  os.makeDir.all(releaseDir)


  val emulatorThreads = 8
  val verilatorArgs = Seq(
    // format: off
    "--x-initial unique",
    "--output-split 100000",
    "--max-num-width 1048576",
    "--main",
    "--timing",
    "--Wno-TIMESCALEMOD",
    // use for coverage
    "--coverage-user",
    "--assert",
    // format: on
  )

  // TODO: this will be replaced by binder API
  // elaborate
  var topName: String = null
  val annos: AnnotationSeq = Seq(
    new chisel3.stage.phases.Elaborate,
    new chisel3.stage.phases.Convert,
  ).foldLeft(
      Seq(
        ChiselGeneratorAnnotation(() => new Cosim),
        PrintFullStackTraceAnnotation,
      ): AnnotationSeq
    ) { case (annos, stage) => stage.transform(annos) }
    .flatMap {
      case FirrtlCircuitAnnotation(circuit) =>
        topName = circuit.main
        os.write.over(elaborateDir / s"$topName.fir", circuit.serialize)
        None
      case _: chisel3.stage.DesignAnnotation[_] => None
      case _: chisel3.stage.ChiselCircuitAnnotation => None
      case PrintFullStackTraceAnnotation => None
      case a => Some(a)
    }
  os.write.over(elaborateDir / s"$topName.anno.json", firrtl.annotations.JsonProtocol.serialize(annos))

  // rtl
  os.proc(
    "firtool",
    elaborateDir / s"$topName.fir", s"--annotation-file=${elaborateDir / s"$topName.anno.json"}",
    "-dedup",
    "-O=release",
    "--disable-all-randomization",
    "--split-verilog",
    "--preserve-values=none",
    "--preserve-aggregate=all",
    "--lowering-options=omitVersionComment",
    "--strip-debug-info",
    s"-o=$rtlDir"
  ).call()
  val verilogs = os.read.lines(rtlDir / "filelist.f")
    .map(str =>
      try {
        os.Path(str)
      } catch {
        case e: IllegalArgumentException if e.getMessage.contains("is not an absolute path") =>
          rtlDir / str.stripPrefix("./")
      }
    )
    .filter(p => p.ext == "v" || p.ext == "sv")


  // TODO: this should goto vcs/arcilator as well
  // verilator
  // todo: copy from resource dir to verilator dir
  val allCSourceFiles = Seq(
    "bridge.cc",
    "custom.cc",
    "dpi.cc",
    "spike.cc"
  ).map { f =>
    os.write.over(emulatorCSrc / f, os.read(resources / f))
    emulatorCSrc / f
  }

  val allCHeaderFiles = Seq(
    "bridge.h",
    "consts.h",
    "custom.h",
    "elfloader.h",
    "exceptions.h",
    "glog.h",
    "sim.h",
    "spike.h",
    "utils.h"
  ).map { f =>
    os.write.over(emulatorCHeader / f, os.read(resources / f))
    emulatorCHeader / f
  }

  os.write(emulatorBuildDir / "CMakeLists.txt",
    // format: off
    s"""cmake_minimum_required(VERSION 3.20)
       |project(emulator)
       |set(CMAKE_CXX_STANDARD 17)
       |
       |find_package(args REQUIRED)
       |find_package(glog REQUIRED)
       |find_package(fmt REQUIRED)
       |find_package(libspike REQUIRED)
       |find_package(verilator REQUIRED)
       |find_package(jsoncpp REQUIRED)
       |find_package(Threads REQUIRED)
       |set(THREADS_PREFER_PTHREAD_FLAG ON)
       |
       |add_executable(emulator
       |${allCSourceFiles.mkString("\n")}
       |)
       |
       |target_include_directories(emulator PUBLIC $emulatorCHeader)
       |
       |target_link_libraries(emulator PUBLIC $${CMAKE_THREAD_LIBS_INIT})
       |target_link_libraries(emulator PUBLIC libspike fmt::fmt glog::glog jsoncpp)  # note that libargs is header only, nothing to link
       |target_compile_definitions(emulator PRIVATE COSIM_VERILATOR)
       |
       |verilate(emulator
       |  SOURCES
       |${verilogs.mkString("\n")}
       |  "TRACE_FST"
       |  TOP_MODULE $topName
       |  PREFIX V$topName
       |  OPT_FAST
       |  THREADS $emulatorThreads
       |  VERILATOR_ARGS ${verilatorArgs.mkString(" ")}
       |)
       |""".stripMargin
    // format: on
  )

  // build verilator
  os.proc(Seq(
    "cmake",
    "-G", "Ninja",
    "-S", emulatorBuildDir,
    "-B", emulatorBuildDir
  ).map(_.toString)).call(emulatorBuildDir)

  // build emulator
  os.proc(Seq("ninja", "-C", emulatorBuildDir).map(_.toString)).call(emulatorBuildDir)

  // build smoke
  os.write.over(swDir / "smoke.S", os.read(resources / "smoke.S"))
  os.proc(Seq(
    "clang-rv32",
    "-mabi=ilp32",
    "-march=rv32i",
    "-mno-relax",
    "-static",
    "-mcmodel=medany",
    "-fvisibility=hidden",
    "-nostdlib",
    "-Wl,--entry=start",
    "-fno-PIC",
    swDir / "smoke.S",
    "-o",
    swDir / "smoke.elf").map(_.toString)
  ).call(swDir)

  // run emulator
  os.proc(Seq(
    emulatorBuildDir / "emulator"
  )).call(env = Map(
    "GLOG_logtostderr" -> "1",
    "COSIM_isa"->"rv32i",
    "COSIM_elf"-> (swDir / "smoke.elf").toString,
    "COSIM_wave"-> (runDir / "wave.fst").toString
  ))

  // release
  os.remove.all(releaseDir)
  os.makeDir.all(releaseDir / "vsrc")
  verilogs.foreach(f => os.copy.into(f, releaseDir / "vsrc"))
  os.makeDir.all(releaseDir / "csrc")
  (allCSourceFiles ++ allCHeaderFiles).foreach(f => os.copy.into(f, releaseDir / "csrc"))
  os.copy.into(os.pwd / "nix", releaseDir)
  os.copy.into(os.pwd / "flake.nix", releaseDir)
  os.copy.into(os.pwd / "flake.lock", releaseDir)
  os.copy.into(os.pwd / "overlay.nix", releaseDir)
  // some post fixes
  os.write(releaseDir / "CMakeLists.txt", os.read.lines(emulatorBuildDir / "CMakeLists.txt")
    .map(_.replace(emulatorCSrc.toString, "csrc"))
    .map(_.replace(emulatorCHeader.toString, "csrc"))
    .map(_.replace(rtlDir.toString, "vsrc"))
    .mkString("\n")
  )
  os.proc("tar", "cvf", os.pwd / "release.tar.xz", "release").call(os.pwd)

}
