package org.podval.tools.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.{ExtensionAware, ExtensionContainer}

object Extensions:
  private def getExtensions(extensionAware: ExtensionAware): ExtensionContainer = extensionAware.getExtensions

  def create[T](
    project: Project,
    name: String,
    clazz: Class[T],
    constructionArguments: Any*
  ): T = getExtensions(project).create(
    name,
    clazz,
    constructionArguments *
  )

  def getByName[T](
    project: Project,
    name: String
  ): T =
    getExtensions(project).getByName(name).asInstanceOf[T]

  def getByType[T](
    extensionAware: ExtensionAware,
    clazz: Class[T]
  ): T =
    getExtensions(extensionAware).getByType(clazz)

  def findByType[T](
    project: Project,
    clazz: Class[T]
  ): Option[T] =
    Option(getExtensions(project).findByType(clazz))

