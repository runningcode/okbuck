package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidAppTarget;

import org.gradle.api.internal.project.DefaultProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;

public class KeystoreRuleComposerTest {

  @Rule public final TemporaryFolder testProjectDir = new TemporaryFolder();
  @Mock AndroidAppTarget androidAppTarget;
  private File buildFile;

  @Before public void setup() throws IOException {
    buildFile = testProjectDir.newFile("build.gradle");
//    MockitoAnnotations.initMocks(this);
  }

  @Test public void testCompose() {
//    AndroidAppTarget appTarget = new AndroidAppTarget(new DefaultProject(), "app");
//    KeystoreRuleComposer.compose(androidAppTarget);


  }
}
