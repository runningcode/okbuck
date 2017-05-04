package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.task.TransformTask
import com.uber.okbuck.core.util.FileUtil

import com.uber.okbuck.rule.base.GenRule
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import static com.uber.okbuck.core.task.TransformTask.TRANSFORM_CACHE

final class TrasformDependencyWriterRuleComposer extends AndroidBuckRuleComposer {

    static final String OPT_TRANSFORM_CLASS = "transform"
    static final String OPT_CONFIG_FILE = "configFile"
    static final String RUNNER_MAIN_CLASS = "com.uber.okbuck.transform.CliTransform"
    public static final String TRANSFORM_RULE = "//" + TRANSFORM_CACHE + ":okbuck_transform";

    private TrasformDependencyWriterRuleComposer() {}

    static List<GenRule> compose(AndroidAppTarget target) {
        return target.transforms.collect { Map<String, String> options ->
            compose(target, options)
        }
    }

    static GenRule compose(AndroidAppTarget target, Map<String, String> options) {
        String transformClass = options.get(OPT_TRANSFORM_CLASS)
        String configFile = options.get(OPT_CONFIG_FILE)

        List<String> input = []
        if (configFile != null) {
            input.add(getTransformConfigRuleForFile(target.project, target.rootProject.file(configFile)))
        }

        String output = "\$OUT"
        List<String> cmds = [
                "echo \"#!/bin/bash\" > ${output};",
                "echo \"set -ex\" >> ${output};",

                "echo \"java " +

                        "-Dokbuck.inJarsDir=\"\\\$1\" " +
                        "-Dokbuck.outJarsDir=\"\\\$2\" " +
                        "-Dokbuck.androidBootClasspath=\"\\\$3\" " +

                        (configFile != null ? "-Dokbuck.configFile=\"\$SRCS\" " : "") +
                        (transformClass != null ? "-Dokbuck.transformClass=\"${transformClass}\" " : "") +

                        " -cp \$(location ${TRANSFORM_RULE}) ${RUNNER_MAIN_CLASS}\" >> ${output};",

                "chmod +x ${output}"]

        String name = getTransformRuleName(target, options);
        return new GenRule(name, input, cmds, false, "${name}_out", true)
    }

    static getTransformRuleName(AndroidAppTarget target, Map<String, String> options) {
        return transform(options.get(OPT_TRANSFORM_CLASS), target)
    }

    static String getTransformConfigRuleForFile(Project project, File config) {
        String path = getTransformFilePathForFile(project, config)
        File configFile = new File("${TransformTask.TRANSFORM_CACHE}/${path}")
        Files.copy(config.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return "//${TransformTask.TRANSFORM_CACHE}:${path}"
    }

    private static String getTransformFilePathForFile(Project project, File config) {
        return FileUtil.getRelativePath(project.rootDir, config).replaceAll('/', '_')
    }
}
