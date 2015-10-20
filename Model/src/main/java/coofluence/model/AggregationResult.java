package coofluence.model;

public class AggregationResult {
    private final String name;
    private final long docCount;

    public AggregationResult(String name, long docCount) {
        this.name = name;
        this.docCount = docCount;
    }
}
