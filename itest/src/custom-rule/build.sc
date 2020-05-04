import $exec.plugins
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.scalalib._
import os._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion           = "2.12.11"
  override def scalacOptions = Seq("-Ywarn-unused")
  def scalafixIvyDeps        = Agg(ivy"com.geirsson::example-scalafix-rule:1.3.0")
}

def verify() =
    T.command {
      val fixedScala = read(pwd / "project" / "src" / "Fix.scala")
      val expected   = """object Fix {
                       |}
                       |// v1 SyntacticRule!
                       |""".stripMargin
      assert(fixedScala == expected)
    }
