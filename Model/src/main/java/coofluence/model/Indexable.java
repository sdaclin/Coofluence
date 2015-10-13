package coofluence.model;

public interface Indexable {
    enum Type {COMMENT, BLOG_POST, PAGE}

    Type getType();
}
