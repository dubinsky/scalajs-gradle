package org.podval.tools.test.filter

enum TestFilterPatternMatch derives CanEqual:
  case Suite
  case TestName    (testName    : String)
  case TestWildCard(testWildCard: String)

object TestFilterPatternMatch:
  def forMethod(method: String): Option[TestFilterPatternMatch] =
    if method.isEmpty || method == "*" then Some(Suite) else
    if method.head == '*' && method.last == '*' then forWildCard(method.tail.init) else
    if method.head == '*' then forWildCard(method.tail) else
    if method.last == '*' then forWildCard(method.init) else
      Some(TestName(method))

  private def forWildCard(wildCard: String): Option[TestFilterPatternMatch] = Some:
    if wildCard.contains("*")
    then Suite // wildCard is not expressible using Selectors
    else TestWildCard(wildCard)
