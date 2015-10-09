package coofluence.index;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Index {
    final static Logger logger = LoggerFactory.getLogger(Index.class);
    private static Node esNode;

    public static Client start() {

        if (esNode == null) {
            esNode = NodeBuilder.nodeBuilder()//
                    .clusterName("coofluence")//
                    .local(true)
                    .settings(ImmutableSettings.settingsBuilder().put("http.enabled", true))
                    .node();
            logger.info("ESNode started");
        }
        return esNode.client();
    }

    public static void stop() {
        if (esNode != null) {
            esNode.stop();
        }
    }

    public static Client getClient() {
        return esNode.client();
    }
}
