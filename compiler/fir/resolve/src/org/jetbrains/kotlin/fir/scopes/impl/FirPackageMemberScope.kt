/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ScopeProcessor
import org.jetbrains.kotlin.fir.scopes.noSubstitution
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirPackageMemberScope(val fqName: FqName, val session: FirSession) : FirScope() {

    private val symbolProvider = FirSymbolProvider.getInstance(session)

    private val classifierCache = mutableMapOf<Name, FirClassifierSymbol<*>?>()

    private val callableCache = mutableMapOf<Name, List<FirCallableSymbol<*>>>()

    override fun processClassifiersByName(
        name: Name,
        processor: ScopeProcessor<FirClassifierSymbol<*>>
    ) {
        if (name.asString().isEmpty()) return


        val symbol = classifierCache.getOrPut(name) {
            val unambiguousFqName = ClassId(fqName, name)
            symbolProvider.getClassLikeSymbolByFqName(unambiguousFqName)
        }

        if (symbol != null) {
            processor.noSubstitution(symbol)
        }
    }

    override fun processFunctionsByName(name: Name, processor: ScopeProcessor<FirFunctionSymbol<*>>) {
        val symbols = callableCache.getOrPut(name) {
            symbolProvider.getTopLevelCallableSymbols(fqName, name)
        }
        for (symbol in symbols) {
            if (symbol is FirFunctionSymbol<*>) {
                processor.noSubstitution(symbol)
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: ScopeProcessor<FirVariableSymbol<*>>) {
        val symbols = callableCache.getOrPut(name) {
            symbolProvider.getTopLevelCallableSymbols(fqName, name)
        }
        for (symbol in symbols) {
            if (symbol is FirPropertySymbol) {
                processor.noSubstitution(symbol)
            }
        }
    }
}
