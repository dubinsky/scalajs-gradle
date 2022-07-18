package org.podval.tools.test

import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable

// Note: from the ScalaTest documentation
class StackSpec extends AnyFlatSpec {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new mutable.Stack[Int]
    stack.push(1)
    stack.push(2)
    assert(stack.pop() === 2)
    assert(stack.pop() === 1)
  }

  ignore should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new mutable.Stack[String]
    intercept[NoSuchElementException] {
      emptyStack.pop()
    }
  }
}
