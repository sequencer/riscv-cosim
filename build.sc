import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import $file.chisel.build
import $file.common

object v {
  val scala = "2.13.11"
}

object chisel extends Chisel

trait Chisel
  extends millbuild.chisel.build.Chisel {
  def crossValue = v.scala

  override def millSourcePath = os.pwd / "chisel"

  def scalaVersion = T(v.scala)
}


object rvcosim extends RVCosim

trait RVCosim
  extends common.RVCosimModule
    with ScalafmtModule {
  override def scalaVersion = T(v.scala)

  override def millSourcePath = os.pwd / "rvcosim"

  def chiselModule = Some(chisel)

  def chiselPluginJar = T(Some(chisel.pluginModule.jar()))

  def chiselIvy = None

  def chiselPluginIvy = None
}

object darkriscv extends Darkriscv

trait Darkriscv
  extends common.HasRVCosimModule
    with ScalafmtModule {
  override def scalaVersion = T(v.scala)

  override def millSourcePath = os.pwd / "darkriscv"

  def rvcosimModule = rvcosim
}
