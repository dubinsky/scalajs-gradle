package org.podval.tools.test.framework

import org.podval.tools.build.{JavaDependencyMaker, Version}

// JUnit4 itself does not contain an sbt test framework runner;
// `com.github.sbt:junit-interface` supplied one (see `JUni4`);
// `MUnit` also uses underlying JUni4 - via its own internal interface.
object JUnit4Underlying extends JavaDependencyMaker:
  override val group: String = "junit"
  override val artifact: String = "junit"
  override val versionDefault: Version = Version("4.13.2")
  override val description: String = "Underlying JUnit4 Test Framework."
