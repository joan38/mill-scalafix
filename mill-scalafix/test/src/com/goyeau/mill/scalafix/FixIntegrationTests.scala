package com.goyeau.mill.scalafix

import munit.FunSuite

class FixIntegrationTests extends FunSuite {
  test("fix should fix the code") {
    val tester = Tester.create(os.rel / "fix")
    val result = tester.eval(Seq("project.fix"))
    assert(result.isSuccess, result.err)

    val fixedScala = os.read(tester.workspacePath / "project" / "src" / "Fix.scala")
    val expected = """object Fix {
                     |  def procedure(): Unit = {}
                     |  def myComplexMethod: Map[Int,String] = 1.to(10).map(i => i -> i.toString).toMap
                     |}
                     |""".stripMargin
    assertEquals(fixedScala, expected)
  }

  test("fix should fix procedure syntax in Scala 2.12") {
    val tester = Tester.create(os.rel / "fix-2.12")
    val result = tester.eval(Seq("__.fix"))
    assert(result.isSuccess, result.err)

    val fixedScala = os.read(tester.workspacePath / "project" / "src" / "Fix.scala")
    val expected = """
                     |
                     |object Fix {
                     |  def procedure(): Unit = {}
                     |}
                     |""".stripMargin
    assertEquals(fixedScala, expected)
  }

  test("fix should fix procedure syntax in Scala 3.5") {
    val tester = Tester.create(os.rel / "fix-3.5")
    val result = tester.eval(Seq("__.fix"))
    assert(result.isSuccess, result.err)

    val fixedScala = os.read(tester.workspacePath / "project" / "src" / "Fix.scala")
    val expected =
      """object Fix {
        |  // use a 3.5.x-only feature to fail if a Scala 3 LTS compiler is used
        |  // https://www.scala-lang.org/blog/2024/08/22/scala-3.5.0-released.html#support-for-binary-integer-literals
        |  def myComplexMethod: Map[Int, String] = 1.to(0B1010).map(i => i -> i.toString).toMap
        |}
        |""".stripMargin
    assertEquals(fixedScala, expected)
  }

  test("fix --check should pass if nothing needs to be fixed") {
    val tester = Tester.create(os.rel / "check")
    val result = tester.eval(Seq("__.fix", "--check"))
    assert(result.isSuccess, result.err)
  }

  test("fix --check should fail if there are things to be fixed") {
    val tester = Tester.create(os.rel / "fix")
    val result = tester.eval(Seq("__.fix", "--check"))
    assert(!result.isSuccess, "Check should have failed")
  }

  test("fix should run custom rules") {
    val tester = Tester.create(os.rel / "custom-rule")
    val result = tester.eval(Seq("__.fix"))
    assert(result.isSuccess, result.err)

    val fixedScala = os.read(tester.workspacePath / "project" / "src" / "Fix.scala")
    val expected = """import scala.language.postfixOps
                     |object Tuple2ZippedSrc213 {
                     |  def zipped(xs: List[Int], ys: List[Int]): Unit = {
                     |    xs.lazyZip(ys)
                     |    xs.lazyZip(ys)
                     |    (xs.lazyZip(ys) )
                     |    (xs.lazyZip(ys))
                     |    xs.lazyZip(ys)
                     |    /* a */
                     |     /* b */ xs /* c */.lazyZip(/* d */ ys /* e */ ) /* f */  /* g */  /* h */
                     |    coll(1).lazyZip(coll(2))
                     |    List(1, 2, 3).lazyZip(Array(1))
                     |  }
                     |  def coll(x: Int): List[Int] = ???
                     |}
                     |""".stripMargin
    assertEquals(fixedScala, expected)
  }

  test("fix on empty project should work") {
    val tester = Tester.create(os.rel / "empty")
    val result = tester.eval(Seq("__.fix"))
    assert(result.isSuccess, result.err)
  }
}
