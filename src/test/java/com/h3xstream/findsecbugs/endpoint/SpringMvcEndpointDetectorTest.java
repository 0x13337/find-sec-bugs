package com.h3xstream.findsecbugs.endpoint;

import edu.umd.cs.findbugs.test.BaseDetectorTest;
import edu.umd.cs.findbugs.test.EasyBugReporter;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SpringMvcEndpointDetectorTest extends BaseDetectorTest {

    @Test
    public void detectSpringController() throws Exception {
        //Locate test code
        String[] files = {
                getClassFilePath("testcode/SpringTestController")
        };

        //Run the analysis
        EasyBugReporter reporter = spy(new EasyBugReporter());
        analyze(files, reporter);


        //Assertions
        for(Integer line : Arrays.asList(18,24)) {
            verify(reporter,never()).doReportBug(
                    bugDefinition()
                            .bugType("XXE")
                            .build()
            );
        }
    }
}
