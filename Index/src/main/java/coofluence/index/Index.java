package coofluence.index;

import com.google.common.base.Throwables;
import coofluence.model.BlogPost;
import coofluence.model.Comment;
import coofluence.model.Page;
import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Index {
    final static Logger logger = LoggerFactory.getLogger(Index.class);
    private static final String ES_IDX = "coofluence";

    private static final String ES_TYPE_CONFIG = "config";
    private static final String ES_TYPE_PAGE = "page";
    private static final String ES_TYPE_BLOG_POST = "blogPost";
    private static final java.lang.String ES_TYPE_COMMENT = "comment";

    private static final String MAPPING_PROPERTIES = "properties";
    private static final String ANALYZER_HTML = "htmlAnalyzer";
    private static Node esNode;
    private static Client client;

    private Index() {
    }

    /**
     * Start the node
     */
    public static void start() {
        if (esNode == null) {
            esNode = NodeBuilder.nodeBuilder()//
                    .clusterName("coofluence")//
                    .local(true)
                    .settings(
                            ImmutableSettings.settingsBuilder()
                                    .put("network.host", "127.0.0.1")
                                    .put("http.enabled", true)
                                    .put("http.cors.enabled", true))
                    .node().start();

        }
        client = esNode.client();
        // Todo find a way to synchronize node start with client
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("ESNode started");
    }

    /**
     * It does what it says
     */
    public static void stop() {
        if (esNode != null) {
            esNode.stop();
        }
    }

    /**
     * It does what it says
     */
    public static void deleteIndex() {
        try {
            client.admin().indices().prepareDelete(ES_IDX).execute().actionGet();
        } catch (IndexMissingException iem) {
            logger.debug("Can't delete Index [{}], Index Missing", ES_IDX);
        }
    }

    /**
     * Init index if necessary
     * Check index existence with ES exists method
     */
    public static void initIndexIfNecessary() {
        try {
            final ExistsResponse existsResponse = client.prepareExists(ES_IDX).execute().actionGet();
            logger.info("Coofluence exists and already initialized.");
            if (!existsResponse.exists()) {
                throw new RuntimeException();
            }
        } catch (RuntimeException e) {
            logger.info("Coofluence need to be initialized.");
            initializeIndex();
        }
    }

    private static void initializeIndex() {
        try {
            // Create Idx
            client.admin().indices()
                    .prepareCreate(ES_IDX)
                    .setSettings(
                            jsonBuilder()
                                    .startObject()
                                    .startObject("analysis")
                                    .startObject("analyzer")
                                    .startObject(ANALYZER_HTML)
                                    .startArray("filter").value("standard").value("lowercase").value("stop").value("asciifolding").endArray()
                                    .startArray("char_filter").value("html_strip").endArray()
                                    .field("tokenizer", "standard")
                                    .endObject()
                                    .endObject()
                                    .endObject()
                                    .endObject())
                    .execute().actionGet();

            // Adding mapping configs
            /*client.admin().indices().preparePutMapping(ES_IDX)
                    .setType(ES_TYPE_CONFIG)
                    .setSource(
                            jsonBuilder()
                                    .startObject()
                                    .startObject(MAPPING_PROPERTIES)
                                    .startObject(ESMapper.CONFIG_LAST_CHANGE_DATE)
                                    .field("type", "date")
                                    .field("index", "no")
                                    .endObject()
                                    .endObject()
                    ).execute().actionGet();*/
            client.admin().indices().preparePutMapping(ES_IDX)
                    .setType(ES_TYPE_PAGE)
                    .setSource(
                            jsonBuilder()
                                    .startObject()
                                    .startObject(MAPPING_PROPERTIES)
                                    .startObject(ESMapper.PAGE_TITLE).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.PAGE_UPDATE_DATE).field("type", "date").endObject()
                                    .startObject(ESMapper.PAGE_AUTHOR_USER_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.PAGE_AUTHOR_DISPLAY_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.PAGE_AUTHOR_KEY).field("type", "string").endObject()
                                    .startObject(ESMapper.PAGE_CONTENT).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.PAGE_SPACE).field("type", "string").endObject()
                                    .startObject(ESMapper.PAGE_TAGS).field("type", "string").endObject()
                                    .endObject()

                    ).execute().actionGet();
            client.admin().indices().preparePutMapping(ES_IDX)
                    .setType(ES_TYPE_BLOG_POST)
                    .setSource(
                            jsonBuilder()
                                    .startObject()
                                    .startObject(MAPPING_PROPERTIES)
                                    .startObject(ESMapper.BLOG_POST_TITLE).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.BLOG_POST_UPDATE_DATE).field("type", "date").endObject()
                                    .startObject(ESMapper.BLOG_POST_AUTHOR_USER_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.BLOG_POST_AUTHOR_DISPLAY_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.BLOG_POST_AUTHOR_KEY).field("type", "string").endObject()
                                    .startObject(ESMapper.BLOG_POST_CONTENT).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.BLOG_POST_SPACE).field("type", "string").endObject()
                                    .startObject(ESMapper.BLOG_POST_TAGS).field("type", "string").endObject()
                                    .endObject()

                    ).execute().actionGet();
            client.admin().indices().preparePutMapping(ES_IDX)
                    .setType(ES_TYPE_COMMENT)
                    .setSource(
                            jsonBuilder()
                                    .startObject()
                                    .startObject(MAPPING_PROPERTIES)
                                    .startObject(ESMapper.COMMENT_TITLE).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.COMMENT_CONTENT).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.COMMENT_UPDATE_DATE).field("type", "date").endObject()
                                    .startObject(ESMapper.COMMENT_AUTHOR_USER_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.COMMENT_AUTHOR_DISPLAY_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.COMMENT_AUTHOR_KEY).field("type", "string").endObject()
                                    .endObject()

                    ).execute().actionGet();
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        /*ESConfig esConfig = new ESConfig();
        esConfig.setLastChangeDate(LocalDateTime.of(1980, Month.JANUARY, 1, 0, 0));

        client.prepareIndex(ES_IDX, ES_TYPE_CONFIG, "1").setSource(ESMapper.toJson(esConfig)).execute().actionGet();
        return esConfig;*/
    }

    public static void indexPage(Page page) {
        try {
            final IndexResponse indexResponse = client.prepareIndex(ES_IDX, ES_TYPE_PAGE, page.getId())
                    .setSource(
                            jsonBuilder().startObject()
                                    .field(ESMapper.PAGE_TITLE, page.getTitle())
                                    .field(ESMapper.PAGE_CONTENT, page.getContent())
                                    .field(ESMapper.PAGE_UPDATE_DATE, page.getUpdateDate())
                                    .field(ESMapper.PAGE_AUTHOR_DISPLAY_NAME, page.getAuthor().getAuthorDisplayName())
                                    .field(ESMapper.PAGE_AUTHOR_USER_NAME, page.getAuthor().getAuthorUserName())
                                    .field(ESMapper.PAGE_AUTHOR_KEY, page.getAuthor().getAuthorKey())
                                    .field(ESMapper.PAGE_SPACE, page.getSpace())
                                    .field(ESMapper.PAGE_TAGS, page.getTags())
                                    .endObject())
                    .execute().actionGet();
            if (!indexResponse.isCreated()) {
                throw new IllegalStateException();
            }
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException("Problem during indexation of page [" + page.getId() + "]", e);
        }
    }

    public static void indexBlogPost(BlogPost page) {
        try {
            final IndexResponse indexResponse = client.prepareIndex(ES_IDX, ES_TYPE_BLOG_POST, page.getId())
                    .setSource(
                            jsonBuilder().startObject()
                                    .field(ESMapper.PAGE_TITLE, page.getTitle())
                                    .field(ESMapper.PAGE_CONTENT, page.getContent())
                                    .field(ESMapper.PAGE_UPDATE_DATE, page.getUpdateDate())
                                    .field(ESMapper.PAGE_AUTHOR_DISPLAY_NAME, page.getAuthor().getAuthorDisplayName())
                                    .field(ESMapper.PAGE_AUTHOR_USER_NAME, page.getAuthor().getAuthorUserName())
                                    .field(ESMapper.PAGE_AUTHOR_KEY, page.getAuthor().getAuthorKey())
                                    .field(ESMapper.PAGE_SPACE, page.getSpace())
                                    .field(ESMapper.PAGE_TAGS, page.getTags())
                                    .endObject())
                    .execute().actionGet();
            if (!indexResponse.isCreated()) {
                throw new IllegalStateException();
            }
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException("Problem during indexation of page [" + page.getId() + "]", e);
        }
    }

    public static void indexComment(Comment comment) {
        try {
            System.out.println(comment);
            final UpdateResponse updateResponse = client.prepareUpdate(ES_IDX, ES_TYPE_PAGE, comment.getContainerId())
                    .setSource(
                            jsonBuilder().startArray("comments")
                                    .startObject()
                                    .field(ESMapper.COMMENT_TITLE, comment.getTitle())
                                    .field(ESMapper.COMMENT_CONTENT, comment.getContent())
                                    .field(ESMapper.COMMENT_UPDATE_DATE, comment.getUpdateDate())
                                    .field(ESMapper.COMMENT_AUTHOR_DISPLAY_NAME, comment.getAuthor().getAuthorDisplayName())
                                    .field(ESMapper.COMMENT_AUTHOR_USER_NAME, comment.getAuthor().getAuthorUserName())
                                    .field(ESMapper.COMMENT_AUTHOR_KEY, comment.getAuthor().getAuthorKey())
                                    .endObject()
                                    .endArray())
                    .execute().actionGet();
            if (!updateResponse.isCreated()) {
                throw new IllegalStateException();
            }
        } catch (IllegalStateException e) {
            throw new RuntimeException("Problem during indexation of page [" + comment.getId() + "]", e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LocalDateTime getMaxUpdatedDate() {
        final SearchResponse searchResponse = client.prepareSearch(ES_IDX).setTypes(ES_TYPE_PAGE).setSearchType(SearchType.DFS_QUERY_AND_FETCH).addField(ESMapper.PAGE_UPDATE_DATE).addSort(ESMapper.PAGE_UPDATE_DATE, SortOrder.DESC).execute().actionGet();
        if (searchResponse.getHits().totalHits() > 0) {
            return LocalDateTime.parse(searchResponse.getHits().getAt(0).field(ESMapper.PAGE_UPDATE_DATE).getValue(), ISO_DATE_TIME);
        } else {
            return LocalDateTime.of(1980, Month.JANUARY, 1, 0, 0);
        }
    }
}
