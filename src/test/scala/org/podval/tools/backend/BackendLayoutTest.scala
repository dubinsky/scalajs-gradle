package org.podval.tools.backend

import org.podval.tools.build.Backend
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BackendLayoutTest extends AnyFlatSpec, Matchers:
  "backend directories" should "be the three source roots and the valid shares" in:
    BackendLayout.isBackendDirectory("js") shouldBe true
    BackendLayout.isBackendDirectory("jvm") shouldBe true
    BackendLayout.isBackendDirectory("native") shouldBe true
    BackendLayout.isBackendDirectory("shared") shouldBe true
    BackendLayout.isBackendDirectory("js-native") shouldBe true
    BackendLayout.isBackendDirectory("shared-jvm-js") shouldBe true
    BackendLayout.sharedBackends("js-native") shouldBe Some(Set(ScalaJSBackend, ScalaNativeBackend))
    BackendLayout.sharedBackends("shared") shouldBe Some(Backend.all)

  "invalid share names" should "be left out" in:
    BackendLayout.isBackendDirectory("shared-js") shouldBe false
    BackendLayout.isBackendDirectory("jvm-jvm") shouldBe false
    BackendLayout.isBackendDirectory("js-jvm-native") shouldBe false
    BackendLayout.isBackendDirectory("src") shouldBe false
    BackendLayout.backend("jvm") shouldBe Some(JvmBackend)

  "child paths" should "use the Gradle project path" in:
    ScalaSettingsExtension.childPath(":", "js") shouldBe ":js"
    ScalaSettingsExtension.childPath(":core", "js") shouldBe ":core:js"
