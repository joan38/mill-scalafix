package com.goyeau.mill.scalafix

import mill.T
import mill.define.Command
import mill.scalalib.scalafmt.ScalafmtModule
import scala.annotation.nowarn

/**
  * Combine Scalafmt and Scalafix together
  */
trait StyleModule extends ScalafmtModule with ScalafixModule {
  @nowarn("msg=pure expression does nothing")
  def style(): Command[Unit] =
    T.command {
      reformat()()
      fix()()
    }

  @nowarn("msg=pure expression does nothing")
  def checkStyle(): Command[Unit] =
    T.command {
      checkFormat()()
      fix("--check")()
    }
}
