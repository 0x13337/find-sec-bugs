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
package com.h3xstream.findsecbugs.crypto;

import com.h3xstream.findbugs.test.BaseDetectorTest;
import com.h3xstream.findbugs.test.EasyBugReporter;
import org.testng.annotations.Test;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class CipherWithNoIntegrityDetectorTest extends BaseDetectorTest {

    /**
     * Test to confirm the fix of https://github.com/h3xstream/find-sec-bugs/issues/24
     * @throws Exception
     */
    @Test
    public void detectCustomDigest() throws Exception {
        //Locate test code
        String[] files = {
                getClassFilePath("testcode/crypto/CipherNoIntegrityBugFixRsa")
        };

        //Run the analysis
        EasyBugReporter reporter = spy(new EasyBugReporter());
        analyze(files, reporter);

        verify(reporter,never()).doReportBug(
                bugDefinition()
                        .bugType("ECB_MODE_TYPE")
                        .build()
        );
    }
}
