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
import edu.umd.cs.findbugs.util.ClassName;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.bcel.generic.ObjectType;

/**
 * Representation of taint dataflow facts (dataflow values) for each slot in
 * {@link TaintFrame}
 *
 * @author David Formanek (Y Soft Corporation, a.s.)
 */
public class Taint {

    public enum State {

        TAINTED(false, true, false),
        UNKNOWN(false, false, true),
        SAFE(true, false, false),
        NULL(true, false, false),
        INVALID(false, false, false);

        private final boolean isSafe;
        private final boolean isTainted;
        private final boolean isUnknown;

        State(boolean isSafe, boolean isTainted, boolean isUnknown) {
            this.isSafe = isSafe;
            this.isTainted = isTainted;
            this.isUnknown = isUnknown;
        }

        public static State merge(State a, State b) {
            if (a == null || b == null) {
                throw new NullPointerException(
                        "use Taint.State." + INVALID.name() + " instead of null"
                );
            }
            if (a == TAINTED || b == TAINTED) {
                return TAINTED;
            }
            if (a == UNKNOWN || b == UNKNOWN) {
                return UNKNOWN;
            }
            if (a == SAFE || b == SAFE) {
                return SAFE;
            }
            if (a == NULL || b == NULL) {
                return NULL;
            }
            assert a == INVALID && b == INVALID;
            return INVALID;
        }
    }

    private State state;
    private static final int INVALID_INDEX = -1;
    private int variableIndex;
    private final Set<TaintLocation> taintLocations;
    private final Set<TaintLocation> unknownLocations;
    private final Set<Integer> parameters;
    private State nonParametricState;
    private ObjectType realInstanceClass;

    public Taint(State state) {
        Objects.requireNonNull(state, "state is null");
        if (state == State.INVALID) {
            throw new IllegalArgumentException("state not allowed");
        }
        this.state = state;
        this.variableIndex = INVALID_INDEX;
        this.unknownLocations = new HashSet<TaintLocation>();
        this.taintLocations = new HashSet<TaintLocation>();
        this.parameters = new HashSet<Integer>();
        nonParametricState = State.INVALID;
        this.realInstanceClass = null;
    }

    public Taint(Taint taint) {
        Objects.requireNonNull(taint, "taint is null");
        assert taint.state != null;
        this.state = taint.state;
        this.variableIndex = taint.variableIndex;
        this.taintLocations = new HashSet<TaintLocation>(taint.taintLocations);
        this.unknownLocations = new HashSet<TaintLocation>(taint.unknownLocations);
        this.parameters = new HashSet<Integer>(taint.getParameters());
        this.nonParametricState = taint.nonParametricState;
        this.realInstanceClass = taint.realInstanceClass;
    }

    public State getState() {
        assert state != null && state != State.INVALID;
        return state;
    }

    void setState(State state) {
        Objects.requireNonNull(state, "state is null");
        if (state == State.INVALID) {
            throw new IllegalArgumentException("state not allowed to be set");
        }
        this.state = state;
    }

    public int getVariableIndex() {
        if (variableIndex == INVALID_INDEX) {
            throw new IllegalStateException("index not set or has been invalidated");
        }
        assert variableIndex >= 0;
        return variableIndex;
    }

    public boolean hasValidVariableIndex() {
        return variableIndex != INVALID_INDEX;
    }

    void setVariableIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("negative index");
        }
        variableIndex = index;
    }

    void invalidateVariableIndex() {
        variableIndex = INVALID_INDEX;
    }

    public void addLocation(TaintLocation location, boolean isKnownTaintSource) {
        Objects.requireNonNull(location, "location is null");
        if (isKnownTaintSource) {
            taintLocations.add(location);
        } else {
            unknownLocations.add(location);
        }
    }

    public Set<TaintLocation> getLocations() {
        if (taintLocations.isEmpty()) {
            return Collections.unmodifiableSet(unknownLocations);
        }
        return Collections.unmodifiableSet(taintLocations);
    }

    public boolean isSafe() {
        return state.isSafe;
    }

    public boolean isTainted() {
        // in context of taint analysis, null value is safe too
        return state.isTainted;
    }

    public boolean isUnknown() {
        return state.isUnknown;
    }

    void addParameter(int parameterIndex) {
        if (parameterIndex < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
        parameters.add(parameterIndex);
    }

    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public Set<Integer> getParameters() {
        return Collections.unmodifiableSet(parameters);
    }

    public State getNonParametricState() {
        return nonParametricState;
    }
    
    void setNonParametricState(State state) {
        Objects.requireNonNull(state, "state is null");
        if (state == State.INVALID) {
            throw new IllegalArgumentException("state not allowed to be set");
        }
        nonParametricState = state;
    }

    public ObjectType getRealInstanceClass() {
        return realInstanceClass;
    }

    void setRealInstanceClass(ObjectType objectType) {
        // can be null
        realInstanceClass = objectType;
    }

    public String getRealInstanceClassName() {
        if (realInstanceClass == null) {
            return null;
        }
        return ClassName.toSlashedClassName(realInstanceClass.getClassName());
    }

    public static Taint valueOf(String stateName) {
        // exceptions thrown from Enum.valueOf
        return valueOf(State.valueOf(stateName));
    }

    public static Taint valueOf(State state) {
        Objects.requireNonNull(state, "state is null");
        if (state == State.INVALID) {
            return null;
        }
        return new Taint(state);
    }

    public static Taint merge(Taint a, Taint b) {
        if (a == null) {
            if (b == null) {
                return null;
            } else {
                return new Taint(b);
            }
        } else if (b == null) {
            return new Taint(a);
        }
        assert a != null && b != null;
        Taint result = new Taint(State.merge(a.getState(), b.getState()));
        if (a.variableIndex == b.variableIndex) {
            result.variableIndex = a.variableIndex;
        }
        result.taintLocations.addAll(a.taintLocations);
        result.taintLocations.addAll(b.taintLocations);
        result.unknownLocations.addAll(a.unknownLocations);
        result.unknownLocations.addAll(b.unknownLocations);
        result.parameters.addAll(a.parameters);
        result.parameters.addAll(b.parameters);
        if (a.hasParameters()) {
            if (b.hasParameters()) {
                result.nonParametricState = State.merge(a.nonParametricState, b.nonParametricState);
            } else {
                result.nonParametricState = State.merge(b.state, a.nonParametricState);
            }
        } else {
            if (b.hasParameters()) {
                result.nonParametricState = State.merge(a.state, b.nonParametricState);
            }
        }
        if (a.realInstanceClass != null && b.realInstanceClass != null) {
            try {
                if (a.realInstanceClass.equals(b.realInstanceClass)
                        || b.realInstanceClass.subclassOf(a.realInstanceClass)) {
                    result.realInstanceClass = a.realInstanceClass;
                } else if (a.realInstanceClass.subclassOf(b.realInstanceClass)) {
                    result.realInstanceClass = b.realInstanceClass;
                }
            } catch (ClassNotFoundException ex) {
                AnalysisContext.reportMissingClass(ex);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Taint)) {
            return false;
        }
        Taint other = (Taint) obj;
        return this.state == other.state
                && this.variableIndex == other.variableIndex
                && this.taintLocations.equals(other.taintLocations)
                && this.unknownLocations.equals(other.unknownLocations)
                && this.parameters.equals(other.parameters)
                && this.nonParametricState == other.nonParametricState
                && Objects.equals(this.realInstanceClass, other.realInstanceClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, variableIndex, taintLocations, unknownLocations,
                parameters, nonParametricState, realInstanceClass);
    }

    @Override
    public String toString() {
        assert state != null;
        StringBuilder sb = new StringBuilder(state.name().substring(0, 1));
        if (hasValidVariableIndex()) {
            sb.append(variableIndex);
        }
        if (!parameters.isEmpty()) {
            sb.append(parameters);
        }
        assert nonParametricState != null;
        if (nonParametricState != State.INVALID) {
            sb.append('(').append(nonParametricState.name().substring(0, 1)).append(')');
        }
        return sb.toString();
    }
}
