/*
 * TwitVizApp.java
 */

package twitviz;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;
import winterwell.jtwitter.Twitter;

/**
 * The main class of the application.
 */
public class TwitVizApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new TwitVizView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of TwitVizApp
     */
    public static TwitVizApp getApplication() {
        return Application.getInstance(TwitVizApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(TwitVizApp.class, args);
    }
}
