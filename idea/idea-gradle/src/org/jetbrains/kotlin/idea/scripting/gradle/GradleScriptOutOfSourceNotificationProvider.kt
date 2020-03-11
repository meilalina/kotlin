/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings

class GradleScriptOutOfSourceNotificationProvider(private val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (!isGradleKotlinScript(file)) return null
        if (file.fileType != KotlinFileType.INSTANCE) return null

        if (isInAffectedGradleProjectFiles(project, file.path)) return null

        return EditorNotificationPanel().apply {
            text("The associated Gradle Project isn't imported.")
            createActionLabel("Add as external script") {
                KotlinScriptingSettings.getInstance(project).addExternalScript(file)
                EditorNotifications.getInstance(project).updateAllNotifications()
            }
            val link = createActionLabel("") {}
            link.setIcon(AllIcons.General.ContextHelp)
            link.setUseIconAsLink(true)
            link.toolTipText = "The external Gradle project needs to be imported to get this script analyzed. <br/>" +
                    "You can import the related Gradle project or click \"Load script configuration\" " +
                    "to get code insight without importing."
        }
    }

    companion object {
        private val KEY = Key.create<EditorNotificationPanel>("GradleScriptOutOfSourceNotification")
    }
}