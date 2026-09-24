package statuscheck.domain;

public record ScanOutput(Report report, LazyConstant<JSON> json, LazyConstant<CSV> csv) {

}
