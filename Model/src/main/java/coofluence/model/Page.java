package coofluence.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by sdaclin on 08/10/2015.
 */
public class Page {
    private final String contentId;
    private String title;
    private String space;
    private String content;
    private Set<String> tags = new HashSet<>();

    public Page(String contentId) {
        this.contentId = contentId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getSpace() {
        return space;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void addTag(String name) {
        this.tags.add(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(contentId).append(" [").append(space).append("] ").append(title).append("\n");
        sb.append("-------------------------------------------------------------------------------------------").append("\n");
        sb.append(content).append("\n");
        sb.append("-------------------------------------------------------------------------------------------").append("\n");
        return sb.toString();
    }
}
