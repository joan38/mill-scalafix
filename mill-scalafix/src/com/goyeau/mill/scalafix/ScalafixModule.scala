package com.goyeau.mill.scalafix

import java.net.URLClassLoader
import mill._
import mill.api.{Loose, Result}
import mill.scalalib._
import mill.define.{Command, Target}
import os._
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixError._
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

trait ScalafixModule extends ScalaModule {
  override def scalacPluginIvyDeps: Target[Loose.Agg[Dep]] =
    super.scalacPluginIvyDeps() ++ Agg(ivy"org.scalameta:::semanticdb-scalac:4.3.10")

  def scalafixConfig: T[Option[Path]]    = T(None)
  def scalafixIvyDeps: T[Loose.Agg[Dep]] = Agg.empty[Dep]

  /**
    * Run Scalafix.
    */
  def fix(args: String*): Command[Unit] =
    T.command(
      ScalafixModule.fixAction(
        allSourceFiles(),
        localClasspath(),
        scalaVersion(),
        scalacOptions(),
        resolveDeps(scalafixIvyDeps)(),
        scalafixConfig(),
        args: _*
      )
    )
}

object ScalafixModule {
  def fixAction(
      sources: Seq[PathRef],
      classpath: Seq[PathRef],
      scalaVersion: String,
      scalacOptions: Seq[String],
      toolClassPath: Agg[PathRef],
      scalafixConfig: Option[Path],
      args: String*
  ): Result[Unit] =
    if (sources.nonEmpty) {
      val scalafix = Scalafix
        .classloadInstance(getClass.getClassLoader)
        .newArguments()
        .withParsedArguments(args.asJava)
        .withWorkingDirectory(pwd.toNIO)
        .withConfig(scalafixConfig.map(_.toNIO).asJava)
        .withClasspath(classpath.map(_.path.toNIO).asJava)
        .withScalaVersion(scalaVersion)
        .withScalacOptions(scalacOptions.asJava)
        .withPaths(sources.map(_.path.toNIO).asJava)
        .withToolClasspath(
          new URLClassLoader(toolClassPath.map(_.path.toNIO.toUri.toURL).toArray, getClass.getClassLoader)
        )

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
