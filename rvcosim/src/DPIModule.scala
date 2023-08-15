package rvcosim

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.probe._
import chisel3.internal.firrtl.{KnownWidth, UnknownWidth}
import chisel3.util.HasExtModuleInline

import scala.collection.mutable.ArrayBuffer

case class DPIElement(name: String, output: Boolean, data: Element)

trait DPIParameter {
  val isImport: Boolean

  def bind(name: String, output: Boolean, isDPIArg: Boolean, data: Element) = {
    val ele = DPIElement(name, output, data)
    require(chisel3.reflect.DataMirror.hasProbeTypeModifier(data), s"$name should be a probe type")
    require(!references.exists(ele => ele.name == name), s"$name already added.")
    references += ele
    if(isDPIArg) {
      dpiReferences += ele
    }
    ele
  }

  val references: ArrayBuffer[DPIElement] = scala.collection.mutable.ArrayBuffer.empty[DPIElement]
  val dpiReferences: ArrayBuffer[DPIElement] = scala.collection.mutable.ArrayBuffer.empty[DPIElement]
  val body: String
}

abstract class DPIModule[T <: DPIParameter](parameter: DPIParameter)
  extends ExtModule
    with HasExtModuleInline
    with HasExtModuleDefine {
  val ref = parameter.references.map { case DPIElement(k, _, v) => k -> define(v, Seq(desiredName, desiredName, k)) }.toMap

  // return binding function and probe signals
  val localDefinition = parameter.references.map {
    case DPIElement(name, _, element) =>
      val width = chisel3.reflect.DataMirror.widthOf(element) match {
        case UnknownWidth() => throw new Exception(s"$desiredName.$name width unknown")
        case KnownWidth(value) => value
      }
      val localDefinitionTpe = if (width != 1) s"[${width - 1}:0] " else ""
      s"logic $localDefinitionTpe$name"
  }.mkString(";\n")

  val dpiArg = parameter.dpiReferences.map {
    case DPIElement(name, output, element) =>
      val direction = if (output) "output " else "input "
      val width = chisel3.reflect.DataMirror.widthOf(element) match {
        case UnknownWidth() => throw new Exception(s"$desiredName.$name width unknown")
        case KnownWidth(value) => value
      }
      val functionParameterTpe = if (width != 1) s"bit[${width - 1}:0] " else ""
      s"$direction$functionParameterTpe$name"
  }.mkString(",\n")

  setInline(
    s"$desiredName.sv",
    s"""module $desiredName;
       |${if (localDefinition.isEmpty) "" else localDefinition + ";"}
       |${if (parameter.isImport) """import "DPI-C" function void""" else """export "DPI-C" function"""} $desiredName(
       |$dpiArg
       |);
       |${parameter.body}
       |endmodule
       |""".stripMargin
  )
}

