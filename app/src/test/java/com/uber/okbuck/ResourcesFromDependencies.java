package com.uber.okbuck;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ResourcesFromDependencies {
  @Test public void mainResourcesAreLoaded() throws IOException {
    InputStream resourceAsStream = ResourcesFromDependencies.class.getResourceAsStream("/default.json");
    assertThat(resourceAsStream).isNotNull();
    resourceAsStream.close();
  }

  @Test public void nonExistingResourceIsNull() throws IOException {
    InputStream resourceAsStream = ResourcesFromDependencies.class.getResourceAsStream("/foo.json");
    assertThat(resourceAsStream).isNull();
  }
}
