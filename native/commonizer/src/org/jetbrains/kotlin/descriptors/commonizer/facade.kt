/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor1
import org.jetbrains.kotlin.descriptors.commonizer.builder.DeclarationsBuilderVisitor2
import org.jetbrains.kotlin.descriptors.commonizer.builder.createGlobalBuilderComponents
import org.jetbrains.kotlin.descriptors.commonizer.core.CommonizationVisitor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.mergeRoots
import org.jetbrains.kotlin.descriptors.commonizer.utils.ResettableClockMark
import org.jetbrains.kotlin.storage.LockBasedStorageManager

fun runCommonization(parameters: Parameters): Result {
    if (!parameters.hasAnythingToCommonize())
        return NothingToCommonize

    println(
        """
        = Before building merged tree
          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
        """.trimIndent()
    )

    val storageManager = LockBasedStorageManager("Declaration descriptors commonization")

    val clock = ResettableClockMark()

    // build merged tree:
    val mergedTree = mergeRoots(storageManager, parameters.targetProviders)

    println(
        """
        = Built merged tree in ${clock.elapsedSinceLast()}
          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
        """.trimIndent()
    )

//    println(
//        """
//        = Interner stats:
//        stringInterner = ${stringInterner.internmentRatio}
//        fqNameInterner = ${fqNameInterner.internmentRatio}
//        nameInterner = ${nameInterner.internmentRatio}
//        CirSimpleType.interner = ${CirSimpleType.interner.internmentRatio}
//        CirAnnotation.interner = ${CirAnnotation.interner.internmentRatio}
//        CirGetter.interner = ${CirGetter.interner.internmentRatio}
//        CirSetter.interner = ${CirSetter.interner.internmentRatio}
//        CirValueParameterImpl.interner = ${CirValueParameterImpl.interner.internmentRatio}
//        """.trimIndent()
//    )

    for (i in 1..10) {
        System.gc()
    }

//    println("NOW!!!")
//    Thread.sleep(30_000)

    println(
        """
        = GC in ${clock.elapsedSinceLast()}
          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
        """.trimIndent()
    )

    // commonize:
    mergedTree.accept(CommonizationVisitor(mergedTree), Unit)

    println(
        """
        = Core commonization performed in ${clock.elapsedSinceLast()}
          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
        """.trimIndent()
    )

    // GC is useless
//    for (i in 1..10) {
//        System.gc()
//    }
//
//    println(
//        """
//        = GC in ${clock.elapsedSinceLast()}
//          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
//          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
//          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
//          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
//        """.trimIndent()
//    )

    // build resulting descriptors:
    val components = mergedTree.createGlobalBuilderComponents(storageManager, parameters.statsCollector)
    mergedTree.accept(DeclarationsBuilderVisitor1(components), emptyList())
    println(
        """
        = New descriptors (stage 1) built in ${clock.elapsedSinceLast()}
          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
        """.trimIndent()
    )

    // GC is useless
//    for (i in 1..10) {
//        System.gc()
//    }
//
//    println(
//        """
//        = GC in ${clock.elapsedSinceLast()}
//          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
//          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
//          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
//          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
//        """.trimIndent()
//    )

    mergedTree.accept(DeclarationsBuilderVisitor2(components), emptyList())
    println(
        """
        = New descriptors (stage 2) built in ${clock.elapsedSinceLast()}
          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
        """.trimIndent()
    )

    // GC is useless
//    for (i in 1..10) {
//        System.gc()
//    }
//
//    println(
//        """
//        = GC in ${clock.elapsedSinceLast()}
//          Free  (MB): ${Runtime.getRuntime().freeMemory() / 1024 / 1024}
//          Used  (MB): ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024}
//          Total (MB): ${Runtime.getRuntime().totalMemory() / 1024 / 1024}
//          Max   (MB): ${Runtime.getRuntime().maxMemory() / 1024 / 1024}
//        """.trimIndent()
//    )

    val modulesByTargets = LinkedHashMap<Target, Collection<ModuleDescriptor>>() // use linked hash map to preserve order
    components.targetComponents.forEach {
        val target = it.target
        check(target !in modulesByTargets)

        modulesByTargets[target] = components.cache.getAllModules(it.index)
    }

    return CommonizationPerformed(modulesByTargets)
}
