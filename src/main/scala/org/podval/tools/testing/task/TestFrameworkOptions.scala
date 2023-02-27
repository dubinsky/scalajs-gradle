package org.podval.tools.testing.task

import org.gradle.api.tasks.Input
import scala.jdk.CollectionConverters.*

// TODO copy the contents of lists to prevent modifications from affecting this
class TestFrameworkOptions extends org.gradle.api.tasks.testing.TestFrameworkOptions:
  private var includeTags: Set[String] = Set.empty
  private var excludeTags: Set[String] = Set.empty

  def copyFrom(other: TestFrameworkOptions): Unit =
    this.includeTags = other.includeTags
    this.excludeTags = other.excludeTags

  @Input def getIncludeTags: java.util.Set[String] = includeTags.asJava
  def setIncludeTags(includeTags: java.util.Collection[String]): Unit = this.includeTags = includeTags.asScala.toSet
  def includeTags(includeTags: String*): TestFrameworkOptions =
    this.includeTags = this.includeTags ++ includeTags
    this

  @Input def getExcludeTags: java.util.Set[String] = excludeTags.asJava
  def setExcludeTags(excludeTags: java.util.Collection[String]): Unit = this.excludeTags = excludeTags.asScala.toSet
  def excludeTags(excludeTags: String*): TestFrameworkOptions =
    this.excludeTags = this.excludeTags ++ excludeTags
    this
