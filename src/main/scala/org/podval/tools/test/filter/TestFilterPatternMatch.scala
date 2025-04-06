package org.podval.tools.test.filter

enum TestFilterPatternMatch derives CanEqual:
  case Suite
  case TestName    (testName    : String)
  case TestWildcard(testWildcard: String)

object TestFilterPatternMatch:
  def forMethod(method: String): Option[TestFilterPatternMatch] =
    if method.isEmpty || method == "*" then Some(Suite) else
    if method.head == '*' && method.last == '*' then forWildcard(method.tail.init) else
    if method.head == '*' then forWildcard(method.tail) else
    if method.last == '*' then forWildcard(method.init) else
      Some(TestName(method))

  private def forWildcard(wildcard: String): Option[TestFilterPatternMatch] = Some:
    if wildcard.contains("*")
    then Suite // wildcard is not expressible using Selectors
    else TestWildcard(wildcard)
