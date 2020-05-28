package com.goyeau.mill.scalafix

import java.net.URLClassLoader
import mill.{Agg, T}
import mill.api.{Logger, Loose, Result}
import mill.scalalib._
import mill.define.{Command, Target}
import os._
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixError._
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import scala.collection.compat._

trait ScalafixModule extends ScalaModule {
  override def scalacPluginIvyDeps: Target[Loose.Agg[Dep]] =
    super.scalacPluginIvyDeps() ++ Agg(ivy"org.scalameta:::semanticdb-scalac:4.3.10")

  def scalafixConfig: T[Option[Path]]    = T(None)
  def scalafixIvyDeps: T[Loose.Agg[Dep]] = Agg.empty[Dep]

  implicit class ResultOps[+A](result: Result[A]) {
    def flatMap[B](f: A => Result[B]): Result[B] =
      result match {
        case Result.Success(value) => f(value)
        case result                => result.asInstanceOf[Result[B]] // scalafix:ok
      }
  }

  /**
    * Run Scalafix.
    */
  def fix(args: String*): Command[Unit] =
    T.command {
      for {
        toolClassPath <- Lib.resolveDependencies(
          repositories,
          resolveCoursierDependency().apply(_),
          Agg(ivy"ch.epfl.scala:scalafix-cli_2.12.11:${BuildInfo.scalafixVersion}") ++
            scalafixIvyDeps()
              .map(ivy => ivy.copy(cross = CrossVersion.Constant("_2.12", platformed = ivy.cross.platformed)))
        )
        result <- ScalafixModule.fixAction(
          T.ctx.log,
          allSourceFiles().map(_.path),
          localClasspath().map(_.path),
          scalaVersion(),
          scalacOptions(),
          toolClassPath.map(_.path),
          scalafixConfig(),
          args: _*
        )
      } yield result
    }
}

object ScalafixModule {
  def fixAction(
      log: Logger,
      sources: Seq[Path],
      classpath: Seq[Path],
      scalaVersion: String,
      scalacOptions: Seq[String],
      toolClassPath: Agg[Path],
      scalafixConfig: Option[Path],
      args: String*
  ): Result[Unit] =
    if (sources.nonEmpty) {
      val toolClassloader = new URLClassLoader(
        toolClassPath.map(_.toNIO.toUri.toURL).iterator.toArray,
        new ScalafixInterfacesClassloader(getClass.getClassLoader)
      )
      val scalafix = Scalafix
        .classloadInstance(toolClassloader)
        .newArguments()
        .withParsedArguments(args.asJava)
        .withWorkingDirectory(pwd.toNIO)
        .withConfig(scalafixConfig.map(_.toNIO).asJava)
        .withClasspath(classpath.map(_.toNIO).asJava)
        .withScalaVersion(scalaVersion)
        .withScalacOptions(scalacOptions.asJava)
        .withPaths(sources.map(_.toNIO).asJava)
        .withToolClasspath(toolClassloader)

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
