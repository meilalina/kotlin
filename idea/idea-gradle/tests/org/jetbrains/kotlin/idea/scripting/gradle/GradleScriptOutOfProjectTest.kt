/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationLoadingTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

class GradleScriptOutOfProjectTest : AbstractScriptConfigurationLoadingTest() {

    override fun setUpTestProject() {
        val rootDir = "idea/testData/script/definition/loading/gradle/"

        val buildGradleKts = File(rootDir).walkTopDown().find { it.name == GradleConstants.KOTLIN_DSL_SCRIPT_NAME }
            ?: error("Couldn't find main script")

        configureScriptFile(rootDir, buildGradleKts)

        testAffectedGradleProjectFiles = true
    }

    override fun tearDown() {
        super.tearDown()

        testAffectedGradleProjectFiles = false
    }

    fun testManualLoadingForUpToDate() {
        assertConfigurationIsNotLoaded()

        KotlinScriptingSettings.getInstance(project).addExternalScript(myFile.virtualFile)

        assertConfigurationWasLoaded()
    }

    fun testManualLoadingForOutOfDate() {
        assertConfigurationIsNotLoaded()

        makeChangesInsideSections()

        assertNoBackgroundTasks()
        assertNoLoading()

        KotlinScriptingSettings.getInstance(project).addExternalScript(myFile.virtualFile)

        assertConfigurationWasLoaded()
    }

    fun testFileAttributesForManualLoading() {
        assertConfigurationIsNotLoaded()

        KotlinScriptingSettings.getInstance(project).addExternalScript(myFile.virtualFile)

        assertConfigurationWasLoaded()

        ScriptConfigurationManager.clearCaches(project)

        assertNotNull(getConfiguration())
    }

    fun testNoNotificationAfterForReload() {
        assertConfigurationIsNotLoaded()

        KotlinScriptingSettings.getInstance(project).addExternalScript(myFile.virtualFile)
        assertConfigurationWasLoaded()

        makeChangesInsideSections()

        KotlinScriptingSettings.getInstance(project).addExternalScript(myFile.virtualFile)
        assertConfigurationWasLoaded()

    }

    private fun makeChangesInsideSections() {
        changeContents(myFile.text.replace(GradleScriptInputsWatcherTest.insidePlaceholder, "application"), myFile)
    }

    private fun assertConfigurationIsNotLoaded() {
        assertNull(getConfiguration())
        assertAndDoAllBackgroundTasks()
        assertNoLoading()
    }

    private fun assertConfigurationWasLoaded() {
        assertAndDoAllBackgroundTasks()
        assertSingleLoading()
        assertNoSuggestedConfiguration()
        assertNotNull(getConfiguration())
    }

    private fun getConfiguration() =
        scriptConfigurationManager.getConfiguration(myFile as KtFile)
}