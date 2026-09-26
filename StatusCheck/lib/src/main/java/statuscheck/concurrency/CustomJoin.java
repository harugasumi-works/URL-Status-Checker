package statuscheck.concurrency;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.function.Consumer;

import statuscheck.domain.ScanResult;

public class CustomJoin implements StructuredTaskScope.Joiner<ScanResult, Void> {
    
    private final Consumer<ScanResult> onResult;
    private final Runnable onTaskFailure;

    public CustomJoin(Consumer<ScanResult> onResult, Runnable onTaskFailure) {
        this.onResult = onResult;
        this.onTaskFailure = onTaskFailure;
    }

	
    @SuppressWarnings("preview")
	@Override 
    public boolean onComplete(Subtask<ScanResult> subtask) { 
    	if (subtask.state() != Subtask.State.SUCCESS) {
    		onTaskFailure.run();
    		return false;
    	}
    	ScanResult result = subtask.get();
    	onResult.accept(result);
        return false;
    }


	@Override
	public Void result() throws Throwable {
		return null;
	}
    
    
	
}
