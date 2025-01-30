package org.podval.tools.scalajs.js

enum ModuleKind derives CanEqual:
  case NoModule
  case ESModule
  case CommonJSModule
