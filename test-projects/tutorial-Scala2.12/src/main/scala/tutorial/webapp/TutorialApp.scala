package tutorial.webapp

import org.scalajs.dom
import org.scalajs.dom.document

object TutorialApp {
  def appendPar(targetNode: dom.Node, text: String): Unit = {
    val parNode = document.createElement("p")
    parNode.textContent = text
    targetNode.appendChild(parNode)
  }

  def addClickedMessage(): Unit =
    appendPar(document.body, "You clicked the button!")

  def setupUI(): Unit = {
    val button = document.createElement("button")
    button.textContent = "Click me!"
    button.addEventListener("click", (e: dom.MouseEvent) => addClickedMessage())
    document.body.appendChild(button)

    appendPar(document.body, "Hello World")
  }

  def main(args: Array[String]): Unit = {
    println("Running!")
    document.addEventListener("DOMContentLoaded", (e: dom.Event) => setupUI())
  }
}
