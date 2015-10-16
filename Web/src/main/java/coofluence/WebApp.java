package coofluence;

import com.google.gson.Gson;
import coofluence.index.Index;
import coofluence.model.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

import java.util.Collections;
import java.util.List;

import static spark.Spark.*;

public class WebApp {
    final static Logger logger = LoggerFactory.getLogger(WebApp.class);

    public static void start() {
        Gson gson = new Gson();

        port(8080);
        externalStaticFileLocation("www");
        //enableCORS("*", "*", "*");

        // Autocomplete endpoint
        get("/rest/autoComplete", "application/json", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                response.header("Content-type", "application/json");
                if (request.queryParams("q") == null) {
                    // Prefetch mode
                    return Collections.emptyList();
                } else {
                    final List<Suggestion> suggestions = Index.getSuggestion(request.queryParams("type"), request.queryParams("q"));
                    return suggestions;
                }
            }
        }, gson::toJson);

        // Search endpoint
        get("/rest/search", "application/json", new Route() {

            @Override
            public Object handle(Request request, Response response) throws Exception {
                response.header("Content-type", "application/json");
                return Index.getSearchResult(request.queryParams("q"));
            }
        }, gson::toJson);

        logger.info("Spark Fwk started");
    }

    public static void stop() {
        Spark.stop();
    }

    private static void enableCORS(final String origin, final String methods, final String headers) {
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
        });
    }
}
