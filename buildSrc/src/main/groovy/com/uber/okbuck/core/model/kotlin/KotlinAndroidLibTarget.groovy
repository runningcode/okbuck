package com.uber.okbuck.core.model.kotlin

import com.android.build.gradle.api.BaseVariant
import com.uber.okbuck.core.model.android.AndroidLibTarget
import org.gradle.api.Project

/**
 * An Android library target
 */
class KotlinAndroidLibTarget extends AndroidLibTarget {

  KotlinAndroidLibTarget(Project project, String name, boolean isTest = false) {
    super(project, name, isTest)
  }

  @Override
  protected BaseVariant getBaseVariant() {
    return project.android.libraryVariants.find { it.name == name } as BaseVariant
  }
}
