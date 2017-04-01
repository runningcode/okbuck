package com.uber.okbuck;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class TestJavaResources {
  @Test public void testResourcesAreLoaded() throws IOException {
    InputStream resourceAsStream = TestJavaResources.class.getResourceAsStream("/test.json");
    assertThat(resourceAsStream).isNotNull();
    resourceAsStream.close();
  }

  @Test public void mainResourcesAreLoaded() throws IOException {
    InputStream resourceAsStream = TestJavaResources.class.getResourceAsStream("/default.json");
    assertThat(resourceAsStream).isNotNull();
    resourceAsStream.close();
  }

  @Test public void nonExistingResourceIsNull() throws IOException {
    InputStream resourceAsStream = TestJavaResources.class.getResourceAsStream("/foo.json");
    assertThat(resourceAsStream).isNull();
  }
}
