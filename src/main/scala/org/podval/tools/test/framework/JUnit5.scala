package org.podval.tools.test.framework

import org.podval.tools.build.{JavaDependency, Version}

// https://github.com/sbt/sbt-jupiter-interface
// https://github.com/sbt/sbt-jupiter-interface/blob/master/src/library/src/main/java/net/aichler/jupiter/api/JupiterFramework.java
object JUnit5 extends FrameworkDescriptor(
  name = "Jupiter",
  displayName = "JUnit5",
  group = "com.github.sbt.junit",
  artifact = "jupiter-interface",
  versionDefault = Version("0.13.3"),
  className = "com.github.sbt.junit.jupiter.api.JupiterFramework",
  sharedPackages = List("com.github.sbt.junit.jupiter.api", "org.junit"),
  tagOptionStyle = OptionStyle.ListWithEq,
  includeTagsOption = "--include-tags",
  excludeTagsOption = "--exclude-tags",
  // This is a JVM-only test framework
  isScalaJSSupported = false,
  // TODO https://github.com/dubinsky/scalajs-gradle/issues/38
  //Comment on the JupiterTestFingerprint.annotationName() says:
  //
  //> return The name of this class. This is to ensure that SBT does not find
  //> any tests so that we can use JUnit Jupiter's test discovery mechanism.
  //
  //Well, mission accomplished: my test scanner does not find any tests, and since
  //I have no idea what "JUnit Jupiter's test discovery mechanism" is,
  //I get the Gradle message "No tests found for given includes".
  //So, no JUnit5 support for now :(
  //
  //I _might_ try to use framework-specific test discovery instead of the Scala Analysis one in the Scala-only setting,
  //but it is not a priority :)
  isJvmSupported = false
) with JavaDependency.Maker
