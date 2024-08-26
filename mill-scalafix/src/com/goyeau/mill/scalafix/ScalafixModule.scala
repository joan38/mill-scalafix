package com.goyeau.mill.scalafix

import com.goyeau.mill.scalafix.ScalafixModule.{filesToFix, fixAction}
import coursier.Repository
import mill.{Agg, T}
import mill.api.{Logger, PathRef, Result}
import mill.scalalib.{Dep, ScalaModule}
import mill.define.Command

import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixError.*
import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*

trait ScalafixModule extends ScalaModule {
  def scalafixConfig: T[Option[os.Path]]       = T(None)
  def scalafixIvyDeps: T[Agg[Dep]]          = Agg.empty[Dep]
  def scalafixScalaBinaryVersion: T[String] = "2.12"

  /** Run Scalafix.
    */
  def fix(args: String*): Command[Unit] =
    T.command {
      fixAction(
        T.ctx().log,
        repositoriesTask(),
        filesToFix(sources()).map(_.path),
        classpath = (compileClasspath() ++ localClasspath() ++ Seq(semanticDbData())).iterator.toSeq.map(_.path),
        scalaVersion(),
        scalafixScalaBinaryVersion(),
        scalacOptions(),
        scalafixIvyDeps(),
        scalafixConfig(),
        args,
        T.workspace
      )
    }
}

object ScalafixModule {
  def fixAction(
      log: Logger,
      repositories: Seq[Repository],
      sources: Seq[os.Path],
      classpath: Seq[os.Path],
      scalaVersion: String,
      scalaBinaryVersion: String,
      scalacOptions: Seq[String],
      scalafixIvyDeps: Agg[Dep],
      scalafixConfig: Option[os.Path],
      args: String*
  ): Result[Unit] = fixAction(log, repositories, sources, classpath, scalaVersion, scalaBinaryVersion, scalacOptions, scalafixIvyDeps, scalafixConfig, args, os.pwd)
  
  def fixAction(
      log: Logger,
      repositories: Seq[Repository],
      sources: Seq[os.Path],
      classpath: Seq[os.Path],
      scalaVersion: String,
      scalaBinaryVersion: String,
      scalacOptions: Seq[String],
      scalafixIvyDeps: Agg[Dep],
      scalafixConfig: Option[os.Path],
      args: Seq[String],
      wd: os.Path
  ): Result[Unit] =
    if (sources.nonEmpty) {
      val scalafix = Scalafix
        .fetchAndClassloadInstance(scalaBinaryVersion, repositories.map(CoursierUtils.toApiRepository).asJava)
        .newArguments()
        .withParsedArguments(args.asJava)
        .withWorkingDirectory(wd.toNIO)
        .withConfig(scalafixConfig.map(_.toNIO).asJava)
        .withClasspath(classpath.map(_.toNIO).asJava)
        .withScalaVersion(scalaVersion)
        .withScalacOptions(scalacOptions.asJava)
        .withPaths(sources.map(_.toNIO).asJava)
        .withToolClasspath(
          Seq.empty.asJava,
          scalafixIvyDeps.map(CoursierUtils.toCoordinates).iterator.toSeq.asJava,
          repositories.map(CoursierUtils.toApiRepository).asJava
        )

      log.info(s"Rewriting and linting ${sources.size} Scala sources against ${scalafix.rulesThatWillRun.size} rules")
      val errors = scalafix.run()
      if (errors.isEmpty) Result.Success(())
      else {
        val errorMessages = errors.map {
          case ParseError => "A source file failed to be parsed"
          case CommandLineError =>
            scalafix.validate().asScala.fold("A command-line argument was parsed incorrectly")(_.getMessage)
          case MissingSemanticdbError =>
            "A semantic rewrite was run on a source file that has no associated META-INF/semanticdb/.../*.semanticdb"
          case StaleSemanticdbError =>
            """The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin.
              |To resolve this error re-compile the project and re-run Scalafix""".stripMargin
          case TestError =>
            "A Scalafix test error was reported. Run `fix` without `--check` or `--diff` to fix the error"
          case LinterError  => "A Scalafix linter error was reported"
          case NoFilesError => "No files were provided to Scalafix so nothing happened"
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
}
