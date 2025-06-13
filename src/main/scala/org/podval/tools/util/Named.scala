package org.podval.tools.util

import org.gradle.api.provider.Property

open class Named(val name: String)

object Named:
  abstract class Companion[N <: Named](what: String, key: Option[String] = None):
    def default: N

    def all: Seq[N]

    final def apply(property: Property[String]): N = forName(property.get)
    final def convention(property: Property[String]): Property[String] = property.convention(discover.name)
    
    private def forName(name: String): N = all
      .find(_.name == name)
      .getOrElse(throw IllegalArgumentException(s"Invalid $what '$name'."))

    final def discover: N = key
      .flatMap(key => Option(System.getenv.get(key)))
      .filterNot(_.isEmpty())
      .map(forName)
      .getOrElse(default)
