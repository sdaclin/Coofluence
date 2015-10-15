package coofluence;

import coofluence.crawler.Crawler;
import coofluence.index.Index;
import coofluence.model.BlogPost;
import coofluence.model.Comment;
import coofluence.model.Page;
import coofluence.tools.CoofluenceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.time.LocalDateTime;

public class Launcher {
    public static Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        // Start ES index
        Index.start();

        if (args != null && args.length == 1 && args[0].equals("recreateIndex")) {
            Index.deleteIndex();
        }

        // Start Web app
        WebApp.start();

        // Init index if necessary
        Index.initIndexIfNecessary();

        // Start crawling
        LocalDateTime lastChangeDate = Index.getMaxUpdatedDate();
        //lastChangeDate = LocalDateTime.of(2015, Month.JANUARY,1,0,0);
        new Crawler(CoofluenceProperty.HTTP_ROOT_URI.getValue()) //
                .withCredentials(CoofluenceProperty.USER_LOGIN.getValue(), CoofluenceProperty.USER_PASS.getValue()) //
                .limitCrawlingTo(5)
                .visit(lastChangeDate, indexable -> {
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
                        case COMMENT: // TODO to be handled when indexing blog post and page
                            final Comment comment = (Comment) indexable;
                            Index.indexComment(comment);
                            logger.info("Comment indexed #{} [{}]", comment.getId(), comment.getTitle());
                    }
                    return null;
                });

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
