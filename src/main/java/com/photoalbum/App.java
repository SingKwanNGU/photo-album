package com.photoalbum;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow window = new MainWindow(primaryStage);
        window.show();

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    @Override
    public void stop() {
        PhotoService.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
