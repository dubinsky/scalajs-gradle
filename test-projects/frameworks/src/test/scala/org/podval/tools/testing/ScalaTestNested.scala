package org.podval.tools.testing

import org.scalatest._

class ScalaTestNested extends Suites(
  new ScalaTestTest,
  //  new StackSpec,
  //  new TwoByTwoTest
)
