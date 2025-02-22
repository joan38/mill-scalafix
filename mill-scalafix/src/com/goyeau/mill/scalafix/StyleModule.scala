package com.goyeau.mill.scalafix

import mill.Command
import mill.Task
import mill.scalalib.scalafmt.ScalafmtModule

/** Combine Scalafmt and Scalafix together
  */
trait StyleModule extends ScalafmtModule with ScalafixModule {
  def style(): Command[Unit] =
    Task.Command {
      reformat()()
      fix()()
    }

  def checkStyle(): Command[Unit] =
    Task.Command {
      checkFormat()()
      fix("--check")()
    }
}
