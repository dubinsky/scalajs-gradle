package org.podval.tools.testing.task

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.testing.junit.JUnitOptions
import scala.jdk.CollectionConverters.*

// Note: see org.podval.tools.testing.task.TestTask for the reason of using JUnitOptions.
class TestFrameworkOptions extends JUnitOptions:
  override def toString: String = s"TestFrameworkOptions(includeTags=$getIncludeCategories, excludeTags=$getExcludeCategories)"
