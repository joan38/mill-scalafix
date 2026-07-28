package fix

import scala.meta.*
import scalafix.v1.*

/** Syntactic rule that rewrites `???` to `()`, used to prove a locally-built rule is loaded via scalafixToolClasspath.
  */
class LocalRule extends SyntacticRule("LocalRule"):
  override def fix(implicit doc: SyntacticDocument): Patch =
    doc.tree.collect { case t @ Term.Name("???") => Patch.replaceTree(t, "()") }.asPatch
