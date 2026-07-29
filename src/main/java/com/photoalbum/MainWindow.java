package com.photoalbum;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainWindow {

    private final Stage stage;
    private final StackPane contentArea;
    private final PhotoGridView photoGridView;
    private final AlbumListView albumListView;
    private final PhotoViewer photoViewer;
    private final Label statusLabel;
    private final Label titleLabel;
    private Button photosTabBtn;
    private Button albumsTabBtn;
    private final BorderPane root;

    private int currentTab = 0;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.photoGridView = new PhotoGridView(this);
        this.albumListView = new AlbumListView(this);
        this.photoViewer = new PhotoViewer(this);

        // Top bar
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: rgba(248,248,248,0.92);");
        topBar.setMinHeight(44);

        titleLabel = new Label("Photos");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#1d1d1f"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button chooseFolderBtn = new Button("Select Folder");
        chooseFolderBtn.setStyle(
                "-fx-background-color: #007AFF; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 6 14 6 14; " +
                "-fx-background-radius: 16; -fx-cursor: hand;"
        );
        chooseFolderBtn.setOnAction(e -> chooseFolder());

        topBar.getChildren().addAll(titleLabel, spacer, chooseFolderBtn);

        // Content
        contentArea = new StackPane();
        contentArea.getChildren().add(photoGridView.getView());

        // Status
        statusLabel = new Label("Ready");
        statusLabel.setFont(Font.font("Microsoft YaHei", 11));
        statusLabel.setTextFill(Color.web("#8e8e93"));
        statusLabel.setPadding(new Insets(4, 16, 4, 16));

        // Tab bar
        HBox tabBar = createTabBar();
        tabBar.setStyle("-fx-background-color: rgba(248,248,248,0.92); " +
                "-fx-border-color: #c6c6c8; -fx-border-width: 0.5 0 0 0;");
        tabBar.setMinHeight(50);
        tabBar.setAlignment(Pos.CENTER);
        tabBar.setSpacing(40);

        // Root
        BorderPane mainPane = new BorderPane();
        mainPane.setTop(topBar);
        mainPane.setCenter(contentArea);
        mainPane.setBottom(new VBox(0, statusLabel, tabBar));
        mainPane.setStyle("-fx-background-color: #f2f2f7;");

        root = mainPane;

        Scene scene = new Scene(root, 420, 780);
        scene.getStylesheets().add(
                getClass().getResource("/com/photoalbum/style.css").toExternalForm()
        );

        stage.setTitle("Photo Album");
        stage.setScene(scene);
        stage.setMinWidth(360);
        stage.setMinHeight(600);

        scanPhotos();
    }

    private HBox createTabBar() {
        photosTabBtn = createTabButton("Photos", true);
        albumsTabBtn = createTabButton("Albums", false);

        photosTabBtn.setOnAction(e -> switchTab(0));
        albumsTabBtn.setOnAction(e -> switchTab(1));

        HBox bar = new HBox(photosTabBtn, albumsTabBtn);
        bar.setAlignment(Pos.CENTER);
        bar.setSpacing(40);
        return bar;
    }

    private Button createTabButton(String text, boolean selected) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Microsoft YaHei", FontWeight.NORMAL, 12));
        btn.setTextFill(selected ? Color.web("#007AFF") : Color.web("#8e8e93"));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4 0 4 0;");
        return btn;
    }

    void switchTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;

        photosTabBtn.setTextFill(tab == 0 ? Color.web("#007AFF") : Color.web("#8e8e93"));
        albumsTabBtn.setTextFill(tab == 1 ? Color.web("#007AFF") : Color.web("#8e8e93"));
        titleLabel.setText(tab == 0 ? "Photos" : "Albums");

        contentArea.getChildren().clear();
        if (tab == 0) {
            contentArea.getChildren().add(photoGridView.getView());
            photoGridView.refresh();
        } else {
            contentArea.getChildren().add(albumListView.getView());
            albumListView.refresh();
        }
    }

    void show() {
        stage.show();
    }

    void scanPhotos() {
        PhotoService service = PhotoService.getInstance();
        service.scanPhotos(() -> {
            statusLabel.setText(service.getStatusMessage());
            photoGridView.refresh();
        });
        statusLabel.setText("Scanning...");
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Photo Folder");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            Path path = dir.toPath();
            PhotoService.getInstance().setCurrentDirectory(path);
            scanPhotos();
        }
    }

    void openViewer(Photo photo, List<Photo> photoList) {
        photoViewer.show(photo, photoList);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(photoViewer.getView());

        root.getBottom().setVisible(false);
        root.getTop().setVisible(false);
        root.setStyle("-fx-background-color: black;");
    }

    void closeViewer() {
        contentArea.getChildren().clear();
        root.getBottom().setVisible(true);
        root.getTop().setVisible(true);
        root.setStyle("-fx-background-color: #f2f2f7;");

        switchTab(currentTab);
        statusLabel.setText(PhotoService.getInstance().getStatusMessage());
    }

    void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    Stage getStage() {
        return stage;
    }

    StackPane getContentArea() {
        return contentArea;
    }

    void refreshCurrentTab() {
        if (currentTab == 0) photoGridView.refresh();
        else albumListView.refresh();
    }
}
