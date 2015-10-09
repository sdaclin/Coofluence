package coofluence;

import coofluence.index.Index;

import java.io.Console;

/**
 * Created by sdaclin on 09/10/2015.
 */
public class Launcher {
    public static void main(String[] args) {
        Index.start();
        WebApp.start();

        // For stand alone launch only. In an ide the console might be null
        Console console = System.console();
        if(console != null) {
            console.format("\nPress ENTER to quit.\n");
            console.readLine();
            WebApp.stop();
            Index.stop();
        }
    }
}
