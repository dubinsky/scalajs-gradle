package org.podval.tools.test.task

// Note: see org.podval.tools.test.task.TestTask for the reason of using JUnitOptions.
class TestFrameworkOptions extends org.gradle.api.tasks.testing.junit.JUnitOptions:
  override def toString: String = s"TestFrameworkOptions(includeTags=$getIncludeCategories, excludeTags=$getExcludeCategories)"
