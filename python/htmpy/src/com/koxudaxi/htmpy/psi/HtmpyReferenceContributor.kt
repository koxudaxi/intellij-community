// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.koxudaxi.htmpy.psi

import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyFormattedStringElement
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.koxudaxi.htmpy.getContext
import com.koxudaxi.htmpy.isHtmpy


class HtmpyReferenceContributor : PsiReferenceContributor() {
    internal object Holder {
        @JvmField
        val TAG_STRING_PATTERN = PlatformPatterns.psiElement()
                //.with(object : PatternCondition<PsiElement>("isFormatFunctionHtmp") {
                //    override fun accepts(expression: PsiElement, context: ProcessingContext): Boolean {
                //        if (expression !is PyFormattedStringElement) return false
                //        return isHtmpy(expression, getContext(expression))
                //    }
                //})
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(Holder.TAG_STRING_PATTERN,
                                            HtmpyReferenceProvider())
    }
}