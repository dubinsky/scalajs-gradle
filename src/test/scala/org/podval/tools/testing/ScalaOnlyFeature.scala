package org.podval.tools.testing

object ScalaOnlyFeature extends Feature("Scala Only"):
  override def maxParallelForks(fixture: Fixture): Int = 2
  override def includeTags(fixture: Fixture): Seq[String] = Seq.empty //Seq("org.scalatest.tags.Slow"),
  override def excludeTags(fixture: Fixture): Seq[String] = Seq("com.mycompany.tags.DbTest", "org.podval.tools.testing.Tags.RequiresDb")
  