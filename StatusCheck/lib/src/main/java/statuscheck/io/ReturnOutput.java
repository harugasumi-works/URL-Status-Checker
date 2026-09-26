package statuscheck.io;

import java.util.List;

import statuscheck.domain.ExecutionResult;
import statuscheck.domain.Report;
import statuscheck.domain.ScanOutput;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;

public class ReturnOutput {
	
	@SuppressWarnings("preview")
	public static ScanOutput fromResults(List<ScanResult> results) {
	    if (results.isEmpty()) {
	        return null;
	    }
	    List<ScanResult> successes = results.stream()
	        .filter(r -> r instanceof ScanResult(_, _, Success(_, _, _)))
	        .toList();
	    List<ScanResult> failures = results.stream()
	        .filter(r -> !(r instanceof ScanResult(_, _, Success(_, _, _))))
	        .toList();
	    return output(new ExecutionResult(successes, failures));
	}
	
	@SuppressWarnings("preview")
	public static ScanOutput output(ExecutionResult result) {
		Report stat = Report.summarize(result);
		return new ScanOutput(stat, 
				LazyConstant.of(() -> {return JsonDto.convert(stat); }),
				LazyConstant.of(() -> {return CsvDto.convert(result); })
			);
	}
	



}
