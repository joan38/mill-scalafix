import $file.plugins
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.scalalib._
import os._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion = "2.12.17"
  def semanticDbEnablePluginScalacOptions =
    super.semanticDbEnablePluginScalacOptions() ++ Seq("-P:semanticdb:synthetics:on")
  def scalafixIvyDeps = Agg(ivy"org.scala-lang.modules::scala-collection-migrations:2.12.0")
}

def verify() =
  T.command {
    val fixedScala = read(pwd / "project" / "src" / "Fix.scala")
    val expected = """import scala.language.postfixOps
                     |object Tuple2ZippedSrc213 {
                     |  def zipped(xs: List[Int], ys: List[Int]): Unit = {
                     |    xs.lazyZip(ys)
                     |    xs.lazyZip(ys)
                     |    (xs.lazyZip(ys) )
                     |    (xs.lazyZip(ys))
                     |    xs.lazyZip(ys)
                     |    /* a */
                     |     /* b */ xs /* c */.lazyZip(/* d */ ys /* e */ ) /* f */  /* g */  /* h */
                     |    coll(1).lazyZip(coll(2))
                     |    List(1, 2, 3).lazyZip(Array(1))
                     |  }
                     |  def coll(x: Int): List[Int] = ???
                     |}
                     |""".stripMargin
    assert(fixedScala == expected)
  }
