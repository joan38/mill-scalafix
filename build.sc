import $ivy.`com.goyeau::mill-git:0.1.0-4-9b459c6`
//import $ivy.`com.goyeau::mill-scalafix:7d08ec1`
import $ivy.`com.lihaoyi::mill-contrib-bsp:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest:0.2.1`
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.1.2`
import com.goyeau.mill.git.GitVersionedPublishModule
//import com.goyeau.mill.scalafix.ScalafixModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import scalalib._
import mill.scalalib.scalafmt.ScalafmtModule

object `mill-scalafix` extends Cross[MillScalafixModule](crossScalaVersions: _*)
class MillScalafixModule(val crossScalaVersion: String)
    extends CrossScalaModule
    with TpolecatModule
    with ScalafmtModule
//    with ScalafixModule
    with GitVersionedPublishModule {
  override def scalacOptions =
    super
      .scalacOptions()
      .filter(!scalaVersion().startsWith("2.13") || !Seq("-Wunused:imports", "-Xfatal-warnings").contains(_))

  lazy val millVersion = millVersionFor(crossScalaVersion)
  override def compileIvyDeps =
    super.compileIvyDeps() ++ Agg(
      ivy"com.lihaoyi::mill-main:$millVersion",
      ivy"com.lihaoyi::mill-scalalib:$millVersion"
    )
  override def ivyDeps =
    super.ivyDeps() ++ Agg(
      ivy"ch.epfl.scala:scalafix-interfaces:0.9.15",
      ivy"org.scala-lang.modules::scala-collection-compat:2.1.6",
      ivy"org.scala-lang.modules::scala-java8-compat:0.9.1"
    )

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

object itest extends Cross[IntegrationTestModule](crossScalaVersions: _*)
class IntegrationTestModule(val crossScalaVersion: String) extends MillIntegrationTestModule {
  override def millSourcePath = super.millSourcePath / ammonite.ops.up

  def millTestVersion  = millVersionFor(crossScalaVersion)
  def pluginsUnderTest = Seq(`mill-scalafix`(crossScalaVersion))
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

lazy val crossScalaVersions = Seq("2.13.2", "2.12.11")
def millVersionFor(scalaVersion: String) = if (scalaVersion.startsWith("2.13")) "0.6.2-35-7d1144" else "0.6.2"
