package coofluence.model;

public class Result {
    private final String link;
    private final String title;
    private final String author;
    private final String content;
    private final String date;

    public Result(String link, String title, String author, String content, String date) {
        this.link = link;

        this.title = title;
        this.author = author;
        this.content = content;
        this.date = date;
    }
}
