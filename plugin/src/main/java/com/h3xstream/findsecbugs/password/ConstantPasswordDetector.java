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
package com.h3xstream.findsecbugs.password;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

/**
 * General detector for hard coded passwords and cryptographic keys
 * 
 * @author David Formanek
 */
@OpcodeStack.CustomUserValue
public class ConstantPasswordDetector extends OpcodeStackDetector {

    private static final String HARD_CODE_PASSWORD_TYPE = "HARD_CODE_PASSWORD";
    private final BugReporter bugReporter;
    private boolean staticInitializerSeen = false;

    // configuration file with password methods
    private static final String CONFIG_DIR = "password-methods";
    private static final String METHODS_FILENAME = "password-methods-all.txt";

    // full method names
    private static final String GET_BYTES_STRING = "java/lang/String.getBytes(Ljava/lang/String;)[B";
    private static final String GET_BYTES = "java/lang/String.getBytes()[B";
    private static final String TO_CHAR_ARRAY = "java/lang/String.toCharArray()[C";
    private static final String BIGINTEGER_CONSTRUCTOR_STRING = "java/math/BigInteger.<init>(Ljava/lang/String;)V";
    private static final String BIGINTEGER_CONSTRUCTOR_STRING_RADIX = "java/math/BigInteger.<init>(Ljava/lang/String;I)V";
    private static final String BIGINTEGER_CONSTRUCTOR_BYTE = "java/math/BigInteger.<init>([B)V";
    private static final String BIGINTEGER_BYTE_SIGNUM = "java/math/BigInteger.<init>(I[B)V";
    
    // suspicious variable names with password or keys
    private static final String PASSWORD_NAMES = ".*(pass|pwd|psw|secret|key|cipher|crypt|des|aes).*";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_NAMES, Pattern.CASE_INSENSITIVE);

    private final Map<String, Integer> sinkMethods = new HashMap<String, Integer>();
    private boolean isFirstArrayStore = false;
    private boolean wasToConstArrayConversion = false;
    private Set<String> hardCodedFields = new HashSet<String>();
    private String calledMethod = null;
    
    public ConstantPasswordDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        try {
            loadMap(METHODS_FILENAME, sinkMethods, "#");
        } catch (IOException ex) {
            throw new RuntimeException("cannot load resources", ex);
        }
    }

    @Override
    public void visit(JavaClass javaClass) {
        staticInitializerSeen = false;
        Method[] methods = javaClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(STATIC_INITIALIZER_NAME)) {
                // check field initialization before visiting methods
                doVisitMethod(method);
                staticInitializerSeen = true;
                break;
            }
        }
        isFirstArrayStore = false;
        wasToConstArrayConversion = false;
        //hardCodedFields.clear();
    }

    @Override
    public void visit(Method method) {
        isFirstArrayStore = false;
        wasToConstArrayConversion = false;
    }

    @Override
    public void sawOpcode(int seen) {
        if (isAlreadyAnalyzed()) {
            return;
        }
        markHardCodedItemsFromFlow();
        printOpCode(seen); // debug
        if (seen == NEWARRAY) {
            isFirstArrayStore = true;
        }
        if (isStoringToArray(seen)) {
            markArraysHardCodedOrNot();
            isFirstArrayStore = false;
        }
        if (wasToConstArrayConversion) {
            markTopItemHardCoded();
            wasToConstArrayConversion = false;
        }
        if (seen == PUTFIELD || seen == PUTSTATIC) {
            saveArrayFieldIfHardCoded();
        }
        if (isInvokeInstruction(seen)) {
            calledMethod = getCalledMethodName();
            wasToConstArrayConversion = isToConstArrayConversion();
            markBigIntegerHardCodedOrNot();
            reportBadSink();
        }
    }

    private boolean isAlreadyAnalyzed() {
        return getMethodName().equals(STATIC_INITIALIZER_NAME) && staticInitializerSeen;
    }

    private void markHardCodedItemsFromFlow() {
        for (int i = 0; i < stack.getStackDepth(); i++) {
            OpcodeStack.Item stackItem = stack.getStackItem(i);
            if (stackItem.getConstant() != null || stackItem.isNull()) {
                setHardCodedItem(stackItem);
                System.out.println("setting uv to item " + i);
            }
            if (hasHardCodedFieldSource(stackItem)) {
                setHardCodedItem(stackItem);
                System.out.println("field in set");
            } else {
                System.out.println("field not in set");
            }
            System.out.println("item " + i + ": " + stackItem);
        }
    }
    
    private boolean hasHardCodedFieldSource(OpcodeStack.Item stackItem) {
        XField xField = stackItem.getXField();
        if (xField == null) {
            return false;
        }
        System.out.println("checking field " + xField);
        String[] split = xField.toString().split(" ");
        int length = split.length;
        if (length < 2) {
            return false;
        }
        String fieldSignature = split[split.length - 1];
        if (!"[C".equals(fieldSignature)
                && !"[B".equals(fieldSignature)
                && !"Ljava/math/BigInteger;".equals(fieldSignature)) {
            return false;
        }
        String fieldName = split[split.length - 2] + fieldSignature;
        System.out.println("Checking if '" + fieldName + "' is in field set");
        return hardCodedFields.contains(fieldName);
    }
    
    private static boolean isStoringToArray(int seen) {
        // TODO add ICONST_X
        return seen == CASTORE || seen == BASTORE || seen == SASTORE || seen == IASTORE;
    }

    private void markArraysHardCodedOrNot() {
        if (isFirstArrayStore
                && hasHardCodedStackItem(0)
                && hasHardCodedStackItem(1)) {
            setHardCodedItem(2);
        }
        if (!hasHardCodedStackItem(0) || !hasHardCodedStackItem(1)) {
            // then array not hard coded
            stack.getStackItem(2).setUserValue(null);
        }
    }

    private void markTopItemHardCoded() {
            assert stack.getStackDepth() > 0;
            setHardCodedItem(0);
            System.out.println("Setting uv: " + stack.getStackItem(0));
            System.out.println("isHardCodedTypeConversion=false");
    }

    private void saveArrayFieldIfHardCoded() {
            if (hasHardCodedStackItem(0)) {
                String fieldName = getFullFieldName();
                hardCodedFields.add(fieldName);
                System.out.println("added to fields: " + fieldName);
            }
    }
    
    private static boolean isInvokeInstruction(int seen) {
        return seen >= INVOKEVIRTUAL && seen <= INVOKEINTERFACE;
    }
    
    private boolean isToConstArrayConversion() {
        return isInMethodWithConst(TO_CHAR_ARRAY, 0)
                || isInMethodWithConst(GET_BYTES, 0)
                || isInMethodWithConst(GET_BYTES_STRING, 1); 
    }

    private void markBigIntegerHardCodedOrNot() {
        if (isInMethodWithConst(BIGINTEGER_CONSTRUCTOR_STRING, 0)
                || isInMethodWithConst(BIGINTEGER_CONSTRUCTOR_BYTE, 0)) {
            setHardCodedItem(1);
        } else if (isInMethodWithConst(BIGINTEGER_CONSTRUCTOR_STRING_RADIX, 1)
                || isInMethodWithConst(BIGINTEGER_BYTE_SIGNUM, 0)) {
            setHardCodedItem(2);
        }
    }
    
    private void reportBadSink() {
        if (sinkMethods.containsKey(calledMethod)) {
            int offset = sinkMethods.get(calledMethod);
            if (hasHardCodedStackItem(offset) && !stack.getStackItem(offset).isNull()) {
                reportBug(calledMethod, Priorities.HIGH_PRIORITY);
            }
        }
    }
    
    private void setHardCodedItem(int stackOffset) {
        setHardCodedItem(stack.getStackItem(stackOffset));
    }

    private void setHardCodedItem(OpcodeStack.Item stackItem) {
        stackItem.setUserValue(Boolean.TRUE);
    }

    private boolean hasHardCodedStackItem(int stackOffset) {
        return stack.getStackItem(stackOffset).getUserValue() != null;
    }
    
    private boolean isInMethodWithConst(String method, int stackOffset) {
        return method.equals(calledMethod) && hasHardCodedStackItem(stackOffset);
    }
    
    private String getFullFieldName() {
        String fieldName = getDottedClassConstantOperand() + "."
                + getNameConstantOperand() + getSigConstantOperand();
        return fieldName;
    }

    private String getCalledMethodName() {
        String methodNameWithSignature = getNameConstantOperand() + getSigConstantOperand();
        return getClassConstantOperand() + "." + methodNameWithSignature;
    }

    private void reportBug(String value, int priority) {
        bugReporter.reportBug(new BugInstance(
                this, HARD_CODE_PASSWORD_TYPE, priority)
                .addClass(this).addMethod(this).addSourceLine(this).addString(value));
    }

    private void loadMap(String filename, Map<String, Integer> map, String separator) throws IOException {
        BufferedReader reader = null;
        try {
            reader = getReader(filename);
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] tuple = line.split(separator);
                map.put(tuple[0], Integer.parseInt(tuple[1]));
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private BufferedReader getReader(String filename) {
        String path = CONFIG_DIR + "/" + filename;
        return new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(path)
        ));
    }
}
