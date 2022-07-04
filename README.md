# Gradle plugin for ScalaJS #

## Summary ##

This is a Gradle plugin for working with Scala.js.
It supports linking ScalaJS code, running and testing it.
It also supports testing normal Scala code (no ScalaJS) using sbt-compatible testing frameworks.

## Motivation ##

I dislike untyped languages, so if I have to write Javascript,
I want to be able to do it in Scala.
Thanks to [Scala.js](https://www.scala-js.org/), this is possible.

I [dislike](http://dub.podval.org/2011/11/08/sbt-why.html) [sbt](https://www.scala-sbt.org/) - the [official
build tool](https://www.scala-js.org/doc/project/) of Scala.js,
which uses [ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin).
I want to be able to use my preferred build tool - [Gradle](https://gradle.org/).

Existing Scala.js Gradle [plugin](https://github.com/gtache/scalajs-gradle) by
[gtache](https://github.com/gtache) seems to be no longer maintained.

Hence, this plugin.

It so transpired that the plugin supports running tests using sbt-compatible testing frameworks
in a way integrated with Gradle and IntelliJ Idea **with or without** ScalaJS.
For plain Scala projects, ScalaJS support can be disabled.

## Applying to a Gradle project ##

Plugin is [published](https://plugins.gradle.org/plugin/org.podval.tools.scalajs)
on the Gradle Plugin Portal. To apply it to a Gradle project:

```groovy
plugins {
  id 'org.podval.tools.scalajs' version '0.1.0'
}
```

Plugin will automatically apply the Scala plugin to the project, so there is no need to manually list
`id 'scala'` in the `plugins` block - but there is no harm in it either.
Either way, it is the responsibility of the project using the plugin to add a standard Scala library
dependency that the Scala plugin requires.

Plugin uses Zinc internally.
It is compiled against a specific version of Zinc, but at runtime uses Zinc
that the Scala plugin configured.

Unless ScalaJS is disabled, plugin will run in ScalaJS mode.
To disable ScalaJS and use the plugin for testing normal Scala code with sbt-compatible testing frameworks,
put the following into the `gradle.properties` file of the project:
```properties
org.podval.tools.scalajs.disabled=true
```

In addition, the *presence* on the [Gradle ScalaTest plugin](https://github.com/maiflai/gradle-scalatest)'s `mode`
property also disables ScalaJS:
```properties
com.github.maiflai.gradle-scalatest.mode = ...
```
(The *value* of the `mode` property is ignored.)
This way, this plugin can be used as a drop-in replacement for
the ScalaTest one ;)  

## Testing ##

Test runs are integrated with Gradle:
- test counts are logged;
- test reports are generated;
- test framework output is logged at an appropriate level.

Test runs are integrated with IntelliJ Idea:
- test counts are displayed;
- tree of tests run with their outcome is displayed;
- colours are suppressed in the framework output.

Plugin replaces the `test` task with one that supports running sbt-compatible test frameworks.
At least one such framework needs to be added to the `testImplementation` configuration.
In the ScalaJS mode, that dependency needs to be a ScalaJS one.

ScalaTest provides both flavours:
- Scala 3, no ScalaJS: `org.scalatest:scalatest_3:3.2.12`
- Scala 2, no ScalaJS: `org.scalatest:scalatest_2.13:3.2.12`
- Scala 3, with ScalaJS: `org.scalatest:scalatest_sjs1_3:3.2.12`
- Scala 2, with ScalaJS: `org.scalatest:scalatest_sjs1_2.13:3.2.12`

Test task added by the plugin is derived from the normal Gradle `test` task, and can be configured
in the traditional way; currently, not all configuration properties are honored.

Whatever Gradle-recognized test framework is applied (JUnit4, JUnit5, TestNG) will be fed appropriate data
to produce test report. All other framework-specific configuration properties are ignored.

All properties regulating parallelism of the test-running are ignored; tests are run sequentially.

Class inclusion/exclusion filters are honored, but method-name-based filtering does not work,
since in frameworks like ScalaTest individual tests are not methods.

Re-running previously failed tests is currently not supported.

If there is a need to have test runs with different configuration, more testing tasks can be added manually.

For plain Scala projects (no ScalaJS), the type of the test task is `org.podval.tools.test.ScalaTestTask`.
Any such task will automatically depend on the `testClasses` task (and `testRuntimeClassPath`).

For ScalaJS projects the type is `org.podval.tools.scalajs.Test`.
Such test tasks has to depend on a `TestLink` task. The `test` task added by the plugin does it automatically;
for manually added tasks this dependency has to be added manually.

## ScalaJS ##

Ths section applies only when ScalaJS support is enabled.

### ScalaJS compiler ###

To support ScalaJS, Scala compiler needs to be configured to produce both the `class` *and* `sjsir` files.

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

*Note:* Such code is not needed, since Gradle Scala plugin already does this.

### Dependencies ###

Plugin uses some dependencies internally:
- ScalaJS linker;
- ScalaJS test adapter;
- ScalaJS JSDOM Node environment.

For Scala 2, ScalaJS compiler plugin is needed.

Plugin also needs some dependencies on the runtime classpath:
- Scala standard library compiled into ScalaJS (only for Scala 3);
- ScalaJS library;
- ScalaJS DOM library;
- ScalaJS test bridge.

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

### Node ###

For running the code and tests, NodeJS has to be installed.
Plugin assumes that the `jsdom` module is installed.
Source map should be enabled for better traces.

```shell
$ npm install source-map-support
$ npm init private
$ npm install jsdom
```

### Linking ###

For linking of the main code, plugin adds `link` task of type `org.podval.tools.scalajs.Link.Main`.
All tasks of this type automatically depend on the `classes` task.

For linking of the test code, plugin adds `linkTest` task of type `org.podval.tools.scalajs.Link.Test`.
All tasks of this type automatically depend on the `testClasses` task.

Each of the tasks exposes a property `JSDirectory` that points to a directory
with the resulting JavaScript, so that it can be copied where needed.
For example:

```groovy
link.doLast {
  project.sync {
    from link.JSDirectory
    into jsDirectory
  }
}
```

Link tasks have a number of properties that can be used to configure linking.
Configurable properties with their defaults are:

```groovy
link {
  optimization     = 'Fast'          // one of: 'Fast', 'Full'
  moduleKind       = 'NoModule'      // one of: 'NoModule', 'ESModule', 'CommonJSModule'
  moduleSplitStyle = 'FewestModules' // one of: 'FewestModules', 'SmallestModules'
  prettyPrint      = false
}
```

Setting `optimization` to `Full`:
- uses `Semantics.optimized`;
- enables `checkIR`;
- enables Closure Compiler (unless `moduleKind` is set to `ESModule`).

For `Link.Main` tasks, a list of module initializers may also be configured:

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

Name of the module initializer ('module1' in the example above) is ignored (but should not be: TODO).

### Running ###

Plugin adds `run` task for running the main code (if it is an application and not a library).
The task automatically depends on the `link` task.

Additional tasks of type `org.podval.tools.scalajs.Run` can be added manually;
their dependency on a corresponding `Link.Main` task must be set manually too.


## Notes and Credits ##

### Linking ###

It is reasonably easy (if repetitive) to configure the Scala compiler and add needed ScalaJS dependencies by hand;
what really pushed me to make this plugin is the difficulty and ugliness involved in
manually setting up ScalaJS linking in a Gradle script.

A Stack Overflow [answer](https://stackoverflow.com/a/65777102/670095)
by [gzm0](https://stackoverflow.com/users/1149944/gzm0) was *extremely* helpful
for understanding how the ScalaJS linker should be called. Thanks!

I also looked at
- [ScalaJS Tutorial](https://www.scala-js.org/doc/tutorial/basic/)
- [ScalaJS Linker](https://github.com/scala-js/scala-js/tree/main/linker-interface)
  [ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin)
- [Old ScalaJS Gradle plugin](https://github.com/gtache/scalajs-gradle) by [gtache](https://github.com/gtache)
- [ScalaJS CLI](https://github.com/scala-js/scala-js-cli/tree/main/src/main/scala/org/scalajs/cli)
- [Implementing Scala.JS Support for Scala 3](https://www.scala-lang.org/2020/11/03/scalajs-for-scala-3.html)

### sbt ###

Basic testing functionality was [requested](https://github.com/dubinsky/scalajs-gradle/issues/7#issue-1256908039)
by [zstone1](https://github.com/zstone1) - thanks for the encouragement!

To figure out how sbt itself integrates with testing frameworks, I had to untangle some sbt code, including:
- sbt.Defaults
- org.scalajs.sbtplugin.ScalaJSPluginInternal
- sbt.Tests
- sbt.TestRunner

Turns out, internals of sbt are a maze of twisted (code) passages, all alike, where pieces of
code are stored in key-value maps, and addition of such maps is used as an override mechanism.
What a disaster!

### Testing ###

Just being able to run the tests with no integration with Gradle or IntelliJ Idea seemed
suboptimal, so I decided to look into proper integrations of things like
`org.scala-js:scalajs-sbt-test-adapter` and `org.scala-sbt:test-interface`.

I perused code from:
- [Gradle](https://github.com/gradle/gradle);
- [IntelliJ Idea](https://github.com/JetBrains/intellij-community);
- [Gradle ScalaTest plugin](https://github.com/maiflai/gradle-scalatest).

This took by far the most of my time, and uncovered a number of surprises:
- IntelliJ Idea instruments Gradle test task with its `IJTestEventLogger` - but *only* if the task is of type
 `org.gradle.api.tasks.testing.Test`. Since I must derive my test task from `Test`,
  and `Test` extends `org.gradle.process.JavaForkOptions`, my test task runs in a forked JVM,
  making debugging of my code difficult (and there seems to be no way to stop the forking).
- IntelliJ Idea integration works only if all the events are generated by the same thread (!),
  and since sbt's testing interface uses callback to generate some events, I had to code
  an event queue with its own thread.
- sbt's testing interface is supported by a number of test frameworks, and once I had
  a Gradle/Idea integration with it in ScalaJS context, it was reasonably easy to re-use it
  to run tests on sbt-compatible frameworks *without* any ScalaJS involved - in plain Scala projects.


## TODO ##

- try more (and add more) sbt-compatible test frameworks to shake out wrong assumptions about the
  (not very well documented) sbt testing interface and test selectors;
- try more complex test setups with suites and figure out what "nested tasks" are;
- look into test tags;
- improve filtering by test names (and enumerate the methods for frameworks that use test methods);
- use (a wrapper for) `org.gradle.api.internal.tasks.testing.TestFramework` to deepen
  the Gradle integration and regain parallel test execution;
- look at the https://github.com/helmethair-co/scalatest-junit-runner;
- apply SourceMapper to the exceptions during running the ScalaJS code - if I can intercept them;

Compare and contrast wit the ScalaTest Gradle plugin:
- https://github.com/maiflai/gradle-scalatest/issues/69
- https://github.com/maiflai/gradle-scalatest/issues/67
