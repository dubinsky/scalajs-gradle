package org.podval.tools.util

import org.gradle.api.Project
import org.gradle.api.plugins.{ExtensionAware, ExtensionContainer}

object Extensions:
  private def getExtensions(extensionAware: ExtensionAware): ExtensionContainer = extensionAware.getExtensions

  def create[T](
    extensionAware: ExtensionAware,
    name: String,
    clazz: Class[T],
    constructionArguments: Any*
  ): T = getExtensions(extensionAware).create(
    name,
    clazz,
    constructionArguments *
  )

  def add[T](
    extensionAware: ExtensionAware,
    name: String,
    extension: T
  ): Unit = getExtensions(extensionAware).add(
    name,
    extension
  )

  def getByName[T](
    extensionAware: ExtensionAware,
    name: String
  ): T =
    getExtensions(extensionAware).getByName(name).asInstanceOf[T]

  def getByType[T](
    extensionAware: ExtensionAware,
    clazz: Class[T]
  ): T =
    getExtensions(extensionAware).getByType(clazz)

  def findByType[T](
    extensionAware: ExtensionAware,
    clazz: Class[T]
  ): Option[T] =
    Option(getExtensions(extensionAware).findByType(clazz))

