import $ivy.`com.goyeau::mill-git:0.1.1`
import $ivy.`com.goyeau::mill-scalafix:0.1.3`
import $ivy.`com.lihaoyi::mill-contrib-bsp:$MILL_VERSION`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest:0.3.1`
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.1.3`
import com.goyeau.mill.git.GitVersionedPublishModule
import com.goyeau.mill.scalafix.StyleModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import scalalib._

object `mill-scalafix`
    extends ScalaModule
    with TpolecatModule
    with StyleModule
    with BuildInfo
    with GitVersionedPublishModule {
  override def scalaVersion = "2.13.2"

  lazy val millVersion = "0.7.4"
  override def compileIvyDeps =
    super.compileIvyDeps() ++ Agg(
      ivy"com.lihaoyi::mill-main:$millVersion",
      ivy"com.lihaoyi::mill-scalalib:$millVersion"
    )
  val scalafixVersion = "0.9.19"
  override def ivyDeps =
    super.ivyDeps() ++ Agg(
      ivy"ch.epfl.scala:scalafix-interfaces:$scalafixVersion",
      ivy"org.scala-lang.modules::scala-collection-compat:2.1.6",
      ivy"org.scala-lang.modules::scala-java8-compat:0.9.1"
    )

  def buildInfoPackageName                     = Some("com.goyeau.mill.scalafix")
  def buildInfoMembers: T[Map[String, String]] = T(Map("scalafixVersion" -> scalafixVersion))

  override def artifactName = "mill-scalafix"
  def pomSettings =
    PomSettings(
      description = "A Scalafix plugin for Mill build tool",
      organization = "com.goyeau",
      url = "https://github.com/joan38/mill-scalafix",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("joan38", "mill-scalafix"),
      developers = Seq(Developer("joan38", "Joan Goyeau", "https://github.com/joan38"))
    )
}

object itest extends MillIntegrationTestModule {
  def millTestVersion  = `mill-scalafix`.millVersion
  def pluginsUnderTest = Seq(`mill-scalafix`)
  override def testInvocations =
    Seq(
      PathRef(sources().head.path / "fix") -> Seq(
        TestInvocation.Targets(Seq("__.fix")),
        TestInvocation.Targets(Seq("verify"))
      ),
      PathRef(sources().head.path / "check") -> Seq(
        TestInvocation.Targets(Seq("__.fix", "--check"))
      ),
      PathRef(sources().head.path / "check-failed") -> Seq(
        TestInvocation.Targets(Seq("__.fix", "--check"), expectedExitCode = 1)
      ),
      PathRef(sources().head.path / "custom-rule") -> Seq(
        TestInvocation.Targets(Seq("__.fix")),
        TestInvocation.Targets(Seq("verify"))
      ),
      PathRef(sources().head.path / "no-source") -> Seq(
        TestInvocation.Targets(Seq("__.fix"))
      )
    )
}
