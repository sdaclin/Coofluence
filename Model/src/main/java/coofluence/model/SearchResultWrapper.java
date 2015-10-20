package coofluence.model;

import java.util.List;

public class SearchResultWrapper {
    private final List<Result> results;
    private final List<AggregationResult> aggregationResults;
    private final Long tookInMillis;
    private final Float maxScore;
    private final Long totalHits;

    public SearchResultWrapper(List<Result> results, List<AggregationResult> aggregationResults, Long tookInMillis, Float maxScore, Long totalHits) {
        this.results = results;
        this.aggregationResults = aggregationResults;
        this.tookInMillis = tookInMillis;
        this.maxScore = maxScore;
        this.totalHits = totalHits;
    }
}
