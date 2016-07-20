import com.hpe.caf.worker.batch.BatchTestControllerProvider;
import com.hpe.caf.worker.testing.TestControllerSingle;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.execution.TestControllerProvider;
import com.hpe.caf.worker.testing.execution.TestRunnerSingle;
import org.testng.annotations.*;

import java.util.Iterator;
import java.util.Set;

/**
 * Created by gibsodom on 26/02/2016.
 */
public class BatchWorkerAcceptanceIT {
    TestControllerProvider testControllerProvider;
    TestControllerSingle controller;

    @BeforeClass
    public void setUp() throws Exception {
        testControllerProvider = new BatchTestControllerProvider();
        controller =  TestRunnerSingle.getTestController(testControllerProvider, false);
        controller.initialise();
    }

    @AfterClass
    public void tearDown() throws Exception{
        controller.close();
    }

    @DataProvider(name = "MainTest")
    public Iterator<Object[]> createData() throws Exception {
        Set<Object[]> s = TestRunnerSingle.setUpTest(testControllerProvider);
        return s.iterator();
    }

    @Test(dataProvider = "MainTest")
    public void testWorker(TestItem testItem) throws Exception {
        controller.runTests(testItem);
    }
}
