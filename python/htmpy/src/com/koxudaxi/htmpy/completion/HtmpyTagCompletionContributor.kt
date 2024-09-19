// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.koxudaxi.htmpy.completion

import com.koxudaxi.htmpy.collectComponents
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
import com.koxudaxi.htmpy.getContextForCodeCompletion
import com.koxudaxi.htmpy.isHtmpy

class HtmpyTagCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      PlatformPatterns.psiElement(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
          parameters: CompletionParameters, context: ProcessingContext, resultSet: CompletionResultSet,
        ) {
          val position = parameters.position
          val containingFile = position.parent.containingFile
          if (containingFile !is PyFile) return
          val hostElement = position.parent.containingFile.getUserData(FileContextUtil.INJECTED_IN_ELEMENT)?.element as? PyStringLiteralExpression
                            ?: return
          val pyFormattedStringElement = hostElement.firstChild as? PyFormattedStringElement ?: return
          val typeContext = getContextForCodeCompletion(pyFormattedStringElement)
          if (!isHtmpy(pyFormattedStringElement, typeContext)) return
          val pyStringLiteralExpression = hostElement.parent as? PyStringLiteralExpression ?: return
          collectComponents(pyStringLiteralExpression, { _, _, _, _ -> {} }, { _, _, _, _ -> },
                            { _, tag, _, _ ->
                              // For tags
                              val startOffset = hostElement.text.indexOf(DUMMY_IDENTIFIER)
                              if (startOffset >= 0 && tag.range.contains(startOffset)) {
                                resultSet.runRemainingContributors(parameters) { completionResult ->
                                  val lookupElement = completionResult.lookupElement
                                  val result = when (lookupElement.psiElement) {
                                    is PyFunction -> completionResult.withLookupElement(
                                      LookupElementDecorator.withInsertHandler(
                                        lookupElement
                                      ) { _, _ -> })

                                    else -> completionResult
                                  }
                                  resultSet.passResult(result)

                                }
                              }
                            })
        }
      })

  }
}