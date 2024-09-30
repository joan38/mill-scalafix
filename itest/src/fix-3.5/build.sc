import $file.plugins
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.scalalib._
import os._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion = "3.5.1"
}

def verify() =
  T.command {
    val fixedScala = read(pwd / "project" / "src" / "Fix.scala")
    val expected = """object Fix {
                     |  def myComplexMethod: Map[Int, String] = 1.to(10).map(i => i -> i.toString).toMap
                     |}
                     |""".stripMargin
    assert(fixedScala == expected)
  }
