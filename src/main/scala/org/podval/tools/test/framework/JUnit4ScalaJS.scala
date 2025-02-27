package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaDependency, Version}

// https://github.com/scala-js/scala-js/tree/main/junit-runtime/src/main/scala
// https://github.com/scala-js/scala-js/blob/main/junit-runtime/src/main/scala/org/scalajs/junit/JUnitFramework.scala
// https://github.com/nicolasstucki/scala-js-junit
// Dependencies:
// ScalaJS:
// Note: does not support included/excluded categories options
object JUnit4ScalaJS extends FrameworkDescriptor(
  name = "Scala.js JUnit test framework",
  displayName = "JUnit4 ScalaJS",
  group = "org.scala-js",
  artifact = "scalajs-junit-test-runtime",
  versionDefault = Version("1.18.2"), // Note: Scala.js version!
  className = "com.novocode.junit.JUnitFramework",
  sharedPackages = List("com.novocode.junit", "junit.framework", "junit.extensions", "org.junit"),
  isJvmSupported = false, // This is a Scala.js-only test framework
  // TODO on Scala 2.13, I get:
  //   Error while loading test class org.podval.tools.test.JUnit4ScalaJSTest failed:
  //   java.lang.ClassNotFoundException: Cannot find org.podval.tools.test.JUnit4ScalaJSTest$scalajs$junit$bootstrapper$
  //     at org.scalajs.junit.JUnitTask.loadBootstrapper(main.js:13275)
  //     at org.scalajs.junit.JUnitTask.execute(main.js:13365)
  //     at <jscode>.{anonymous}()(main.js:6610)
  //     at scala.scalajs.runtime.AnonFunction1.apply(main.js:18956)
  //     at <jscode>.{anonymous}()(main.js:7834)
  //     at scala.scalajs.runtime.AnonFunction1.apply(main.js:18956)
  //     at scala.concurrent.impl.Promise$Transformation.run(main.js:30742)
  //     at <jscode>.{anonymous}()(main.js:20970)
  //     at <jscode>.processTicksAndRejections(node:internal/process/task_queues:96)
  // but on Scala 3 everything works...
  // What is going on?
  isScala2Supported = false
) with ScalaDependency.MakerScala2Jvm
