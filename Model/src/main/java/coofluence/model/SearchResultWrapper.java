package coofluence.model;

import java.util.List;

public class SearchResultWrapper {
    private final List<Result> results;
    private final Long tookInMillis;
    private final Float maxScore;
    private final Long totalHits;

    public SearchResultWrapper(List<Result> results, Long tookInMillis, Float maxScore, Long totalHits) {
        this.results = results;
        this.tookInMillis = tookInMillis;
        this.maxScore = maxScore;
        this.totalHits = totalHits;
    }
}
