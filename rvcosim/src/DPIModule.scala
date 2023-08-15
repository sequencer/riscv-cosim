package chisel3

import chisel3.experimental.ExtModule
import chisel3.probe._
import chisel3.internal.firrtl.{KnownWidth, UnknownWidth}
import chisel3.util.HasExtModuleInline

import scala.collection.mutable.ArrayBuffer

case class DPIElement[T <: Element](name: String, output: Boolean, data: T)

case class DPIReference[T <: Element](name: String, ref: T)

abstract class DPIModule
  extends ExtModule
    with HasExtModuleInline
    with HasExtModuleDefine {
  val isImport: Boolean
  val references: ArrayBuffer[DPIElement[_]] = scala.collection.mutable.ArrayBuffer.empty[DPIElement[_]]
  val dpiReferences: ArrayBuffer[DPIElement[_]] = scala.collection.mutable.ArrayBuffer.empty[DPIElement[_]]

  def bind[T <: Element](name: String, output: Boolean, isDPIArg: Boolean, data: T) = {
    val ref = define(data, Seq(desiredName, desiredName, name))
    val ele = DPIElement(name, output, ref)
    require(chisel3.reflect.DataMirror.hasProbeTypeModifier(data), s"$name should be a probe type")
    require(!references.exists(ele => ele.name == name), s"$name already added.")
    references += ele
    if (isDPIArg) {
      dpiReferences += ele
    }
    DPIReference(name, ref)
  }


  val body: String

  // Magic to execute post-hook
  private[chisel3] override def generateComponent() = {
    // return binding function and probe signals
    val localDefinition = references.map {
      case DPIElement(name, _, element) =>
        val width = chisel3.reflect.DataMirror.widthOf(element) match {
          case UnknownWidth() => throw new Exception(s"$desiredName.$name width unknown")
          case KnownWidth(value) => value
        }
        val localDefinitionTpe = if (width != 1) s"[${width - 1}:0] " else ""
        s"logic $localDefinitionTpe$name"
    }.mkString(";\n")

    val dpiArg = dpiReferences.map {
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
         |${if (isImport) """import "DPI-C" function void""" else """export "DPI-C" function"""} $desiredName(
         |$dpiArg
         |);
         |$body
         |endmodule
         |""".stripMargin
    )
    super.generateComponent()
  }
}

