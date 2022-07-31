package org.podval.tools.test

// Note: this one is serialized, so I am using serializable types for parameters
final class TestTagging(
  val include: Array[String],
  val exclude: Array[String]
) extends Serializable:

  import TestTagging.isListed

  def allowed(tags: Array[String]): Boolean =
    (include.isEmpty || isListed(include, tags)) && !isListed(exclude, tags)

object TestTagging:
  def isListed(list: Array[String], tags: Array[String]): Boolean = tags.exists(list.contains)
