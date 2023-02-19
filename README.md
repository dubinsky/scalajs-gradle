# Gradle plugin for ScalaJS #

## Summary ##

This is a Gradle plugin for working with Scala.js.
It supports linking ScalaJS code, running and testing it.

This plugin also supports testing plain Scala code (no ScalaJS) using sbt-compatible testing frameworks.

Supports ScalaJS 1; default version: 1.13.0.

NodeJS has to be installed separately; supports versions that ScalaJS supports: 16 and possibly 17.

Requires Gradle 7.6.

Plugin is written in Scala 3.
Scala 2.12 on the *project* classpath is supported, but not on the *buildscript* classpath;
Gradle plugins or explicit `buildScript` additions that use Scala 2.12 (or earlier)
will break the plugin.

Gradle build file snippets below use the Groovy syntax, not the Kotlin one.

Gradle daemon does not feel changes to the test classes and needs to be stopped for those changes to be reflected in the build (TODO does this have anything to do with this plugin?). 

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
  id 'org.podval.tools.scalajs' version '0.4.2'
}
```

Plugin will automatically apply the Scala plugin to the project, so there is no need to manually list
`id 'scala'` in the `plugins` block - but there is no harm in it either.
Either way, it is the responsibility of the project using the plugin to add a standard Scala library
dependency that the Scala plugin requires.

Unless ScalaJS is disabled, plugin will run in ScalaJS mode.
To disable ScalaJS and use the plugin for testing normal Scala code with sbt-compatible testing frameworks,
put the following into the `gradle.properties` file of the project:
```properties
org.podval.tools.scalajs.disabled=true
```

In addition, the *presence* of the [Gradle ScalaTest plugin](https://github.com/maiflai/gradle-scalatest)'s `mode`
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

Currently, the following test frameworks are supported:

| Name       | Scala 3                          | ScalaJS                            | Notes                   |
|------------|----------------------------------|------------------------------------|-------------------------|
| JUnit4     | `com.github.sbt:junit-interface` | not available                      | brings in `junit:junit` |
| ScalaTest  | `org.scalatest:scalatest_3`      | `org.scalatest:scalatest_sjs1_3`   |                         |
| ScalaCheck | `org.scalacheck:scalacheck_3`    | `org.scalacheck:scalacheck_sjs1_3` |                         |
| Specs2     | `org.specs2:specs2-core_3`       | `org.specs2:specs2-core_sjs1_3`    |                         |
| mUnit      | `org.scalameta:munit_3`          | `org.scalameta:munit_sjs1_3`       | brings in `junit:junit` |
| uTest      | `com.lihaoyi:utest_3`            | `com.lihaoyi:utest_sjs1_3`         |                         |

For Scala 2.13, use `_2.13` artifacts instead of the `_3` ones; for Scala 2.12 - `_2.12`.

Multiple test frameworks can be used at the same time.

Test task added by the plugin is derived from the normal Gradle `test` task, and can be configured
in the traditional way; currently, not all configuration properties are honored.

Any Gradle-recognized test framework configured on the test task (JUnit4, JUnit5, TestNG) and its configuration options
are ignored.

ScalaJS tests are run sequentially; Scala tests are forked/parallelized in accordance with the forking options.

Class inclusion/exclusion filters are honored, but method-name-based filtering does not work,
since in frameworks like ScalaTest individual tests are not methods.

If there is a need to have test runs with different configuration, more testing tasks can be added manually.

For plain Scala projects (no ScalaJS), the type of the test task is `org.podval.tools.test.ScalaTestTask`.
Any such task will automatically depend on the `testClasses` task (and `testRuntimeClassPath`).

For ScalaJS projects the type is `org.podval.tools.scalajs.Test`.
Such test tasks have to depend on a `TestLink` task. The `test` task added by the plugin does it automatically;
for manually added tasks this dependency has to be added manually.

### Output ###

Handling of the test events and output is configured in the [`test.testLogging`](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.logging.TestLoggingContainer.html).
Indenting of the output is hard-coded in the [`TestEventLogger.onOutput()`](https://github.com/gradle/gradle/blob/master/subprojects/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/logging/TestEventLogger.java#L63);
addition of the test name and the name of the output stream at the top of each indented batch (output of the same test) is hard-coded in the [`AbstractTestLogger.logEvent()`](https://github.com/gradle/gradle/blob/master/subprojects/testing-base/src/main/java/org/gradle/api/internal/tasks/testing/logging/AbstractTestLogger.java#L51).

None of this applies when running in the IntelliJ Idea: in `addTestListener.groovy`, it [suppresses](https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/java/resources/org/jetbrains/plugins/gradle/java/addTestListener.groovy#L30) the output and error events and [adds](https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/java/resources/org/jetbrains/plugins/gradle/java/addTestListener.groovy#L29) its own test and output listener [`IJTestEventLogger`](https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/resources/org/jetbrains/plugins/gradle/IJTestLogger.groovy) that does no batching, indenting or adding.

Test counts are printed after the run by the `TestCountLogger` - if there are failing tests. IntelliJ Idea also print the counts in its test UI.

If no tests were found (there are none or all were filtered out), 
Gradle outputs an error message "No tests found for given includes";
this message can be suppressed by setting `test.filter.    failOnNoMatchingTests = false`.

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
  scalaCompilerPlugins "org.scala-js:scalajs-compiler_$scalaVersion:1.11.0"
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
- ScalaJS JSDOM Node environment;
- Zinc.

For Scala 2, ScalaJS compiler plugin is needed.

Plugin also needs some dependencies on the runtime classpath:
- Scala standard library compiled into ScalaJS (only for Scala 3);
- ScalaJS library;
- ScalaJS DOM library;
- ScalaJS test bridge.

Plugin adds missing dependencies automatically.

Plugin is compiled against specific versions of ScalaJS and ScalaJS JSDOM Node environment,
but uses the versions configured in the `scalajs` configuration that it creates.

Plugin is compiled against a specific version of Zinc, but at runtime uses the version of Zinc
configured in the Scala plugin.

If you declare a `scalajs-library` dependency explicitly, plugin chooses the same
version for the ScalaJS dependencies it adds
(`scalajs-linker`, `scalajs-sbt-test-adapter`, `scalajs-test-bridge`, `scalajs-compiler`).

Plugin uses version 2.4.0 of the ScalaJS DOM library (`scalajs-dom`) - unless a different version
is configured explicitly.

Example with all dependencies listed for Scala 3:
```groovy
final String scalaVersion       = '3.2.2'
final String scala2versionMinor = '2.13'
final String scalaJsVersion     = '1.11.0'

dependencies {
  implementation "org.scala-lang:scala3-library_3:$scalaVersion"
  implementation "org.scala-lang:scala3-library_sjs1_3:$scalaVersion"
  implementation "org.scala-js:scalajs-library_$scala2versionMinor:$scalaJsVersion"
  implementation "org.scala-js:scalajs-dom_sjs1_3:2.4.0"

  scalajs "org.scala-js:scalajs-linker_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-sbt-test-adapter_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-env-jsdom-nodejs_$scala2versionMinor:1.1.0"
  
  testImplementation "org.scala-js:scalajs-test-bridge_$scala2versionMinor:$scalaJsVersion"

  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.15"
}
```

And - with only the required dependencies:
```groovy
final String scalaVersion       = '3.2.2'

dependencies {
  implementation "org.scala-lang:scala3-library_3:$scalaVersion"
  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.15"
}
```

Example with explicit dependencies for Scala 2:
```groovy
final String scalaVersion       = '2.13.10'
final String scala2versionMinor = '2.13'
final String scalaJsVersion     = '1.11.0'

dependencies {
  implementation "org.scala-lang:scala-library:$scalaVersion"
  implementation "org.scala-js:scalajs-library_$scala2versionMinor:$scalaJsVersion"
  implementation "org.scala-js:scalajs-dom_sjs1_3:2.4.0"
  
  scalajs "org.scala-js:scalajs-linker_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-sbt-test-adapter_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-env-jsdom-nodejs_$scala2versionMinor:1.1.0"
  
  scalaCompilerPlugins "org.scala-js:scalajs-compiler_$scalaVersion:$scalaJsVersion"

  testImplementation "org.scala-js:scalajs-test-bridge_$scala2versionMinor:$scalaJsVersion"

  // for ScalaTest tests:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.15"
}
```

And - with only the required dependencies:
```groovy
final String scalaVersion       = '2.13.10'
final String scala2versionMinor = '2.13'

dependencies {
  implementation "org.scala-lang:scala-library:$scalaVersion"
  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_$scala2versionMinor:3.2.15"
}
```

### Node ###

For running the code and tests, NodeJS has to be installed.
Plugin assumes that the `jsdom` module is installed.
Source map should be enabled for better traces.

```shell
$ npm init private
$ npm install jsdom
$ npm install source-map-support
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
  main { 
    className = '<fully qualified class name>'
    mainMethodName = 'main'
    mainMethodHasArgs = false
  }
  //...
}
```

Name of the module initializer ('main' in the example above) becomes the module id.

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
- [ScalaJS sbt plugin](https://github.com/scala-js/scala-js/tree/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin)
- [Old ScalaJS Gradle plugin](https://github.com/gtache/scalajs-gradle) by [gtache](https://github.com/gtache)
- [ScalaJS CLI](https://github.com/scala-js/scala-js-cli/tree/main/src/main/scala/org/scalajs/cli)
- [Implementing Scala.JS Support for Scala 3](https://www.scala-lang.org/2020/11/03/scalajs-for-scala-3.html)

Support for Scala 2.12 was [requested](https://github.com/dubinsky/scalajs-gradle/issues/9)
by [machaval](https://github.com/machaval) - thanks for the encouragement!

### Testing ###

Basic testing functionality was [requested](https://github.com/dubinsky/scalajs-gradle/issues/7)
by [zstone1](https://github.com/zstone1) - thanks for the encouragement!

To figure out how sbt itself integrates with testing frameworks, I had to untangle some sbt code, including:
- `sbt.Defaults`
- `sbt.Tests`
- `sbt.TestRunner`
- `sbt.ForkTests`
- `org.scalajs.sbtplugin.ScalaJSPluginInternal`

Turns out, internals of sbt are a maze of twisted (code) passages, all alike, where pieces of
code are stored in key-value maps, and addition of such maps is used as an override mechanism.
What a disaster!

Just being able to run the tests with no integration with Gradle or IntelliJ Idea seemed
suboptimal, so I decided to look into proper integrations of things like
`org.scala-js:scalajs-sbt-test-adapter` and [`org.scala-sbt:test-interface`](https://github.com/sbt/test-interface).

I perused code from:
- [Gradle](https://github.com/gradle/gradle);
- [IntelliJ Idea](https://github.com/JetBrains/intellij-community);
- [Gradle ScalaTest plugin](https://github.com/maiflai/gradle-scalatest).

This took by far the most of my time (and takes up more than 3/4 of the plugin code), and uncovered a number of surprises.

sbt's testing interface is supported by a number of test frameworks, and once I had
a Gradle/Idea integration with it in ScalaJS context, it was reasonably easy to re-use it
to run tests on sbt-compatible frameworks **without** any ScalaJS involved - in plain Scala projects.

There are **two** testing interfaces in `org.scala-sbt:test-interface:1.0`;
I use the one used by the ScalaJS sbt plugin - presumably the new one ;)

I had to develop an approach to add dependencies dynamically,
with correct versions and built for correct version of Scala which may be different from the one
plugin uses (so that Scala 2.12 can be supported).

IntelliJ Idea instruments Gradle test task with its `IJTestEventLogger` - but *only* if the task is of type
`org.gradle.api.tasks.testing.Test`. Since I must derive my test task from `Test`,
and `Test` extends `org.gradle.process.JavaForkOptions`, my test task runs in a forked JVM,
making debugging of my code difficult.

Turns out that IntelliJ Idea integration only works when all the calls to
the IJ listener happen from the same thread
(it probably uses some thread-local variable to set up cross-process communications).
Since some of the calls are caused by the call-back from the sbt testing interface's
event handler, I get "Test events were not received" in the Idea test UI.
It would have been nice if this fact was documented somewhere :(
I coded an event queue with its own thread, but then discovered that:
- Gradle provides a mechanism that ensures that all the calls are made from the same thread: Actor.createActor().getProxy();
- when tests are parallelized, MaxNParallelTestClassProcessor is used, which already does that, so I do not need to.

sbt-based test discovery produces more information than just the class name:
- fingerprint
- selectors
- framework that recognized the test (supporting multiple testing frameworks in the same project
  probably is not a critical requirement, but sbt does it, so I must too ;)

When tests are parallelized, I do not want to read the compiler analysis file in every test worker
and fish for this information again; of course, the serializer Gradle installs in the connection to the
worker does not support my needs; it also makes undocumented and not statically checked assumptions
about test ids (always composite, both the scope and id Longs) which I do not want to conform too.

So, I need to use a different serializer.
Of course, both org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor
and org.gradle.api.internal.tasks.testing.worker.TestWorker
hard-code the org.gradle.api.internal.tasks.testing.worker.TestEventSerializer,
and the only way I found to change that was
to copy both classes in whole, translate them into Scala and change one line in each...
