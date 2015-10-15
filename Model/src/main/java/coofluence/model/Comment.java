package coofluence.model;

import java.time.LocalDateTime;

public class Comment implements Indexable {
    private String containerId;

    @Override
    public Type getType() {
        return Type.COMMENT;
    }

    @Override
    public String getAuthorUserName() {
        return author.getAuthorUserName();
    }

    public Comment(String id) {
        this.id = id;
    }

    private final String id;
    private LocalDateTime updateDate;
    private String title;
    private String content;

    private Author author;

    public String getId() {
        return id;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getContainerId() {
        return containerId;
    }
}
