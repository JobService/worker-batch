import com.hpe.caf.worker.batch.BatchTestControllerProvider;
import com.hpe.caf.worker.testing.TestControllerSingle;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.UseAsTestName;
import com.hpe.caf.worker.testing.UseAsTestName_TestBase;
import com.hpe.caf.worker.testing.execution.TestControllerProvider;
import com.hpe.caf.worker.testing.execution.TestRunnerSingle;
import org.testng.annotations.*;

import java.util.Iterator;
import java.util.Set;

/**
 * Created by gibsodom on 26/02/2016.
 */
public class BatchWorkerAcceptanceIT extends UseAsTestName_TestBase {
    TestControllerProvider testControllerProvider;
    TestControllerSingle controller;

    @BeforeClass
    public void setUp() throws Exception {
        testControllerProvider = new BatchTestControllerProvider();

    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
        controller =  TestRunnerSingle.getTestController(testControllerProvider, false);
        controller.initialise();
    }

    @AfterMethod
    public void tearDown() throws Exception{
        controller.close();
    }

    @DataProvider(name = "MainTest")
    public Iterator<Object[]> createData() throws Exception {
        Set<Object[]> s = TestRunnerSingle.setUpTest(testControllerProvider);
        return s.iterator();
    }

    @UseAsTestName(idx = 1)
    @Test(dataProvider = "MainTest")
    public void testWorker(TestItem testItem, String testName) throws Exception {
        controller.runTests(testItem);
    }
}
