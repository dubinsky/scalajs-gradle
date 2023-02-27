package org.podval.tools.testing

import org.specs2.*

final class Spec2Test /*HelloWorldSpec*/ extends Specification:
  def is = s2"""

  This is a specification to check the 'Hello world' string

  The 'Hello world' string should
    contain 11 characters $e1
    start with 'Hello' $e2
    end with 'world' $e3
    fail to end with 'xxx' $e4

    """

    def e1 = "Hello world" must haveSize(11)
    def e2 = "Hello world" must startWith("Hello")
    def e3 = "Hello world" must endWith("world")
    def e4 = "Hello world" must endWith("xxx")
