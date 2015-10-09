package coofluence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

import static spark.Spark.get;
import static spark.Spark.port;

public class WebApp {
    final static Logger logger = LoggerFactory.getLogger(WebApp.class);

    public static void start(){
        port(8088);
        get("/hello", (req, res) -> "Hello World");
        logger.info("Spark Fwk started");
    }

    public static void stop(){
        Spark.stop();
    }
}
