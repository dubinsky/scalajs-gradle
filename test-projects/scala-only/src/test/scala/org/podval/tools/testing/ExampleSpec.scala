package org.podval.tools.testing

import org.scalatest.{Tag, TagAnnotation}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.tagobjects.Slow

@Tags.RequiresDb
class ExampleSpec extends AnyFlatSpec {

  "The Scala language" must "add correctly" taggedAs(Slow) in {
    val sum = 1 + 1
    assert(sum === 2)
  }

  it must "subtract correctly" taggedAs(Slow, ExampleSpec.DbTest) in {
    val diff = 4 - 1
    assert(diff === 3)
  }
}

object ExampleSpec:
  object DbTest extends Tag("com.mycompany.tags.DbTest")

object Tags:
  import java.lang.annotation.{ElementType, Retention, RetentionPolicy, Target};

  @TagAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @Target(Array(ElementType.METHOD, ElementType.TYPE))
  class RequiresDb extends scala.annotation.Annotation
