# Gradle plugin for ScalaJS #

## Summary ##

This is a Gradle plugin for working with Scala.js.

This is not yet a full-blown plugin: it is not published to the Gradle Plugin Portal.
To use it right now one needs to check out this repository
and include it in the compound build...

Plugin adds to the Gradle project two new tasks: `fastLinkJS` and `fullLinkJS`.
Both of those tasks depend on the `classes` task.

## Motivation ##

I dislike untyped languages, so if I have to write Javascript,
I want to be able to do it in Scala.
Thanks to [Scala.js](https://www.scala-js.org/), this is possible.

I dislike [sbt](https://www.scala-sbt.org/) - the [official
build tool](https://www.scala-js.org/doc/project/) of Scala.js.
I want to be able to use my preferred build tool - [Gradle](https://gradle.org/).

Existing Scala.js Gradle [plugin](https://github.com/gtache/scalajs-gradle) by
[gtache](https://github.com/gtache) seems to be no longer maintained.

Hence, this plugin.


## Applying to a Gradle project ##

Plugin is [published](https://plugins.gradle.org/plugin/org.podval.tools.scalajs)
on the Gradle Plugin Portal. To apply it to a Gradle project:

```groovy
plugins {
  id 'org.podval.tools.scalajs' version '0.0.1'
}
```

Plugin will automatically apply the `scala` plugin to the project, so there is no need to manually do
`id 'scala'` - but there is no harm in it either.

## Configuration ##

Plugin adds to the Gradle project an extension `scalajs`,
which can be used to configure it.
Configurable properties with their defaults are:

```groovy
scalajs {
  outputDirectory  = File(project.getBuildDir, 'js')
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

## Credits ##

- Stack Overflow [answer](https://stackoverflow.com/a/65777102/670095)
by [gzm0](https://stackoverflow.com/users/1149944/gzm0) was
*extremely* helpful. Thanks!
- [ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin)
- [ScalaJS Tutorial](https://www.scala-js.org/doc/tutorial/basic/)
- [ScalaJS CLI](https://github.com/scala-js/scala-js-cli/tree/main/src/main/scala/org/scalajs/cli)
- [ScalaJS Linker](https://github.com/scala-js/scala-js/tree/main/linker-interface)
- [Implementing Scala.JS Support for Scala 3](https://www.scala-lang.org/2020/11/03/scalajs-for-scala-3.html)
