# Gradle plugin for ScalaJS #

## Summary ##

This is a Gradle plugin for working with Scala.js.
It supports linking ScalaJS code, running and testing it.

This plugin also supports testing plain Scala code (no ScalaJS) using sbt-compatible testing frameworks.

Supports ScalaJS 1; default version: 1.18.2.

Node.js has to be installed separately; supports versions that ScalaJS supports: 16 and possibly 17.

Plugin requires Gradle 8.12.

Plugin is written in Scala 3.
Scala 2.12 on the *project* classpath is supported, but not on the *buildScript* classpath;
Gradle plugins or explicit `buildScript` additions that use Scala 2.12 (or earlier)
will break the plugin.

Gradle build file snippets below use the Groovy syntax, not the Kotlin one.

Gradle daemon does not feel changes to the test classes and needs to be stopped for those changes to be reflected
in the build (TODO does this have anything to do with this plugin?). 

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

For years, I used [Gradle ScalaTest plugin](https://github.com/maiflai/gradle-scalatest)
to run my Scala Tests. Thank you, [maiflai](https://github.com/maiflai)!
Since my plugin integrates with Gradle - and through it, with IntelliJ Idea - some of the 
issues that maiflai's plugin has my does not:
[Test events were not received](https://github.com/maiflai/gradle-scalatest/issues/67),
[ASCII Control Characters Printed](https://github.com/maiflai/gradle-scalatest/issues/69).
I never tried an alternative ScalaTest integration 
[scalatest-junit-runner](https://github.com/helmethair-co/scalatest-junit-runner),
and if you need `JUnit5` that is probably a way to go, since my plugin does not support `JUnit5`
(it does support `ScalaJS` though :).


## Applying to a Gradle project ##

Plugin is [published](https://plugins.gradle.org/plugin/org.podval.tools.scalajs)
on the Gradle Plugin Portal. To apply it to a Gradle project:

```groovy
plugins {
  id 'org.podval.tools.scalajs' version '0.4.16'
}
```

Plugin will automatically apply the Scala plugin to the project, so there is no need to manually list
`id 'scala'` in the `plugins` block - but there is no harm in it either.
Either way, it is the responsibility of the project using the plugin to add a standard Scala library
dependency that the Scala plugin requires.

Plugin forces resolution of the `implementation` and `testImplementation` configurations
and some others and must be thus applied *after* any plugins that add dependencies to those configurations.
One such plugin is the Gradle Plugin Portal Publishing Plugin, which applies Gradle Plugin Plugin,
which adds dependencies to configurations.

Unless ScalaJS is disabled, plugin will run in ScalaJS mode.
To disable ScalaJS and use the plugin for testing plain Scala code with sbt-compatible testing frameworks,
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

| Name       | group:artifact                   | Notes                                             |
|------------|----------------------------------|---------------------------------------------------|
| JUnit4     | `com.github.sbt:junit-interface` | brings in `junit:junit`; does not support ScalaJS |
| mUnit      | `org.scalameta:munit_3`          | brings in `junit:junit`; does support ScalaJS?!   |
| ScalaCheck | `org.scalacheck:scalacheck_3`    |                                                   |
| ScalaTest  | `org.scalatest:scalatest_3`      |                                                   |
| Specs2     | `org.specs2:specs2-core_3`       |                                                   |
| uTest      | `com.lihaoyi:utest_3`            |                                                   |
| ZIO Test   | `dev.zio:zio-test-sbt_3`         | tests are `objects`, not `classes`                |

For ScalaJS, artifact is suffixed with `_sjs1`; for instance, `org.scalatest:scalatest_sjs1_3` instead of
`org.scalatest:scalatest_3`.

For Scala 2.13, use `_2.13` artifacts instead of the `_3` ones; for Scala 2.12 - `_2.12`.

Multiple test frameworks can be used at the same time.

Test task added by the plugin is derived from the normal Gradle `test` task, and can be configured
in the traditional way; currently, not all configuration properties are honored.

Plugin introduces its own Gradle test framework: `useSbt`.
Plugin auto-applies this Gradle test framework to each test task.
Re-configuring the Gradle test framework (via `useJUnit`, `useTestNG` or `useJUnitPlatform`) is not supported.

File-name based test scan is not supported by this plugin;
`isScanForTestClasses` must be at its default value `true`.

ScalaJS tests are run sequentially; Scala tests are forked/parallelized in accordance with the forking options.

Class inclusion/exclusion filters are honored, but method-name-based filtering does not work,
since in frameworks like ScalaTest individual tests are not methods.

Tests can be filtered by tags, for example:
```groovy
test {
  useSbt {
    includeCategories = ['org.scalatest.tags.Slow']
    excludeCategories = ['com.mycompany.tags.DbTest', 'com.mycompany.tags.RequiresDb']
  }
}
```

If there is a need to have test runs with different configuration, more testing tasks can be added manually.

For plain Scala projects (no ScalaJS), the type of the test task is `org.podval.tools.testing.TestTaskScala`.
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
this message can be suppressed by setting `test.filter.failOnNoMatchingTests = false`.

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

Plugin does this automatically unless a dependency on `scalajs-compiler` is declared explicitly.

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

Plugin is compiled against a specific version of Zinc, but at runtime uses Zinc
configured in the Scala plugin.

If you declare a `scalajs-library` dependency explicitly, plugin chooses the same
version for the ScalaJS dependencies it adds
(`scalajs-linker`, `scalajs-sbt-test-adapter`, `scalajs-test-bridge`, `scalajs-compiler`).

Plugin uses version 2.8.0 of the ScalaJS DOM library (`scalajs-dom`) - unless a different version
is configured explicitly.

Example with all dependencies listed for Scala 3:
```groovy
final String scalaVersion       = '3.6.3'
final String scala2versionMinor = '2.13'
final String scalaJsVersion     = '1.18.2'

dependencies {
  implementation "org.scala-lang:scala3-library_3:$scalaVersion"
  implementation "org.scala-lang:scala3-library_sjs1_3:$scalaVersion"
  implementation "org.scala-js:scalajs-library_$scala2versionMinor:$scalaJsVersion"
  implementation "org.scala-js:scalajs-dom_sjs1_3:2.8.0"

  scalajs "org.scala-js:scalajs-linker_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-sbt-test-adapter_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-env-jsdom-nodejs_$scala2versionMinor:1.1.0"
  
  testImplementation "org.scala-js:scalajs-test-bridge_$scala2versionMinor:$scalaJsVersion"

  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.19"
}
```

And - with only the required dependencies:
```groovy
final String scalaVersion       = '3.6.3'

dependencies {
  implementation "org.scala-lang:scala3-library_3:$scalaVersion"
  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.19"
}
```

Example with explicit dependencies for Scala 2:
```groovy
final String scalaVersion       = '2.13.14'
final String scala2versionMinor = '2.13'
final String scalaJsVersion     = '1.18.2'

dependencies {
  implementation "org.scala-lang:scala-library:$scalaVersion"
  implementation "org.scala-js:scalajs-library_$scala2versionMinor:$scalaJsVersion"
  implementation "org.scala-js:scalajs-dom_sjs1_3:2.8.0"
  
  scalajs "org.scala-js:scalajs-linker_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-sbt-test-adapter_$scala2versionMinor:$scalaJsVersion"
  scalajs "org.scala-js:scalajs-env-jsdom-nodejs_$scala2versionMinor:1.1.0"
  
  scalaCompilerPlugins "org.scala-js:scalajs-compiler_$scalaVersion:$scalaJsVersion"

  testImplementation "org.scala-js:scalajs-test-bridge_$scala2versionMinor:$scalaJsVersion"

  // for ScalaTest tests:  
  testImplementation "org.scalatest:scalatest_sjs1_3:3.2.19"
}
```

And - with only the required dependencies:
```groovy
final String scalaVersion       = '2.13.14'
final String scala2versionMinor = '2.13'

dependencies {
  implementation "org.scala-lang:scala-library:$scalaVersion"
  // a test framework:  
  testImplementation "org.scalatest:scalatest_sjs1_$scala2versionMinor:3.2.19"
}
```

### Node ###

For running the code and tests, Node.js has to be available.

In ScalaJS mode, plugin adds `node` extension to the project.
This extension can be used to specify the version of Node.js to use and Node modules to install.
This extension can be retrieved by calling `NodeExtension.get(project)`.

If `build.gradle` specifies a version of Node.js to be used via `node.version="..."`,
plugin will install the specified version under `~/.gradle/nodejs` and use it;
if version of Node.js is not specified, "ambient" Node.js will be used.

Plugin adds tasks `node` and `npm` for executing `node` and `npm` commands using the same version of Node.js;
those tasks can be used from the command line like this:
```shell
./gradlew npm --npm-arguments 'version'
./gradlew node --node-arguments '...'
 ```

ScalaJS does not support versions of Node.js newer than "16.19.1", so none of the "17.9.1", "18.15.0", "19.8.1".
I do not know anything about Node.js, and find this surprising - but I am sure there is a good
technical or political reason for this ;)

Node modules for the project are in the `node_modules` directory in the project root.

Before executing `run` or `test` tasks, if `package.json` file does not exist, plugin runs `npm init private`.

Plugin installs `jsdom` Node modules required for `org.scala-js:scalajs-env-jsdom-nodejs`

Plugin also installs any modules specified in the `node.modules = [..., ...]`.
To get better traces, one can add `source-map-support` module.


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

To figure out how `sbt` itself integrates with testing frameworks, I had to untangle some `sbt` code, including:
- `sbt.Defaults`
- `sbt.Tests`
- `sbt.TestRunner`
- `sbt.ForkTests`
- `org.scalajs.sbtplugin.ScalaJSPluginInternal`

Turns out, internals of `sbt` are a maze of twisted (code) passages, all alike, where pieces of
code are stored in key-value maps, and addition of such maps is used as an override mechanism.
What a disaster!

Just being able to run the tests with no integration with Gradle or IntelliJ Idea seemed
suboptimal, so I decided to look into proper integrations of things like
`org.scala-js:scalajs-sbt-test-adapter` and [`org.scala-sbt:test-interface`](https://github.com/sbt/test-interface).

I perused code from:
- [Gradle](https://github.com/gradle/gradle);
- [IntelliJ Idea](https://github.com/JetBrains/intellij-community);
- [Gradle ScalaTest plugin](https://github.com/maiflai/gradle-scalatest).

This took _by far_ the most of my time (and takes up more than 3/4 of the plugin code), and uncovered a number of surprises.

#### sbt test interface
sbt's testing interface is supported by a number of test frameworks, and once I had
a Gradle/Idea integration with it in ScalaJS context, it was reasonably easy to re-use it
to run tests on sbt-compatible frameworks **without** any ScalaJS involved - in plain Scala projects.

There are **two** testing interfaces in `org.scala-sbt:test-interface:1.0`;
I use the one used by the ScalaJS sbt plugin - presumably the new one ;)

#### Dynamic Dependencies
I had to develop an approach to add dependencies dynamically,
with correct versions and built for correct version of Scala which may be different from the one
plugin uses (so that Scala 2.12 can be supported).

#### Running in IntelliJ Idea
IntelliJ Idea instruments Gradle test task with its `IJTestEventLogger` - but *only* if the task is of type
`org.gradle.api.tasks.testing.Test`. Since I must derive my test task from `Test`,
and `Test` extends `org.gradle.process.JavaForkOptions`, my test task runs in a forked JVM,
making debugging of my code difficult.

#### Proxying Test Events
Turns out that IntelliJ Idea integration only works when all the calls to
the IJ listener happen from the same thread
(it probably uses some thread-local variable to set up cross-process communications).
Since some of the calls are caused by the call-back from the sbt testing interface's
event handler, I get "Test events were not received" in the Idea test UI.
It would have been nice if this fact was documented somewhere :(
I coded an event queue with its own thread, but then discovered that:
- Gradle provides a mechanism that ensures that all the calls are made from the same thread: Actor.createActor().getProxy();
- when tests are parallelized, MaxNParallelTestClassProcessor is used, which already does that, so I do not need to.
I packaged adding Gradle's proxying as a `SingleThreddingTestResultProcessor` - but somehow thing work now even without it...

#### Additional Test Information
sbt-based test discovery produces more information than just the class name:
- fingerprint
- selectors
- framework that recognized the test (supporting multiple testing frameworks in the same project
  probably is not a critical requirement, but sbt does it, so I must too ;)

When tests are parallelized, I do not want to read the compiler analysis file in every test worker
and fish for this information again. For a while, I used modified serializer to get additional information
obtained during test discovery to the worker; of course, serializer is hard-coded in the Gradle code,
so to use mine I had to modify three Gradle files...
I even made a [pull request](https://github.com/gradle/gradle/pull/24088) to add flexibility
in this regard to Gradle.
But then I realized that I can encode additional information I need to get to the worker in the test class name!
So now there is only one Gradle file that I need to modify: `DefaultTestExecuter`.
Modification needed is - not to fork the JVM when running ScalaJS tests (they have to run in the same JVM
where the test frameworks were loaded).

#### Test Ids and Serialization
`org.gradle.internal.remote.internal.hub.DefaultMethodArgsSerializer` 
seems to make a decision which serializer registry to use based on the
outcome of the `SerializerRegistry.canSerialize()` call
for the class of the first parameter of a method;
test id is the first parameter of the `TestResultProcessor.output()`, `completed()` and `failure()` calls.
Without some hackery like registering a serializer for `AnyRef` and disambiguating
in the `SerializerRegistry.build()` call,
neither `null` nor `String` are going to work as ids.

This is probably the reason why Gradle:
- makes all test ids `CompositeIdGenerator.CompositeId`
- registers a `Serializer[CompositeIdGenerator.CompositeId]` in `TestEventSerializer`.

Gradle just wants to attract attention to its `TestEventSerializer`, so it registers
serializers for the types of the first parameters of all methods - including the test ids ;)

And since the minimum of composed is two, Gradle uses test ids that are composite of two Longs.

AbstractTestTask installs `StateTrackingTestResultProcessor`
which keeps track of all tests that are executing in all `TestWorker`s.
That means that test ids must be scoped per `TestWorker`.
Each `TestWorker` has an `idGenerator` which it uses to generate `WorkerTestClassProcessor.workerSuiteId`;
that same `idGenerator` can be used to generate sequential ids for the tests in the worker,
satisfying the uniqueness requirements - and resulting in the test ids always being
a composite of exactly two *Longs*!

Because tests are scoped by the workers, it does not seem possible to group test results by framework.

#### Testing the Tests

I coded a neat way to test the plugin itself and 
various features of the various frameworks and their support by the plugin:
Fixture, Feature, ForClass, GroupingFuncSpec, Platform, SourceFile, TestProject.

#### Framework Peculiarities
JUnit4 (and MUnit which seems to be based on JUnit4) report incorrect class and method names for test method events:
both are `<class name>.<method name>`; method names like this just look stupid, but class names look
like new classes to Gradle, so test report is corrupted. I had to work around it.

MUnit (but not JUnit4!) and UTest write to standard output/error instead of logging via supplied sbt logger,
so their output does not go through my `TestClassProcessor.output()`;
do I need to modify capturing to get their output?

Comment on the JupiterTestFingerprint.annotationName() says:
> return The name of this class. This is to ensure that SBT does not find
> any tests so that we can use JUnit Jupiter's test discovery mechanism.

Well, mission accomplished: my test scanner does not find any tests, and since
I have no idea what "JUnit Jupiter's test discovery mechanism" is,
I get the Gradle message "No tests found for given includes".
So, no JUnit5 support for now :(

I *might* try to use framework-specific test discovery instead of the Scala Analysis one in the Scala-only setting,
but it is not a priority :)

ScalaCheck processes test *methods* as nested tasks; other frameworks just run them and report the results
via event handler. UTest uses `NestedTestSelector` for this, while others use `TestSelector`.
ScalaCheck reports test suite completion via event handler, unlike others ;)
ScalaTest does not return nested tasks for nested suites (or anything, according to the documentation
of its Runner); events for the tests in the nested suites have `NetsedTestSelector`.

When tagging classes used for inclusion/exclusion are not available, MUnit crashes with a `ClassNotFound` -
but `ScalaTest` does not.

#### TODO

How is MUnit, that is JUnit4-based, supported by ScalaJS - and JUnit4 itself is not?

Test test filtering. For example, why supplying `--tests "*"` is NOT the same as not supplying any?

Document what Test configuration methods are honoured.

Document inability to debug ScalaJS code or tests.

Generate the framework table programmatically.

Fine-tune the stack traces.

from https://github.com/scalatest/scalatest/blob/main/jvm/core/src/main/scala/org/scalatest/tools/Framework.scala#L267
> selectors will always at least have one SuiteSelector, according to javadoc of TaskDef

and:
> In new Framework API, it is now a specified behavior that Framework's runner method will be called
> to get a Runner instance once per project run.

According to the Runner documentation (?), summary returned was already sent to the logger? Runner.done():
> The test framework may send a summary (i.e., a message giving total tests succeeded, failed, and so on)
> to the user via a log message. If so, it should return the summary from done.
> If not, it should return an empty string.
> The client may use the return value of done to decide whether to display its own summary message.

Relax the restrictions on the plugin application order.

I may want to try replacing AnalysisDetector with reading the class files.

When I apply the plugin to `opentorah-util`, I get:
> Could not generate a decorated class for type ScalaJSPlugin.
> Class Not Found: org/opentorah/build/DependencyVersion

Clean up adding the plugin classes to the worker's classpath (and possibly reflective access to the
implementation classpath).

Add new tags to the plugin's portal page: manually at https://github.com/gradle/plugin-portal-requests !

Look at Gradle's new test hotness: JVM test suite plugin.

#### Test Tagging

- ScalaTest:
  - tests are taggable, but not suites?
  - test exclusion works, test inclusion does not: nothing runs
- ScalaCheck:
  - no tagging

#### TestDescriptor hierarchy

```scala
org.gradle.api.tasks.testing.TestDescriptor
  org.gradle.api.internal.tasks.testing.TestDescriptorInternal     // adds id
    org.gradle.api.internal.tasks.testing.DecoratingTestDescriptor // attaches parent
      // above is used by org.gradle.api.internal.tasks.testing.results.StateTrackingTestResultProcessor
      // and org.gradle.api.internal.tasks.testing.logging.TestWorkerProgressListener
      // set up in org.gradle.api.tasks.testing.AbstractTestTask
    org.gradle.api.internal.tasks.testing.TestDescriptorInternal.UnknownTestDescriptor
      // above is used by org.gradle.api.internal.tasks.testing.results.StateTrackingTestResultProcessor
      // in cases that should not happen
    org.gradle.api.internal.tasks.testing.AbstractTestDescriptor   // getParent -> null
      org.gradle.api.internal.tasks.testing.DefaultTestDescriptor
        // above is used by org.gradle.api.internal.tasks.testing.junit.JUnitTestEventAdapter
        // and org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestExecutionListener
        org.gradle.api.internal.tasks.testing.DefaultTestMethodDescriptor
          // above is used by org.gradle.api.internal.tasks.testing.testng.TestNGTestResultProcessorAdapter
      org.gradle.api.internal.tasks.testing.DefaultTestSuiteDescriptor
        // above is used by org.gradle.api.internal.tasks.testing.testng.TestNGTestResultProcessorAdapter
        org.gradle.api.internal.tasks.testing.processors.TestMainAction.RootTestSuiteDescriptor
        org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor.WorkerTestSuiteDescriptor
        org.gradle.api.internal.tasks.testing.DefaultNestedTestSuiteDescriptor
          // above is used in org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestExecutionListener
        org.gradle.api.internal.tasks.testing.DefaultTestClassDescriptor
          // above is used in
          // org.gradle.api.internal.tasks.testing.logging.TestWorkerProgressListener
          // org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestExecutionListener
          // org.gradle.api.internal.tasks.testing.junit.TestClassExecutionEventGenerator
          // org.gradle.api.internal.tasks.testing.testng.TestNGTestResultProcessorAdapter
```
