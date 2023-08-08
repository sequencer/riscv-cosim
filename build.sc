import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import $file.common

object v {
  val scala = "2.13.10"
  val chisel = ivy"org.chipsalliance::chisel:6.0.0-M2"
  val chiselPlugin = ivy"org.chipsalliance:::chisel-plugin:6.0.0-M2"
}

object rvcosim extends RVCosim

trait RVCosim
  extends common.RVCosimModule
    with ScalafmtModule {
  override def scalaVersion = T(v.scala)

  override def millSourcePath = os.pwd / "rvcosim"

  def chiselModule = None

  def chiselPluginJar = None

  def chiselIvy = Some(v.chisel)

  def chiselPluginIvy = Some(v.chiselPlugin)
}

object darkriscv extends Darkriscv

trait Darkriscv
  extends common.HasRVCosimModule
    with ScalafmtModule {
  override def scalaVersion = T(v.scala)

  override def millSourcePath = os.pwd / "darkriscv"

  def rvcosimModule = rvcosim
}
