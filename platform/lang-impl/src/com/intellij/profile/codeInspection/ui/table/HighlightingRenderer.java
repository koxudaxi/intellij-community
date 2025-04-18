// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.profile.codeInspection.ui.table;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.util.Pair;
import com.intellij.profile.codeInspection.ui.HighlightingChooser;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

@ApiStatus.Internal
public abstract class HighlightingRenderer extends ComboBoxTableRenderer<TextAttributesKey> {
  @Unmodifiable
  private final List<? extends Pair<TextAttributesKey, @Nls String>> myEditorAttributesKey;

  static final TextAttributesKey EDIT_HIGHLIGHTING = TextAttributesKey.createTextAttributesKey("-");

  HighlightingRenderer(@NotNull @Unmodifiable List<? extends Pair<TextAttributesKey, @Nls String>> editorAttributesKey) {
    super(editorAttributesKey.stream().map(pair -> pair.first).toArray(TextAttributesKey[]::new));
    myEditorAttributesKey = editorAttributesKey;
    withClickCount(1);
  }

  @Override
  protected int getPreferredSizeMaxValues() {
    return 0; // there can be long values inside, and this makes the popup appearing on mouse hover unnecessarily large
  }

  @Override
  protected @Nls String getTextFor(@NotNull TextAttributesKey value) {
    if (HighlightingChooser.ATTRIBUTES_CUSTOM_NAMES.containsKey(value)) {
      return HighlightingChooser.ATTRIBUTES_CUSTOM_NAMES.get(value).get();
    }

    String text = value.getExternalName();
    for (Pair<TextAttributesKey, @Nls String> pair: myEditorAttributesKey) {
      if (value == pair.first) {
        text = pair.second;
        break;
      }
    }
    text = HighlightingChooser.stripColorOptionCategory(text);
    return text;
  }

  @Override
  protected ListSeparator getSeparatorAbove(TextAttributesKey value) {
    return value == EDIT_HIGHLIGHTING ? new ListSeparator() : null;
  }

  @Override
  public void onClosed(@NotNull LightweightWindowEvent event) {
    super.onClosed(event);
    if (getCellEditorValue() == EDIT_HIGHLIGHTING) {
      openColorSettings();
    }
  }

  abstract void openColorSettings();
}
