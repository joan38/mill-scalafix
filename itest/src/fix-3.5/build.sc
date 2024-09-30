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
                     |  // use a 3.5.x-only feature to fail if a Scala 3 LTS compiler is used 
                     |  // https://www.scala-lang.org/blog/2024/08/22/scala-3.5.0-released.html#support-for-binary-integer-literals 
                     |  def myComplexMethod: Map[Int, String] = 1.to(0B1010).map(i => i -> i.toString).toMap
                     |}
                     |""".stripMargin
    println(fixedScala)
    assert(fixedScala == expected)
  }
