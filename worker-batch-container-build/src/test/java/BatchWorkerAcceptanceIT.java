import com.hpe.caf.worker.batch.BatchTestControllerProvider;
import com.hpe.caf.worker.testing.execution.TestRunner;
import org.junit.Test;

/**
 * Created by gibsodom on 26/02/2016.
 */
public class BatchWorkerAcceptanceIT {
    @Test
    public void testWorker() throws Exception {

        TestRunner.runTests(new BatchTestControllerProvider());
    }
}
