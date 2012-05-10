package com.h3xstream.findsecbugs;

import static org.mockito.Mockito.*;

import edu.umd.cs.findbugs.test.BaseDetectorTest;
import edu.umd.cs.findbugs.test.EasyBugReporter;
import org.testng.annotations.Test;

public class PredictableRandomDetectorTest extends BaseDetectorTest {

	
	@Test
	public void detectUsePredictableRandom() throws Exception {
        //Locate test code
		String[] files = {
                getClassFilePath("com/h3xstream/findsecbugs/testcode/InsecureRandom")
        };

        //Run the analysis
		EasyBugReporter reporter = spy(new EasyBugReporter());
        analyze(files, reporter);

        //Assertions
        //1rst variation new Random()
		verify(reporter).doReportBug(
                bugDefinition()
                    .bugType("SECURITY_PREDICTABLE_RANDOM")
                    .inClass("InsecureRandom")
                    .inMethod("test1")
                    .atLine(11)
                .build()
        );
        //2nd variation Math.random()
        verify(reporter).doReportBug(
                bugDefinition()
                    .bugType("SECURITY_PREDICTABLE_RANDOM")
                    .inClass("InsecureRandom")
                    .inMethod("test2")
                    .atLine(18)
                .build()
        );

	}

}
