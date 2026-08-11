object Fix:
  def myComplexMethod: Map[Int, String] =
    1.to(10).map(i => i -> i.toString).toMap
