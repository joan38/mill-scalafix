package com.goyeau.mill.scalafix

import mill.T
import mill.define.Command
import mill.scalalib.scalafmt.ScalafmtModule

/** Combine Scalafmt and Scalafix together
  */
trait StyleModule extends ScalafmtModule with ScalafixModule {
  def style(): Command[Unit] =
    T.command {
      reformat()()
      fix()()
    }

  def checkStyle(): Command[Unit] =
    T.command {
      checkFormat()()
      fix("--check")()
    }
}
