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
  forJVM = ForBackend.notSupported,
  forJS = ForBackend.notSupported,
  forNative = ForBackend.notSupported
) with JavaDependency.Maker
