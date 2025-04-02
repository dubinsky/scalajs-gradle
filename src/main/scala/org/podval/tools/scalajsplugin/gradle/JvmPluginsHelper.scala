package org.podval.tools.scalajsplugin.gradle

import org.gradle.api.{Project, Task}
import org.gradle.api.file.{DirectoryProperty, SourceDirectorySet}
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultSourceSetOutput
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.internal.JvmPluginsHelper as Original
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.{AbstractCompile, CompileOptions}
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.{SourceSet, TaskProvider}
import org.gradle.internal.Cast

object JvmPluginsHelper:
  def compileAgainstJavaOutputs (compileTask: AbstractCompile, sourceSet: SourceSet, objectFactory: ObjectFactory): Unit =
    Original.compileAgainstJavaOutputs(compileTask, sourceSet, objectFactory)
    
  def configureAnnotationProcessorPath(
      project: Project,
      sourceRoot: String,
      sourceSet: SourceSet,
      sourceDirectorySet: SourceDirectorySet,
      options: CompileOptions,
    ): Unit =
      DslObject(options).getConventionMapping.map("annotationProcessorPath", () => sourceSet.getAnnotationProcessorPath)
  
      convention(
        project,
        sourceRoot,
        options.getGeneratedSourceOutputDirectory,
        "generated/sources/annotationProcessor",
        sourceSet,
        sourceDirectorySet
      )

  def configureOutputDirectoryForSourceSet(
    project: Project,
    sourceRoot: String,
    sourceSet: SourceSet,
    sourceDirectorySet: SourceDirectorySet,
    compileTask: TaskProvider[? <: Task]
  ) : Unit =
    convention(
      project,
      sourceRoot,
      sourceDirectorySet.getDestinationDirectory,
      "classes",
      sourceSet,
      sourceDirectorySet
    )

    val sourceSetOutput = Cast.cast(classOf[DefaultSourceSetOutput], sourceSet.getOutput)
    sourceSetOutput.getClassesDirs.from(sourceDirectorySet.getDestinationDirectory).builtBy(compileTask)
    sourceSetOutput.getGeneratedSourcesDirs.from(compileTask.map(_.asInstanceOf[ScalaCompile].getOptions).flatMap(_.getGeneratedSourceOutputDirectory))
    sourceDirectorySet.compiledBy(compileTask, _.asInstanceOf[AbstractCompile].getDestinationDirectory)

  // to clean up code duplication
  private def convention(
    project: Project,
    sourceRoot: String,
    directoryProperty: DirectoryProperty,
    path: String,
    sourceSet: SourceSet,
    sourceDirectorySet: SourceDirectorySet
  ) =
    directoryProperty.convention(
      project.getLayout.getBuildDirectory.dir(
        s"$sourceRoot/$path/${sourceDirectorySet.getName}/${sourceSet.getName}"
      )
    )
