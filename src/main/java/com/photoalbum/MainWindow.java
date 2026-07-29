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
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
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
   private Button chooseFolderBtn;
   private final BorderPane root;

   private HBox batchBar;
   private Button selectBtn;
   private Label batchLabel;
   private boolean selectMode = false;

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

       chooseFolderBtn = new Button("Select Folder");
       chooseFolderBtn.setStyle(
                "-fx-background-color: #007AFF; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-padding: 6 14 6 14; " +
                "-fx-background-radius: 16; -fx-cursor: hand;"
        );
       chooseFolderBtn.setOnAction(e -> chooseFolder());

       selectBtn = new Button("Select");
       selectBtn.setStyle("-fx-text-fill: #007AFF; -fx-background-color: transparent; " +
               "-fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 6 8 6 8;");
       selectBtn.setOnAction(e -> toggleSelectMode());

       topBar.getChildren().addAll(titleLabel, spacer, selectBtn, chooseFolderBtn);

       // Content
        contentArea = new StackPane();
        contentArea.getChildren().add(photoGridView.getView());

        // Status
        statusLabel = new Label("Ready");
        statusLabel.setFont(Font.font("Microsoft YaHei", 11));
        statusLabel.setTextFill(Color.web("#8e8e93"));
       statusLabel.setPadding(new Insets(4, 16, 4, 16));

       // Batch action bar (hidden by default)
       batchBar = new HBox();
       batchBar.setAlignment(Pos.CENTER);
       batchBar.setSpacing(14);
       batchBar.setPadding(new Insets(8, 16, 8, 16));
       batchBar.setStyle("-fx-background-color: rgba(248,248,248,0.95); " +
               "-fx-border-color: #c6c6c8; -fx-border-width: 0.5 0 0 0;");
       batchBar.setVisible(false);
       batchBar.setManaged(false);

       batchLabel = new Label("");
       batchLabel.setFont(Font.font("Microsoft YaHei", 12));
       batchLabel.setTextFill(Color.web("#1d1d1f"));

      SVGPath outline = new SVGPath();
      outline.setContent("M9 4v1H5v2h14V5h-4V4H9z" +
              "M6 7v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6z");
      outline.setFill(Color.TRANSPARENT);
      outline.setStroke(Color.web("#1d1d1f"));
      outline.setStrokeWidth(1.2);

      SVGPath lines = new SVGPath();
      lines.setContent("M11.5 9v10M15.5 9v10");
      lines.setStroke(Color.web("#1d1d1f"));
      lines.setStrokeWidth(0.8);

      StackPane trashIcon = new StackPane(outline, lines);
      trashIcon.setMinSize(24, 24);
      trashIcon.setMaxSize(24, 24);

      Button batchDelBtn = new Button();
      batchDelBtn.setGraphic(trashIcon);
      batchDelBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
      batchDelBtn.setOnAction(e -> batchDelete());

      SVGPath playIcon = new SVGPath();
      playIcon.setContent("M8 5v14l11-7z");
      playIcon.setFill(Color.web("#1d1d1f"));

      Button batchSlideBtn = new Button();
      batchSlideBtn.setGraphic(playIcon);
      batchSlideBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6;");
      batchSlideBtn.setOnAction(e -> startSlideshow());

       Region batchSpacer = new Region();
       HBox.setHgrow(batchSpacer, Priority.ALWAYS);
       batchBar.getChildren().addAll(batchSlideBtn, batchLabel, batchSpacer, batchDelBtn);

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
       mainPane.setBottom(new VBox(0, batchBar, statusLabel, tabBar));
       mainPane.setStyle("-fx-background-color: #f2f2f7;");

        root = mainPane;

       Scene scene = new Scene(root, 390, 844);
       scene.getStylesheets().add(
               getClass().getResource("/com/photoalbum/style.css").toExternalForm()
       );

       stage.setTitle("Photo Album");
       stage.setScene(scene);
       stage.setMinWidth(320);
       stage.setMinHeight(568);

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

       selectBtn.setVisible(tab == 0);
       if (tab != 0 && selectMode) toggleSelectMode();

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

       if (currentTab == 0) {
           contentArea.getChildren().add(photoGridView.getView());
           photoGridView.refresh();
       } else {
           contentArea.getChildren().add(albumListView.getView());
           albumListView.refresh();
       }
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
    void toggleSelectMode() {
        selectMode = !selectMode;
        if (selectMode) {
            selectBtn.setText("Cancel");
            chooseFolderBtn.setVisible(false);
            photoGridView.enterSelectMode();
            batchBar.setVisible(true);
            batchBar.setManaged(true);
            batchLabel.setText("0 selected");
        } else {
            selectBtn.setText("Select");
            chooseFolderBtn.setVisible(true);
            photoGridView.exitSelectMode();
            batchBar.setVisible(false);
            batchBar.setManaged(false);
        }
    }

    void updateBatchCount(int count) {
        batchLabel.setText(count + " selected");
    }

    private void batchFavorite() {
        Set<Photo> selected = photoGridView.getSelectedPhotos();
        if (selected.isEmpty()) return;
        for (Photo p : selected) p.setFavorite(true);
        toggleSelectMode();
        setStatus("Favorited " + selected.size() + " photos");
    }

  private void batchDelete() {
       Set<Photo> selected = photoGridView.getSelectedPhotos();
       if (selected.isEmpty()) return;
       showConfirmDialog();
   }

   private void showConfirmDialog() {
       VBox dialog = new VBox(8);
       dialog.setAlignment(Pos.CENTER);
       dialog.setPadding(new Insets(12, 14, 12, 14));
       dialog.setStyle("-fx-background-color: rgba(255,255,255,0.96); -fx-background-radius: 14; " +
               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
       dialog.setMaxWidth(100);
       dialog.setMaxHeight(100);
       dialog.setId("confirmDialog");

       Label warning = new Label("Please confirm delete?");
       warning.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 11));
       warning.setTextFill(Color.web("#1d1d1f"));
       warning.setWrapText(true);
       warning.setAlignment(Pos.CENTER);
       warning.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

       Button yesBtn = new Button("Delete");
       yesBtn.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 10));
       yesBtn.setTextFill(Color.WHITE);
       yesBtn.setStyle("-fx-background-color: #FF3B30; -fx-background-radius: 12; " +
               "-fx-cursor: hand; -fx-padding: 4 12 4 12;");
       yesBtn.setOnAction(e -> { hideConfirmDialog(); executeBatchDelete(); });

       Button noBtn = new Button("Cancel");
       noBtn.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 10));
       noBtn.setTextFill(Color.web("#1d1d1f"));
       noBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #1d1d1f; " +
               "-fx-border-radius: 12; -fx-background-radius: 12; " +
               "-fx-cursor: hand; -fx-padding: 4 12 4 12;");
       noBtn.setOnAction(e -> hideConfirmDialog());

       HBox btnRow = new HBox(10, yesBtn, noBtn);
       btnRow.setAlignment(Pos.CENTER);

       dialog.getChildren().addAll(warning, btnRow);
       contentArea.getChildren().add(dialog);
       StackPane.setAlignment(dialog, Pos.CENTER);
   }

   private void hideConfirmDialog() {
       contentArea.getChildren().removeIf(n -> "confirmDialog".equals(n.getId()));
   }

   private void executeBatchDelete() {
       Set<Photo> selected = photoGridView.getSelectedPhotos();
       if (selected.isEmpty()) return;
       PhotoService service = PhotoService.getInstance();
       for (Photo p : selected) service.deletePhoto(p);
       toggleSelectMode();
       scanPhotos();
       setStatus("Deleted " + selected.size() + " photos");
   }

   private void startSlideshow() {
       Set<Photo> selected = photoGridView.getSelectedPhotos();
       if (selected.isEmpty()) return;
       List<Photo> slides = new java.util.ArrayList<>(selected);
       toggleSelectMode();
       photoViewer.startSlideshow(slides);
       contentArea.getChildren().clear();
       contentArea.getChildren().add(photoViewer.getView());
       root.getBottom().setVisible(false);
       root.getTop().setVisible(false);
       root.setStyle("-fx-background-color: black;");
   }

   boolean isSelectMode() { return selectMode; }

}
