package statusCheck.concurrency;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.function.Consumer;

import statusCheck.domain.ExecutionResult;
import statusCheck.domain.ScanResult;
import statusCheck.domain.Success;

public class CustomJoin implements StructuredTaskScope.Joiner<ScanResult, ExecutionResult> {
	
	private final ConcurrentLinkedQueue<ScanResult> successes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ScanResult> failures = new ConcurrentLinkedQueue<>();
    
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
    	if (result instanceof ScanResult(_, _, Success(_, _, _))) {
            successes.add(subtask.get());
        } else failures.add(subtask.get());
        return false;
    }
	
    @Override
    public ExecutionResult result() { //
        return new ExecutionResult(List.copyOf(successes), List.copyOf(failures));
    }
}
