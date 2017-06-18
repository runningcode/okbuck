package com.uber.okbuck.core.model.base;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.groovy.GroovyLibTarget;
import com.uber.okbuck.core.model.java.JavaAppTarget;
import com.uber.okbuck.core.model.java.JavaLibTarget;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.model.kotlin.KotlinLibTarget;
import com.uber.okbuck.core.util.ProjectUtil;

import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePluginConvention;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TargetCache {

    private static final Converter<String, String> NAME_CONVERTER =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN);

    private final Map<Project, Map<String, Target>> store = new HashMap<>();
    private final Map<Project, Map<String, Target>> artifactNameToTarget = new HashMap<>();

    public Map<String, Target> getTargets(Project project) {
        Map<String, Target> projectTargets = store.get(project);
        if (projectTargets == null) {
            ProjectType type = ProjectUtil.getType(project);
            switch (type) {
                case KOTLIN_ANDROID_APP:
                case ANDROID_APP:
                    projectTargets = new HashMap<>();
                    for (BaseVariant v : project.getExtensions()
                            .getByType(AppExtension.class)
                            .getApplicationVariants()) {
                        projectTargets.put(v.getName(), new AndroidAppTarget(project, v.getName()));
                    }
                    break;
                case KOTLIN_ANDROID_LIB:
                case ANDROID_LIB:
                    projectTargets = new HashMap<>();
                    Map<String, Target> projectArtifacts = new HashMap<>();
                    String archiveBaseName = project.getConvention().getPlugin(BasePluginConvention.class)
                            .getArchivesBaseName();
                    for (BaseVariant v : project.getExtensions()
                            .getByType(LibraryExtension.class)
                            .getLibraryVariants()) {
                        Target target = new AndroidLibTarget(project, v.getName());
                        projectTargets.put(v.getName(), target);

                        String artifactName = String.format("%s-%s", archiveBaseName,
                                NAME_CONVERTER.convert(v.getName()));
                        projectArtifacts.put(artifactName, target);
                    }
                    artifactNameToTarget.put(project, projectArtifacts);
                    break;
                case GROOVY_LIB:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new GroovyLibTarget(project, JvmTarget.MAIN));
                    break;
                case KOTLIN_LIB:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new KotlinLibTarget(project, JvmTarget.MAIN));
                    break;
                case JAVA_APP:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new JavaAppTarget(project, JvmTarget.MAIN));
                    break;
                case JAVA_LIB:
                    projectTargets = Collections.singletonMap(JvmTarget.MAIN,
                            new JavaLibTarget(project, JvmTarget.MAIN));
                    break;
                default:
                    projectTargets = Collections.emptyMap();
                    break;
            }
            store.put(project, projectTargets);
        }

        return projectTargets;
    }

    @Nullable
    public Target getTargetForOutput(Project targetProject, File artifact) {
        Target result;
        ProjectType type = ProjectUtil.getType(targetProject);
        switch (type) {
            case ANDROID_LIB:
            case KOTLIN_ANDROID_LIB:
                result = artifactNameToTarget.get(targetProject)
                            .get(FilenameUtils.getBaseName(artifact.getName()));
                if (result == null) {
                    throw new IllegalStateException("No target found for " + artifact.toString());
                }
                break;
            case GROOVY_LIB:
            case JAVA_APP:
            case JAVA_LIB:
            case KOTLIN_LIB:
                result = getTargets(targetProject).values().iterator().next();
                break;
            default:
                throw new IllegalStateException(type + " not handled");
        }
        return result;
    }
}
