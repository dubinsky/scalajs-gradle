package org.podval.tools.util

import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.plugins.{JavaPluginExtension, JvmTestSuitePlugin}
import org.gradle.api.tasks.SourceSet
import org.gradle.api.{Action, Project}
import org.gradle.util.internal.TextUtil

object Configurations:
  def scalaCompilerPluginsName: String = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME

  def testScalaCompilerPluginsName: String = TextUtil.toLowerCamelCase(s"test $scalaCompilerPluginsName")

  private def sourceSet(project: Project, sourceSetName: String): SourceSet = Extensions
    .getByType(project, classOf[JavaPluginExtension])
    .getSourceSets
    .getByName(sourceSetName)

  def sourceSet(project: Project, isTest: Boolean): SourceSet =
    if isTest
    then testSourceSet(project)
    else mainSourceSet(project)

  def mainSourceSet(project: Project): SourceSet = sourceSet(project, JvmConstants.JAVA_MAIN_FEATURE_NAME)
  def testSourceSet(project: Project): SourceSet = sourceSet(project, JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)

  def configuration(project: Project, configurationName: String): Configuration = project
    .getConfigurations
    .getByName(configurationName)

  def implementationName(project: Project): String = mainSourceSet(project).getImplementationConfigurationName
  def implementation(project: Project): Configuration = configuration(project, implementationName(project))
  def runtimeClasspathName(project: Project): String = mainSourceSet(project).getRuntimeClasspathConfigurationName
  def runtimeClasspath(project: Project): Configuration = configuration(project, runtimeClasspathName(project))
  def testImplementationName(project: Project): String = testSourceSet(project).getImplementationConfigurationName
  def testImplementation(project: Project): Configuration = configuration(project, testImplementationName(project))
  def testRuntimeOnlyName(project: Project): String = testSourceSet(project).getRuntimeOnlyConfigurationName

  def addDependency(
    project: Project,
    configurationName: String,
    dependencyNotation: AnyRef
  ): Unit = project.getDependencies.add(
    configurationName,
    dependencyNotation
  )

  def create(
    project: Project,
    configurationName: String,
    isTransitive: Boolean,
    description: String,
    jvmPluginServices: Option[JvmPluginServices]
  ): Configuration = project.getConfigurations.create(
    configurationName,
    new Action[Configuration]:
      override def execute(configuration: Configuration): Unit =
        configuration.setTransitive   (isTransitive)
        configuration.setCanBeResolved(true)  // TODO should be false; when (if ever) ScalaBasePlugin is cleaned up, copy it here.
        configuration.setCanBeDeclared(true)
        configuration.setCanBeConsumed(false)
        configuration.setDescription(description)
        jvmPluginServices.foreach(_.configureAsRuntimeClasspath(configuration))
  )
