package org.podval.tools.scalajs.testing

// Note: based on sbt.Tests from org.scala-sbt.actions
/**
 * Summarizes a test run.
 *
 * @param name The name of the test framework providing this summary.
 * @param summaryText The summary message for tests run by the test framework.
 */
final case class Summary(name: String, summaryText: String)
