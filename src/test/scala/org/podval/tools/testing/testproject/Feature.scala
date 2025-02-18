package org.podval.tools.testing.testproject

final class Feature(
  val name: String,
  val maxParallelForks: Int = 1, // Note: >1 ignored on Scala.js
  val includeTestNames: Seq[String] = Seq.empty,
  val excludeTestNames: Seq[String] = Seq.empty,
  val commandLineIncludeTestNames: Seq[String] = Seq.empty,
  val includeTags: Seq[String] = Seq.empty,
  val excludeTags: Seq[String] = Seq.empty
)
