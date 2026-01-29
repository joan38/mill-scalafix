package com.goyeau.mill.scalafix

import munit.FunSuite
import scala.concurrent.duration.*

class StyleIntegrationTests extends FunSuite:
  override val munitTimeout: Duration = 1.minute

  test("style should fix and format the code") {
    val tester = Tester.create(os.rel / "style")
    val result = tester.eval(Seq("project.style"))
    assert(result.isSuccess, result.err)

    val fixedScala = os.read(tester.workspacePath / "project" / "src" / "Fix.scala")
    val expected   = """
                       |
                       |object Fix {
                       |  def myComplexMethod: Map[Int, String] = 1.to(10).map(i => i -> i.toString).toMap
                       |}
                       |""".stripMargin
    assertEquals(fixedScala, expected)
  }

  test("checkStyle should pass if nothing needs to be fixed or formatted") {
    val tester = Tester.create(os.rel / "style-check")
    val result = tester.eval(Seq("project.checkStyle"))
    assert(result.isSuccess, result.err)
  }

  test("checkStyle should fail if there are things to be fixed or formatted") {
    val tester = Tester.create(os.rel / "style")
    val result = tester.eval(Seq("project.checkStyle"))
    assert(!result.isSuccess, "Check should have failed")
  }
