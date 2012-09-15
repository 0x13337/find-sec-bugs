package com.h3xstream.findsecbugs.injection.sql;

import com.h3xstream.findbugs.test.BaseDetectorTest;
import com.h3xstream.findbugs.test.EasyBugReporter;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class HibernateInjectionSourceTest extends BaseDetectorTest {

    @Test
    public void detectInjection() throws Exception {
        //Locate com.h3xstream.findbugs.test code
        String[] files = {
                getClassFilePath("testcode/sqli/HibernateSql")
        };

        //Run the analysis
        EasyBugReporter reporter = spy(new EasyBugReporter());
        analyze(files, reporter);

        for (Integer line : Arrays.asList(18, 20, 22)) {
            verify(reporter).doReportBug(
                bugDefinition()
                        .bugType("SQL_INJECTION")
                        .inClass("HibernateSql").inMethod("testQueries").atLine(line)
                        .build()
            );
        }

        //Only the previous 3 cases should be marked as vulnerable
        verify(reporter, times(3)).doReportBug(
            bugDefinition()
                    .bugType("SQL_INJECTION")
                    .build()
        );
    }
}
