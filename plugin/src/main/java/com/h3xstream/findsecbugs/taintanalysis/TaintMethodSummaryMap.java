/**
 * Find Security Bugs
 * Copyright (c) Philippe Arteau, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.h3xstream.findsecbugs.taintanalysis;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Map of taint summaries for all known methods
 *
 * This class extends HashMap:
 * <ul>
 *  <li>The key is the method signature (ie :
 org/hibernate/Session.createQuery(Ljava/lang/String;)Lorg/hibernate/Query;)</li>
 *  <li>The value is the behavior of the method
 *  ("0" for param index 0 is tainted,
 *  "UNKNOWN" if the method does not become tainted base on the value,
 *  "TAINTED" if the result must be consider unsafe)</li>
 * </ul>
 *
 * @author David Formanek (Y Soft Corporation, a.s.)
 */
public class TaintMethodSummaryMap extends HashMap<String, TaintMethodSummary> {
    
    private static final long serialVersionUID = 1L;
    private final Map<String, TaintClassSummary> taintClassSummaryMap = new HashMap<String, TaintClassSummary>();

    /**
     * Dumps all the summaries for debugging
     * 
     * @param output stream where to output the summaries
     */
    public void dump(PrintStream output) {
        TreeSet<String> keys = new TreeSet<String>(keySet());
        for (String key : keys) {
            output.println(key + ":" + get(key));
        }
    }

    /**
     * Loads method summaries from stream checking the format
     * 
     * @param input input stream of configured summaries
     * @param checkRewrite whether to check duplicit summaries
     * @throws IOException if cannot read the stream or the format is bad
     * @throws IllegalArgumentException for bad method format
     * @throws IllegalStateException if there are duplicit configurations
     */
    public void load(InputStream input, final boolean checkRewrite) throws IOException {
        new TaintMethodSummaryMapLoader().load(input, new TaintMethodSummaryMapLoader.TaintMethodSummaryReceiver() {
            @Override
            public void receiveTaintMethodSummary(String typeSignature, String summary) throws IOException {
                if (TaintMethodSummary.accepts(typeSignature)) {
                    if (checkRewrite && containsKey(typeSignature)) {
                        throw new IllegalStateException("Summary for " + typeSignature + " already loaded");
                    }
                    TaintMethodSummary taintMethodSummary = TaintMethodSummary.load(summary);
                    put(typeSignature, taintMethodSummary);
                    return;
                }

                if (TaintClassSummary.accepts(typeSignature)) {
                    if (checkRewrite && taintClassSummaryMap.containsKey(typeSignature)) {
                        throw new IllegalStateException("Summary for " + typeSignature + " already loaded");
                    }
                    TaintClassSummary taintClassSummary = TaintClassSummary.load(summary);
                    taintClassSummaryMap.put(typeSignature, taintClassSummary);
                    return;
                }

                throw new IllegalArgumentException("Invalid full method name " + typeSignature + " configured");
            }
        });
    }


    public boolean isClassImmutable(String typeSignature) {
        if (!isClassType(typeSignature)) {
            return false;
        }

        TaintClassSummary summary = taintClassSummaryMap.get(typeSignature);
        if (summary == null) {
            return false;
        }

        return summary.isImmutable();
    }

    public boolean isClassTaintSafe(String typeSignature) {
        if (!isClassType(typeSignature)) {
            return false;
        }

        TaintClassSummary taintClassSummary = getClassSummary(typeSignature);
        if (taintClassSummary == null) {
            return false;
        }

        return taintClassSummary.getTaintState().equals(Taint.State.SAFE);
    }

    public Taint.State getClassTaintState(String typeSignature, Taint.State defaultState) {
        if (!isClassType(typeSignature)) {
            return defaultState;
        }

        TaintClassSummary taintClassSummary = getClassSummary(typeSignature);

        if (taintClassSummary == null) {
            return defaultState;
        }

        Taint.State classSummaryTaintState = taintClassSummary.getTaintState();

        if (classSummaryTaintState.equals(TaintClassSummary.DEFAULT_TAINT_STATE)) {
            return defaultState;
        }

        return classSummaryTaintState;
    }

    public TaintClassSummary getClassSummary(String typeSignature) {
        if (!isClassType(typeSignature)) {
            return null;
        }

        return taintClassSummaryMap.get(typeSignature);
    }

    private boolean isClassType(String typeSignature) {
        return typeSignature != null && typeSignature.length() > 2 && typeSignature.charAt(0) == 'L';
    }
}
