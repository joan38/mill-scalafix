object Fix {
  // use a 3.5.x-only feature to fail if a Scala 3 LTS compiler is used
  // https://www.scala-lang.org/blog/2024/08/22/scala-3.5.0-released.html#support-for-binary-integer-literals
  def myComplexMethod = 1.to(0B1010).map(i => i -> i.toString).toMap
}
