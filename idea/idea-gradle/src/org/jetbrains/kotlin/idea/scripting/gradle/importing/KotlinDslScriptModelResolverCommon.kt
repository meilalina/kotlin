/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Pair
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslModelsParameters.*
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel.SCRIPTS_GRADLE_PROPERTY_NAME
import org.jetbrains.kotlin.idea.scripting.gradle.collectGradleScripts
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

internal val LOG = Logger.getInstance(KotlinDslScriptModelResolverCommon::class.java)

abstract class KotlinDslScriptModelResolverCommon : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinDslScriptsModel::class.java)
    }

    override fun getExtraJvmArgs(): List<Pair<String, String>> {
        return listOf(
            Pair(
                PROVIDER_MODE_SYSTEM_PROPERTY_NAME,
                STRICT_CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE
            )
        )
    }

    override fun getExtraCommandLineArgs(): List<String> {
        val extra = arrayListOf("-P$CORRELATION_ID_GRADLE_PROPERTY_NAME=${System.nanoTime()}")
        val project = findProject()
        if (project != null) {
            extra.add("-P$SCRIPTS_GRADLE_PROPERTY_NAME=${collectGradleScripts(project).joinToString("|")}")
        }

        return extra
    }

    private fun findProject(): Project? {
        val projectPath = resolverCtx.projectPath
        if (projectPath.isNotBlank()) {
            return ProjectManager.getInstance().openProjects.find {
                it.basePath == projectPath
            }
        }
        return null
    }

    @Suppress("unused")
    protected fun KotlinDslScriptsModel.toListOfScriptModels(): List<KotlinDslScriptModel> =
        scriptModels.map { (file, model) ->
            val messages = mutableListOf<KotlinDslScriptModel.Message>()

            model.exceptions.forEach {
                messages.add(
                    KotlinDslScriptModel.Message(
                        KotlinDslScriptModel.Severity.ERROR, it
                    )
                )
            }

            model.editorReports.forEach {
                messages.add(
                    KotlinDslScriptModel.Message(
                        when (it.severity) {
                            EditorReportSeverity.WARNING -> KotlinDslScriptModel.Severity.WARNING
                            else -> KotlinDslScriptModel.Severity.ERROR
                        },
                        it.message,
                        it.position?.let { position ->
                            KotlinDslScriptModel
                                .Position(position.line, position.column)
                        }
                    ))
            }

            // todo(KT-34440): take inputs snapshot before starting import
            KotlinDslScriptModel(
                file.absolutePath,
                System.currentTimeMillis(),
                model.classPath.map { it.absolutePath },
                model.sourcePath.map { it.absolutePath },
                model.implicitImports,
                messages
            )
        }

    protected fun processScriptModel(
        ideProject: DataNode<ProjectData>,
        model: KotlinDslScriptsModel,
        projectName: String
    ) {
        if (model is BrokenKotlinDslScriptsModel) {
            LOG.error(
                "Couldn't get KotlinDslScriptsModel for $projectName:\n${model.message}\n${model.stackTrace}"
            )
        } else {
            ideProject.KOTLIN_DSL_SCRIPT_MODELS.addAll(model.toListOfScriptModels())
        }
    }
}