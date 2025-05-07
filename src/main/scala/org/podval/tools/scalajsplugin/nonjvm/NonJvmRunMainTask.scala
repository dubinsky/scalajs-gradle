package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.DefaultTask

abstract class NonJvmRunMainTask[L <: NonJvmLinkTask[L]] extends DefaultTask with NonJvmRunTask[L]
