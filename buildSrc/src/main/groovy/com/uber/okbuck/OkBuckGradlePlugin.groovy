package com.uber.okbuck

import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.base.TargetCache
import com.uber.okbuck.core.model.java.JavaTarget
import com.uber.okbuck.core.task.OkBuckCleanTask
import com.uber.okbuck.core.task.OkBuckTask
import com.uber.okbuck.core.task.TransformTask
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.LintUtil
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.core.util.RobolectricUtil

import com.uber.okbuck.extension.ExperimentalExtension
import com.uber.okbuck.extension.IntellijExtension
import com.uber.okbuck.extension.LintExtension
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.extension.RetrolambdaExtension
import com.uber.okbuck.extension.TestExtension
import com.uber.okbuck.extension.TransformExtension
import com.uber.okbuck.extension.WrapperExtension
import com.uber.okbuck.generator.BuckFileGenerator
import com.uber.okbuck.wrapper.BuckWrapperTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

// Dependency Tree
//
//                 okbuck
//            /               \
//           /                v
//          /             okbuckClean
//         /        /          |          \
//        /        v           v          v
//       /     :p1:okbuck   :p2:okbuck   :p3:okbuck     ...
//      |        /           /               /
//      v       v           v               v
//                setupOkbuck
//

class OkBuckGradlePlugin implements Plugin<Project> {

    public static final String EXTERNAL_DEP_BUCK_FILE = "thirdparty/BUCK_FILE"
    public static final String OKBUCK = "okbuck"
    public static final String OKBUCK_CLEAN = 'okbuckClean'
    public static final String BUCK = "BUCK"
    public static final String EXPERIMENTAL = "experimental"
    public static final String INTELLIJ = "intellij"
    public static final String TEST = "test"
    public static final String WRAPPER = "wrapper"
    public static final String BUCK_WRAPPER = "buckWrapper"
    public static final String DEFAULT_CACHE_PATH = ".okbuck/cache"
    public static final String EXTRA_DEP_CACHE_PATH = ".okbuck/cache/extra"
    public static final String GROUP = "okbuck"
    public static final String BUCK_LINT = "buckLint"
    public static final String BUCK_LINT_LIBRARY = "buckLintLibrary"
    public static final String LINT = "lint"
    public static final String TRANSFORM = "transform"
    public static final String RETROLAMBDA = "retrolambda"
    public static final String CONFIGURATION_EXTERNAL = "externalOkbuck"
    public static final String OKBUCK_DEFS = ".okbuck/defs/DEFS"
    public static final String OKBUCK_STATE = ".okbuck/state/STATE"
    public static final String OKBUCK_GEN = ".okbuck/gen"

    // Project level globals
    public DependencyCache depCache
    public DependencyCache lintDepCache
    public TargetCache targetCache
    public String retrolambdaCmd

    void apply(Project project) {
        // Create extensions
        OkBuckExtension okbuckExt = project.extensions.create(OKBUCK, OkBuckExtension, project)
        WrapperExtension wrapper = okbuckExt.extensions.create(WRAPPER, WrapperExtension)
        ExperimentalExtension experimental = okbuckExt.extensions.create(EXPERIMENTAL, ExperimentalExtension)
        TestExtension test = okbuckExt.extensions.create(TEST, TestExtension)
        IntellijExtension intellij = okbuckExt.extensions.create(INTELLIJ, IntellijExtension)
        LintExtension lint = okbuckExt.extensions.create(LINT, LintExtension, project)
        RetrolambdaExtension retrolambda = okbuckExt.extensions.create(RETROLAMBDA, RetrolambdaExtension)
        okbuckExt.extensions.create(TRANSFORM, TransformExtension)

        // Create configurations
        project.configurations.maybeCreate(TransformTask.CONFIGURATION_TRANSFORM)
        Configuration externalOkbuck = project.configurations.maybeCreate(CONFIGURATION_EXTERNAL)

        // Create tasks
        Task setupOkbuck = project.task('setupOkbuck')
        setupOkbuck.setGroup(GROUP)
        setupOkbuck.setDescription("Setup okbuck cache and dependencies")

        Task okBuck = project.tasks.create(OKBUCK, OkBuckTask, {
            okBuckExtension = okbuckExt
        })
        okBuck.dependsOn(setupOkbuck)


        // Fetch transform deps if needed
        Task transform = project.tasks.create("setupTransform", TransformTask, {
            transform = experimental.transform
        })
        setupOkbuck.dependsOn(transform)

        // Create target cache
        targetCache = new TargetCache()

        project.afterEvaluate {
            // Create wrapper task
            project.tasks.create(BUCK_WRAPPER, BuckWrapperTask, {
                repo = wrapper.repo
                watch = wrapper.watch
                sourceRoots = wrapper.sourceRoots
                ignoredDirs = wrapper.ignoredDirs
            })

            // Create extra dependency caches if needed
            Map<String, Configuration> extraConfigurations = okbuckExt.extraDepCaches.collectEntries { String cacheName ->
                [cacheName, project.configurations.maybeCreate("${cacheName}ExtraDepCache")]
            }

            setupOkbuck.doFirst {
                if (!System.getProperty("okbuck.wrapper", "false").toBoolean()) {
                    throw new IllegalArgumentException("Okbuck cannot be invoked without 'okbuck.wrapper' set to true. Use buckw instead")
                }
            }


            // Configure setup task
            setupOkbuck.doLast {
                // Cleanup gen folder
                FileUtil.deleteQuietly(project.projectDir.toPath().resolve(OKBUCK_GEN))

                addSubProjectRepos(project as Project, okbuckExt.buckProjects as Set<Project>)
                Set<Configuration> projectConfigurations = configurations(okbuckExt.buckProjects)
                projectConfigurations.addAll([externalOkbuck])

                depCache = new DependencyCache(
                        "external",
                        project,
                        DEFAULT_CACHE_PATH,
                        projectConfigurations,
                        EXTERNAL_DEP_BUCK_FILE,
                        true,
                        true,
                        intellij.sources,
                        !lint.disabled,
                        okbuckExt.buckProjects)

                // Fetch Lint deps if needed
                if (!lint.disabled && lint.version != null) {
                    LintUtil.fetchLintDeps(project, lint.version)
                }


                // Fetch Retrolambda deps if needed
                boolean hasRetrolambda = okbuckExt.buckProjects.find {
                    it.plugins.hasPlugin('me.tatarka.retrolambda')
                } != null
                if (hasRetrolambda) {
                    RetrolambdaUtil.fetchRetrolambdaDeps(project, retrolambda)
                }

                // Fetch robolectric deps if needed
                if (test.robolectric) {
                    RobolectricUtil.download(project)
                }

                extraConfigurations.each { String cacheName, Configuration extraConfiguration ->
                    new DependencyCache(cacheName, project, "${EXTRA_DEP_CACHE_PATH}/${cacheName}",
                        Collections.singleton(extraConfiguration), EXTERNAL_DEP_BUCK_FILE)
                }
            }

            // Create clean task
            Task okBuckClean = project.tasks.create(OKBUCK_CLEAN, OkBuckCleanTask, {
                projects = okbuckExt.buckProjects
            })
            okBuck.dependsOn(okBuckClean)

            // Configure buck projects
            configureBuckProjects(okbuckExt.buckProjects.findAll { it.buildFile.exists() },
                    setupOkbuck,
                    okBuckClean)
        }
    }

    private static Set<Configuration> configurations(Set<Project> projects) {
        Set<Configuration> configurations = new HashSet() as Set<Configuration>
        projects.each { Project p ->
            ProjectUtil.getTargets(p).values().each {
                if (it instanceof JavaTarget) {
                    configurations.addAll(it.depConfigurations())
                }
                if (it instanceof AndroidAppTarget && it.instrumentationTarget) {
                    configurations.addAll(it.instrumentationTarget.depConfigurations())
                }
            }
        }
        return configurations
    }

    private static void configureBuckProjects(Set<Project> buckProjects, Task depends, Task dependent) {
        buckProjects.each { Project buckProject ->
            buckProject.configurations.maybeCreate(BUCK_LINT)
            buckProject.configurations.maybeCreate(BUCK_LINT_LIBRARY)
            Task okbuckProjectTask = buckProject.tasks.maybeCreate(OKBUCK)
            okbuckProjectTask.doLast {
                BuckFileGenerator.generate(buckProject)
            }
            okbuckProjectTask.dependsOn(depends)
            dependent.dependsOn(okbuckProjectTask)
        }
    }

    /**
     * This is required to let the root project super configuration resolve
     * all recursively copied configurations.
     */
    private static void addSubProjectRepos(Project rootProject, Set<Project> subProjects) {
        Map<Object, ArtifactRepository> reduced = [:]

        subProjects.each { Project subProject ->
            subProject.repositories.asMap.values().each {
                if (it instanceof MavenArtifactRepository) {
                    reduced.put(it.url, it)
                } else if (it instanceof IvyArtifactRepository) {
                    reduced.put(it.url, it)
                } else if (it instanceof FlatDirectoryArtifactRepository) {
                    reduced.put(it.dirs, it)
                } else {
                    rootProject.repositories.add(it)
                }
            }
        }

        rootProject.repositories.addAll(reduced.values())
    }
}
