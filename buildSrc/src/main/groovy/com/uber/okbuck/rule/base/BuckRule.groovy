package com.uber.okbuck.rule.base

import com.uber.okbuck.core.model.base.RuleType

import static com.uber.okbuck.core.model.base.RuleType.ANDROID_LIBRARY
import static com.uber.okbuck.core.model.base.RuleType.KOTLIN_ANDROID_LIBRARY

abstract class BuckRule {

    final String name
    private final String mRuleType
    private final Set<String> mVisibility
    private final Set<String> mDeps
    private final Set<String> mExtraBuckOpts

    BuckRule(RuleType ruleType, String name, List<String> visibility = [], List<String> deps = [],
             Set<String> extraBuckOpts = []) {
        this.name = name
        // TODO Clean this up, the rule name generation is a bit hacky
        mRuleType = ruleType == KOTLIN_ANDROID_LIBRARY ? ANDROID_LIBRARY.name().toLowerCase() : ruleType.name().toLowerCase()
        mVisibility = new LinkedHashSet(visibility)
        mDeps = new LinkedHashSet(deps) // de-dup dependencies
        mExtraBuckOpts = extraBuckOpts
    }

    /**
     * Print this rule into the printer.
     */
    void print(PrintStream printer) {
        printer.println("${mRuleType}(")

        if (name != null) {
            printer.println("\tname = '${name}',")
        }
        printContent(printer)
        mExtraBuckOpts.each { String option ->
            printer.println("\t${option},")
        }
        if (!mDeps.empty) {
            printer.println("\tdeps = [")
            mDeps.sort().each { String dep ->
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }
        if (!mVisibility.empty) {
            printer.println("\tvisibility = [")
            for (String visibility : mVisibility) {
                printer.println("\t\t'${visibility}',")
            }
            printer.println("\t],")
        }
        printer.println(")")
        printer.println()
    }

    /**
     * Print rule content.
     *
     * @param printer The printer.
     */
    protected abstract void printContent(PrintStream printer)
}
