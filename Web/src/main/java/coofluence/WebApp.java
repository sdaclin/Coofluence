package coofluence;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import java.util.Arrays;

import static spark.Spark.*;

public class WebApp {
    final static Logger logger = LoggerFactory.getLogger(WebApp.class);

    public static void start(){
        Gson gson = new Gson();

        port(8080);
        //externalStaticFileLocation("www");
        enableCORS("*", "*", "*");
        get("/rest/autoComplete", "application/json", (req, res) -> {
            res.header("Content-type", "application/json");
            return Arrays.asList(new PageJson("Truc bidule", "2015-01-12", "Plip, Ploum", "sdaclin"), new PageJson("Blah blah", "2015-01-12", "Plip, Ploum", "sdaclin"), new PageJson("Machin chouette", "2015-01-12", "Plip, Ploum", "smendez"));
        }, gson::toJson);
        logger.info("Spark Fwk started");
    }

    public static void stop(){
        Spark.stop();
    }

    private static void enableCORS(final String origin, final String methods, final String headers) {
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
        });
    }

    private static class PageJson {
        private final String title;
        private final String date;
        private final String tags;
        private final String author;

        private PageJson(String title, String date, String tags, String author) {
            this.title = title;
            this.date = date;
            this.tags = tags;
            this.author = author;
        }
    }
}
