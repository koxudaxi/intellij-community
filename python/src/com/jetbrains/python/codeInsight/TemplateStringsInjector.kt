package com.jetbrains.python.codeInsight

import com.intellij.lang.Language
import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyClassLikeType
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import kotlin.math.min

private const val TYPING_PROTOCOLS = "typing.Protocol"
private const val TEMPLATE_TYPE_NAME = "Template"
private const val SOURCE_ATTRIBUTE_NAME = "source"
private const val ANNOTATED = "Annotated"
private const val LITERAL = "Literal"
private const val MAX_PREFIX_LENGTH = 3
private const val TEMPLATE_STRING_PREFIX = 't'
private val LANGUAGES = mapOf(
  "html" to HTMLLanguage.INSTANCE,
  "xml" to XMLLanguage.INSTANCE,
)

private fun hasTemplateStringsPrefix(text: String): Boolean = text.take(min(MAX_PREFIX_LENGTH, text.length)).any {
  TEMPLATE_STRING_PREFIX == it.lowercaseChar()
}

fun isTemplateString(psiElement: PsiElement): Boolean =
  psiElement is PyStringLiteralExpression
  && psiElement.firstChild is PyFormattedStringElement
  && hasTemplateStringsPrefix(psiElement.firstChild.firstChild.text)

class TemplateStringsInjector : PyInjectorBase() {


  private fun getLiteralText(pySubscriptionExpression: PySubscriptionExpression): String? {
    if (pySubscriptionExpression.children[0].text != LITERAL) return null
    val literal = pySubscriptionExpression.children[1] as? PyStringLiteralExpression ?: return null
    return literal.stringValue
  }

  private fun getLanguageFromText(text: String): Language? = LANGUAGES[text.lowercase()]

  private fun isTemplateType(pyClassLikeType: PyClassLikeType, context: TypeEvalContext): Boolean =
    pyClassLikeType.name == TEMPLATE_TYPE_NAME && pyClassLikeType.getSuperClassTypes(context).filterNotNull().any { templateSuperClass -> templateSuperClass.classQName == TYPING_PROTOCOLS }

  private fun getLanguageFromTemplateType(pyType: PyType, context: TypeEvalContext): Language? {
    val pyClassType = pyType as? PyClassType ?: return null
    if (!isTemplateType(pyClassType, context)) {
      if (!pyClassType.getSuperClassTypes(context).filterNotNull().any { isTemplateType(it, context) }) return null
    }
    val sourceAttribute = pyClassType.pyClass.findClassAttribute(SOURCE_ATTRIBUTE_NAME, true, context) ?: return null
    val annotation = sourceAttribute.annotation?.children?.filterIsInstance<PySubscriptionExpression>()?.firstOrNull() ?: return null
    if (annotation.children.size < 2) return null
    if (annotation.children[0].text != ANNOTATED) return null
    val annotationValue = annotation.children[1] as? PyTupleExpression ?: return null
    val language = annotationValue.children[1]
    // TODO: Improve language detection
    if (language !is PyReferenceExpression) return null
    val pyElement = language.reference.resolve()
    if (pyElement !is PyTargetExpression) return null
    val pySubscriptionExpression = pyElement.findAssignedValue() as? PySubscriptionExpression ?: return null
    val literalText = getLiteralText(pySubscriptionExpression) ?: return null
    return getLanguageFromText(literalText)
  }

  private fun getTypeFromAssignment(host: PsiElement, context: TypeEvalContext): PyType? {
    val assignment = host.parent as? PyAssignmentStatement ?: return null
    val target = assignment.firstChild as? PyTargetExpression ?: return null
    return context.getType(target)
  }

  override fun registerInjection(
    registrar: MultiHostRegistrar,
    context: PsiElement,
  ): PyInjectionUtil.InjectionResult {
    val result = super.registerInjection(registrar, context)
    return if (result === PyInjectionUtil.InjectionResult.EMPTY &&
               context is PsiLanguageInjectionHost &&
               context.getContainingFile() is PyFile &&
               isTemplateString(context)
    ) {
      return registerPyElementInjection(registrar, context)
    }
    else result
  }

  override fun getInjectedLanguage(context: PsiElement): Language? {
    return null
  }

  private fun injectLanguage(registrar: MultiHostRegistrar, host: PyStringLiteralExpression, language: Language): PyInjectionUtil.InjectionResult {
    registrar.startInjecting(language)
    host.decodedFragments.forEach {
      if (!it.second.startsWith("{")) {
        registrar.addPlace("", "", host, TextRange(it.first.startOffset, it.first.endOffset))
      }
    }
    try {
      registrar.doneInjecting()
    }
    catch (e: Exception) {
      return PyInjectionUtil.InjectionResult.EMPTY
    }
    return PyInjectionUtil.InjectionResult(true, true)
  }

  private fun registerPyElementInjection(
    registrar: MultiHostRegistrar,
    host: PsiLanguageInjectionHost,
  ): PyInjectionUtil.InjectionResult {
    if (host !is PyStringLiteralExpression) return PyInjectionUtil.InjectionResult.EMPTY
    val context = TypeEvalContext.codeAnalysis(host.project, host.containingFile)
    val assignmentType = getTypeFromAssignment(host, context)
    if (assignmentType is PyClassType) {
      val language = getLanguageFromTemplateType(assignmentType, context)
      if (language is Language) {
        return injectLanguage(registrar, host, language)
      }
    }
    val pyCallExpression = (host.parent as? PyArgumentList)?.callExpression
    if (pyCallExpression != null) {
      val pyParameter = pyCallExpression.multiMapArguments(PyResolveContext.defaultContext(context))
        .firstOrNull()
        ?.mappedParameters
        ?.filter { it.key == host }
        ?.firstNotNullOfOrNull { it.value }
      if (pyParameter != null) {
        val parameterType = pyParameter.getType(context)
        if (parameterType is PyType) {
          val language = getLanguageFromTemplateType(parameterType, context)
          if (language is Language) {
            return injectLanguage(registrar, host, language)
          }
        }
      }

    }
    return PyInjectionUtil.InjectionResult.EMPTY
  }
}
