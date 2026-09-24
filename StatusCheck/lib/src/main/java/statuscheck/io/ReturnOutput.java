package statuscheck.io;

import statuscheck.domain.ExecutionResult;
import statuscheck.domain.Report;
import statuscheck.domain.ScanOutput;

public class ReturnOutput {
	
	@SuppressWarnings("preview")
	public static ScanOutput output(ExecutionResult result) {
		Report stat = Report.summarize(result);
		return new ScanOutput(Report.summarize(result), 
				LazyConstant.of(() -> {return JsonDto.convert(stat); }),
				LazyConstant.of(() -> {return CsvDto.convert(result); })
			);
	}
}
