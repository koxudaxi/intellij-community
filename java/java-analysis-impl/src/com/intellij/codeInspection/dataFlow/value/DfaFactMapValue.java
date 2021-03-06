// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.dataFlow.value;

import com.intellij.codeInspection.dataFlow.DfaFactMap;
import com.intellij.codeInspection.dataFlow.DfaFactType;
import com.intellij.codeInspection.dataFlow.DfaNullability;
import com.intellij.codeInspection.dataFlow.rangeSet.LongRangeSet;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DfaFactMapValue extends DfaValue {
  private final DfaFactMap myFacts;

  DfaFactMapValue(DfaValueFactory factory, DfaFactMap facts) {
    super(factory);
    myFacts = facts;
  }

  public <T> DfaValue withFact(@NotNull DfaFactType<T> factType, @Nullable T value) {
    DfaFactMap newFacts = myFacts.with(factType, value);
    return newFacts == myFacts ? this : getFactory().getFactFactory().createValue(newFacts);
  }

  public DfaFactMap getFacts() {
    return myFacts;
  }

  @Nullable
  public <T> T get(@NotNull DfaFactType<T> factType) {
    return myFacts.get(factType);
  }

  @Override
  public String toString() {
    return myFacts.toString();
  }

  public static class Factory {
    private final DfaValueFactory myFactory;
    private final Map<DfaFactMap, DfaFactMapValue> myValues = new HashMap<>();

    Factory(DfaValueFactory factory) {
      myFactory = factory;
    }

    public <T> DfaValue createValue(@NotNull DfaFactType<T> factType, @Nullable T value) {
      if (factType == DfaFactType.RANGE && value instanceof LongRangeSet) {
        if (((LongRangeSet)value).isEmpty()) {
          throw new IllegalArgumentException("Empty range is disallowed (a bottom value)");
        }
        Long constantValue = ((LongRangeSet)value).getConstantValue();
        if (constantValue != null) {
          return myFactory.getConstFactory().createFromValue(constantValue, PsiType.LONG);
        }
      }
      return createValue(DfaFactMap.EMPTY.with(factType, value));
    }

    public DfaValue createValue(DfaFactMap facts) {
      if (facts == DfaFactMap.EMPTY) {
        return DfaUnknownValue.getInstance();
      }
      if (facts.get(DfaFactType.NULLABILITY) == DfaNullability.NULL) {
        return myFactory.getConstFactory().getNull();
      }
      return myValues.computeIfAbsent(facts, f -> new DfaFactMapValue(myFactory, f));
    }
  }
}
