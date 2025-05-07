package org.podval.tools.scalajsplugin.gradle

//import com.google.common.collect.ImmutableList;
//import org.gradle.api.JavaVersion;
//import org.gradle.api.artifacts.Configuration;
//import org.gradle.api.artifacts.ConfigurationContainer;
//import org.gradle.api.attributes.LibraryElements;
//import org.gradle.api.file.Directory;
//import org.gradle.api.file.SourceDirectorySet;
//import org.gradle.api.internal.ConventionMapping;
//import org.gradle.api.internal.GeneratedSubclasses;
//import org.gradle.api.internal.IConventionAware;
//import org.gradle.api.internal.artifacts.configurations.ConfigurationRole;
//import org.gradle.api.internal.artifacts.configurations.ConfigurationRoles;
//import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationContainerInternal;
//import org.gradle.api.internal.artifacts.configurations.RoleBasedConfigurationCreationRequest;
//import org.gradle.api.internal.artifacts.configurations.UsageDescriber;
//import org.gradle.api.internal.file.FileTreeInternal;
//import org.gradle.api.internal.plugins.DslObject;
//import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
//import org.gradle.api.internal.tasks.JvmConstants;
//import org.gradle.api.internal.tasks.compile.CompilationSourceDirs;
//import org.gradle.api.internal.tasks.compile.JavaCompileExecutableUtils;
//import org.gradle.api.internal.tasks.testing.TestExecutableUtils;
//import org.gradle.api.model.ObjectFactory;
//import org.gradle.api.plugins.internal.DefaultJavaPluginConvention;
//import org.gradle.api.plugins.internal.JavaConfigurationVariantMapping;
//import org.gradle.api.plugins.internal.JvmPluginsHelper;
//import org.gradle.api.plugins.internal.NaggingJavaPluginConvention;
//import org.gradle.api.provider.Provider;
//import org.gradle.api.reporting.DirectoryReport;
//import org.gradle.api.reporting.ReportingExtension;
//import org.gradle.api.tasks.Copy;
//import org.gradle.api.tasks.SourceSet;
//import org.gradle.api.tasks.SourceSetContainer;
//import org.gradle.api.tasks.TaskProvider;
//import org.gradle.api.tasks.compile.AbstractCompile;
//import org.gradle.api.tasks.compile.JavaCompile;
//import org.gradle.api.tasks.javadoc.Javadoc;
//import org.gradle.api.tasks.javadoc.internal.JavadocExecutableUtils;
//import org.gradle.api.tasks.testing.JUnitXmlReport;
//import org.gradle.api.tasks.testing.Test;
//import org.gradle.internal.Cast;
//import org.gradle.internal.artifacts.configurations.AbstractRoleBasedConfigurationCreationRequest;
//import org.gradle.internal.deprecation.DeprecatableConfiguration;
//import org.gradle.internal.deprecation.DeprecationLogger;
//import org.gradle.internal.jvm.JavaModuleDetector;
//import org.gradle.jvm.tasks.Jar;
//import org.gradle.jvm.toolchain.JavaToolchainService;
//import org.gradle.jvm.toolchain.JavaToolchainSpec;
//import org.gradle.jvm.toolchain.internal.DefaultToolchainSpec;
//import org.gradle.language.base.plugins.LifecycleBasePlugin;
//import org.gradle.language.jvm.tasks.ProcessResources;
//
//import javax.annotation.Nullable;
//import javax.inject.Inject;
//import java.io.File;
//import java.lang.reflect.Method;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.Callable;
//import java.util.function.BiFunction;
//import java.util.function.Supplier;

import org.gradle.api.Project
import org.gradle.api.internal.provider.PropertyFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.internal.DefaultJavaPluginExtension
import org.gradle.api.plugins.jvm.internal.{JvmLanguageUtilities, JvmPluginServices}

// see org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaBasePlugin as Original

//
///**
// * <p>A {@link org.gradle.api.Plugin} which compiles and tests Java source, and assembles it into a JAR file.</p>
// *
// * This plugin is automatically applied to most projects that build any JVM language source.  It creates a {@link JavaPluginExtension}
// * extension named {@code java} that is used to configure all jvm-related components in the project.
// *
// * It is responsible for configuring the conventions of any {@link SourceSet}s that are present and used by
// * (for example) the Java, Groovy, or Kotlin plugins.
// *
// * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html">Java plugin reference</a>
// */

object JavaBasePlugin:
  def CHECK_TASK_NAME: String = Original.CHECK_TASK_NAME
  def BUILD_NEEDED_TASK_NAME: String = Original.BUILD_NEEDED_TASK_NAME
  def BUILD_DEPENDENTS_TASK_NAME: String = Original.BUILD_DEPENDENTS_TASK_NAME
  def DOCUMENTATION_GROUP: String = Original.DOCUMENTATION_GROUP
  
final class JavaBasePlugin(
  project: Project,
  jvmPluginServices: JvmPluginServices,
  propertyFactory: PropertyFactory,
  getJvmLanguageUtils: JvmLanguageUtilities
):
  private val javaClasspathPackaging: Boolean = java.lang.Boolean.getBoolean(Original.COMPILE_CLASSPATH_PACKAGING_SYSTEM_PROPERTY)
  private def objectFactory: ObjectFactory = project.getObjects

  // TODO uncomment

//  def apply(): Unit =
    // BasePlugin          - nothing to do, everything was already done
    // JvmEcosystemPlugin  - nothing to do, everything was already done
    // ReportingBasePlugin - nothing to do, everything was already done
    // JvmToolchainsPlugin - nothing to do, everything was already done

//    val javaPluginExtension: DefaultJavaPluginExtension = addExtensions(project)
//
//    configureCompileDefaults(project, javaPluginExtension)
//    configureSourceSetDefaults(project, javaPluginExtension)
//    configureJavaDoc(project, javaPluginExtension)
//
//    configureTest(project, javaPluginExtension)
//    configureBuildNeeded(project)
//    configureBuildDependents(project)
//    configureArchiveDefaults(project)

//    @SuppressWarnings("deprecation")
//    private DefaultJavaPluginExtension addExtensions(final Project project) {
//        DefaultToolchainSpec toolchainSpec = objectFactory.newInstance(DefaultToolchainSpec.class);
//        SourceSetContainer sourceSets = (SourceSetContainer) project.getExtensions().getByName("sourceSets");
//        DefaultJavaPluginExtension javaPluginExtension = (DefaultJavaPluginExtension) project.getExtensions().create(JavaPluginExtension.class, "java", DefaultJavaPluginExtension.class, project, sourceSets, toolchainSpec);
//        DeprecationLogger.whileDisabled(() ->
//            project.getConvention().getPlugins().put("java", new NaggingJavaPluginConvention(objectFactory.newInstance(DefaultJavaPluginConvention.class, project, javaPluginExtension))));
//        return javaPluginExtension;
//    }
//
//    private void configureSourceSetDefaults(Project project, final JavaPluginExtension javaPluginExtension) {
//        javaPluginExtension.getSourceSets().all(sourceSet -> {
//
//            ConfigurationContainer configurations = project.getConfigurations();
//
//            defineConfigurationsForSourceSet(sourceSet, (RoleBasedConfigurationContainerInternal) configurations);
//            definePathsForSourceSet(sourceSet, project);
//
//            createProcessResourcesTask(sourceSet, sourceSet.getResources(), project);
//            TaskProvider<JavaCompile> compileTask = createCompileJavaTask(sourceSet, sourceSet.getJava(), project);
//            createClassesTask(sourceSet, project);
//
//            configureLibraryElements(compileTask, sourceSet, configurations, objectFactory);
//            configureTargetPlatform(compileTask, sourceSet, configurations);
//        });
//    }
//
//    private void configureLibraryElements(TaskProvider<JavaCompile> compileJava, SourceSet sourceSet, ConfigurationContainer configurations, ObjectFactory objectFactory) {
//        Provider<LibraryElements> libraryElements = compileJava.flatMap(x -> x.getModularity().getInferModulePath())
//            .map(inferModulePath -> {
//                if (javaClasspathPackaging) {
//                    return LibraryElements.JAR;
//                }
//
//                // If we are compiling a module, we require JARs of all dependencies as they may potentially include an Automatic-Module-Name
//                List<File> sourcesRoots = CompilationSourceDirs.inferSourceRoots((FileTreeInternal) sourceSet.getJava().getAsFileTree());
//                if (JavaModuleDetector.isModuleSource(inferModulePath, sourcesRoots)) {
//                    return LibraryElements.JAR;
//                } else {
//                    return LibraryElements.CLASSES;
//                }
//            })
//            .map(value -> objectFactory.named(LibraryElements.class, value));
//
//        Configuration compileClasspath = configurations.getByName(sourceSet.getCompileClasspathConfigurationName());
//        compileClasspath.getAttributes().attributeProvider(
//            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
//            libraryElements
//        );
//    }
//
//    private void configureTargetPlatform(TaskProvider<JavaCompile> compileTask, SourceSet sourceSet, ConfigurationContainer configurations) {
//        getJvmLanguageUtils().useDefaultTargetPlatformInference(configurations.getByName(sourceSet.getCompileClasspathConfigurationName()), compileTask);
//        getJvmLanguageUtils().useDefaultTargetPlatformInference(configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()), compileTask);
//    }
//
//    private TaskProvider<JavaCompile> createCompileJavaTask(final SourceSet sourceSet, final SourceDirectorySet javaSource, final Project project) {
//        final TaskProvider<JavaCompile> compileTask = project.getTasks().register(sourceSet.getCompileJavaTaskName(), JavaCompile.class, javaCompile -> {
//            ConventionMapping conventionMapping = javaCompile.getConventionMapping();
//            conventionMapping.map("classpath", sourceSet::getCompileClasspath);
//
//            JvmPluginsHelper.configureAnnotationProcessorPath(sourceSet, javaSource, javaCompile.getOptions(), project);
//            javaCompile.setDescription("Compiles " + javaSource + ".");
//            javaCompile.setSource(javaSource);
//
//            Provider<JavaToolchainSpec> toolchainOverrideSpec = project.provider(() ->
//                JavaCompileExecutableUtils.getExecutableOverrideToolchainSpec(javaCompile, propertyFactory));
//            javaCompile.getJavaCompiler().convention(getToolchainTool(project, JavaToolchainService::compilerFor, toolchainOverrideSpec));
//
//            String generatedHeadersDir = "generated/sources/headers/" + javaSource.getName() + "/" + sourceSet.getName();
//            javaCompile.getOptions().getHeaderOutputDirectory().convention(project.getLayout().getBuildDirectory().dir(generatedHeadersDir));
//
//            JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
//            javaCompile.getModularity().getInferModulePath().convention(javaPluginExtension.getModularity().getInferModulePath());
//        });
//        JvmPluginsHelper.configureOutputDirectoryForSourceSet(sourceSet, javaSource, project, compileTask, compileTask.map(JavaCompile::getOptions));
//
//        return compileTask;
//    }
//
//    private void createProcessResourcesTask(final SourceSet sourceSet, final SourceDirectorySet resourceSet, final Project target) {
//        TaskProvider<ProcessResources> processResources = target.getTasks().register(sourceSet.getProcessResourcesTaskName(), ProcessResources.class, resourcesTask -> {
//            resourcesTask.setDescription("Processes " + resourceSet + ".");
//            new DslObject(resourcesTask.getRootSpec()).getConventionMapping().map("destinationDir", (Callable<File>) () -> sourceSet.getOutput().getResourcesDir());
//            resourcesTask.from(resourceSet);
//        });
//        DefaultSourceSetOutput output = Cast.uncheckedCast(sourceSet.getOutput());
//        output.setResourcesContributor(processResources.map(Copy::getDestinationDir), processResources);
//    }
//
//    private void createClassesTask(final SourceSet sourceSet, Project target) {
//        sourceSet.compiledBy(
//            target.getTasks().register(sourceSet.getClassesTaskName(), classesTask -> {
//                classesTask.setGroup(LifecycleBasePlugin.BUILD_GROUP);
//                classesTask.setDescription("Assembles " + sourceSet.getOutput() + ".");
//                classesTask.dependsOn(sourceSet.getOutput().getDirs());
//                classesTask.dependsOn(sourceSet.getCompileJavaTaskName());
//                classesTask.dependsOn(sourceSet.getProcessResourcesTaskName());
//            })
//        );
//    }
//
//    private void definePathsForSourceSet(final SourceSet sourceSet, final Project project) {
//        ConventionMapping outputConventionMapping = ((IConventionAware) sourceSet.getOutput()).getConventionMapping();
//        outputConventionMapping.map("resourcesDir", () -> {
//            String classesDirName = "resources/" + sourceSet.getName();
//            return project.getLayout().getBuildDirectory().dir(classesDirName).get().getAsFile();
//        });
//
//        sourceSet.getJava().srcDir("src/" + sourceSet.getName() + "/java");
//        sourceSet.getResources().srcDir("src/" + sourceSet.getName() + "/resources");
//    }
//
//    private void defineConfigurationsForSourceSet(SourceSet sourceSet, RoleBasedConfigurationContainerInternal configurations) {
//        String implementationConfigurationName = sourceSet.getImplementationConfigurationName();
//        String runtimeOnlyConfigurationName = sourceSet.getRuntimeOnlyConfigurationName();
//        String compileOnlyConfigurationName = sourceSet.getCompileOnlyConfigurationName();
//        String compileClasspathConfigurationName = sourceSet.getCompileClasspathConfigurationName();
//        String annotationProcessorConfigurationName = sourceSet.getAnnotationProcessorConfigurationName();
//        String runtimeClasspathConfigurationName = sourceSet.getRuntimeClasspathConfigurationName();
//        String sourceSetName = sourceSet.toString();
//
//        SourceSetConfigurationCreationRequest implementationRequest = new SourceSetConfigurationCreationRequest(sourceSet.getName(), implementationConfigurationName, ConfigurationRoles.DEPENDENCY_SCOPE);
//        Configuration implementationConfiguration = configurations.maybeCreate(implementationRequest);
//        implementationConfiguration.setVisible(false);
//        implementationConfiguration.setDescription("Implementation only dependencies for " + sourceSetName + ".");
//
//        SourceSetConfigurationCreationRequest compileOnlyRequest = new SourceSetConfigurationCreationRequest(sourceSet.getName(), compileOnlyConfigurationName, ConfigurationRoles.DEPENDENCY_SCOPE);
//        Configuration compileOnlyConfiguration = configurations.maybeCreate(compileOnlyRequest);
//        compileOnlyConfiguration.setVisible(false);
//        compileOnlyConfiguration.setDescription("Compile only dependencies for " + sourceSetName + ".");
//
//        SourceSetConfigurationCreationRequest compileClasspathRequest = new SourceSetConfigurationCreationRequest(sourceSet.getName(), compileClasspathConfigurationName, ConfigurationRoles.RESOLVABLE);
//        Configuration compileClasspathConfiguration = configurations.maybeCreate(compileClasspathRequest);
//        compileClasspathConfiguration.setVisible(false);
//        compileClasspathConfiguration.extendsFrom(compileOnlyConfiguration, implementationConfiguration);
//        compileClasspathConfiguration.setDescription("Compile classpath for " + sourceSetName + ".");
//        jvmPluginServices.configureAsCompileClasspath(compileClasspathConfiguration);
//
//        @SuppressWarnings("deprecation")
//        SourceSetConfigurationCreationRequest annotationProcessorRequest = new SourceSetConfigurationCreationRequest(sourceSet.getName(), annotationProcessorConfigurationName, ConfigurationRoles.RESOLVABLE_DEPENDENCY_SCOPE);
//        Configuration annotationProcessorConfiguration = configurations.maybeCreate(annotationProcessorRequest);
//        annotationProcessorConfiguration.setVisible(false);
//        annotationProcessorConfiguration.setDescription("Annotation processors and their dependencies for " + sourceSetName + ".");
//        jvmPluginServices.configureAsRuntimeClasspath(annotationProcessorConfiguration);
//
//        SourceSetConfigurationCreationRequest runtimeOnlyRequest = new SourceSetConfigurationCreationRequest(sourceSet.getName(), runtimeOnlyConfigurationName, ConfigurationRoles.DEPENDENCY_SCOPE);
//        Configuration runtimeOnlyConfiguration = configurations.maybeCreate(runtimeOnlyRequest);
//        runtimeOnlyConfiguration.setVisible(false);
//        runtimeOnlyConfiguration.setDescription("Runtime only dependencies for " + sourceSetName + ".");
//
//        SourceSetConfigurationCreationRequest runtimeClasspathRequest = new SourceSetConfigurationCreationRequest(sourceSet.getName(), runtimeClasspathConfigurationName, ConfigurationRoles.RESOLVABLE);
//        Configuration runtimeClasspathConfiguration = configurations.maybeCreate(runtimeClasspathRequest);
//        runtimeClasspathConfiguration.setVisible(false);
//        runtimeClasspathConfiguration.setDescription("Runtime classpath of " + sourceSetName + ".");
//        runtimeClasspathConfiguration.extendsFrom(runtimeOnlyConfiguration, implementationConfiguration);
//        jvmPluginServices.configureAsRuntimeClasspath(runtimeClasspathConfiguration);
//
//        sourceSet.setCompileClasspath(compileClasspathConfiguration);
//        sourceSet.setRuntimeClasspath(sourceSet.getOutput().plus(runtimeClasspathConfiguration));
//        sourceSet.setAnnotationProcessorPath(annotationProcessorConfiguration);
//    }
//
//    private void configureCompileDefaults(final Project project, final DefaultJavaPluginExtension javaExtension) {
//        project.getTasks().withType(AbstractCompile.class).configureEach(compile -> {
//            JvmPluginsHelper.configureCompileDefaults(compile, javaExtension, (@Nullable JavaVersion rawConvention, Supplier<JavaVersion> javaVersionSupplier) -> {
//                if (compile instanceof JavaCompile) {
//                    JavaCompile javaCompile = (JavaCompile) compile;
//                    if (javaCompile.getOptions().getRelease().isPresent()) {
//                        return JavaVersion.toVersion(javaCompile.getOptions().getRelease().get());
//                    }
//                    if (rawConvention != null) {
//                        return rawConvention;
//                    }
//                    return JavaVersion.toVersion(javaCompile.getJavaCompiler().get().getMetadata().getLanguageVersion().toString());
//                }
//
//                return javaVersionSupplier.get();
//            });
//
//            compile.getDestinationDirectory().convention(project.getProviders().provider(new BackwardCompatibilityOutputDirectoryConvention(compile)));
//        });
//    }
//
//    private void configureJavaDoc(final Project project, final JavaPluginExtension javaPluginExtension) {
//        project.getTasks().withType(Javadoc.class).configureEach(javadoc -> {
//            javadoc.getConventionMapping().map("destinationDir", () -> new File(javaPluginExtension.getDocsDir().get().getAsFile(), "javadoc"));
//            javadoc.getConventionMapping().map("title", () -> project.getExtensions().getByType(ReportingExtension.class).getApiDocTitle());
//
//            Provider<JavaToolchainSpec> toolchainOverrideSpec = project.provider(() ->
//                JavadocExecutableUtils.getExecutableOverrideToolchainSpec(javadoc, propertyFactory));
//            javadoc.getJavadocTool().convention(getToolchainTool(project, JavaToolchainService::javadocToolFor, toolchainOverrideSpec));
//        });
//    }
//
//    private void configureBuildNeeded(Project project) {
//        project.getTasks().register(BUILD_NEEDED_TASK_NAME, buildTask -> {
//            buildTask.setDescription("Assembles and tests this project and all projects it depends on.");
//            buildTask.setGroup(BasePlugin.BUILD_GROUP);
//            buildTask.dependsOn(BUILD_TASK_NAME);
//        });
//    }
//
//    private void configureBuildDependents(Project project) {
//        project.getTasks().register(BUILD_DEPENDENTS_TASK_NAME, buildTask -> {
//            buildTask.setDescription("Assembles and tests this project and all projects that depend on it.");
//            buildTask.setGroup(BasePlugin.BUILD_GROUP);
//            buildTask.dependsOn(BUILD_TASK_NAME);
//        });
//    }
//
//    private void configureArchiveDefaults(Project project) {
//        // TODO: Gradle 8.1+: Deprecate `getLibsDirectory` in BasePluginExtension and move it to `JavaPluginExtension`
//        BasePluginExtension basePluginExtension = project.getExtensions().getByType(BasePluginExtension.class);
//
//        project.getTasks().withType(Jar.class).configureEach(task -> task.getDestinationDirectory().convention(basePluginExtension.getLibsDirectory()));
//    }
//
//    private void configureTest(final Project project, final JavaPluginExtension javaPluginExtension) {
//        project.getTasks().withType(Test.class).configureEach(test -> configureTestDefaults(test, project, javaPluginExtension));
//    }
//
//    private void configureTestDefaults(final Test test, Project project, final JavaPluginExtension javaPluginExtension) {
//        DirectoryReport htmlReport = test.getReports().getHtml();
//        JUnitXmlReport xmlReport = test.getReports().getJunitXml();
//
//        xmlReport.getOutputLocation().convention(javaPluginExtension.getTestResultsDir().dir(test.getName()));
//        htmlReport.getOutputLocation().convention(javaPluginExtension.getTestReportDir().dir(test.getName()));
//        test.getBinaryResultsDirectory().convention(javaPluginExtension.getTestResultsDir().dir(test.getName() + "/binary"));
//        test.workingDir(project.getProjectDir());
//
//        Provider<JavaToolchainSpec> toolchainOverrideSpec = project.provider(() ->
//            TestExecutableUtils.getExecutableToolchainSpec(test, propertyFactory));
//        test.getJavaLauncher().convention(getToolchainTool(project, JavaToolchainService::launcherFor, toolchainOverrideSpec));
//    }
//
//    private <T> Provider<T> getToolchainTool(
//        Project project,
//        BiFunction<JavaToolchainService, JavaToolchainSpec, Provider<T>> toolMapper,
//        Provider<JavaToolchainSpec> toolchainOverride
//    ) {
//        JavaToolchainService service = project.getExtensions().getByType(JavaToolchainService.class);
//        JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
//        return toolchainOverride.orElse(extension.getToolchain())
//            .flatMap(spec -> toolMapper.apply(service, spec));
//    }
//
//    /**
//     * Convention to fall back to the 'destinationDir' output for backwards compatibility with plugins that extend AbstractCompile
//     * and override the deprecated methods.
//     */
//    private static class BackwardCompatibilityOutputDirectoryConvention implements Callable<Directory> {
//        private final AbstractCompile compile;
//        private boolean recursiveCall;
//
//        public BackwardCompatibilityOutputDirectoryConvention(AbstractCompile compile) {
//            this.compile = compile;
//        }
//
//        @SuppressWarnings("deprecation")
//        @Override
//        @Nullable
//        public Directory call() throws Exception {
//            Method getter = GeneratedSubclasses.unpackType(compile).getMethod("getDestinationDir");
//            if (getter.getDeclaringClass() == AbstractCompile.class) {
//                // Subclass has not overridden the getter, so ignore
//                return null;
//            }
//
//            // Subclass has overridden the getter, so call it
//
//            if (recursiveCall) {
//                // Already querying AbstractCompile.getDestinationDirectory()
//                // In that case, this convention should not be used.
//                return null;
//            }
//            recursiveCall = true;
//            File legacyValue;
//            try {
//                // This will call a subclass implementation of getDestinationDir(), which possibly will not call the overridden getter
//                // In the Kotlin plugin, the subclass manages its own field which will be used here.
//                // This was to support tasks that extended AbstractCompile and had their own getDestinationDir().
//                // We actually need to keep this as compile.getDestinationDir to maintain compatibility.
//                legacyValue = compile.getDestinationDir();
//            } finally {
//                recursiveCall = false;
//            }
//            if (legacyValue == null) {
//                return null;
//            } else {
//                return compile.getProject().getLayout().getProjectDirectory().dir(legacyValue.getAbsolutePath());
//            }
//        }
//    }
//
//    /**
//     * An {@link AbstractRoleBasedConfigurationCreationRequest} that provides context for error messages and warnings
//     * emitted when creating the configurations implicitly associated with a {@link SourceSet}.
//     */
//    private static final class SourceSetConfigurationCreationRequest extends AbstractRoleBasedConfigurationCreationRequest {
//        private final String sourceSetName;
//
//        private SourceSetConfigurationCreationRequest(String sourceSetName, String configurationName, ConfigurationRole role) {
//            super(configurationName, role);
//            this.sourceSetName = sourceSetName;
//        }
//
//        public String getSourceSetName() {
//            return sourceSetName;
//        }
//
//        @Override
//        public void warnAboutNeedToMutateUsage(DeprecatableConfiguration conf) {
//            String msgDiscovery = getUsageDiscoveryMessage(conf);
//            String msgExpectation = getUsageExpectationMessage();
//
//            DeprecationLogger.deprecate(msgDiscovery + msgExpectation)
//                .withAdvice(getUsageMutationAdvice())
//                .willBecomeAnErrorInGradle9()
//                .withUserManual("building_java_projects", "sec:implicit_sourceset_configurations")
//                .nagUser();
//        }
//
//        private String getUsageDiscoveryMessage(DeprecatableConfiguration conf) {
//            String currentUsageDesc = UsageDescriber.describeCurrentUsage(conf);
//            return String.format("When creating configurations during sourceSet %s setup, Gradle found that configuration %s already exists with permitted usage(s):\n" +
//                "%s\n", sourceSetName, getConfigurationName(), currentUsageDesc);
//        }
//
//        private String getUsageExpectationMessage() {
//            String expectedUsageDesc = UsageDescriber.describeRole(getRole());
//            return String.format("Yet Gradle expected to create it with the usage(s):\n" +
//                "%s\n" +
//                "Gradle will mutate the usage of configuration %s to match the expected usage. This may cause unexpected behavior. Creating configurations with reserved names", expectedUsageDesc, getConfigurationName());
//        }
//
//        @Override
//        public void failOnInabilityToMutateUsage() {
//            List<String> resolutions = ImmutableList.of(
//                RoleBasedConfigurationCreationRequest.getDefaultReservedNameAdvice(getConfigurationName()),
//                getUsageMutationAdvice()
//            );
//            throw new UnmodifiableUsageException(getConfigurationName(), resolutions);
//        }
//
//        private String getUsageMutationAdvice() {
//            return String.format("Create source set %s prior to creating or accessing the configurations associated with it.", sourceSetName);
//        }
//    }

