package com.uber.okbuck.extension;

import com.uber.okbuck.core.annotation.Experimental;

import org.gradle.api.tasks.Input;

@Experimental
public class ExperimentalExtension {

    /**
     * Whether transform rules are to be generated
     */
    @Input
    public boolean transform = false;
}
