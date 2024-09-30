import $file.plugins
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.scalalib._
import os._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion  = "2.12.19"
  def scalacOptions = Seq("-Ywarn-unused")
  def ivyDeps = Agg(ivy"org.scala-lang.modules::scala-collection-compat:2.12.0")
}

def verify() =
  T.command {
    val fixedScala = read(pwd / "project" / "src" / "Fix.scala")
    val expected = """
                     |
                     |object Fix {
                     |  def procedure(): Unit = {}
                     |}
                     |""".stripMargin
    assert(fixedScala == expected)
  }
