# scala-meta unsafe lazy val implementation

This is an experiment with scala-meta to implement an "unsafe" version of the scala `lazy val`. This version works similar to the scala lazy val but with no synchronization. The idea is that this could be used to provide lazy initialization in situations where you know only one thread at a time will be accessing members of the class, such as inside an actor.

This implementation uses a single `Long` as a bitmap to tell which lazy vals have been initialized, so it supports a maximum of 64 lazy vals.

## Example

Given the following code:

```scala
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
```

the generated scala code should be something like:

```scala
class Test {
  private[this] final var _lazy_bitmap_1: Long = 0L
  private[this] final var _lazy_4: String = _
  def foo: String = {
    val _shifted_bitmap_5 = 1L << 0
    if ((_lazy_bitmap_1 & _shifted_bitmap_5) == 0) {
      _lazy_4 = {
        println("initializing foo")
        "foo!"
      }
      _lazy_bitmap_1 |= _shifted_bitmap_5
    }
    _lazy_4
  }
  private[this] final var _lazy_6: String = _
  def bar: String = {
    val _shifted_bitmap_7 = 1L << 1
    if ((_lazy_bitmap_1 & _shifted_bitmap_7) == 0) {
      _lazy_6 = {
        println("initializing bar")
        "bar!"
      }
      _lazy_bitmap_1 |= _shifted_bitmap_7
    }
    _lazy_6
  }
}
```
