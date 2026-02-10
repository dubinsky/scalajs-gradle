package org.podval.tools.jvm

import org.podval.tools.build.{JavaDependency, Version}

// JUnit4 itself does not contain an sbt test framework runner;
// `com.github.sbt:junit-interface` supplies one (see JUnit4Jvm);
// `MUnit` also uses underlying JUnit4 - via its own internal interface.
object JUnit4Underlying extends JavaDependency(
  name = "Underlying JUnit4 Test Framework",
  group = "junit",
  versionDefault = Version("4.13.2"),
  artifact = "junit"
)
