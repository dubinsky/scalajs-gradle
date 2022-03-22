# Gradle plugin for ScalaJS #

## Motivation ##

...

## Use ##

This is not (yet?) a full-blown plugin.

It is not even published to the Gradle Plugin Portal.

To use it right now one needs to check out this repository
and include it in the compound build: ... .

## Configuration ##

...

## Limitations and Possible Enhancements ##

- add ability to configure a list of entry points;
- are exports enough to produce JavaScript for multiple games?
- how to cross-compile using shared/jvm/js?
  ([the way sbt does it](https://www.scala-js.org/doc/project/cross-build.html))
- discover the main class
- discover the version of ScalaJS from the classpath and use reflection
to match the version of the linker
- discover the version of Scala from the classpath and configure
  (or at least check the configuration of) the Scala compiler
- add more configurable options
- make link tasks depend on the 'classes' one

## Credits ##

Stack Overflow [answer](https://stackoverflow.com/a/65777102/670095)
by [gzm0](https://stackoverflow.com/users/1149944/gzm0) was
*extremely* helpful. Thanks!

[ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin)

[Tutorial](https://www.scala-js.org/doc/tutorial/basic/)

[Scala JS CLI](https://github.com/scala-js/scala-js-cli/tree/main/src/main/scala/org/scalajs/cli)

ScalaJS Linker

[IMPLEMENTING SCALA.JS SUPPORT FOR SCALA 3](https://www.scala-lang.org/2020/11/03/scalajs-for-scala-3.html)

[The old plugin](https://github.com/gtache/scalajs-gradle)
