package statusCheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class LazyConstantTest {

	@SuppressWarnings("preview")
	@Test
	void supplierInvokedOnceUnderConcurrentFirstAccess() throws Exception {
	    AtomicInteger calls = new AtomicInteger();
	    LazyConstant<String> lazy = LazyConstant.of(() -> {
	        calls.incrementAndGet();
	        return "value";
	    });

	    var n = 10;
	    var pool = Executors.newFixedThreadPool(n);
	    var ready = new CountDownLatch(n);
	    var go = new CountDownLatch(1);
	    List<Future<String>> futures = new ArrayList<>();
	    for (int i = 0; i < n; i++) {
	        futures.add(pool.submit(() -> {
	            ready.countDown();
	            go.await();
	            return lazy.get();
	        }));
	    }
	    ready.await();
	    go.countDown();
	    for (var f : futures) f.get();
	    pool.shutdown();

	    assertEquals(1, calls.get());
	}
}
