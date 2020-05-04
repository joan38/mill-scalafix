import $ivy.`com.goyeau::mill-git:0.1.0`
import $ivy.`com.lihaoyi::mill-contrib-bsp:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest:0.2.1`
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.1.2`
import com.goyeau.mill.git.GitVersionedPublishModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import scalalib._
import mill.scalalib.scalafmt.ScalafmtModule

object `mill-scalafix` extends ScalaModule with TpolecatModule with ScalafmtModule with GitVersionedPublishModule {
  def scalaVersion = "2.12.11"

  def millVersion = "0.6.2"
  override def compileIvyDeps =
    Agg(
      ivy"com.lihaoyi::mill-main:$millVersion",
      ivy"com.lihaoyi::mill-scalalib:$millVersion"
    )
  override def ivyDeps =
    Agg(
      ivy"ch.epfl.scala:::scalafix-cli:0.9.15",
      ivy"org.scala-lang.modules::scala-java8-compat:0.9.1"
    )

  def pomSettings =
    PomSettings(
      description = artifactName(),
      organization = "com.goyeau",
      url = "https://github.com/joan38/mill-scalafix",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("joan38", "mill-scalafix"),
      developers = Seq(Developer("joan38", "Joan Goyeau", "https://github.com/joan38"))
    )
}

object itest extends MillIntegrationTestModule {
  def millTestVersion  = "0.6.2"
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
