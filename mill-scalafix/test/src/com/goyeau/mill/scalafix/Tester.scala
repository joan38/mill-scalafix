package com.goyeau.mill.scalafix

import mill.testkit.IntegrationTester
import os.RelPath

object Tester {
  def create(project: RelPath) = {
    val resourceFolder = os.Path(sys.env("MILL_TEST_RESOURCE_DIR"))
    new IntegrationTester(
      clientServerMode = true,
      workspaceSourcePath = resourceFolder / project,
      millExecutable = os.Path(sys.env("MILL_EXECUTABLE_PATH"))
    )
  }
}
