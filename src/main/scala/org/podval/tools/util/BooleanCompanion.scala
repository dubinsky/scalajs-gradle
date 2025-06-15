package org.podval.tools.util

import org.gradle.api.provider.Property

abstract class BooleanCompanion(what: String, key: Option[String] = None):
  def default: Boolean = false
  
  final def apply(property: Property[Boolean]): Boolean = property.get

  final def convention(property: Property[Boolean]): Property[Boolean] = property.convention(discover)
  
  final def discover: Boolean = key
    .flatMap(key => Option(System.getenv.get(key)))
    .filterNot(_.isEmpty())
    //.exists(_.toBoolean)
    .map(_.toBoolean)
    .getOrElse(default)
