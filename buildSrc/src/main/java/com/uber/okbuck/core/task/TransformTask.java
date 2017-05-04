package com.uber.okbuck.core.task;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.util.FileUtil;

import org.gradle.api.internal.AbstractTask;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Collections;

public class TransformTask extends AbstractTask {

  public static final String TRANSFORM_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/transform";

  public static final String CONFIGURATION_TRANSFORM = "transform";
  private static final String TRANSFORM_FOLDER = "transform/";
  private static final String TRANSFORM_BUCK_FILE = "BUCK_FILE";
  private static final String TRANSFORM_JAR = "transform-cli-1.1.0.jar";

  @Input boolean transform;

  public TransformTask() {
    // Never up to date; this task isn't safe to run incrementally.
    getOutputs().upToDateWhen(Specs.satisfyNone());
  }

  @TaskAction
  public void fetchTransformDeps() {
    if (transform) {
      new DependencyCache("transform",
              getProject().getRootProject(),
              TRANSFORM_CACHE,
              Collections.singleton(getProject().getConfigurations().getByName(CONFIGURATION_TRANSFORM)),

              TRANSFORM_FOLDER + TRANSFORM_BUCK_FILE);

      FileUtil.copyResourceToProject(
              TRANSFORM_FOLDER + TRANSFORM_BUCK_FILE, new File(cacheDirectory(), "BUCK"));
      FileUtil.copyResourceToProject(
              TRANSFORM_FOLDER + TRANSFORM_JAR, new File(cacheDirectory(), TRANSFORM_JAR));
    }
  }

  @OutputDirectory
  public File cacheDirectory() {
    return new File(TRANSFORM_CACHE);
  }
}
