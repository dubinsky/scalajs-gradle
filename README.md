# Gradle plugin for ScalaJS #

## Summary ##

This is a Gradle plugin for working with Scala.js.
It supports linking ScalaJS code, running and testing it.

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
  id 'org.podval.tools.scalajs' version '0.0.4'
}
```

Plugin will automatically apply the Scala plugin to the project, so there is no need to manually list
`id 'scala'` in the `plugins` block - but there is no harm in it either.
Either way, it is the responsibility of the project using the plugin to add a standard Scala library
dependency that the Scala plugin requires.

For testing ScalaJS code, a 'testImplementation' dependency on the testing framework that supports
ScalaJS needs to be added.

### ScalaJS compiler ###

If the project uses Scala 3, all it takes is to pass `-scalajs` option to the Scala compiler, since
Scala 3 compiler has ScalaJS support built in:

```groovy
tasks.withType(ScalaCompile) {
  scalaCompileOptions.with {
    additionalParameters = [ '-scalajs' ]
  }
}
```

Plugin adds this option to the main and test Scala compilation tasks if it is not present.

If the project uses Scala 2, ScalaJS compiler plugin dependency needs to be declared:

```groovy
dependencies {
  scalaCompilerPlugins "org.scala-js:scalajs-compiler_$scalaVersion:$scalaJsVersion"
}
```

Plugin does this automatically unless a dependency on `scala-compiler` is declared explicitly.

To enable Scala compiler plugins, their classpaths need to be given to the compiler
via a `-Xplugin:` option. Examples of the Gradle build script code that do that abound:

```groovy
tasks.withType(ScalaCompile) {
  scalaCompileOptions.additionalParameters = [
    '-Xplugin:' + configurations.scalaCompilerPlugin.asPath
  ]
}
```

*Note:* Such code is not needed, since Gardle Scala plugin already does this.

### Dependencies ###

Plugin uses some dependencies internally:
- ScalaJS linker;
- ScalaJS test adapter;
- ScalaJS JSDOM Node environment;
- Zinc.

For Scala 2, ScalaJS compiler plugin is needed.

Plugin also needs some dependencies on the runtime classpath:
- Scala standard library compiled into ScalaJS (only for Scala 3);
- ScalaJS library;
- ScalaJS DOM library;
- ScalaJS test bridge.

Plugin is compiled against a specific version of Zinc, but at runtime uses Zinc
that the Scala plugin configured.

Plugin is compiled against specific versions of ScalaJS and ScalaJS JSDOM Node environment,
but uses the versions configured in the `scalajs` configuration that it creates.

Plugin adds missing dependencies automatically.
If you declare a `scalajs-library` dependency explicitly, plugin chooses the same
version of the ScalaJS dependencies it adds
(`scalajs-linker`, `scalajs-sbt-test-adapter`, `scalajs-test-bridge`, `scalajs-compiler`)
will be of the same version.

Example with all dependencies listed for Scala 3:
```groovy
final String scalaVersion       = '3.1.3'
final String scala2versionMinor = '2.13'
final String scalaJsVersion     = '1.10.1'

dependencies {
  implementation "org.scala-lang:scala3-library_3:$scalaVersion"
  implementation "org.scala-lang:scala3-library_sjs1_3:$scalaVersion"
  implementation "org.scala-js:scalajs-library_$scala2versionMinor:$scalaJsVersion"
  implementation "org.scala-js:scalajs-dom_sjs1_3:2.2.0"

  scalajs "org.scala-js:scalajs-linker_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-sbt-test-adapter_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-env-jsdom-nodejs_$scala2versionMinor:1.1.0"
  
  testImplementation "org.scala-js:scalajs-test-bridge_$scala2versionMinor:$scalaJsVersion"

  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.12"
}
```

And - with only the required dependencies:
```groovy
final String scalaVersion       = '3.1.3'

dependencies {
  implementation "org.scala-lang:scala3-library_3:$scalaVersion"
  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.12"
}
```

Example with explicit dependencies for Scala 2:
```groovy
final String scalaVersion       = '2.13.8'
final String scala2versionMinor = '2.13'
final String scalaJsVersion     = '1.10.1'

dependencies {
  implementation "org.scala-lang:scala-library:$scalaVersion"
  implementation "org.scala-js:scalajs-library_$scala2versionMinor:$scalaJsVersion"
  implementation "org.scala-js:scalajs-dom_sjs1_3:2.2.0"
  
  scalajs "org.scala-js:scalajs-linker_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-sbt-test-adapter_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-env-jsdom-nodejs_$scala2versionMinor:1.1.0"
  
  scalaCompilerPlugins "org.scala-js:scalajs-compiler_$scalaVersion:$scalaJsVersion"

  testImplementation "org.scala-js:scalajs-test-bridge_$scala2versionMinor:$scalaJsVersion"

  // for ScalaTest tests:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.12"
}
```

And - with only the required dependencies:
```groovy
final String scalaVersion       = '2.13.8'
final String scala2versionMinor = '2.13'

dependencies {
  implementation "org.scala-lang:scala-library:$scalaVersion"
  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_$scala2versionMinor:3.2.12"
}
```

## ScalaJS Linker ##

Following the tradition created by the sbt ScalaJS plugin, two flavours of linker configuration
('stages') are supported:
`FastOpt` (fast linking, normally used during development) and
`FullOpt` (fully optimized linking, better suited for production).

Fully optimized linking:
- uses `Semantics.optimized`;
- enables `checkIR`;
- enables Closure Compiler (unless `moduleKind` is set to `ESModule`).

## Node ##

For running the code and tests, NodeJS has to be installed.
Plugin assumes that the `jsdom` module is installed.
Source map should be enabled for better traces.

```shell
$ npm install source-map-support
$ npm init private
$ npm install jsdom
```

## Tasks ##

### Linking ###

Plugin adds tasks for ScalaJS linking of the main code: 
`sjsLinkFastOpt`, `sjsLinkFullOpt` and `sjsLink`.
The ones with the suffix configure use the corresponding flavour of the linker configuration;
the one without the suffix uses the flavour configured by the plugin extension.

Each of those tasks depends on the `classes` task to produce the `class` *and* `sjsir` files.

Each of the tasks exposes a property `JSDirectory` that points to a directory
with the resulting JavaScript, so that it can be copied where needed.
For example:

```groovy
sjsLinkFastOpt.doLast {
  project.sync {
    from sjsLinkFastOpt.JSDirectory
    into jsDirectory
  }
}
```

Plugin adds corresponding tasks for linking the test code:
`sjsLinkTestFastOpt`, `sjsLinkTestFullOpt` and `sjsLinkTest`.
Those tasks are used by the testing tasks tha the plugin adds,
but are unlikely to be of interest on their own.

### Running ###

Plugin adds tasks for running the main code (if it is an application and not a library):
`sjsRunFastOpt`, `sjsRunFullOpt` and `sjsRun`.
Each of those tasks depend on corresponding `sjsLink*` task.

### Testing ###

Plugin adds tasks for running the tests:
`sjsTestFastOpt`, `sjsTestFullOpt` and `sjsTest`.
Each of those tasks depend on corresponding `sjsLinkTest*` task.


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

## Credits ##

- Stack Overflow [answer](https://stackoverflow.com/a/65777102/670095)
by [gzm0](https://stackoverflow.com/users/1149944/gzm0) was
*extremely* helpful. Thanks!
- [ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin)
- [ScalaJS Tutorial](https://www.scala-js.org/doc/tutorial/basic/)
- [ScalaJS CLI](https://github.com/scala-js/scala-js-cli/tree/main/src/main/scala/org/scalajs/cli)
- [ScalaJS Linker](https://github.com/scala-js/scala-js/tree/main/linker-interface)
- [Old ScalaJS Gradle plugin](https://github.com/gtache/scalajs-gradle) by
  [gtache](https://github.com/gtache)
- [Implementing Scala.JS Support for Scala 3](https://www.scala-lang.org/2020/11/03/scalajs-for-scala-3.html)
