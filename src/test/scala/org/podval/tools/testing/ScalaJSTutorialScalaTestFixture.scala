package org.podval.tools.testing

import ForClass.*

object ScalaJSTutorialScalaTestFixture extends Fixture(
  framework = org.podval.tools.testing.framework.ScalaTest,
  mainSources = Seq(
    SourceFile(name = "TutorialApp", content =
      s"""import org.scalajs.dom
         |import org.scalajs.dom.document
         |
         |object TutorialApp {
         |  def appendPar(targetNode: dom.Node, text: String): Unit = {
         |    val parNode = document.createElement("p")
         |    parNode.textContent = text
         |    targetNode.appendChild(parNode)
         |  }
         |
         |  def addClickedMessage(): Unit =
         |    appendPar(document.body, "You clicked the button!")
         |
         |  def setupUI(): Unit = {
         |    val button = document.createElement("button")
         |    button.textContent = "Click me!"
         |    button.addEventListener("click", (e: dom.MouseEvent) => addClickedMessage())
         |    document.body.appendChild(button)
         |
         |    appendPar(document.body, "Hello World")
         |  }
         |
         |  def main(args: Array[String]): Unit = {
         |    println("Running!!!")
         |    document.addEventListener("DOMContentLoaded", (e: dom.Event) => setupUI())
         |  }
         |}
         |
         |""".stripMargin
    )
  ),
  testSources = Seq(
    SourceFile(name = "TutorialTest", content =
      s"""import org.scalatest.funsuite.AnyFunSuite
         |import scala.scalajs.js
         |import org.scalajs.dom
         |import org.scalajs.dom.document
         |import org.scalajs.dom.ext._
         |
         |class TutorialTest extends AnyFunSuite {
         |  // Initialize App
         |  TutorialApp.setupUI()
         |
         |  test("HelloWorld") { assert(document.querySelectorAll("p").count(_.textContent == "Hello World") == 1) }
         |  test("HelloWorld failure") { assert(document.querySelectorAll("p").count(_.textContent == "Hello World!!!") == 1) }
         |
         |  test("ButtonClick") {
         |    def messageCount =
         |      document.querySelectorAll("p").count(_.textContent == "You clicked the button!")
         |
         |    val button = document.querySelector("button").asInstanceOf[dom.html.Button]
         |    assert(button != null && button.textContent == "Click me!")
         |    assert(messageCount == 0)
         |
         |    for (c <- 1 to 5) {
         |      button.click()
         |      assert(messageCount == c)
         |    }
         |  }
         |}
         |
         |""".stripMargin
    ),
    SourceFile(name = "TutorialTest2", content =
      s"""import org.scalatest.funsuite.AnyFunSuite
         |import org.scalajs.dom
         |import org.scalajs.dom.document
         |import org.scalajs.dom.ext._
         |import scala.scalajs.js
         |
         |class TutorialTest2 extends AnyFunSuite {
         |  test("2*2 success") { assert(2*2 == 4) }
         |  test("2*2 failure") { assert(2*2 == 5) }
         |  test("2*2 output") { println("2*2=4") }
         |}
         |""".stripMargin
    )
  ),
  checks = Seq(
    forClass(className = "TutorialTest",
      failedCount(1),
      skippedCount(0),
      passed("HelloWorld"),
      failed("HelloWorld failure"),
      passed("ButtonClick")
    ),
    forClass(className = "TutorialTest2",
      failedCount(1),
      skippedCount(0),
      passed("2*2 success"),
      failed("2*2 failure")
    )
  ),
  runOutputExpectations = Seq(
    "Running!!!"
  )
)
