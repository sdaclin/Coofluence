package coofluence.index;

import com.google.common.base.Throwables;
import coofluence.model.ESConfig;
import coofluence.model.Page;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Index {
    final static Logger logger = LoggerFactory.getLogger(Index.class);
    private static final String ES_IDX = "coofluence";
    private static final String ES_MAPPING = "_mapping";

    private static final String ES_TYPE_CONFIG = "config";
    private static final String ES_TYPE_PAGE = "page";
    public static final String MAPPING_PROPERTIES = "properties";
    public static final String ANALYZER_HTML = "htmlAnalyzer";
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
                    .node();
            logger.info("ESNode started");
        }
        client = esNode.client();
    }

    /**
     * Stop the node
     */
    public static void stop() {
        if (esNode != null) {
            esNode.stop();
        }
    }

    public static void deleteIndex() {
        try {
            client.admin().indices().prepareDelete(ES_IDX).execute().actionGet();
        } catch (IndexMissingException iem) {
            logger.debug("Can't delete Index [{}], Index Missing", ES_IDX);
        }
    }

    /**
     * Return date of the most recent element in the index
     */
    public static ESConfig getOrInitESConfig() {
        ESConfig esConfig;
        try {
            final GetResponse global = client.prepareGet(ES_IDX, ES_TYPE_CONFIG, "1").execute().actionGet();
            esConfig = ESMapper.toConfig(global);
        } catch (IndexMissingException e) {
            logger.info("Coofluence need to be initialized.");
            esConfig = initializeIndex();
        }
        return esConfig;
    }

    private static ESConfig initializeIndex() {
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
            client.admin().indices().preparePutMapping(ES_IDX)
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
                    ).execute().actionGet();
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
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        ESConfig esConfig = new ESConfig();
        Calendar calendar = Calendar.getInstance();
        calendar.set(1980, Calendar.JANUARY, 1);
        esConfig.setLastChangeDate(LocalDateTime.of(1980, Month.JANUARY, 1, 0, 0));

        client.prepareIndex(ES_IDX, ES_TYPE_CONFIG, "1").setSource(ESMapper.toJson(esConfig)).execute().actionGet();
        return esConfig;
    }

    public static void indexPage(Page page) {
        try {
            client.prepareIndex(ES_IDX, ES_TYPE_PAGE, page.getId())
                    .setSource(
                            jsonBuilder().startObject()
                                    .field(ESMapper.PAGE_TITLE, page.getTitle())
                                    .field(ESMapper.PAGE_CONTENT, page.getContent())
                                    .field(ESMapper.PAGE_UPDATE_DATE, page.getUpdateDate())
                                    .field(ESMapper.PAGE_AUTHOR_DISPLAY_NAME, page.getAuthorDisplayName())
                                    .field(ESMapper.PAGE_AUTHOR_USER_NAME, page.getAuthorUserName())
                                    .field(ESMapper.PAGE_AUTHOR_KEY, page.getAuthorKey())
                                    .field(ESMapper.PAGE_SPACE, page.getSpace())
                                    .field(ESMapper.PAGE_TAGS, page.getTags())
                                    .endObject())
                    .execute().actionGet();
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }
}
