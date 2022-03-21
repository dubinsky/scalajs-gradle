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

## Possible Enhancements ##

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
