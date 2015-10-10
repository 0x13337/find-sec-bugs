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
package com.h3xstream.findsecbugs.injection.sql;

import com.h3xstream.findsecbugs.injection.InjectionPoint;
import com.h3xstream.findsecbugs.injection.InjectionSource;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * Focus on hibernate API for SQL/HQL injection.
 */
public class HibernateInjectionSource implements InjectionSource {

    protected static final String SQL_INJECTION_TYPE = "SQL_INJECTION_HIBERNATE";

    @Override
    public InjectionPoint getInjectableParameters(InvokeInstruction ins, ConstantPoolGen cpg, InstructionHandle insHandle) {
        //ByteCode.printOpCode(ins, cpg);

        if (ins instanceof INVOKESTATIC || ins instanceof INVOKEINTERFACE) {
            String methodName = ins.getMethodName(cpg);
            String methodSignature = ins.getSignature(cpg);
            String className = ins.getClassName(cpg);

            //Criterion.sqlRestriction
            if (ins instanceof INVOKESTATIC && className.equals("org.hibernate.criterion.Restrictions") && methodName.equals("sqlRestriction")){
                if(methodSignature.equals("(Ljava/lang/String;)Lorg/hibernate/criterion/Criterion;")) {
                    return new InjectionPoint(new int[]{0}, SQL_INJECTION_TYPE);
                }
                else if (methodSignature.equals("(Ljava/lang/String;Ljava/lang/Object;Lorg/hibernate/type/Type;)Lorg/hibernate/criterion/Criterion;")) {
                    return new InjectionPoint(new int[]{2}, SQL_INJECTION_TYPE);
                }
                else if (methodSignature.equals("(Ljava/lang/String;[Ljava/lang/Object;[Lorg/hibernate/type/Type;)Lorg/hibernate/criterion/Criterion;")) {
                    return new InjectionPoint(new int[]{2}, SQL_INJECTION_TYPE);
                }
            }
            //Session.createQuery
            else if (ins instanceof INVOKEINTERFACE && className.equals("org.hibernate.Session") && methodName.equals("createQuery") &&
                    methodSignature.equals("(Ljava/lang/String;)Lorg/hibernate/Query;")) {
                return new InjectionPoint(new int[]{0}, SQL_INJECTION_TYPE);
            }
            //Session.createSQLQuery
            else if (ins instanceof INVOKEINTERFACE && className.equals("org.hibernate.Session") && methodName.equals("createSQLQuery") &&
                    methodSignature.equals("(Ljava/lang/String;)Lorg/hibernate/SQLQuery;")) {
                return new InjectionPoint(new int[]{0}, SQL_INJECTION_TYPE);
            }
        }

        return InjectionPoint.NONE;
    }

}
