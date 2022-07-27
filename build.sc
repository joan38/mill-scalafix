import $ivy.`com.goyeau::mill-git::0.2.3`
import $ivy.`com.goyeau::mill-scalafix::0.2.8`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest::0.5.0`
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.0`
import com.goyeau.mill.git.{GitVersionModule, GitVersionedPublishModule}
import com.goyeau.mill.scalafix.StyleModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import scalalib._

val millVersions                           = Seq("0.10.5", "0.9.12")
def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(millVersion)

object `mill-scalafix` extends Cross[MillScalafixCross](millVersions: _*)
class MillScalafixCross(millVersion: String)
    extends CrossModuleBase
    with TpolecatModule
    with StyleModule
    with BuildInfo
    with GitVersionedPublishModule {
  override def crossScalaVersion = "2.13.8"
  override def artifactSuffix    = s"_mill${millBinaryVersion(millVersion)}" + super.artifactSuffix()
  override def scalacOptions =
    super.scalacOptions().filterNot(opt => millVersion.startsWith("0.10") && opt == "-Xfatal-warnings")

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-main:$millVersion",
    ivy"com.lihaoyi::mill-scalalib:$millVersion"
  )
  val scalafixVersion = "0.10.1"
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"ch.epfl.scala:scalafix-interfaces:$scalafixVersion",
    ivy"org.scala-lang.modules::scala-collection-compat:2.8.0",
    ivy"org.scala-lang.modules::scala-java8-compat:1.0.2"
  )

  val semanticdbScalac = ivy"org.scalameta:::semanticdb-scalac:4.5.11"

  override def buildInfoPackageName = Some("com.goyeau.mill.scalafix")
  override def buildInfoMembers = Map(
    "scalafixVersion"  -> scalafixVersion,
    "semanticdbScalac" -> semanticdbScalac.dep.version
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

/** Dummy module to trigger Scala Stewards updates of the semanticdb-scalac dependency used in the plugin via BuildInfo
  */
object ScalaStewardDummyModule extends ScalaModule {
  def scalaVersion = `mill-scalafix`(millVersions.head).scalaVersion
  def ivyDeps      = Agg(`mill-scalafix`(millVersions.head).semanticdbScalac)
}

object itest extends Cross[ITestCross](millVersions: _*)
class ITestCross(millVersion: String) extends MillIntegrationTestModule {
  override def millSourcePath   = super.millSourcePath / os.up
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
    ),
    PathRef(sources().head.path / "no-source") -> Seq(
      TestInvocation.Targets(Seq("__.fix"))
    )
  )
}
