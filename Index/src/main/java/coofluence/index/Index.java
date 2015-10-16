package coofluence.index;

import com.google.common.base.Throwables;
import coofluence.model.*;
import coofluence.tools.CoofluenceProperty;
import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class Index {
    final static Logger logger = LoggerFactory.getLogger(Index.class);
    private static final String ES_IDX = "coofluence";

    private static final String ES_TYPE_PAGE = "page";
    private static final String ES_TYPE_BLOG_POST = "blogPost";

    private static final String MAPPING_PROPERTIES = "properties";
    private static final String ANALYZER_HTML = "htmlAnalyzer";
    private static final int CONTENT_PREVIEW_SIZE = 450;
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
            client.admin().indices().preparePutMapping(ES_IDX)
                    .setType(ES_TYPE_PAGE)
                    .setSource(
                            jsonBuilder()
                                    .startObject()
                                    .startObject(MAPPING_PROPERTIES)
                                    .startObject(ESMapper.ES_FIELD_ID).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_TITLE).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.ES_FIELD_UPDATE_DATE).field("type", "date").endObject()
                                    .startObject(ESMapper.ES_FIELD_AUTHOR_USER_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_AUTHOR_DISPLAY_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_AUTHOR_KEY).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_CONTENT).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.ES_FIELD_SPACE).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_TAGS).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_PAGE_SUGGEST)
                                    .field("type", "completion")
                                    .field("index_analyzer", ANALYZER_HTML)
                                    .field("search_analyzer", ANALYZER_HTML)
                                    .field("payloads", true)
                                    .endObject()
                                    .endObject()

                    ).execute().actionGet();
            client.admin().indices().preparePutMapping(ES_IDX)
                    .setType(ES_TYPE_BLOG_POST)
                    .setSource(
                            jsonBuilder()
                                    .startObject()
                                    .startObject(MAPPING_PROPERTIES)
                                    .startObject(ESMapper.ES_FIELD_ID).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_TITLE).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.ES_FIELD_UPDATE_DATE).field("type", "date").endObject()
                                    .startObject(ESMapper.ES_FIELD_AUTHOR_USER_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_AUTHOR_DISPLAY_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_AUTHOR_KEY).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_CONTENT).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.ES_FIELD_SPACE).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_FIELD_TAGS).field("type", "string").endObject()
                                    .startObject(ESMapper.ES_BLOG_POST_SUGGEST)
                                    .field("type", "completion")
                                    .field("index_analyzer", ANALYZER_HTML)
                                    .field("search_analyzer", ANALYZER_HTML)
                                    .field("payloads", true)
                                    .endObject()
                                    .endObject()

                    ).execute().actionGet();
            /*client.admin().indices().preparePutMapping(ES_IDX)
                    .setType(ES_TYPE_COMMENT)
                    .setSource(
                            jsonBuilder()
                                    .startObject()
                                    .startObject(MAPPING_PROPERTIES)
                                    .startObject(ESMapper.COMMENT_ID).field("type", "string").endObject()
                                    .startObject(ESMapper.COMMENT_TITLE).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.COMMENT_CONTENT).field("type", "string").field("analyzer", ANALYZER_HTML).endObject()
                                    .startObject(ESMapper.COMMENT_UPDATE_DATE).field("type", "date").endObject()
                                    .startObject(ESMapper.COMMENT_AUTHOR_USER_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.COMMENT_AUTHOR_DISPLAY_NAME).field("type", "string").endObject()
                                    .startObject(ESMapper.COMMENT_AUTHOR_KEY).field("type", "string").endObject()
                                    .endObject()

                    ).execute().actionGet();*/
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public static void indexPage(Page page) {
        try {
            final Document jsoupPageDocument = Jsoup.parse(page.getContent());
            final IndexResponse indexResponse = client.prepareIndex(ES_IDX, ES_TYPE_PAGE, page.getId())
                    .setSource(
                            jsonBuilder().startObject()
                                    .field(ESMapper.ES_FIELD_ID, page.getId())
                                    .field(ESMapper.ES_FIELD_TITLE, page.getTitle())
                                    .field(ESMapper.ES_FIELD_HEADING_1, jsoupPageDocument.select("h1").text())
                                    .field(ESMapper.ES_FIELD_HEADING_2, jsoupPageDocument.select("h2").text())
                                    .field(ESMapper.ES_FIELD_HEADING_3, jsoupPageDocument.select("h3").text())
                                    .field(ESMapper.ES_FIELD_EMPHASIS, jsoupPageDocument.select("em").text() + " " + jsoupPageDocument.select("b").text())
                                    .field(ESMapper.ES_FIELD_CONTENT, jsoupPageDocument.text())
                                    .field(ESMapper.ES_FIELD_UPDATE_DATE, page.getUpdateDate())
                                    .field(ESMapper.ES_FIELD_AUTHOR_DISPLAY_NAME, page.getAuthor().getAuthorDisplayName())
                                    .field(ESMapper.ES_FIELD_AUTHOR_USER_NAME, page.getAuthor().getAuthorUserName())
                                    .field(ESMapper.ES_FIELD_AUTHOR_KEY, page.getAuthor().getAuthorKey())
                                    .field(ESMapper.ES_FIELD_SPACE, page.getSpace())
                                    .field(ESMapper.ES_FIELD_TAGS, page.getTags().stream().reduce((id, acc) -> id + " " + acc).orElse(""))
                                    .startObject(ESMapper.ES_PAGE_SUGGEST)
                                    .field("input", page.getTitle())
                                    .field("payload", CoofluenceProperty.HTTP_ROOT_URI.getValue() + "/pages/viewpage.action?pageId=" + page.getId())
                                    .endObject()
                                    .endObject())
                    .execute().actionGet();
            if (!indexResponse.isCreated()) {
                logger.info("Page #{} is already created", page.getId());
            }
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException("Problem during indexation of page [" + page.getId() + "]", e);
        }
    }

    public static void indexBlogPost(BlogPost blogPost) {
        try {
            final Document jsoupBlogPostDocument = Jsoup.parse(blogPost.getContent());
            final IndexResponse indexResponse = client.prepareIndex(ES_IDX, ES_TYPE_BLOG_POST, blogPost.getId())
                    .setSource(
                            jsonBuilder().startObject()
                                    .field(ESMapper.ES_FIELD_ID, blogPost.getId())
                                    .field(ESMapper.ES_FIELD_TITLE, blogPost.getTitle())
                                    .field(ESMapper.ES_FIELD_HEADING_1, jsoupBlogPostDocument.select("h1").text())
                                    .field(ESMapper.ES_FIELD_HEADING_2, jsoupBlogPostDocument.select("h2").text())
                                    .field(ESMapper.ES_FIELD_HEADING_3, jsoupBlogPostDocument.select("h3").text())
                                    .field(ESMapper.ES_FIELD_EMPHASIS, jsoupBlogPostDocument.select("em").text() + " " + jsoupBlogPostDocument.select("b").text())
                                    .field(ESMapper.ES_FIELD_CONTENT, jsoupBlogPostDocument.text())
                                    .field(ESMapper.ES_FIELD_UPDATE_DATE, blogPost.getUpdateDate())
                                    .field(ESMapper.ES_FIELD_AUTHOR_DISPLAY_NAME, blogPost.getAuthor().getAuthorDisplayName())
                                    .field(ESMapper.ES_FIELD_AUTHOR_USER_NAME, blogPost.getAuthor().getAuthorUserName())
                                    .field(ESMapper.ES_FIELD_AUTHOR_KEY, blogPost.getAuthor().getAuthorKey())
                                    .field(ESMapper.ES_FIELD_SPACE, blogPost.getSpace())
                                    .field(ESMapper.ES_FIELD_TAGS, blogPost.getTags().stream().reduce((id, acc) -> id + " " + acc).orElse(""))
                                    .startObject(ESMapper.ES_BLOG_POST_SUGGEST)
                                    .field("input", blogPost.getTitle())
                                    .field("payload",
                                            CoofluenceProperty.HTTP_ROOT_URI.getValue() + CoofluenceProperty.HTTP_VIEW_PAGE_PATH.getValue() + blogPost.getId())
                                    .endObject()
                                    .endObject())
                    .execute().actionGet();
            if (!indexResponse.isCreated()) {
                logger.info("BlogPost #{} is already created", blogPost.getId());
            }
        } catch (IOException | IllegalStateException e) {
            throw new RuntimeException("Problem during indexation of blogPost [" + blogPost.getId() + "]", e);
        }
    }

    /*public static void indexComment(Comment comment) {
        try {
            System.out.println(comment);
            final UpdateResponse updateResponse = client.prepareUpdate(ES_IDX, ES_TYPE_PAGE, comment.getContainerId())
                    .setSource(
                            jsonBuilder().startArray("comments")
                                    .startObject()
                                    .field(ESMapper.COMMENT_ID, comment.getId())
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
        } catch (Exception e) {
            throw new RuntimeException("Problem during indexation of page [" + comment.getId() + "]", e);
        }
    }*/

    /**
     * Retrouve la date max du dernier contenu indexé
     *
     * @return
     */
    public static LocalDateTime getMaxUpdatedDate() {
        final SearchResponse searchResponse = client.prepareSearch(ES_IDX)
                .setTypes(ES_TYPE_PAGE)
                .setSearchType(SearchType.DFS_QUERY_AND_FETCH)
                .addField(ESMapper.ES_FIELD_UPDATE_DATE).addSort(ESMapper.ES_FIELD_UPDATE_DATE, SortOrder.DESC)
                .execute().actionGet();
        if (searchResponse.getHits().totalHits() > 0) {
            return LocalDateTime.parse(searchResponse.getHits().getAt(0).field(ESMapper.ES_FIELD_UPDATE_DATE).getValue(), ISO_DATE_TIME);
        } else {
            return LocalDateTime.of(1980, Month.JANUARY, 1, 0, 0);
        }
    }

    /**
     * Autocomplete the query q parameter
     * Need to be able to suggests by the middle of the word
     *
     * @param type
     * @param q
     * @return
     */
    public static List<Suggestion> getSuggestion(String type, String q) {
        final SuggestRequestBuilder suggestRequestBuilder = client.prepareSuggest(ES_IDX);

        if (type.equals("page")) {
            suggestRequestBuilder.addSuggestion(new CompletionSuggestionBuilder("completeSuggestion").text(q).field(ESMapper.ES_PAGE_SUGGEST).size(10));
        } else if (type.equals("blogPost")) {
            suggestRequestBuilder.addSuggestion(new CompletionSuggestionBuilder("completeSuggestion").text(q).field(ESMapper.ES_BLOG_POST_SUGGEST).size(10));
        }
        final SuggestResponse completeMe = suggestRequestBuilder.execute().actionGet();


        List<Suggestion> suggestions = new ArrayList<>();
        for (Suggest.Suggestion.Entry.Option me : completeMe.getSuggest().getSuggestion("completeSuggestion").getEntries().get(0).getOptions()) {
            final CompletionSuggestion.Entry.Option esSuggestion = (CompletionSuggestion.Entry.Option) me;
            suggestions.add(new Suggestion(esSuggestion.getText().string(), esSuggestion.getPayloadAsString()));
        }
        return suggestions;
    }

    /**
     * Finds results in ES for the query q
     *
     * @param q
     * @return
     */
    public static SearchResultWrapper getSearchResult(String q) {
        final SearchResponse usualSearch = client.prepareSearch(ES_IDX).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setTypes(ES_TYPE_PAGE, ES_TYPE_BLOG_POST)
                .setQuery(QueryBuilders
                        .queryStringQuery(q)
                        .field(ESMapper.ES_FIELD_TITLE).boost(3)
                        .field(ESMapper.ES_FIELD_CONTENT)
                        .field(ESMapper.ES_FIELD_TAGS).boost(3)
                        .field(ESMapper.ES_FIELD_HEADING_1).boost(3)
                        .field(ESMapper.ES_FIELD_HEADING_2).boost(2)
                        .field(ESMapper.ES_FIELD_HEADING_3).boost(1.5f)
                        .field(ESMapper.ES_FIELD_EMPHASIS).boost(1.5f))
                .addHighlightedField(ESMapper.ES_FIELD_CONTENT, CONTENT_PREVIEW_SIZE)
                .addFields(ESMapper.ES_FIELD_ID, ESMapper.ES_FIELD_TITLE, ESMapper.ES_FIELD_CONTENT, ESMapper.ES_FIELD_AUTHOR_DISPLAY_NAME, ESMapper.ES_FIELD_UPDATE_DATE, ESMapper.ES_FIELD_TAGS, ESMapper.ES_FIELD_SPACE)
                .setFrom(0)
                .setSize(20)
                .execute().actionGet();
        List<Result> results = new ArrayList<>();
        for (SearchHit searchHitFields : usualSearch.getHits()) {
            String content;
            if (searchHitFields.highlightFields().containsKey("content")) {
                content = searchHitFields.highlightFields().get("content").getFragments()[0].string();
            } else {
                String rawContent = (String) searchHitFields.field(ESMapper.ES_FIELD_CONTENT).value();
                if (rawContent.length() < CONTENT_PREVIEW_SIZE) {
                    content = rawContent;
                } else {
                    content = rawContent.substring(0, CONTENT_PREVIEW_SIZE);
                }
            }
            results.add(new Result(
                    CoofluenceProperty.HTTP_ROOT_URI.getValue() + CoofluenceProperty.HTTP_VIEW_PAGE_PATH.getValue() + searchHitFields.field(ESMapper.ES_FIELD_ID).value(),
                    searchHitFields.field(ESMapper.ES_FIELD_TITLE).value(),
                    searchHitFields.field(ESMapper.ES_FIELD_AUTHOR_DISPLAY_NAME).value(),
                    content,
                    searchHitFields.field(ESMapper.ES_FIELD_UPDATE_DATE).value(),
                    searchHitFields.field(ESMapper.ES_FIELD_TAGS).value(),
                    searchHitFields.field(ESMapper.ES_FIELD_SPACE).value()));
        }
        SearchResultWrapper srw = new SearchResultWrapper(results, usualSearch.getTookInMillis(), isNaN(usualSearch.getHits().maxScore(), 0f), usualSearch.getHits().totalHits());
        return srw;
    }

    private static Float isNaN(Float floatToCheck, Float replacement) {
        return floatToCheck.isNaN() ? replacement : floatToCheck;
    }
}
