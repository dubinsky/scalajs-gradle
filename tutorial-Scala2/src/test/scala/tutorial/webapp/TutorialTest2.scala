package tutorial.webapp

import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.ext._
import org.scalatest.funsuite.AnyFunSuite
import scala.scalajs.js

class TutorialTest2 extends AnyFunSuite {

  test("2*2 success") {
    assert(2*2 == 4)
  }

  test("2*2 failure") {
    assert(2*2 == 5)
  }

  test("2*2 output") {
    println("2*2=4")
  }
}
