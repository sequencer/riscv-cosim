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

  val emulatorThreads = 8
  val verilatorArgs = Seq(
    // format: off
    "--x-initial unique",
    "--output-split 100000",
    "--max-num-width 1048576",
    "--main",
    "--timing",
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
       |  ${verilogs.mkString("\n")}
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
}
