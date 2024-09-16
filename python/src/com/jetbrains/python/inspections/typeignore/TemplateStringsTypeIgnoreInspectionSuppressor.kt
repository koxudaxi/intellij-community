// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.inspections.typeignore

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.codeInsight.isTemplateString
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression

class TemplateStringsTypeIgnoreInspectionSuppressor: InspectionSuppressor {

  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    if (element is PsiFile) return false
    if (element.containingFile !is PyFile) return false
    if (toolId !in inspectionsToSuppress) return false
    if (element is PyStringLiteralExpression) {
      return isTemplateString(element)
    }
    val pyReferenceExpression = element as? PyReferenceExpression
    if (pyReferenceExpression != null) {
      val targetExpression = pyReferenceExpression.reference.resolve() as? PyTargetExpression
      val pyStringLiteralExpression = targetExpression?.findAssignedValue() as? PyStringLiteralExpression
      if (pyStringLiteralExpression != null) {
        return isTemplateString(pyStringLiteralExpression)
      }
    }
    return false
  }

  override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
    return SuppressQuickFix.EMPTY_ARRAY
  }

  companion object {
    private val inspectionsToSuppress = listOf("PyUnresolvedReferences",
                                               "PyTypeHints",
                                               "PyTypeChecker",
                                               "PyRedeclaration",
                                               "PyArgumentList",
                                               "PyFinal",
                                               "PyProtocol",
                                               "PyTypedDict")
  }
}