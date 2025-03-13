package org.podval.tools.test.testproject

final class Feature(
  val name: String,
  val maxParallelForks: Int = 1, // values greater than 1 are ignored on Scala.js
  val includeTestNames: Seq[String] = Seq.empty,
  val excludeTestNames: Seq[String] = Seq.empty,
  val commandLineIncludeTestNames: Seq[String] = Seq.empty,
  val includeTags: Seq[String] = Seq.empty,
  val excludeTags: Seq[String] = Seq.empty
) derives CanEqual:
  override def hashCode(): Int = name.hashCode
  override def equals(obj: Any): Boolean = obj match
    case that: Feature => this.name == that.name
    case _ => false
