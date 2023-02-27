## TestClassProcessor

```
org.podval.tools.testing.task.DefaultTestExecuter.execute:
  org.gradle.api.internal.tasks.testing.processors.PatternMatchTestClassProcessor
  org.gradle.api.internal.tasks.testing.processors.RunPreviousFailedFirstTestClassProcessor
  org.gradle.api.internal.tasks.testing.processors.MaxNParallelTestClassProcessor
  org.gradle.api.internal.tasks.testing.processors.RestartEveryNTestClassProcessor
  
org.podval.tools.testing.task.TestFramework.createTestExecuter.createTestClassProcessor:
  org.podval.tools.testing.processors.TaskDefTestEncodingTestClassProcessor                    or NonForkingTestClassProcessor
  org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor

...

  org.gradle.api.internal.tasks.testing.worker.WorkerTestClassProcessor
```

## TestResultProcessor

```
org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor:
  org.gradle.api.internal.tasks.testing.results.AttachParentTestResultProcessor  
  org.gradle.api.internal.tasks.testing.processors.CaptureTestOutputTestResultProcessor

...

org.podval.tools.testing.task.TestFramework.createTestExecuter.execute:
  org.podval.tools.testing.results.FixUpRootTestOutputTestResultProcessor
  org.podval.tools.testing.results.SourceMappingTestResultProcessor
  org.podval.tools.testing.results.TracingTestResultProcessor
  
org.gradle.api.internal.tasks.testing.processors.TestMainAction:
  org.gradle.api.internal.tasks.testing.results.AttachParentTestResultProcessor  
    
org.gradle.api.tasks.testing.AbstracTestTask:
  org.gradle.api.internal.tasks.testing.results.StateTrackingTestResultProcessor  
```
