# mill-scalafix

[![Maven Central](https://img.shields.io/maven-central/v/com.goyeau/mill-scalafix_mill0.11_2.13)](https://mvnrepository.com/artifact/com.goyeau/mill-scalafix_mill0.11_2.13)

A [scalafix](https://scalacenter.github.io/scalafix) plugin for Mill build tool.


## Usage

### Fix sources

*build.sc*:
```scala
import $ivy.`com.goyeau::mill-scalafix::<latest version>`
import com.goyeau.mill.scalafix.ScalafixModule
import mill.scalalib._

object project extends ScalaModule with ScalafixModule {
  def scalaVersion = "2.13.8"
}
```

```shell script
> mill project.fix
[29/29] project.fix
/project/project/src/MyClass.scala:12:11: error: [DisableSyntax.var] mutable state should be avoided
  private var hashLength = 7
          ^^^
1 targets failed
mill-git.fix A Scalafix linter error was reported
```

### Using External Rules

You're also able to use external Scalafix rules by adding them like the below
example:

```scala
def scalafixIvyDeps = Agg(ivy"com.github.xuwei-k::scalafix-rules:0.3.0")
```

### Scalafix Arguments

mill-scalafix takes any argument that can be passed to the [Scalafix the command line tool](https://scalacenter.github.io/scalafix/docs/users/installation.html#command-line).
You could for example check that all files have been fixed with scalafix. We usually use that to enforce rules in CI:
```shell script
> mill project.fix --check
[30/30] project.fix
--- /project/project/src/Fix.scala
+++ <expected fix>
@@ -1,3 +1,3 @@
 object Fix {
-  def procedure() {}
+  def procedure(): Unit = {}
 }
1 targets failed
project.fix A Scalafix test error was reported. Run `fix` without `--check` or `--diff` to fix the error
```


## Related projects

* [scalafix](https://github.com/scalacenter/scalafix)
* Inspired by [sbt-scalafix](https://github.com/scalacenter/sbt-scalafix)


## Contributing

Contributions are more than welcome!
See [CONTRIBUTING.md](CONTRIBUTING.md) for all the information and getting help.
