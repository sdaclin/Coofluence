package coofluence.crawler;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import coofluence.model.Page;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Created by sdaclin on 07/10/2015.
 */
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

    public void visit(Function<Page,Void> function){
        // Authenticate
        if(confluenceUserLogin != null && confluenceUserPass != null) {
            authenticate();
        }

        // Get list of pages
        HttpResponse<JsonNode> jsonNodeHttpResponse = null;
        try {
            jsonNodeHttpResponse = Unirest.get(confluenceHttpRootUri + "/rest/api/content")
                    .queryString("expand", "body.view,metadata,space").asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        logger.debug(String.valueOf(jsonNodeHttpResponse));
        JSONArray results = (JSONArray) jsonNodeHttpResponse.getBody().getObject().get("results");
        for (int i = 0; i < results.length(); i++) {
            JSONObject resultJson = results.getJSONObject(i);
            if (!resultJson.get("type").equals("page")) {
                throw new UnsupportedOperationException("type different from page not supported yet");
            }
            if (!resultJson.get("status").equals("current")) {
                throw new UnsupportedOperationException("status different from \"current\" not supported yet");
            }
            String contentId = resultJson.getString("id");
            Page page = new Page(contentId);
            page.setTitle(resultJson.getString("title"));
            page.setContent(resultJson.getJSONObject("body").getJSONObject("view").getString("value"));
            page.setSpace(resultJson.getJSONObject("space").getString("name"));

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
            function.apply(page);
        }
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
