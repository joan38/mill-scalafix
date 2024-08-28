import scala.language.postfixOps
object Tuple2ZippedSrc213 {
  def zipped(xs: List[Int], ys: List[Int]): Unit = {
    (xs, ys).zipped
    (xs, ys).zipped
    (xs, ys).zipped((xs, ys).zipped)
    (xs, ys).zipped
    /* a */
    ( /* b */ xs /* c */, /* d */ ys /* e */ ) /* f */ . /* g */ zipped /* h */
    (coll(1), coll(2)).zipped
    (List(1, 2, 3), Array(1)).zipped
  }
  def coll(x: Int): List[Int] = ???
}
