package org.podval.tools.test.run

import org.gradle.api.internal.tasks.testing.{DirectoryBasedTestDefinition, TestDefinition, TestDefinitionProcessor,
  TestResultProcessor}
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

// In Gradle 9.3.0 org.gradle.api.internal.tasks.testing.processors.RunPreviousFailedFirstTestDefinitionProcessor
// insists on TestDefinition being either ClassTestDefinition or DirectoryBasedTestDefinition
// and throws an exception on my TestClassRun, so I had to fork this class too...

/**
 * In order to speed up the development feedback cycle, this class guarantee previous failed test classes
 * to be passed to its delegate first.
 */
class RunPreviousFailedFirstTestDefinitionProcessor[D <: TestDefinition](
  previousFailedTestClasses: java.util.Set[String],
  previousFailedTestDefinitionDirectories: java.util.Set[File],
  delegate: TestDefinitionProcessor[D]
) extends TestDefinitionProcessor[D]:
  private val prioritizedTestDefinitions: java.util.LinkedHashSet[D] = new java.util.LinkedHashSet[D]()
  private val otherTestDefinitions: java.util.LinkedHashSet[D] = new java.util.LinkedHashSet[D]()

  override def startProcessing(resultProcessor: TestResultProcessor): Unit = delegate.startProcessing(resultProcessor)

  override def processTestDefinition(testDefinition: D): Unit =
    if wasPreviouslyRun(testDefinition)
    then prioritizedTestDefinitions.add(testDefinition)
    else otherTestDefinitions.add(testDefinition)

  override def stop(): Unit =
    for test: D <- prioritizedTestDefinitions.asScala do delegate.processTestDefinition(test)
    for test: D <- otherTestDefinitions.asScala do delegate.processTestDefinition(test)
    delegate.stop()

  override def stopNow(): Unit = delegate.stopNow()

  private def wasPreviouslyRun(testDefinition: TestDefinition): Boolean = testDefinition match
    case directoryBasedTestDefinition: DirectoryBasedTestDefinition =>
      previousFailedTestDefinitionDirectories.contains(directoryBasedTestDefinition.getTestDefinitionsDir)
    case _ =>
      previousFailedTestClasses.contains(testDefinition.getId)
