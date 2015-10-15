package coofluence.model;

import java.util.List;

import static java.util.Arrays.asList;

public class Result {
    private final String link;
    private final String title;
    private final String author;
    private final String content;
    private final String date;
    private final List<String> tags;
    private final String space;

    public Result(String link, String title, String author, String content, String date, String tags, String space) {
        this.link = link;
        this.title = title;
        this.author = author;
        this.content = content;
        this.date = date;
        this.tags = asList(tags.split(" "));
        this.space = space;
    }
}
