package org.podval.tools.testing.worker

// Note: this one is Java-serialized, so I am using serializable types for parameters
final class TestTagsFilter(
  val include: Array[String],
  val exclude: Array[String]
) extends Serializable:
  override def toString: String =
    val includeStr: String = include.mkString("Include[", ", ", "]")
    val excludeStr: String = exclude.mkString("Exclude[", ", ", "]")
    s"$includeStr $excludeStr"
