package org.podval.tools.test.framework

import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.{Framework, Runner}

sealed abstract class FrameworkProvider:
  def frameworkName: String = framework.name
  def frameworkDescriptor: FrameworkDescriptor = FrameworkDescriptor.forName(frameworkName)
  lazy val framework: Framework = frameworkOpt.get

  final def frameworkOpt: Option[Framework] =
    try Class
      .forName(frameworkDescriptor.className)
      .getDeclaredConstructor()
      .newInstance()
    match
      case framework: Framework => Some(framework)
      case other =>
        FrameworkProvider.logger.error(s"${other.getClass.getName} is not an SBT framework!")
        None
    catch
      case _: ClassNotFoundException => None

  final def runner(
    isRunningInIntelliJ: Boolean,
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Runner = framework.runner(
    frameworkDescriptor.args(
      isRunningInIntelliJ = isRunningInIntelliJ,
      includeTags = includeTags,
      excludeTags = excludeTags
    ),
    Array.empty,
    framework.getClass.getClassLoader
  )

object FrameworkProvider:
  private val logger: Logger = LoggerFactory.getLogger(FrameworkDescriptor.getClass)

  def apply(name: String): FrameworkProvider = new FrameworkProvider:
    override def frameworkName: String = name

  def apply(it: Framework): FrameworkProvider = new FrameworkProvider:
    override lazy val framework: Framework = it

  def apply(descriptor: FrameworkDescriptor): FrameworkProvider = new FrameworkProvider:
    override def frameworkDescriptor: FrameworkDescriptor = descriptor
