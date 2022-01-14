import $ivy.`com.goyeau::mill-git:0.2.2`
import $ivy.`com.goyeau::mill-scalafix:0.2.5`
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:$MILL_VERSION`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest_mill0.9:0.4.1-30-f29f55`
import $ivy.`io.github.davidgregory084::mill-tpolecat:0.2.0`
import com.goyeau.mill.git.{GitVersionModule, GitVersionedPublishModule}
import com.goyeau.mill.scalafix.StyleModule
import de.tobiasroeser.mill.integrationtest._
import io.github.davidgregory084.TpolecatModule
import mill._
import mill.contrib.buildinfo.BuildInfo
import mill.scalalib.api.Util.scalaNativeBinaryVersion
import mill.scalalib.publish.{Developer, License, PomSettings, VersionControl}
import scalalib._

val millVersions       = Seq("0.9.12", "0.10.0-M5")
val millBinaryVersions = millVersions.map(scalaNativeBinaryVersion)

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(millVersion)

def millVersion(binaryVersion: String) =
  millVersions.find(v => millBinaryVersion(v) == binaryVersion).get

object `mill-scalafix` extends Cross[MillScalafixCross](millBinaryVersions: _*)
class MillScalafixCross(millBinaryVersion: String)
    extends ScalaModule
    with TpolecatModule
    with StyleModule
    with BuildInfo
    with GitVersionedPublishModule {
  override def millSourcePath = super.millSourcePath / os.up
  override def artifactName   = s"mill-scalafix_mill$millBinaryVersion"
  override def scalacOptions =
    super.scalacOptions().filterNot(opt => millBinaryVersion.startsWith("0.10") && opt == "-Xfatal-warnings")
  override def scalaVersion = "2.13.6"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-main:${millVersion(millBinaryVersion)}",
    ivy"com.lihaoyi::mill-scalalib:${millVersion(millBinaryVersion)}"
  )
  val scalafixVersion = "0.9.34"
  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"ch.epfl.scala:scalafix-interfaces:$scalafixVersion",
    ivy"org.scala-lang.modules::scala-collection-compat:2.6.0",
    ivy"org.scala-lang.modules::scala-java8-compat:1.0.2"
  )

  val semanticdbScalac = ivy"org.scalameta:::semanticdb-scalac:4.4.32"

  override def buildInfoPackageName = Some("com.goyeau.mill.scalafix")
  override def buildInfoMembers = Map(
    "scalafixVersion"  -> scalafixVersion,
    "semanticdbScalac" -> semanticdbScalac.dep.version
  )

  override def publishVersion = GitVersionModule.version(withSnapshotSuffix = true)()
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

/** Dummy module to trigger Scala Stewards updates of the semanticdb-scalac dependency used in the plugin via BuildInfo
  */
object ScalaStewardDummyModule extends ScalaModule {
  def scalaVersion = `mill-scalafix`(millBinaryVersions.head).scalaVersion
  def ivyDeps      = Agg(`mill-scalafix`(millBinaryVersions.head).semanticdbScalac)
}

object itest extends Cross[ITestCross]("0.9.10", "0.10.0-M5")
class ITestCross(millVersion: String) extends MillIntegrationTestModule {
  override def millSourcePath   = super.millSourcePath / os.up
  override def millTestVersion  = millVersion
  override def pluginsUnderTest = Seq(`mill-scalafix`(millBinaryVersion(millVersion)))
  override def testInvocations =
    Seq[(PathRef, Seq[TestInvocation.Targets])](
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
