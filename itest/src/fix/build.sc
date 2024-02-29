import $file.plugins
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.scalalib._
import os._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion               = "2.13.13"
  def scalafixScalaBinaryVersion = mill.scalalib.api.ZincWorkerUtil.scalaBinaryVersion(scalaVersion())
  def scalacOptions              = Seq("-Ywarn-unused")
}

def verify() =
  T.command {
    val fixedScala = read(pwd / "project" / "src" / "Fix.scala")
    val expected = """object Fix {
                     |  def procedure(): Unit = {}
                     |  def myComplexMethod: Map[Int,String] = 1.to(10).map(i => i -> i.toString).toMap
                     |}
                     |""".stripMargin
    assert(fixedScala == expected)
  }
