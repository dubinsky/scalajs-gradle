package org.podval.tools.build

final class JavaDependency(
  override val maker: JavaDependencyMaker
) extends SimpleDependency[JavaDependency]
