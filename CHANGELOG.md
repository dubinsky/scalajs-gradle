# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.2] - 2022-06-21
- running the linked code;
- running tests;

**BREAKING CHANGES:**
- task names changed
- configuration property `scalajs.outputDirectory` removed

Plugin:
- split plugin classes;
- stage configuration;
- distinct output directories for main/test, fast/full optimization;
- write linker report to disk;
- tasks for linking of the test code;
- depend on opentorah-util;
- depend on org.scala-js:scalajs-env-jsdom-nodejs for running on Node;
- tasks for running the JavaScript code on Node;
- dependencies and Gradle updated;

Testing ScalaJS:
- depend on org.scala-js:scalajs-sbt-test-adapter;
- depend on org.scala-sbt:test-interface;
- depend on org.scala-sbt:compiler-interface;
- depend on org.scala-sbt:zinc (persist, core, apiinfo);
- glue code inspired by a few classes from sbt (org.scala-sbt.testing, org.scala-sbt.actions);
- tasks for testing;

ScalaJS tutorial:
- added sbt-based project based on it;
- updated its dependencies;
- switched to Scala 3;
- switched to ScalaTest;
- added Gradle setup to it, using the plugin via composite build;
- depend on org.scala-js:scalajs-dom_sjs1;
- depend on org.scalatest:scalatest_sjs1 for tests;
- depend on org.scala-js:scalajs-test-bridge (something the plugin should add automatically);

## [0.0.1] - 2022-05-12
- first release;
- basic functionality (ScalaJS linker);
