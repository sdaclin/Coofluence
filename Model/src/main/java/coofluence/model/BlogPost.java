package coofluence.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class BlogPost implements Indexable {
    @Override
    public Type getType() {
        return Type.BLOG_POST;
    }

    private final String id;

    private LocalDateTime updateDate;
    private String title;
    private String content;

    private Author author;

    private String space;
    private Set<String> tags = new HashSet<>();

    public BlogPost(String id) {
        this.id = id;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }


    public String getId() {
        return id;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void addTag(String name) {
        this.tags.add(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(id).append(" [").append(space).append("] ").append(title).append("\n");
        sb.append("-------------------------------------------------------------------------------------------").append("\n");
        sb.append(content).append("\n");
        sb.append("-------------------------------------------------------------------------------------------").append("\n");
        return sb.toString();
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }
}
