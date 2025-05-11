package org.podval.tools.scalajsplugin.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.{Category, Usage}
import org.gradle.api.internal.artifacts.configurations.ConfigurationRolesForMigration
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, SourceSetContainer}
import org.gradle.api.tasks.scala.ScalaCompile
import scala.jdk.CollectionConverters.*

// TODO - this is reported by `./gradlew resolvableConfigurations` even without me touching anything:
// Consumable configurations with identical capabilities within a project
// (other than the default configuration) must have unique attributes,
// but configuration ':incrementalScalaAnalysisFormain' and [configuration ':incrementalScalaAnalysisElements']
// contain identical attribute sets.
// Consider adding an additional attribute to one of the configurations to disambiguate them.
// For more information, please refer to
// https://docs.gradle.org/8.13/userguide/upgrading_version_7.html#unique_attribute_sets
// in the Gradle documentation.

// Adopted from org.gradle.api.plugins.scala.ScalaBasePlugin.
object ScalaBasePluginAsLibrary:
  def configureCompilerPluginsConfiguration(
    project: Project,
    scalaCompilerPluginsConfigurationName: String,
    jvmPluginServices: JvmPluginServices
  ): Unit =
    val plugins: Configuration = project.asInstanceOf[ProjectInternal].getConfigurations.resolvableDependencyScopeUnlocked(
      scalaCompilerPluginsConfigurationName
    )
    plugins.setTransitive(false)
    jvmPluginServices.configureAsRuntimeClasspath(plugins)

  def configureIncrementalAnalysisElements(
    project: Project,
    incrementalAnalysisUsageName: String,
    incrementalAnalysisCategoryName: String,
    incrementalScalaAnalysisElementsConfigurationName: String,
    backendDisplayName: String
  ): Unit =
    val incrementalAnalysisUsage: Usage = project.getObjects.named(classOf[Usage], incrementalAnalysisUsageName)
    val incrementalAnalysisCategory: Category = project.getObjects.named(classOf[Category], incrementalAnalysisCategoryName)

    val incrementalAnalysisElements: Configuration = project.asInstanceOf[ProjectInternal].getConfigurations.migratingUnlocked(
      incrementalScalaAnalysisElementsConfigurationName,
      ConfigurationRolesForMigration.CONSUMABLE_DEPENDENCY_SCOPE_TO_CONSUMABLE
    )
    incrementalAnalysisElements.setVisible(false)
    incrementalAnalysisElements.setDescription(s"Incremental compilation analysis files for $backendDisplayName")
    incrementalAnalysisElements.getAttributes.attribute(Usage.USAGE_ATTRIBUTE, incrementalAnalysisUsage)
    incrementalAnalysisElements.getAttributes.attribute(Category.CATEGORY_ATTRIBUTE, incrementalAnalysisCategory)

  def configureCompileDefaults(
    project: Project,
    scalaCompilerPluginsConfigurationName: String,
    mainScalaCompileTaskName: String,
    testScalaCompileTaskName: String
  ): Unit =
    configureCompileDefaults(
      project,
      mainScalaCompileTaskName,
      scalaCompilerPluginsConfigurationName
    )
    configureCompileDefaults(
      project,
      testScalaCompileTaskName,
      scalaCompilerPluginsConfigurationName
    )

  private def configureCompileDefaults(
    project: Project,
    scalaCompileTaskName: String,
    scalaCompilerPluginsConfigurationName: String
  ): Unit =
    project.getTasks.withType(classOf[ScalaCompile]).configureEach(compile =>
      if compile.getName == scalaCompileTaskName then compile.getConventionMapping.map(
        "scalaCompilerPlugins",
        () => project.getConfigurations.getAt(scalaCompilerPluginsConfigurationName)
      )
    )

  def configureSourceSetDefaults(
    project: Project,
    sourceRoot: String,
    sharedSourceRoot: String,
    mainSourceSetName: String,
    testSourceSetName: String
  ): Unit =
    val sourceSets: SourceSetContainer = project.getExtensions.getByType(classOf[JavaPluginExtension]).getSourceSets

    configureSourceSetDefaults(
      project,
      sourceSets.getByName(mainSourceSetName),
      sourceRoot = sourceRoot,
      sharedSourceRoot = sharedSourceRoot,
      srcDirectory = "main"
    )

    configureSourceSetDefaults(
      project,
      sourceSets.getByName(testSourceSetName),
      sourceRoot = sourceRoot,
      sharedSourceRoot = sharedSourceRoot,
      srcDirectory = "test"
    )

  private def configureSourceSetDefaults(
    project: Project,
    sourceSet: SourceSet,
    sourceRoot: String,
    sharedSourceRoot: String,
    srcDirectory: String
  ): Unit = sourceSet
    .getExtensions
    .getByType(classOf[ScalaSourceDirectorySet])
    .setSrcDirs(
      Seq(
        s"$sourceRoot/src/$srcDirectory/scala",
        s"$sharedSourceRoot/src/$srcDirectory/scala",
        s"src/$srcDirectory/scala"
      )
        .map(project.file)
        .asJava
    )
