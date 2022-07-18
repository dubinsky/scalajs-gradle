package tutorial.webapp

import org.scalatest.funsuite.AnyFunSuite
import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.ext._

class TutorialTest extends AnyFunSuite {

  // Initialize App
  TutorialApp.setupUI()

  test("HelloWorld") {
    assert(document.querySelectorAll("p").count(_.textContent == "Hello World") == 1)
  }

  test("HelloWorld failure") {
    assert(document.querySelectorAll("p").count(_.textContent == "Hello World!!!") == 1)
  }

  test("ButtonClick") {
    def messageCount =
      document.querySelectorAll("p").count(_.textContent == "You clicked the button!")

    val button = document.querySelector("button").asInstanceOf[dom.html.Button]
    assert(button != null && button.textContent == "Click me!")
    assert(messageCount == 0)

    for (c <- 1 to 5) {
      button.click()
      assert(messageCount == c)
    }
  }
}
