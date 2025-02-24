package org.podval.tools.testing.framework

import org.podval.tools.testing.testproject.ForClass.*
import org.podval.tools.testing.testproject.{Feature, Fixture, ForClass, SourceFile}

object JUnit4Fixture extends Fixture(
  framework = org.podval.tools.testing.framework.JUnit4,
  testSources = Seq(SourceFile("JUnit4Test",
    s"""import org.junit.Assert.{assertArrayEquals, assertEquals, assertFalse, assertNotNull, assertNotSame, assertNull,
       |  assertSame, assertTrue}
       |import org.junit.Assume.assumeTrue
       |import org.junit.Test
       |import org.junit.experimental.categories.Category
       |import org.hamcrest.MatcherAssert.assertThat
       |import org.hamcrest.CoreMatchers.{allOf, anyOf, both, containsString, equalTo, everyItem, hasItems, not, sameInstance,
       |  startsWith}
       |import org.hamcrest.core.CombinableMatcher
       |
       |trait IncludedTest
       |trait ExcludedTest
       |
       |@Test
       |final class JUnit4Test:
       |  @Test def successNotIncluded(): Unit = assertFalse("should be false", false)
       |  @Test def assumeFalse(): Unit = assumeTrue(false)
       |  @Test @Category(Array(classOf[IncludedTest])) def success(): Unit = assertTrue("should be true", true)
       |  @Test @Category(Array(classOf[IncludedTest])) def failure(): Unit = assertTrue("should be true", false)
       |  @Test @Category(Array(classOf[IncludedTest], classOf[ExcludedTest])) def excluded(): Unit = assertTrue("should be excluded", false)
       |  
       |  @Test def testAssertArrayEquals(): Unit =
       |    val expected: Array[Byte] = "trial".getBytes
       |    val actual: Array[Byte] = "trial".getBytes
       |    assertArrayEquals("failure - byte arrays not same", expected, actual)
       |
       |  @Test def testAssertEquals(): Unit = assertEquals("strings should be equal", "text", "text")
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
       |""".stripMargin
  ))
):
  override def checks(feature: Feature): Seq[ForClass] = feature match
    case FrameworksTest.basicFunctionality =>
      Seq(
        forClass("JUnit4Test",
          passed("successNotIncluded"),
          skipped("assumeFalse"),
          passed("success"),
          failed("failure"),
          absent("excluded"),
          
          testCount(14),
          failedCount(1),
          skippedCount(1)
        )
      )
    case FrameworksTest.withTagInclusions =>
      Seq(
        forClass("JUnit4Test",
          absent("successNotIncluded"),
          passed("success"),
          failed("failure"),
          absent("excluded"),
  
          testCount(2),
          failedCount(1),
          skippedCount(0)
        )
      ) 
    
      
