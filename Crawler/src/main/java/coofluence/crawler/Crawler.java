package coofluence.crawler;

import com.google.common.base.Preconditions;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import coofluence.model.*;
import coofluence.tools.CoofluenceProperty;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Crawler {
    final static Logger logger = LoggerFactory.getLogger(Crawler.class);
    public static final Integer CONFULENCE_REST_QUERY_LIMIT = 50;

    private final String confluenceHttpRootUri;
    private String confluenceUserLogin;
    private String confluenceUserPass;
    private Integer nbPageMaxToCrawlDuringThisSession;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public Crawler(String confluenceHttpRootUri) {
        this.confluenceHttpRootUri = confluenceHttpRootUri;
    }

    public Crawler limitCrawlingTo(int nbPageMaxToCrawlDuringThisSession) {
        this.nbPageMaxToCrawlDuringThisSession = nbPageMaxToCrawlDuringThisSession;
        return this;
    }

    public Crawler withCredentials(String confluenceUserLogin, String confluenceUserPass) {
        this.confluenceUserLogin = confluenceUserLogin;
        this.confluenceUserPass = confluenceUserPass;
        return this;
    }

    public void visit(LocalDateTime refreshDate, Function<Indexable, Void> function) {
        final LocalDateTime[] refreshDateHandler = new LocalDateTime[1];
        refreshDateHandler[0] = refreshDate;

        // Authenticate
        if (confluenceUserLogin != null && confluenceUserPass != null) {
            authenticate();
        }

        Runnable indexerTask = new Runnable() {
            @Override
            public void run() {
                LocalDateTime threadRefreshDate = refreshDateHandler[0];
                logger.info("Start crawling batch from [{}]", threadRefreshDate);

                // Get list of pages
                int currentPage = 0;
                HttpResponse<JsonNode> jsonNodeHttpResponse = null;
                JSONObject responseJsonBody;
                do {
                    // Query current page
                    try {

                        jsonNodeHttpResponse = Unirest.get(confluenceHttpRootUri + "/rest/api/content/search")//
                                .queryString("cql", "type not in (comment,attachment) and lastmodified>\"" + threadRefreshDate.minusMinutes(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\" order by lastmodified asc")
                                .queryString("expand", "body.view,metadata,space,version")//
                                .queryString("start", currentPage * CONFULENCE_REST_QUERY_LIMIT)
                                .queryString("limit", CONFULENCE_REST_QUERY_LIMIT)
                                .asJson();
                    } catch (UnirestException e) {
                        logger.error("Something goes wrong when querying confluence REST API [{}]", e.getMessage());
                        return;
                    }

                    assert jsonNodeHttpResponse != null;
                    responseJsonBody = jsonNodeHttpResponse.getBody().getObject();
                    //Preconditions.checkState(responseJsonBody.get("size").equals(CONFULENCE_REST_QUERY_LIMIT), "The query limit should be the same that the returned one");

                    // Handle results
                    JSONArray results = (JSONArray) responseJsonBody.get("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject resultJson = results.getJSONObject(i);
                        final String type = (String) resultJson.get("type");
                        Preconditions.checkState(resultJson.get("status").equals("current"), "Only current status is handled for now [%s]", resultJson.get("status"));
                        Indexable toIndex;
                        switch (type) {
                            case "page":
                                toIndex = extractPageFromJSON(resultJson);
                                Preconditions.checkState(((Page) toIndex).getUpdateDate() != null);
                                refreshDateHandler[0] = ((Page) toIndex).getUpdateDate();
                                break;
                            case "blogpost":
                                toIndex = extractBlogPostFromJSON(resultJson);
                                Preconditions.checkState(((BlogPost) toIndex).getUpdateDate() != null);
                                refreshDateHandler[0] = ((BlogPost) toIndex).getUpdateDate();
                                break;
                            default:
                                logger.error("Content type not supported yet [{}]", type);
                                continue;
                        }
                        function.apply(toIndex);
                    }
                    currentPage++;
                } while ((Integer) responseJsonBody.get("size") > 0
                        && (nbPageMaxToCrawlDuringThisSession == null || currentPage < nbPageMaxToCrawlDuringThisSession));
                logger.info("End of indexation batch");
            }
        };
        scheduler.scheduleWithFixedDelay(indexerTask, 0, Long.valueOf(CoofluenceProperty.CONFLUENCE_POLLING_FREQUENCY_SECONDS.getValue()), TimeUnit.SECONDS);

    }

    private Indexable extractPageFromJSON(JSONObject resultJson) {
        String contentId = resultJson.getString("id");
        Page page = new Page(contentId);
        page.setTitle(resultJson.getString("title"));
        page.setContent(resultJson.getJSONObject("body").getJSONObject("view").getString("value"));
        page.setSpace(resultJson.getJSONObject("space").getString("name"));
        final JSONObject versionJson = resultJson.getJSONObject("version");
        Author author = extractAuthor(versionJson);
        page.setAuthor(author);
        page.setUpdateDate(extractUpdateDate(versionJson));

        HttpResponse<JsonNode> labelsJson = null;
        try {
            labelsJson = Unirest.get(confluenceHttpRootUri + "/rest/api/content/" + contentId + "/label").asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        JSONArray labelsResults = labelsJson.getBody().getObject().getJSONArray("results");
        for (int j = 0; j < labelsResults.length(); j++) {
            page.addTag(labelsResults.getJSONObject(j).getString("name"));
        }
        return page;
    }

    private Indexable extractBlogPostFromJSON(JSONObject resultJson) {
        String contentId = resultJson.getString("id");
        BlogPost blogPost = new BlogPost(contentId);
        blogPost.setTitle(resultJson.getString("title"));
        blogPost.setContent(resultJson.getJSONObject("body").getJSONObject("view").getString("value"));
        blogPost.setSpace(resultJson.getJSONObject("space").getString("name"));
        final JSONObject versionJson = resultJson.getJSONObject("version");
        Author author = extractAuthor(versionJson);
        blogPost.setAuthor(author);
        blogPost.setUpdateDate(extractUpdateDate(versionJson));

        HttpResponse<JsonNode> labelsJson = null;
        try {
            labelsJson = Unirest.get(confluenceHttpRootUri + "/rest/api/content/" + contentId + "/label").asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        JSONArray labelsResults = labelsJson.getBody().getObject().getJSONArray("results");
        for (int j = 0; j < labelsResults.length(); j++) {
            blogPost.addTag(labelsResults.getJSONObject(j).getString("name"));
        }
        return blogPost;
    }

    private Indexable extractCommentFromJSON(JSONObject resultJson) {
        final String contentId = resultJson.getString("id");
        Comment comment = new Comment(contentId);
        comment.setContent(resultJson.getJSONObject("body").getJSONObject("view").getString("value"));
        comment.setTitle(resultJson.getString("title"));
        final JSONObject versionJson = resultJson.getJSONObject("version");
        comment.setAuthor(extractAuthor(versionJson));
        comment.setUpdateDate(extractUpdateDate(versionJson));

        HttpResponse<JsonNode> containerJson = null;
        try {
            containerJson = Unirest.get(confluenceHttpRootUri + "/rest/api/content/" + contentId)
                    .queryString("expand", "container").asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        comment.setContainerId(containerJson.getBody().getObject().getJSONObject("container").getString("id"));
        return comment;
    }

    private LocalDateTime extractUpdateDate(JSONObject versionJson) {
        return LocalDateTime.parse(versionJson.getString("when"), DateTimeFormatter.ISO_DATE_TIME);
    }

    private Author extractAuthor(JSONObject versionJson) {
        final JSONObject byJson = versionJson.getJSONObject("by");
        Author author = new Author();
        if (byJson.has("userKey")) {
            author.setAuthorUserName(byJson.getString("username"));
            author.setAuthorDisplayName(byJson.getString("displayName"));
            author.setAuthorKey(byJson.getString("userKey"));
        } else {
            author.setAuthorDisplayName("Anonymous");
            author.setAuthorUserName("John Doe");
            author.setAuthorKey("");
        }
        return author;
    }

    private void authenticate() {
        try {
            Unirest.get(confluenceHttpRootUri + "/rest/api/content")
                    .queryString("os_username", confluenceUserLogin)
                    .queryString("os_password", confluenceUserPass).asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }
}
