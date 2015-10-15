package coofluence.tools;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.Properties;

public enum CoofluenceProperty {
    HTTP_ROOT_URI,
    HTTP_VIEW_PAGE_PATH,
    USER_LOGIN,
    USER_PASS;

    final static Logger logger = LoggerFactory.getLogger(CoofluenceProperty.class);
    private static final String PATH = "coofluence.properties";
    private static Properties properties;

    private String value;

    private void init() {
        if (properties == null) {
            properties = new Properties();
            try {
                properties.load(new FileReader(PATH));
            } catch (Exception e) {
                logger.error("Can't load properties from file ["+PATH+"]");
                System.exit(1);
            }
        }
        value = (String) properties.get(this.toString());
        Preconditions.checkState(value != null, "Can't find property [%s] in file [%s]",this.toString(),PATH);
    }

    public String getValue() {
        if (value == null) {
            init();
        }
        return value;
    }

}
