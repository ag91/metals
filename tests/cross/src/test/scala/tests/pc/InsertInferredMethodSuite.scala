package tests.pc

import java.net.URI

import scala.meta.internal.jdk.CollectionConverters._
import scala.meta.internal.metals.CompilerOffsetParams
import scala.meta.internal.metals.TextEdits

import munit.Location
import munit.TestOptions
import org.eclipse.{lsp4j => l}
import tests.BaseCodeActionSuite

class InsertInferredMethodSuite extends BaseCodeActionSuite {

  checkEdit(
    "simple",
    """|
       |trait Main {
       |  def method1(s : String) = 123
       |
       |  method1(<<otherMethod>>(1))
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def method1(s : String) = 123
       |
       |  def otherMethod(arg0: Int): String = ???
       |  method1(otherMethod(1))
       |}
       |""".stripMargin
  )

  checkEdit(
    "simple-with-expression",
    """|
       |trait Main {
       |  def method1(s : String) = 123
       |
       |  method1(<<otherMethod>>( (1 + 123).toDouble ))
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def method1(s : String) = 123
       |
       |  def otherMethod(arg0: Double): String = ???
       |  method1(otherMethod( (1 + 123).toDouble ))
       |}
       |""".stripMargin
  )

  checkEdit(
    "custom-type",
    """|
       |trait Main {
       |    def method1(b: Double, s : String) = 123
       |
       |    case class User(i : Int)
       |    val user = User(1)
       |
       |    method1(0.0, <<otherMethod>>(user, 1))
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |    def method1(b: Double, s : String) = 123
       |
       |    case class User(i : Int)
       |    val user = User(1)
       |
       |    def otherMethod(arg0: User, arg1: Int): String = ???
       |    method1(0.0, otherMethod(user, 1))
       |}
       |""".stripMargin
  )
  checkEdit(
    "custom-type2",
    """|
       |trait Main {
       |    def method1(b: Double, s : String) = 123
       |
       |    case class User(i : Int)
       |    val user = User(1)
       |    <<otherMethod>>(user, 1)
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |    def method1(b: Double, s : String) = 123
       |
       |    case class User(i : Int)
       |    val user = User(1)
       |    def otherMethod(arg0: User, arg1: Int) = ???
       |    otherMethod(user, 1)
       |}
       |""".stripMargin
  )

  // doesn't work currently, User(1) is not being typed
  checkEdit(
    "custom-type-advanced".ignore,
    """|
       |trait Main {
       |    def method1(b: Double, s : String) = 123
       |
       |    case class User(i : Int)
       |
       |    <<otherMethod>>(User(1), 1)
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |    def method1(b: Double, s : String) = 123
       |
       |    case class User(i : Int)
       |
       |    def otherMethod(arg0: User, arg1: Int): String = ???
       |    method1(0.0, otherMethod(user, 1))
       |}
       |""".stripMargin
  )

  checkEdit(
    "with-imports",
    """|import java.nio.file.Files
       |
       |trait Main {
       |  def main() = {
       |    def method1(s : String) = 123
       |      val path = Files.createTempDirectory("")
       |      method1(<<otherMethod>>(path))
       |    }
       |}
       |
       |""".stripMargin,
    """|import java.nio.file.Files
       |import java.nio.file.Path
       |
       |trait Main {
       |  def main() = {
       |    def method1(s : String) = 123
       |      val path = Files.createTempDirectory("")
       |      def otherMethod(arg0: Path): String = ???
       |      method1(otherMethod(path))
       |    }
       |}
       |""".stripMargin
  )

  checkEdit(
    "lambda",
    """|
       |trait Main {
       |  def main() = {
       |    def method1(s : String => Int) = 123
       |    method1(<<otherMethod>>)
       |  }
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def main() = {
       |    def method1(s : String => Int) = 123
       |    def otherMethod(arg0: String): Int = ???
       |    method1(otherMethod)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "lambda-2",
    """|
       |trait Main {
       |  def main() = {
       |    def method1(s : (String, Float) => Int) = 123
       |    method1(<<otherMethod>>)
       |  }
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def main() = {
       |    def method1(s : (String, Float) => Int) = 123
       |    def otherMethod(arg0: String, arg1: Float): Int = ???
       |    method1(otherMethod)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "lambda-0",
    """|
       |trait Main {
       |  def main() = {
       |    def method1(s : => Int) = 123
       |    method1(<<otherMethod>>)
       |  }
       |}
       |
       |""".stripMargin,
    """|
       |trait Main {
       |  def main() = {
       |    def method1(s : => Int) = 123
       |    def otherMethod: Int = ???
       |    method1(otherMethod)
       |  }
       |}
       |
       |""".stripMargin
  )

  checkEdit(
    "lambda-0-with-fn-arg",
    """|
       |trait Main {
       |  def main() = {
       |    def method1(s : String => Int) = s("123")
       |    method1(<<lol>>)
       |  }
       |}
       |
       |""".stripMargin,
    """|
       |trait Main {
       |  def main() = {
       |    def method1(s : String => Int) = s("123")
       |    def lol(arg0: String): Int = ???
       |    method1(lol)
       |  }
       |}
       |
       |""".stripMargin
  )

  checkEdit(
    "lambda-generic",
    """|
       |trait Main {
       |  def main() = {
       |    val list = List(1, 2, 3)
       |    list.map(<<otherMethod>>)
       |  }
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def main() = {
       |    val list = List(1, 2, 3)
       |    def otherMethod(arg0: Int) = ???
       |    list.map(otherMethod)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "lambda-generic-filter",
    """|
       |trait Main {
       |  def main() = {
       |    val list = List(1, 2, 3)
       |    list.filter(<<otherMethod>>)
       |  }
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def main() = {
       |    val list = List(1, 2, 3)
       |    def otherMethod(arg0: Int) = ???
       |    list.filter(otherMethod)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "lambda-generic-foldLeft".ignore,
    """|
       |trait Main {
       |  def main() = {
       |    val list = List(1, 2, 3)
       |    list.foldLeft(0)(<<otherMethod>>)
       |  }
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def main() = {
       |    val list = List(1, 2, 3)
       |    def otherMethod(arg0: Int, arg1: Int) = ???
       |    list.foldLeft(0)(otherMethod)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "lambda-generic-with-arguments",
    """|
       |trait Main {
       |  def main() = {
       |    val list = List((1, 2))
       |    list.map{case (x,y) => <<otherMethod>>(x,y)}
       |  }
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def main() = {
       |    val list = List((1, 2))
       |    def otherMethod(arg0: Int, arg1: Int) = ???
       |    list.map{case (x,y) => otherMethod(x,y)}
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "lambda-generic-with-mixed-arguments",
    """|
       |trait Main {
       |  def main() = {
       |    val y = "hi"
       |    val list = List(1)
       |    list.map(x => <<otherMethod>>(x,y))
       |  }
       |}
       |
       |""".stripMargin,
    """|trait Main {
       |  def main() = {
       |    val y = "hi"
       |    val list = List(1)
       |    def otherMethod(arg0: Int, arg1: String) = ???
       |    list.map(x => otherMethod(x,y))
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "class-method-with-no-body",
    """|
       |class X()
       |trait Main {
       |  def main() = {
       |    val x = new X()
       |    val a = true
       |    val b = "test"
       |    x.<<otherMethod>>(a, b, 1)
       |  }
       |}
       |
       |""".stripMargin,
    """|class X() {
       |  def otherMethod(arg0: Boolean, arg1: String, arg2: Int) = ???
       |}
       |trait Main {
       |  def main() = {
       |    val x = new X()
       |    val a = true
       |    val b = "test"
       |    x.otherMethod(a, b, 1)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "class-method-with-empty-body",
    """|
       |class X() {}
       |trait Main {
       |  def main() = {
       |    val x = new X()
       |    val a = true
       |    val b = "test"
       |    x.<<otherMethod>>(a, b, 1)
       |  }
       |}
       |
       |""".stripMargin,
    """|class X() {
       |  def otherMethod(arg0: Boolean, arg1: String, arg2: Int) = ???
       |}
       |trait Main {
       |  def main() = {
       |    val x = new X()
       |    val a = true
       |    val b = "test"
       |    x.otherMethod(a, b, 1)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "class-method-with-body",
    """|
       |class X() {
       |  val x = 1
       |}
       |trait Main {
       |  def main() = {
       |    val x = new X()
       |    val a = true
       |    val b = "test"
       |    x.<<otherMethod>>(a, b, 1)
       |  }
       |}
       |
       |""".stripMargin,
    """|class X() {
       |  def otherMethod(arg0: Boolean, arg1: String, arg2: Int) = ???
       |
       |  val x = 1
       |}
       |trait Main {
       |  def main() = {
       |    val x = new X()
       |    val a = true
       |    val b = "test"
       |    x.otherMethod(a, b, 1)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "trait-method-with-body",
    """|
       |trait X {
       |  val x = 1
       |}
       |trait Main {
       |  def main() = {
       |    val x: X = ???
       |    val a = true
       |    val b = "test"
       |    x.<<otherMethod>>(a, b, 1)
       |  }
       |}
       |
       |""".stripMargin,
    """|trait X {
       |  def otherMethod(arg0: Boolean, arg1: String, arg2: Int) = ???
       |
       |  val x = 1
       |}
       |trait Main {
       |  def main() = {
       |    val x: X = ???
       |    val a = true
       |    val b = "test"
       |    x.otherMethod(a, b, 1)
       |  }
       |}
       |""".stripMargin
  )

  checkEdit(
    "object-method-with-body",
    """|
       |object X {
       |  val x = 1
       |}
       |trait Main {
       |  def main() = {
       |    val a = true
       |    val b = "test"
       |    X.<<otherMethod>>(a, b, 1)
       |  }
       |}
       |
       |""".stripMargin,
    """|object X {
       |  def otherMethod(arg0: Boolean, arg1: String, arg2: Int) = ???
       |
       |  val x = 1
       |}
       |trait Main {
       |  def main() = {
       |    val a = true
       |    val b = "test"
       |    X.otherMethod(a, b, 1)
       |  }
       |}
       |""".stripMargin
  )

  def checkEdit(
      name: TestOptions,
      original: String,
      expected: String,
      compat: Map[String, String] = Map.empty
  )(implicit location: Location): Unit =
    test(name) {
      val edits = getInferredMethod(original)
      val (code, _, _) = params(original)
      val obtained = TextEdits.applyEdits(code, edits)
      assertNoDiff(obtained, getExpected(expected, compat, scalaVersion))
    }

  def getInferredMethod(
      original: String,
      filename: String = "file:/A.scala"
  ): List[l.TextEdit] = {
    val (code, _, offset) = params(original)
    val result = presentationCompiler
      .insertInferredMethod(
        CompilerOffsetParams(URI.create(filename), code, offset, cancelToken)
      )
      .get()
    result.asScala.toList
  }

}