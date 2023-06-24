import $file.plugins
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.scalalib._
import os._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion  = "2.13.10"
  def scalacOptions = Seq("-Ywarn-unused")
}

def verify() =
  T.command {
    val fixedScala = read(pwd / "project" / "src" / "Fix.scala")
    val expected = """object Fix {
                     |  def procedure(): Unit = {}
                     |}
                     |""".stripMargin
    assert(fixedScala == expected)
  }
