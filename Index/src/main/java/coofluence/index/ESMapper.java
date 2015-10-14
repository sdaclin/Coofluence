package coofluence.index;

import com.google.common.base.Throwables;
import coofluence.model.ESConfig;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ESMapper {

    public static final String CONFIG_LAST_CHANGE_DATE = "lastChangeDate";

    public static final String PAGE_TITLE = "title";
    public static final String PAGE_UPDATE_DATE = "updateDate";
    public static final String PAGE_CONTENT = "content";
    public static final String PAGE_AUTHOR_DISPLAY_NAME = "authorDisplayName";
    public static final String PAGE_AUTHOR_USER_NAME = "authorUserName";
    public static final String PAGE_TAGS = "tags";
    public static final String PAGE_AUTHOR_KEY = "authorKey";
    public static final String PAGE_SPACE = "space";

    public static final String BLOG_POST_TITLE = "title";
    public static final String BLOG_POST_UPDATE_DATE = "updateDate";
    public static final String BLOG_POST_CONTENT = "content";
    public static final String BLOG_POST_AUTHOR_DISPLAY_NAME = "authorDisplayName";
    public static final String BLOG_POST_AUTHOR_USER_NAME = "authorUserName";
    public static final String BLOG_POST_TAGS = "tags";
    public static final String BLOG_POST_AUTHOR_KEY = "authorKey";
    public static final String BLOG_POST_SPACE = "space";

    public static final String COMMENT_TITLE = "title";
    public static final String COMMENT_CONTENT = "content";
    public static final String COMMENT_UPDATE_DATE = "updateDate";
    public static final String COMMENT_AUTHOR_USER_NAME = "authorUserName";
    public static final String COMMENT_AUTHOR_DISPLAY_NAME = "authorDisplayName";
    public static final String COMMENT_AUTHOR_KEY = "authorKey";

    public static final String ES_SUGGEST = "suggest";

    public static ESConfig toConfig(GetResponse response) {
        final ESConfig esConfig = new ESConfig();
        esConfig.setLastChangeDate(LocalDateTime.parse((String) response.getSource().get("lastChangeDate"), ISO_DATE_TIME));
        return esConfig;
    }

    public static IndexRequestBuilder map(IndexRequestBuilder indexRequestBuilder, ESConfig esConfig) {
        return indexRequestBuilder.setSource("lastChangeDate", esConfig.getLastChangeDate());
    }

    public static XContentBuilder toJson(ESConfig esConfig) {
        try {
            return jsonBuilder()
                    .startObject()
                    .field(CONFIG_LAST_CHANGE_DATE, esConfig.getLastChangeDate())
                    .endObject();
        } catch (IOException e) {
            Throwables.propagate(e);
            return null;
        }
    }
}
