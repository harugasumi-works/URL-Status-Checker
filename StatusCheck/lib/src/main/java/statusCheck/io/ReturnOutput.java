package statusCheck.io;

import statusCheck.domain.ExecutionResult;
import statusCheck.domain.Report;
import statusCheck.domain.ScanOutput;

public class ReturnOutput {
	
	@SuppressWarnings("preview")
	public static ScanOutput output(ExecutionResult result) {
		Report stat = Report.summarize(result);
		return new ScanOutput(Report.summarize(result), 
				LazyConstant.of(() -> {return JSON_DTO.convert(stat); }),
				LazyConstant.of(() -> {return CSV_DTO.convert(result); })
			);
	}
}
