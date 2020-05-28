import $exec.plugins
import com.goyeau.mill.scalafix.ScalafixModule
import mill.scalalib._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion           = "2.13.2"
  override def scalacOptions = Seq("-Ywarn-unused")
}
