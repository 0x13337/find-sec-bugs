/**
 * Find Security Bugs
 * Copyright (c) 2014, Philippe Arteau, All rights reserved.
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
package com.h3xstream.findsecbugs.injection;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

public interface InjectionSource {

    /**
     * Before starting intensive analysis on variable flow and iterating on every instruction,
     * this function will make sure the injection type can occurs in the current class base on
     * its constant pool gen. All classes dependencies can be found in this pool.
     *
     * @param cpg
     * @return
     */
    boolean isCandidate(ConstantPoolGen cpg);

    /**
     * The implementation should identify method that are susceptible to injection and return
     * parameters index that can injected.
     *
     * @param ins       Instruction visit
     * @param cpg       ConstantPool (needed to find the class name and method name associate to instruction)
     * @param insHandle instruction handle (needed to look at the instruction around the current instruction)
     * @return
     */
    int[] getInjectableParameters(InvokeInstruction ins, ConstantPoolGen cpg, InstructionHandle insHandle);
}
