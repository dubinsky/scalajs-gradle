package org.podval.tools.test.run

import org.podval.tools.test.taskdef.Selectors
import org.podval.tools.util.Scala212Collections.{arrayAppend, arrayFind}
import sbt.testing.Selector

final class SkippedTests:
  private var selectors: Array[Selector] = Array.empty

  def contains(eventFor: Running): Boolean = arrayFind(selectors, Selectors.equal(_, eventFor.selector)).isDefined

  def add(eventFor: Running): Unit = selectors = arrayAppend(selectors, eventFor.selector)
