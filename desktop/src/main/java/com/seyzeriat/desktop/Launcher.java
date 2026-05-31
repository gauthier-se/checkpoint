package com.seyzeriat.desktop;

import javafx.application.Application;

/**
 * A launcher class to start the JavaFX application.
 * This is used as a workaround for JavaFX module path issues when running from an IDE or as a fat JAR.
 */
public class Launcher {
    
    /**
     * The main entry point for the launcher.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
    }
}
