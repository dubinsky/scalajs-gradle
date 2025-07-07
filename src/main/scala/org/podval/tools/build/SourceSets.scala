package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.JvmConstants
import org.gradle.api.plugins.{JavaPluginExtension, JvmTestSuitePlugin}
import org.gradle.api.tasks.SourceSet

object SourceSets:
  private def get(project: Project, sourceSetName: String): SourceSet = project
    .getExtensions
    .getByType(classOf[JavaPluginExtension])
    .getSourceSets
    .getByName(sourceSetName)
    
  def get(project: Project, isTest: Boolean): SourceSet =
    if isTest
    then testSourceSet(project)
    else mainSourceSet(project)

  def mainSourceSet(project: Project): SourceSet = get(project, JvmConstants.JAVA_MAIN_FEATURE_NAME       )
  def testSourceSet(project: Project): SourceSet = get(project, JvmTestSuitePlugin.DEFAULT_TEST_SUITE_NAME)
  
  def implementationConfigurationName(project: Project): String = 
    mainSourceSet(project).getImplementationConfigurationName

  def runtimeClasspathConfigurationName(project: Project): String =
    mainSourceSet(project).getRuntimeClasspathConfigurationName
    
  def testImplementationConfigurationName(project: Project): String = 
    testSourceSet(project).getImplementationConfigurationName
    
  def testRuntimeOnlyConfigurationName(project: Project): String =
    testSourceSet(project).getRuntimeOnlyConfigurationName
  
  def getConfiguration(project: Project, configurationName: String): Configuration =
    project.getConfigurations.getByName(configurationName)