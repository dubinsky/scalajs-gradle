package org.podval.tools.test

import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.{AfterAll, AfterEach, BeforeAll, BeforeEach, Disabled, Test}

@Test
final class JUnit5Test /*StandardTests*/:
  @BeforeAll def initAll(): Unit = ()

  @AfterAll def tearDownAll(): Unit = ()

  @BeforeEach def init(): Unit = ()

  @Test def succeedingTest(): Unit = ()

  @Test def failingTest(): Unit =
    fail("a failing test")

  @Test
  @Disabled("for demonstration purposes") def skippedTest(): Unit =
    // not executed
    ()

  @Test def abortedTest(): Unit =
    assumeTrue("abc".contains("Z"))
    fail("test should have been aborted")

  @AfterEach def tearDown(): Unit = ()
