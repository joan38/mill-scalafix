import $ivy.`com.goyeau::mill-git::0.2.5`
import $ivy.`com.goyeau::mill-scalafix::0.4.0`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.7.1`
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.5`
import com.goyeau.mill.git.{GitVersionModule, GitVersionedPublishModule}
import com.goyeau.mill.scalafix.StyleModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.define.Cross
import mill.scalalib.api.ZincWorkerUtil.scalaNativeBinaryVersion
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import scalalib._

val millVersions                           = Seq("0.10.15", "0.11.9")
def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(millVersion)

object `mill-scalafix` extends Cross[MillScalafixCross](millVersions)
trait MillScalafixCross
    extends CrossModuleBase
    with TpolecatModule
    with StyleModule
    with GitVersionedPublishModule
    with Cross.Module[String] {
  def millVersion                = crossValue
  override def crossScalaVersion = "2.13.14"
  override def artifactSuffix    = s"_mill${millBinaryVersion(millVersion)}" + super.artifactSuffix()

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-main:$millVersion",
    ivy"com.lihaoyi::mill-scalalib:$millVersion"
  )
  val scalafixVersion = "0.12.1"
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"ch.epfl.scala:scalafix-interfaces:$scalafixVersion",
    ivy"org.scala-lang.modules::scala-collection-compat:2.12.0",
    ivy"org.scala-lang.modules::scala-java8-compat:1.0.2"
  )

  override def publishVersion = GitVersionModule.version(withSnapshotSuffix = true)()
  def pomSettings = PomSettings(
    description = "A Scalafix plugin for Mill build tool",
    organization = "com.goyeau",
    url = "https://github.com/joan38/mill-scalafix",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("joan38", "mill-scalafix"),
    developers = Seq(Developer("joan38", "Joan Goyeau", "https://github.com/joan38"))
  )
}

object itest extends Cross[ITestCross](millVersions)
trait ITestCross extends MillIntegrationTestModule with Cross.Module[String] {
  def millVersion               = crossValue
  override def millTestVersion  = millVersion
  override def pluginsUnderTest = Seq(`mill-scalafix`(millVersion))
  override def testInvocations = Seq[(PathRef, Seq[TestInvocation.Targets])](
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
    )
  )
}
