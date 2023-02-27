package org.podval.tools.testing.worker

// Note: this one is Java-serialized, so I am using serializable types for parameters
final class TestTagsFilter(
  val include: Array[String],
  val exclude: Array[String]
) extends Serializable
