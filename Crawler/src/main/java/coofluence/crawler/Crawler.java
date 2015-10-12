package coofluence.crawler;

import com.google.common.base.Preconditions;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import coofluence.model.Indexable;
import coofluence.model.Page;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class Crawler {
    final static Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final String confluenceHttpRootUri;
    private String confluenceUserLogin;
    private String confluenceUserPass;

    public Crawler(String confluenceHttpRootUri) {
        this.confluenceHttpRootUri = confluenceHttpRootUri;
    }

    public Crawler withCredentials(String confluenceUserLogin, String confluenceUserPass) {
        this.confluenceUserLogin = confluenceUserLogin;
        this.confluenceUserPass = confluenceUserPass;
        return this;
    }

    public void visit(LocalDateTime refreshDate, Function<Indexable, Void> function) {
        logger.info("Start crawling from [{}]", refreshDate);

        // Authenticate
        if (confluenceUserLogin != null && confluenceUserPass != null) {
            authenticate();
        }

        // Get list of pages
        HttpResponse<JsonNode> jsonNodeHttpResponse = null;
        try {
            jsonNodeHttpResponse = Unirest.get(confluenceHttpRootUri + "/rest/api/content/search")//
                    .queryString("cql", "created>" + refreshDate.format(DateTimeFormatter.ISO_DATE) + " order by created asc")
                    .queryString("expand", "body.view,metadata,space,version")//
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        JSONArray results = (JSONArray) jsonNodeHttpResponse.getBody().getObject().get("results");
        for (int i = 0; i < results.length(); i++) {
            JSONObject resultJson = results.getJSONObject(i);
            final String type = (String) resultJson.get("type");
            Preconditions.checkState(resultJson.get("status").equals("current"), "Only current status is handled for now [%s]", resultJson.get("status"));
            Indexable toIndex;
            switch (type) {
                case "page":
                    toIndex = createPage(resultJson);
                    break;
                default:
                    logger.error("Content type not supported yet [{}]", type);
                    continue;
            }
            function.apply(toIndex);
        }
    }

    private Indexable createPage(JSONObject resultJson) {
        String contentId = resultJson.getString("id");
        Page page = new Page(contentId);
        page.setTitle(resultJson.getString("title"));
        page.setContent(resultJson.getJSONObject("body").getJSONObject("view").getString("value"));
        page.setSpace(resultJson.getJSONObject("space").getString("name"));
        final JSONObject versionJson = resultJson.getJSONObject("version");
        final JSONObject byJson = versionJson.getJSONObject("by");
        page.setAuthorUserName(byJson.getString("username"));
        page.setAuthorDisplayName(byJson.getString("displayName"));
        page.setAuthorKey(byJson.getString("userKey"));
        page.setUpdateDate(LocalDateTime.parse(versionJson.getString("when"), DateTimeFormatter.ISO_DATE_TIME));

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
