package org.podval.tools.test

import org.opentorah.build.Gradle
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.Framework
import scala.jdk.CollectionConverters.*

abstract class TestTaskScala extends TestTask:
  setDescription(s"Test using sbt frameworks")
  final override protected def canFork: Boolean = true
  final override def sourceMapper: Option[SourceMapper] = None

  final override def testEnvironment: TestEnvironment = new TestEnvironment:
    override def close(): Unit = ()

    override def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework] =
      // TODO classpath without this everything breaks - event though this adds what is already there to itself;
      // I guess something somewhere looks at the top classloader only instead of listing all the jars
      // by visiting the parent classloaders...
      // Two questions:
      // - is it me or Gradle?
      // - what really needs to be on the list?
      // I *think* the issue is that in FrameworkDescriptor I call Class.forName() with the local classloader,
      // so I need to affect its classpath or something...
      // It would be cleaner to pass the correct classLoader around, but FrameworkSerializer also instantiates the Framework...
//      println(Gradle.collectClassPath(getClass.getClassLoader).mkString("----- TestTaskScala classpath before adding:\n", "\n", "\n-----"))
//      println(getClasspath.asScala.mkString("----- TestTaskScala adding:\n", "\n", "\n-----"))
//      println(Gradle.collectClassPath(getClass.getClassLoader).mkString("----- TestTaskScala classpath after adding:\n", "\n", "\n-----"))

      Gradle.addToClassPath(this, getClasspath.asScala)
      descriptors.flatMap(_.maybeInstantiate)
