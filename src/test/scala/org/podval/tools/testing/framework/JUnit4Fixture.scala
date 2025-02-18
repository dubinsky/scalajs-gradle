package org.podval.tools.testing.framework

import org.podval.tools.testing.testproject.ForClass.*
import org.podval.tools.testing.testproject.{Feature, Fixture, ForClass, SourceFile}

object JUnit4Fixture extends Fixture(
  framework = org.podval.tools.testing.framework.JUnit4,
  testSources = Seq(SourceFile("JUnit4Test",
    s"""import org.junit.Assert.{assertArrayEquals, assertEquals, assertFalse, assertNotNull, assertNotSame, assertNull,
       |  assertSame, assertTrue}
       |import org.hamcrest.MatcherAssert.assertThat
       |import org.hamcrest.CoreMatchers.{allOf, anyOf, both, containsString, equalTo, everyItem, hasItems, not, sameInstance,
       |  startsWith}
       |import org.hamcrest.core.CombinableMatcher
       |import org.junit.Test
       |import org.junit.experimental.categories.Category
       |
       |trait ExcludedTest
       |trait IncludedTest
       |
       |@Test
       |final class JUnit4Test:
       |  @Test @Category(Array(classOf[ExcludedTest])) def excluded(): Unit = assertTrue("should be excluded", false)
       |  
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
       |  // JUnit Matchers assertThat
       |  @Test def testAssertThatBothContainsString(): Unit = assertThat("albumen", both(containsString("a")).and(containsString("b")))
       |  @Test def testAssertThatHasItems(): Unit = assertThat(java.util.Arrays.asList("one", "two", "three"), hasItems("one", "three"))
       |  @Test def testAssertThatEveryItemContainsString(): Unit = assertThat(java.util.Arrays.asList("fun", "ban", "net"), everyItem(containsString("n")))
       |
       |  // Core Hamcrest Matchers with assertThat
       |  @Test def testAssertThatHamcrestCoreMatchers(): Unit =
       |    assertThat("good", allOf(equalTo("good"), startsWith("good")))
       |    assertThat("good", not(allOf(equalTo("bad"), equalTo("good"))))
       |    assertThat("good", anyOf(equalTo("bad"), equalTo("good")))
       |    assertThat(7, not(CombinableMatcher.either[Int](equalTo(3)).or(equalTo(4))))
       |    assertThat(new Object, not(sameInstance(new Object)))
       |
       |  @Test def testAssertTrue(): Unit = assertTrue("failure - should be true", true)
       |  @Test def failure(): Unit = assertTrue("failure - should be true", false)
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = Seq(
    forClass("JUnit4Test",
      absent("excluded"),
      failedCount(1),
      skippedCount(0),
      passed("testAssertNotNull"),
      passed("testAssertNotSame"),
      passed("testAssertThatHasItems"),
      failed("failure")
    )
  )

