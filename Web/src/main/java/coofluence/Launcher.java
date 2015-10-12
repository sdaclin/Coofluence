package coofluence;

import coofluence.crawler.Crawler;
import coofluence.index.Index;
import coofluence.model.ESConfig;
import coofluence.model.Page;
import coofluence.tools.CoofluenceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;

public class Launcher {
    public static Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        // Start ES index
        Index.start();

        Index.deleteIndex();

        // Read index refresh date
        ESConfig ESConfig = Index.getOrInitESConfig();

        new Crawler(CoofluenceProperty.HTTP_ROOT_URI.getValue()) //
                .withCredentials(CoofluenceProperty.USER_LOGIN.getValue(), CoofluenceProperty.USER_PASS.getValue()) //
                .visit(ESConfig.getLastChangeDate(), indexable -> {
                    switch (indexable.getType()) {
                        case PAGE:
                            Index.indexPage((Page) indexable);
                            break;
                    }
                    return null;
                });

        // Start Web app
        WebApp.start();

        // For stand alone launch only. In an ide the console might be null
        Console console = System.console();
        if (console != null) {
            console.format("\nPress ENTER to quit.\n");
            console.readLine();
            WebApp.stop();
            Index.stop();
        }
    }
}
