package org.podval.tools.test.framework

import org.podval.tools.test.testproject.ForClass.*
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, SourceFile}

object JUnit4ScalaJSFixture extends Fixture(
  framework = org.podval.tools.test.framework.JUnit4ScalaJS,
  testSources = Seq(SourceFile("JUnit4ScalaJSTest",
    s"""import org.junit.Assert.{assertArrayEquals, assertEquals, assertFalse, assertNotNull, assertNotSame, assertNull,
       |  assertSame, assertTrue}
       |import org.junit.Test
       |
       |@Test
       |final class JUnit4ScalaJSTest:
       |  @Test def testAssertArrayEquals(): Unit =
       |    val expected: Array[Byte] = "trial".getBytes
       |    val actual: Array[Byte] = "trial".getBytes
       |    assertArrayEquals("failure - byte arrays not same", expected, actual)
       |
       |  @Test def testAssertEquals(): Unit = assertEquals("failure - strings are not equal", "text", "text")
       |  @Test def testAssertFalse(): Unit = assertFalse("failure - should be false", false)
       |  @Test def testAssertNotNull(): Unit = assertNotNull("should not be null", new Object)
       |  @Test def testAssertNotSame(): Unit = assertNotSame("should not be same Object", new Object, new Object)
       |  @Test def testAssertNull(): Unit = assertNull("should be null", null)
       |
       |  @Test def testAssertSame(): Unit =
       |    val aNumber: Integer = Integer.valueOf(768)
       |    assertSame("should be same", aNumber, aNumber)
       |
       |  @Test def testAssertTrue(): Unit = assertTrue("failure - should be true", true)
       |  @Test def failure(): Unit = assertTrue("failure - should be true", false)
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("JUnit4ScalaJSTest",
      // TODO test report does not show failure, although failure event is emitted?
  //    failedCount(1),
      skippedCount(0),
      passed("testAssertNotNull"),
      passed("testAssertNotSame"),
  //    failed("failure")
    )
  )

