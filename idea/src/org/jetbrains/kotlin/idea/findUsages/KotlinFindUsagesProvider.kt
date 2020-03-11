/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.java.JavaFindUsagesProvider
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit

class KotlinFindUsagesProvider : FindUsagesProvider {
    private val javaProvider by lazy { JavaFindUsagesProvider() }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
        psiElement is KtNamedDeclaration

    override fun getWordsScanner(): WordsScanner? = KotlinWordsScanner()

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return when (element) {
            is KtNamedFunction -> KotlinFindUsagesBundle.message("kotlin.lang.function")
            is KtClass -> KotlinFindUsagesBundle.message("kotlin.lang.class")
            is KtParameter -> KotlinFindUsagesBundle.message("kotlin.lang.parameter")
            is KtProperty -> if (element.isLocal)
                KotlinFindUsagesBundle.message("kotlin.lang.variable")
            else
                KotlinFindUsagesBundle.message("kotlin.lang.property")
            is KtDestructuringDeclarationEntry -> KotlinFindUsagesBundle.message("kotlin.lang.variable")
            is KtTypeParameter -> KotlinFindUsagesBundle.message("kotlin.lang.type.parameter")
            is KtSecondaryConstructor -> KotlinFindUsagesBundle.message("kotlin.lang.constructor")
            is KtObjectDeclaration -> KotlinFindUsagesBundle.message("kotlin.lang.object")
            else -> ""
        }
    }

    private val KtDeclaration.containerDescription: String?
        get() {
            containingClassOrObject?.let { return getDescriptiveName(it) }
            (parent as? KtFile)?.parent?.let { return getDescriptiveName(it) }
            return null
        }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is PsiDirectory, is PsiPackage, is PsiFile -> javaProvider.getDescriptiveName(element)
            is KtClassOrObject -> {
                if (element is KtObjectDeclaration && element.isObjectLiteral()) return "<unnamed>"
                element.fqName?.asString() ?: element.name ?: "<unnamed>"
            }
            is KtProperty -> (element.name ?: "") + (element.containerDescription?.let { " of $it" } ?: "")
            is KtFunction -> {
                val name = element.name ?: ""
                val descriptor = element.unsafeResolveToDescriptor() as FunctionDescriptor
                val renderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS
                val paramsDescription =
                    descriptor.valueParameters.joinToString(prefix = "(", postfix = ")") { renderer.renderType(it.type) }
                val returnType = descriptor.returnType
                val returnTypeDescription = if (returnType != null && !returnType.isUnit()) renderer.renderType(returnType) else null
                val funDescription = "$name$paramsDescription" + (returnTypeDescription?.let { ": $it" } ?: "")
                return funDescription + (element.containerDescription?.let { " of $it" } ?: "")
            }
            is KtLabeledExpression -> element.getLabelName() ?: ""
            is KtImportAlias -> element.name ?: ""
            is KtLightElement<*, *> -> element.kotlinOrigin?.let { getDescriptiveName(it) } ?: ""
            is KtParameter -> {
                if (element.isPropertyParameter()) {
                    (element.name ?: "") + (element.containerDescription?.let { " of $it" } ?: "")
                } else {
                    element.name ?: ""
                }
            }
            is PsiNamedElement -> element.name ?: ""
            else -> ""
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
