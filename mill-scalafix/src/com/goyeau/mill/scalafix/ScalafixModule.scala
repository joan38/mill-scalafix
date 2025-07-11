package com.goyeau.mill.scalafix

import com.goyeau.mill.scalafix.ScalafixModule.filesToFix
import com.goyeau.mill.scalafix.ScalafixModule.fixAction
import coursier.Repository
import mill.Command
import mill.PathRef
import mill.T
import mill.Task
import mill.api.BuildCtx
import mill.api.Logger
import mill.api.Result
import mill.scalalib.Dep
import mill.scalalib.ScalaModule
import scalafix.interfaces.ScalafixError.*

import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

trait ScalafixModule extends ScalaModule {
  def scalafixConfig: T[Option[os.Path]] = Task(None)
  def scalafixIvyDeps: T[Seq[Dep]]       = Seq.empty[Dep]

  /** Override this to filter out repositories that don't need to be passed to scalafix
    *
    * Repositories passed to scalafix need to be converted to the coursier-interface API (coursierapi.*). This can be an
    * issue for non-Maven or Ivy repositories. Overriding this task and filtering those repositories out allows to work
    * around that.
    */
  def scalafixRepositories: Task[Seq[Repository]] = Task.Anon {
    repositoriesTask().filter {
      case repo if repo.getClass.getName == "mill.scalalib.JavaModule$InternalRepo" =>
        // Change to this when bumping to Mill 0.13.x:
        // case _: mill.scalalib.JavaModule.InternalRepo =>
        // no need to pass Mill's internal repository to scalafix
        false
      case _ => true
    }
  }

  @deprecated("Scalafix now follows scalaVersion", since = "0.4.2")
  def scalafixScalaBinaryVersion: T[String] = "2.12"

  /** Run Scalafix.
    */
  def fix(args: String*): Command[Unit] =
    Task.Command {
      fixAction(
        Task.ctx().log,
        scalafixRepositories(),
        filesToFix(sources()).map(_.path),
        classpath = (compileClasspath() ++ localClasspath() ++ Seq(semanticDbData())).iterator.toSeq.map(_.path),
        scalaVersion(),
        scalacOptions(),
        scalafixIvyDeps(),
        scalafixConfig(),
        args,
        BuildCtx.workspaceRoot
      )
    }
}

object ScalafixModule {
  @deprecated("Use overload without scalaBinaryVersion and with wd instead", since = "0.4.2")
  def fixAction(
      log: Logger,
      repositories: Seq[Repository],
      sources: Seq[os.Path],
      classpath: Seq[os.Path],
      scalaVersion: String,
      scalaBinaryVersion: String,
      scalacOptions: Seq[String],
      scalafixIvyDeps: Seq[Dep],
      scalafixConfig: Option[os.Path],
      args: String*
  ): Result[Unit] = fixAction(
    log,
    repositories,
    sources,
    classpath,
    scalaVersion,
    scalacOptions,
    scalafixIvyDeps,
    scalafixConfig,
    args,
    os.pwd
  )

  def fixAction(
      log: Logger,
      repositories: Seq[Repository],
      sources: Seq[os.Path],
      classpath: Seq[os.Path],
      scalaVersion: String,
      scalacOptions: Seq[String],
      scalafixIvyDeps: Seq[Dep],
      scalafixConfig: Option[os.Path],
      args: Seq[String],
      wd: os.Path
  ): Result[Unit] =
    if (sources.nonEmpty) {
      val scalafix = ScalafixCache
        .getOrElseCreate(scalaVersion, repositories, scalafixIvyDeps)
        .withParsedArguments(args.asJava)
        .withWorkingDirectory(wd.toNIO)
        .withConfig(scalafixConfig.map(_.toNIO).toJava)
        .withClasspath(classpath.map(_.toNIO).asJava)
        .withScalaVersion(scalaVersion)
        .withScalacOptions(scalacOptions.asJava)
        .withPaths(sources.map(_.toNIO).asJava)

      log.info(s"Rewriting and linting ${sources.size} Scala sources against ${scalafix.rulesThatWillRun.size} rules")
      val errors = scalafix.run()
      if (errors.isEmpty) Result.Success(())
      else {
        val errorMessages = errors.map {
          case ParseError => "A source file failed to be parsed"
          case CommandLineError =>
            scalafix.validate().toScala.fold("A command-line argument was parsed incorrectly")(_.getMessage)
          case MissingSemanticdbError =>
            "A semantic rewrite was run on a source file that has no associated META-INF/semanticdb/.../*.semanticdb"
          case StaleSemanticdbError =>
            """The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin.
              |To resolve this error re-compile the project and re-run Scalafix""".stripMargin
          case TestError =>
            "A Scalafix test error was reported. Run `fix` without `--check` or `--diff` to fix the error"
          case LinterError  => "A Scalafix linter error was reported"
          case NoFilesError => "No files were provided to Scalafix so nothing happened"
          case NoRulesError => "No Scalafix rules were found. Make sure a `rules` set is defined in .scalafix.conf"
          case _            => "Something unexpected happened running Scalafix"
        }
        Result.Failure(errorMessages.mkString("\n"))
      }
    } else Result.Success(())

  def filesToFix(sources: Seq[PathRef]): Seq[PathRef] =
    for {
      pathRef <- sources if os.exists(pathRef.path)
      file <-
        if (os.isDir(pathRef.path)) os.walk(pathRef.path).filter(file => os.isFile(file) && (file.ext == "scala"))
        else Seq(pathRef.path)
    } yield PathRef(file)

  @deprecated("Use overload without scalaBinaryVersion instead", since = "0.4.2")
  def fixAction(
      log: Logger,
      repositories: Seq[Repository],
      sources: Seq[os.Path],
      classpath: Seq[os.Path],
      scalaVersion: String,
      scalaBinaryVersion: String,
      scalacOptions: Seq[String],
      scalafixIvyDeps: Seq[Dep],
      scalafixConfig: Option[os.Path],
      args: Seq[String],
      wd: os.Path
  ): Result[Unit] = fixAction(
    log,
    repositories,
    sources,
    classpath,
    scalaVersion,
    scalacOptions,
    scalafixIvyDeps,
    scalafixConfig,
    args,
    wd
  )
}
