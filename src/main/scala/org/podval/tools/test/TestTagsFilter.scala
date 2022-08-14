package org.podval.tools.test

// Note: this one is serialized, so I am using serializable types for parameters
final class TestTagsFilter(
  val include: Array[String],
  val exclude: Array[String]
) extends Serializable:

  import TestTagsFilter.isListed

  def allowed(tags: Array[String]): Boolean =
    (include.isEmpty || TestTagsFilter.isListed(include, tags)) && !TestTagsFilter.isListed(exclude, tags)

object TestTagsFilter:
  private def isListed(list: Array[String], tags: Array[String]): Boolean = tags.exists(list.contains)
