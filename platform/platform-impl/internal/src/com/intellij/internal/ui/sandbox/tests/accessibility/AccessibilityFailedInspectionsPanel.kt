// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.internal.ui.sandbox.tests.accessibility

import com.intellij.internal.ui.sandbox.UISandboxPanel
import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.panel
import javax.accessibility.*
import javax.swing.*

internal class AccessibilityFailedInspectionsPanel : UISandboxPanel {
  override val title: String = "Failed inspections"
  override fun createContent(disposable: Disposable): JComponent {
    return panel {
      row {
        text("This page shows examples of components with accessibility problems and is intended to test the accessibility audit feature in the UI Inspector.")
      }
      group("AccessibleStateSetContainsFocusableInspection") {
        row {
          // not focusable
          val button = object : JButton() {
            override fun getAccessibleContext(): AccessibleContext {
              return object : AccessibleJButton() {
                override fun getAccessibleText(): AccessibleText? {
                  return null
                }

                override fun isEnabled(): Boolean {
                  return true
                }

                override fun isShowing(): Boolean {
                  return true
                }

                override fun isVisible(): Boolean {
                  return true
                }
              }
            }
            override fun isFocusable(): Boolean {
              return false
            }
          }
          button.accessibleContext.accessibleDescription = "AccessibleStateSetContainsFocusableInspection"
          cell(button)

          button("Normal Button") {}
        }
      }
      group("AccessibleActionNotNullInspection") {
        row {
          // null accessibleAction
          val component = object : JCheckBox() {
            override fun getAccessibleContext(): AccessibleContext {
              return object : AccessibleJComponent() {
                override fun getAccessibleAction(): AccessibleAction? {
                  return null
                }
              }
            }
          }
          component.accessibleContext.accessibleDescription = "Checkbox description"
          cell(component)

          checkBox("Normal checkbox")
        }
        group("AccessibleNameAndDescriptionNotEqualInspection") {
          row {
            //stateSet is full and name is null
            val button = JButton()
            button.accessibleContext.accessibleDescription = "button"
            button.accessibleContext.accessibleName = "button"
            cell(button)

            button("Normal Button") {}
          }
        }
        group("AccessibleValueNotNullInspection") {
          row {
            // stateSet is full and name is null
            val bar = object : JProgressBar() {
              override fun getAccessibleContext(): AccessibleContext {
                return object : AccessibleJComponent() {
                  override fun getAccessibleRole(): AccessibleRole {
                    return AccessibleRole.PROGRESS_BAR
                  }

                  override fun getAccessibleValue(): AccessibleValue? {
                    return null
                  }
                }
              }
            }
            bar.accessibleContext.accessibleDescription = "checkbox role and action null"
            cell(bar)

            val label = JLabel("Normal progress bar")
            val progressBar = JProgressBar()
            label.labelFor = progressBar
            cell(progressBar)
            cell(label)
          }
        }
        group("2 Failed Inspections") {
          row {
            //password role and text null
            val password = object : JPasswordField() {
              override fun getAccessibleContext(): AccessibleContext {
                return object : AccessibleJPasswordField() {
                  override fun getAccessibleText(): AccessibleText? {
                    return null
                  }
                }
              }
            }
            password.accessibleContext.accessibleDescription = "password role and text null"
            cell(password)

            val label = JLabel("Normal password field")
            val passwordField = JPasswordField()
            label.labelFor = passwordField
            cell(passwordField)
            cell(label)
          }
        }
        group("3 Failed Inspections") {
          row {
            //password role and text null
            val button = object : JButton() {
              override fun getAccessibleContext(): AccessibleContext {
                return object : AccessibleJButton() {
                  override fun getAccessibleText(): AccessibleText? {
                    return null
                  }

                  override fun isEnabled(): Boolean {
                    return true
                  }

                  override fun isShowing(): Boolean {
                    return true
                  }

                  override fun isVisible(): Boolean {
                    return true
                  }

                  override fun getAccessibleName(): String {
                    return ""
                  }

                  override fun getAccessibleAction(): AccessibleAction? {
                    return null
                  }
                }
              }

              override fun isFocusable(): Boolean {
                return true
              }
            }
            button.accessibleContext.accessibleDescription = "password role and text null"
            cell(button)

            val label = JLabel("Normal password field")
            val passwordField = JPasswordField()
            label.labelFor = passwordField
            cell(passwordField)
            cell(label)
          }
        }

      }
    }
  }
}