/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildErrorLoop
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.expressions.impl.FirBlockImpl
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.name.ClassId

inline val FirAnnotationCall.coneClassLikeType: ConeClassLikeType?
    get() = ((annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeClassLikeType)

inline val FirAnnotationCall.classId: ClassId?
    get() = coneClassLikeType?.lookupTag?.classId

fun <T> buildConstOrErrorExpression(source: FirSourceElement?, kind: FirConstKind<T>, value: T?, diagnostic: FirDiagnostic): FirExpression =
    value?.let {
        buildConstExpression(source, kind, it)
    } ?: buildErrorExpression {
        this.source = source
        this.diagnostic = diagnostic
    }

inline val FirCall.arguments: List<FirExpression> get() = argumentList.arguments

inline val FirCall.argument: FirExpression get() = argumentList.arguments.first()

inline val FirCall.argumentMapping: Map<FirExpression, FirValueParameter>?
    get() = (argumentList as? FirResolvedArgumentList)?.mapping

fun FirExpression.toResolvedCallableReference(): FirResolvedNamedReference? {
    return (this as? FirQualifiedAccess)?.calleeReference as? FirResolvedNamedReference
}

fun FirExpression.toResolvedCallableSymbol(): FirCallableSymbol<*>? {
    return toResolvedCallableReference()?.resolvedSymbol as FirCallableSymbol<*>?
}

fun buildErrorLoop(source: FirSourceElement?, diagnostic: FirDiagnostic): FirErrorLoop {
    return buildErrorLoop {
        this.source = source
        this.diagnostic = diagnostic
    }
}

fun buildErrorExpression(source: FirSourceElement?, diagnostic: FirDiagnostic): FirErrorExpression {
    return buildErrorExpression {
        this.source = source
        this.diagnostic = diagnostic
    }
}

fun <D : Any> FirBlock.transformStatementsIndexed(transformer: FirTransformer<D>, dataProducer: (Int) -> D?): FirBlock {
    when (this) {
        is FirBlockImpl -> statements.transformInplace(transformer, dataProducer)
        is FirSingleExpressionBlock -> {
            dataProducer(0)?.let { transformStatements(transformer, it) }
        }
    }
    return this
}

fun <D : Any> FirBlock.transformAllStatementsExceptLast(transformer: FirTransformer<D>, data: D): FirBlock {
    val threshold = statements.size - 1
    return transformStatementsIndexed(transformer) { index ->
        data.takeIf { index < threshold }
    }
}