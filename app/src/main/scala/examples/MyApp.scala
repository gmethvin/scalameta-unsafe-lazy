package examples

import com.gmethvin.macros.unsafelazy._

object MyApp extends App {

  val unsafe = new Unsafe
  unsafe.foo
  unsafe.foo

  val extendsUnsafe = new ExtendsUnsafe
  extendsUnsafe.foo
  extendsUnsafe.foo

}

trait Foo
trait Bar

@UnsafeLazy
class Test {
  @lazyInit val foo: String = {
    println("initializing foo")
    "foo!"
  }
  @lazyInit val bar: String = {
    println("initializing bar")
    "bar!"
  }
}

@UnsafeLazy
class Unsafe extends Foo with Bar {
  @lazyInit private val f1: Int = 1
  @lazyInit private val f2: Int = 2
  @lazyInit protected val f3: Int = 3
  @lazyInit protected val f4: Int = 4
  @lazyInit final val f5: Int = 5
  @lazyInit final val f6: Int = 6
  @lazyInit val f7: Int = 7
  @lazyInit val f8: Int = 8

  @lazyInit val foo: String = {
    println("initializing foo")
    "hello"
  }

  @lazyInit val `foo bar`: String = "foobar"

  println(s"loaded $getClass")
}

@UnsafeLazy
class ExtendsUnsafe extends Unsafe {
  @lazyInit override val foo: String = {
    println("initializing new foo")
    super.foo
  }
}
