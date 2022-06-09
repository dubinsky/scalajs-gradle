# Gradle plugin for ScalaJS #

## Summary ##

This is a Gradle plugin for working with Scala.js.
It supports linking ScalaJS code and running it.
Testing is not yet supported.

## Motivation ##

I dislike untyped languages, so if I have to write Javascript,
I want to be able to do it in Scala.
Thanks to [Scala.js](https://www.scala-js.org/), this is possible.

I dislike [sbt](https://www.scala-sbt.org/) - the [official
build tool](https://www.scala-js.org/doc/project/) of Scala.js,
which uses [ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin).
I want to be able to use my preferred build tool - [Gradle](https://gradle.org/).

Existing Scala.js Gradle [plugin](https://github.com/gtache/scalajs-gradle) by
[gtache](https://github.com/gtache) seems to be no longer maintained.

Hence, this plugin.


## Applying to a Gradle project ##

### Plugin ###

Plugin is [published](https://plugins.gradle.org/plugin/org.podval.tools.scalajs)
on the Gradle Plugin Portal. To apply it to a Gradle project:

```groovy
plugins {
  id 'org.podval.tools.scalajs' version '0.0.1'
}
```

Plugin will automatically apply the `scala` plugin to the project, so there is no need to manually do
`id 'scala'` - but there is no harm in it either.

### ScalaJS libraries ###

It is the responsibility of the project using the plugin to add as dependencies:
- Scala standard library;
- Scala standard library compiled into ScalaJS (the same version as the above);
- ScalaJS library;
- ScalaJS DOM library if needed.

For example:
```groovy
final String scalaVersion       = '3.1.3'
final String scala2versionMinor = '2.13'
final String scalaJsVersion     = '1.10.0'

dependencies {
  implementation "org.scala-lang:scala3-library_3:$scalaVersion"
  implementation "org.scala-lang:scala3-library_sjs1_3:$scalaVersion"
  implementation "org.scala-js:scalajs-library_$scala2versionMinor:$scalaJsVersion"
  implementation "org.scala-js:scalajs-dom_sjs1_3:2.2.0"

  // for ScalaTest tests:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.12"
}
```

### ScalaJS compiler ###

Plugin does not configure nor verifies configuration of the ScalaJS compiler
(but should; see https://github.com/dubinsky/scalajs-gradle/issues/2).
Project using the plugin is responsible for configuring the Scala compiler to produce
`.sjsir` files in addition to the `.class` files.

If the project uses Scala 3, all it takes is to pass `-scalajs` option to the Scala compiler, since
Scala 3 compiler has ScalaJS support built in:

```groovy
tasks.withType(ScalaCompile) {
  scalaCompileOptions.additionalParameters = [
    '-scalajs'
  ]
}
```

If the project uses Scala 2, ScalaJS compiler plugin has to be enabled:
```groovy
dependencies {
  scalaCompilerPlugin 'scalajs-compiler_2.13.4-1.4.0.jar'
}
tasks.withType(ScalaCompile) {
  scalaCompileOptions.additionalParameters = [
    '-Xplugin:' + configurations.scalaCompilerPlugin.asPath
  ]
}
```

### ScalaJS Linker ###

Plugin uses hard-coded version of the ScalaJS linker and does not provide a way to change it
(but should; see https://github.com/dubinsky/scalajs-gradle/issues/3).

## Tasks ##

Plugin adds to the Gradle project two new tasks: `fastLinkJS` and `fullLinkJS`.
Both of those tasks depend on the `classes` task.

## Configuration ##

Plugin adds to the Gradle project an extension `scalajs`,
which can be used to configure it.
Configurable properties with their defaults are:

```groovy
scalajs {
  moduleKind       = 'NoModule'      // one of: 'NoModule', 'ESModule', 'CommonJSModule'
  moduleSplitStyle = 'FewestModules' // one of: 'FewestModules', 'SmallestModules'
  prettyPrint      = false
  moduleInitializers {
  }
}
```

Multiple module initializers can be configured:

```groovy
moduleInitializers {
  module1 { 
    className = '<fully qualified class name>'
    mainMethodName = 'main'
    mainMethodHasArgs = false
  }
  ...
}
```

Name of the module initializer ('module1' in the example above) is ignored.

## Compared to the sbt extension ##

## Credits ##

- Stack Overflow [answer](https://stackoverflow.com/a/65777102/670095)
by [gzm0](https://stackoverflow.com/users/1149944/gzm0) was
*extremely* helpful. Thanks!
- [ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin)
- [ScalaJS Tutorial](https://www.scala-js.org/doc/tutorial/basic/)
- [ScalaJS CLI](https://github.com/scala-js/scala-js-cli/tree/main/src/main/scala/org/scalajs/cli)
- [ScalaJS Linker](https://github.com/scala-js/scala-js/tree/main/linker-interface)
- [Implementing Scala.JS Support for Scala 3](https://www.scala-lang.org/2020/11/03/scalajs-for-scala-3.html)
