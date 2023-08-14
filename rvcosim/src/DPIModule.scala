package rvcosim

import chisel3._
import chisel3.experimental.ExtModule
import chisel3.probe._
import chisel3.internal.firrtl.{KnownWidth, UnknownWidth}
import chisel3.util.HasExtModuleInline

import scala.collection.SeqMap

trait DPIParameter {
  val isImport: Boolean
  val references: SeqMap[String, chisel3.Element] = SeqMap.empty
  val body: String
}

abstract class DPIModule[T <: DPIParameter](parameter: DPIParameter)
  extends ExtModule
    with HasExtModuleInline
    with HasExtModuleDefine {
  // return binding function and probe signals
  val dpiModuleMap = parameter.references.map {
    case (name, element) =>
      val direction = chisel3.reflect.DataMirror.specifiedDirectionOf(element) match {
        case SpecifiedDirection.Unspecified => throw new Exception(s"$desiredName.$name direction unknown")
        case SpecifiedDirection.Output => "output"
        case SpecifiedDirection.Input => "input"
        case SpecifiedDirection.Flip => throw new Exception(s"$desiredName.$name direction flip")
      }
      val width = chisel3.reflect.DataMirror.widthOf(element) match {
        case UnknownWidth() => throw new Exception(s"$desiredName.$name width unknown")
        case KnownWidth(value) => value
      }
      val tpe = s"bit[${width - 1}:0]"
      val localDefinition = s"logic $tpe $name"
      val functionParameter = s"$direction $tpe $name"
      name -> (localDefinition, functionParameter)
  }
  val localDefinition: String = dpiModuleMap.view.mapValues(_._1).mkString(";\n")
  val functionParameter: String = dpiModuleMap.view.mapValues(_._2).mkString(",\n")

  val dpiRefMap = parameter.references.map{ case (k, v) => k -> define(v, Seq(desiredName, desiredName, k)) }
    setInline(
          s"$desiredName.sv",
      s"""module $desiredName;
       |$localDefinition
       |${if (parameter.isImport) """import "DPI-C" function void""" else """export "DPI-C" function"""} $desiredName($functionParameter);
       |${parameter.body}
       |endmodule
       |""".stripMargin
        )
}

