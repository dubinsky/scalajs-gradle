package org.podval.tools.test.taskdef

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.{Fingerprint, Framework, Runner}

// TODO maybe turn this upside-out and embed framework identifiers as a separate class?
abstract class TestClassRun(
  final override val getTestClassName: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val testNames: Array[String],
  val testWildCards: Array[String]
) extends TestClassRunInfo:

  final def frameworkDescriptor: FrameworkDescriptor = FrameworkDescriptor.forName(frameworkName)

  def frameworkName: String

  def framework: Framework

  final def makeRunner(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Runner =
    val args: Array[String] = frameworkDescriptor.args(
      includeTags = includeTags,
      excludeTags = excludeTags
    )
    
    // We are running the runner in *this* JVM, so remote arguments are not used?
    val remoteArgs: Array[String] = Array.empty

    val frameworkClassLoader: ClassLoader = framework.getClass.getClassLoader

    framework.runner(
      args,
      remoteArgs,
      frameworkClassLoader
    )

object TestClassRun extends Ops[TestClassRun]("@"):
  override protected def toStrings(value: TestClassRun): Array[String] = Array(
    value.frameworkName,
    value.getTestClassName,
    Fingerprints.write(value.fingerprint),
    Ops.toString(value.explicitlySpecified),
    Strings.Many.write(value.testNames),
    Strings.Many.write(value.testWildCards)
  )

  override protected def fromStrings(strings: Array[String]): TestClassRunForking = TestClassRunForking(
    frameworkName = strings(0),
    getTestClassName = strings(1),
    fingerprint = Fingerprints.read(strings(2)),
    explicitlySpecified = Ops.toBoolean(strings(3)),
    testNames = Strings.Many.read(strings(4)),
    testWildCards = Strings.Many.read(strings(5))
  )
