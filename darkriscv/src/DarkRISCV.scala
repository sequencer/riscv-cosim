package rvcosim.darkriscv

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.util.{HasExtModuleInline, HasExtModuleResource}
import firrtl.stage.FirrtlCircuitAnnotation
import rvcosim._
import chisel3.probe._

class DarkRISCVParameter extends CoreParameter {
  override val ifAddressWidth: Int = 32
  override val ifDataWidth: Int = 32
  override val lsuAddressWidth: Int = 32
  override val lsuDataWidth: Int = 32
}

class DarkRISCVWrapper extends Core {
  def parameter: CoreParameter = new DarkRISCVParameter

  val darkriscv = Module(new DarkRISCV)
  darkriscv.clock := clock
  darkriscv.reset := reset
  darkriscv.halt := false.B
  darkriscv.idata := instructionFetch.response.data
  instructionFetch.request.bits.address := darkriscv.iaddr
  instructionFetch.request.valid := true.B
  darkriscv.datai := loadStore.response.bits.data
  loadStore.request.bits.data := darkriscv.datao
  loadStore.request.bits.address := darkriscv.daddr
  loadStore.request.bits.maskByte := darkriscv.be
  loadStore.request.bits.writeEnable := darkriscv.wr
  loadStore.request.valid := darkriscv.wr || darkriscv.rd
  // dontcare darkriscv.idle
  // dontcare debug
  define(rfWriteValid, darkriscv.rfWriteValid)
  define(rfWriteData, darkriscv.rfWriteData)
  define(rfWriteAddress, darkriscv.rfWriteAddress)
  define(rfWriteFp, RWProbeValue(WireDefault(false.B)))
  define(rfWriteVector, RWProbeValue(WireDefault(false.B)))
}

class DarkRISCV extends ExtModule
  with HasExtModuleResource
  with HasExtModuleDefine
  with HasExtModuleInline {
  override def desiredName: String = "darkriscv"
  /* input             CLK,   // clock */
  val clock = IO(Input(Clock())).suggestName("CLK")
  /* input             RES,   // reset */
  val reset = IO(Input(Reset())).suggestName("RES")
  /* input             HLT,   // halt */
  val halt = IO(Input(Bool())).suggestName("HLT")
  /* input      [31:0] IDATA, // instruction data bus */
  val idata = IO(Input(UInt(32.W))).suggestName("IDATA")
  /* output     [31:0] IADDR, // instruction addr bus */
  val iaddr = IO(Output(UInt(32.W))).suggestName("IADDR")
  /* input      [31:0] DATAI, // data bus (input) */
  val datai = IO(Input(UInt(32.W))).suggestName("DATAI")
  /* output     [31:0] DATAO, // data bus (output) */
  val datao = IO(Output(UInt(32.W))).suggestName("DATAO")
  /* output     [31:0] DADDR, // addr bus */
  val daddr = IO(Output(UInt(32.W))).suggestName("DADDR")
  /* output     [ 3:0] BE,   // byte enable */
  val be = IO(Output(UInt(4.W))).suggestName("BE")
  /* output            WR,    // write enable */
  val wr = IO(Output(Bool())).suggestName("WR")
  /* output            RD,    // read enable */
  val rd = IO(Output(Bool())).suggestName("RD")
  /* output            IDLE,   // idle output */
  val idle = IO(Output(Bool())).suggestName("IDLE")
  /* output [3:0]  DEBUG       // old-school osciloscope based debug! :) */
  val debug = IO(Output(UInt(4.W))).suggestName("DEBUG")

  // XMR
  val rfWriteValid = define(RWProbe(Bool()), Seq("darkriscv", "darkriscv", "XRES"))
  val rfWriteAddress = define(RWProbe(UInt(5.W)), Seq("darkriscv", "darkriscv", "DPTR"))
  val rfWriteData = define(RWProbe(UInt(32.W)), Seq("darkriscv", "darkriscv", "XIDATA"))


  setInline(
    "config.v",
    """//`define __3STAGE__
      |//`define __RV32E__
      |//`define __THREADS__ 3
      |//`define __MAC16X16__
      |//`define __FLEXBUZZ__
      |//`define __INTERRUPT__
      |`define __RESETPC__ 32'd0
      |//`define __INTERACTIVE__
      |//`define __PERFMETER__
      |//`define __REGDUMP__
      |//`define __HARVARD__
      |`ifdef __HARVARD__
      |    `define MLEN 13 // MEM[12:0] ->  8KBytes LENGTH = 0x2000
      |`else
      |    `define MLEN 12 // MEM[12:0] -> 4KBytes LENGTH = 0x1000
      |    //`define MLEN 15 // MEM[12:0] -> 16KBytes LENGTH = 0x8000 for coremark!
      |`endif
      |//`define __RMW_CYCLE__
      |""".stripMargin)
  addResource("darkriscv.v")
}

object RunDarkRISCV extends App {

  import chisel3.stage.ChiselGeneratorAnnotation
  import firrtl.AnnotationSeq

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
    new chisel3.stage.phases.Convert
  ).foldLeft(
      Seq(
        ChiselGeneratorAnnotation(() => new Cosim(new DarkRISCVWrapper))
      ): AnnotationSeq
    ) { case (annos, stage) => stage.transform(annos) }
    .flatMap {
      case FirrtlCircuitAnnotation(circuit) =>
        topName = circuit.main
        os.write.over(elaborateDir / s"$topName.fir", circuit.serialize)
        None
      case _: chisel3.stage.DesignAnnotation[_] => None
      case _: chisel3.stage.ChiselCircuitAnnotation => None
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
    "csr.h",
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
