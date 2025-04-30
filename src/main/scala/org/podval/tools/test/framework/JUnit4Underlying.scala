package org.podval.tools.test.framework

import org.podval.tools.build.{JavaDependency, Version}

// JUnit4 itself does not contain an sbt test framework runner;
// `com.github.sbt:junit-interface` supplied one (see `JUni4`);
// `MUnit` also uses underlying JUni4 - via its own internal interface.
object JUnit4Underlying extends JavaDependency.Maker:
  override def group: String = "junit"
  override def artifact: String = "junit"
  override def versionDefault: Version = Version("4.13.2")
  override def description: String = "Underlying JUnit4 Test Framework."
