package com.goyeau.mill.scalafix

import coursier.Repository
import mill.{Agg, T}
import mill.api.{Logger, Loose, Result}
import mill.scalalib._
import mill.define.{Command, Target}
import os._
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixError._
import scala.compat.java8.OptionConverters._
import scala.jdk.CollectionConverters._

trait ScalafixModule extends ScalaModule {
  override def scalacPluginIvyDeps: Target[Loose.Agg[Dep]] =
    super.scalacPluginIvyDeps() ++ Agg(ivy"org.scalameta:::semanticdb-scalac:4.4.0")

  def scalafixConfig: T[Option[Path]]       = T(None)
  def scalafixIvyDeps: T[Agg[Dep]]          = Agg.empty[Dep]
  def scalafixScalaBinaryVersion: T[String] = "2.12"

  /** Run Scalafix.
    */
  def fix(args: String*): Command[Unit] =
    T.command {
      ScalafixModule.fixAction(
        T.ctx().log,
        repositoriesTask(),
        allSourceFiles().map(_.path),
        localClasspath().map(_.path),
        scalaVersion(),
        scalafixScalaBinaryVersion(),
        scalacOptions(),
        scalafixIvyDeps(),
        scalafixConfig(),
        args: _*
      )
    }
}

object ScalafixModule {
  def fixAction(
      log: Logger,
      repositories: Seq[Repository],
      sources: Seq[Path],
      classpath: Seq[Path],
      scalaVersion: String,
      scalaBinaryVersion: String,
      scalacOptions: Seq[String],
      scalafixIvyDeps: Agg[Dep],
      scalafixConfig: Option[Path],
      args: String*
  ): Result[Unit] =
    if (sources.nonEmpty) {
      val scalafix = Scalafix
        .fetchAndClassloadInstance(scalaBinaryVersion, repositories.map(CoursierUtils.toApiRepository).asJava)
        .newArguments()
        .withParsedArguments(args.asJava)
        .withWorkingDirectory(pwd.toNIO)
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
}
