package org.podval.tools.testing

open class Feature(val name: String):
  def maxParallelForks(fixture: Fixture): Int = 1
  def includeTestNames(fixture: Fixture): Seq[String] = Seq.empty
  def excludeTestNames(fixture: Fixture): Seq[String] = Seq.empty
  def includeTags(fixture: Fixture): Seq[String] = Seq.empty
  def excludeTags(fixture: Fixture): Seq[String] = Seq.empty
  def commandLineIncludeTestNames(fixture: Fixture): Seq[String] = Seq.empty
  def checks(fixture: Fixture): Seq[ForClass] = fixture.checks
