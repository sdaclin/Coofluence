package coofluence;

import coofluence.crawler.Crawler;
import coofluence.index.Index;
import coofluence.model.BlogPost;
import coofluence.model.Indexable;
import coofluence.model.Page;
import coofluence.tools.CoofluenceProperty;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.time.LocalDateTime;

public class Launcher {
    public static final String CMD_LINE_RECREATE_INDEX = "recreateIndex";
    public static final String CMD_LINE_WEB_CONTENT_OFF = "webContentOff";
    public static final String CMD_LINE_CRAWLER_OFF = "crawlerOff";
    public static final String CMD_LINE_CRAWLER_START_DATE = "crawlerStartDate";
    public static final String CMD_LINE_HELP = "help";
    public static Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws ParseException {
        // Handle startup options
        Options options = new Options();
        options.addOption(CMD_LINE_HELP, false, "Print CLI help");
        options.addOption(CMD_LINE_WEB_CONTENT_OFF, "Prevent the publishing of the GUI web app. The rest endpoint is still available.");
        options.addOption(CMD_LINE_RECREATE_INDEX, "Drop and recreate index content");
        options.addOption(CMD_LINE_CRAWLER_OFF, "Start the crawler");
        options.addOption(CMD_LINE_CRAWLER_START_DATE, true, "The date from when the crawler crawls content. Use ISO Format as \"YYYY-MM-DDTHH:mm\"");

        CommandLineParser parser = new DefaultParser();
        final CommandLine cmdLine = parser.parse(options, args);

        if (cmdLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar coofluence.jar", options);
            System.exit(0);
        }

        // Start Elastic search index
        Index.start();
        if (cmdLine.hasOption(CMD_LINE_RECREATE_INDEX)) {
            Index.deleteIndex();
        }

        // Start Web app
        WebApp.start(!cmdLine.hasOption(CMD_LINE_WEB_CONTENT_OFF));

        // Init index if necessary
        Index.initIndexIfNecessary();

        if (!cmdLine.hasOption(CMD_LINE_CRAWLER_OFF)) {
            // Start crawling
            LocalDateTime lastChangeDate;
            if (cmdLine.getOptionValue(CMD_LINE_CRAWLER_START_DATE) != null) {
                lastChangeDate = LocalDateTime.parse(CMD_LINE_CRAWLER_START_DATE);
            } else {
                lastChangeDate = Index.getMaxUpdatedDate();
            }
            //lastChangeDate = LocalDateTime.of(2015, Month.JANUARY,1,0,0);
            new Crawler(CoofluenceProperty.HTTP_ROOT_URI.getValue()) //
                    .withCredentials(CoofluenceProperty.USER_LOGIN.getValue(), CoofluenceProperty.USER_PASS.getValue()) //
                    .limitCrawlingTo(5)
                    .visit(lastChangeDate, indexable -> {
                        if (shouldFilter(indexable)) {
                            return null;
                        }
                        switch (indexable.getType()) {
                            case PAGE:
                                final Page page = (Page) indexable;
                                Index.indexPage(page);
                                logger.info("Page indexed #{} [{}]", page.getId(), page.getTitle());
                                break;
                            case BLOG_POST:
                                final BlogPost blogPost = (BlogPost) indexable;
                                Index.indexBlogPost(blogPost);
                                logger.info("BlogPost indexed #{} [{}]", blogPost.getId(), blogPost.getTitle());
                                break;
                        }
                        return null;
                    });
        }

        // For stand alone launch only. In an ide the console might be null
        Console console = System.console();
        if (console != null) {
            console.format("\nPress ENTER to quit.\n");
            console.readLine();
            WebApp.stop();
            Index.stop();
        } else {
            // Infinite loop to keep the sout open
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Apply some basic rules to filter an indexable content
     *
     * @param indexable
     * @return
     */
    private static boolean shouldFilter(Indexable indexable) {
        if (indexable.getAuthorUserName().equals("rd-tnd-writer")) {
            return true;
        }
        return false;
    }
}
