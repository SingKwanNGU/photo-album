package com.photoalbum;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class PhotoViewer {

    private final MainWindow mainWindow;

   private final StackPane root;
   private final BorderPane rootLayout;
   private final HBox navBar;
   private final StackPane imageContainer;
   private final ImageView imageView;
   private final HBox bottomOverlay;
  private Label photoNameLabel;
  private Label photoDateLabel;
  private Label timeLabel;

  private Button favoriteBtn;
   private SVGPath heartPath;
   private boolean confirmVisible = false;
   private Timeline slideshowTimer;
   private boolean slideshowMode = false;

   private List<Photo> photoList;
    private int currentIndex;
    private Photo currentPhoto;

    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private final Scale scaleTransform;
    private final Translate translateTransform;

    private double dragStartX, dragStartY;
    private double dragOffsetX, dragOffsetY;
    private boolean isDragging;
    private boolean isZoomed;

    private boolean overlayVisible = true;

    private final ImageView prevImageView;
    private final ImageView nextImageView;

    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 5.0;
    private static final double SWIPE_THRESHOLD = 80;

    public PhotoViewer(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        scaleTransform = new Scale(1, 1, 0, 0);
        translateTransform = new Translate(0, 0);

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getTransforms().addAll(scaleTransform, translateTransform);
        imageView.setCursor(Cursor.OPEN_HAND);

        prevImageView = new ImageView();
        prevImageView.setPreserveRatio(true);
        prevImageView.setSmooth(true);
        prevImageView.setOpacity(0);
        prevImageView.setVisible(false);

        nextImageView = new ImageView();
        nextImageView.setPreserveRatio(true);
        nextImageView.setSmooth(true);
        nextImageView.setOpacity(0);
        nextImageView.setVisible(false);

       imageContainer = new StackPane(prevImageView, nextImageView, imageView);
       imageContainer.setStyle("-fx-background-color: black;");
       imageContainer.setAlignment(Pos.CENTER);
       imageContainer.setPrefSize(390, 520);
       imageContainer.setMaxSize(390, 520);

       imageView.setFitWidth(390);
       imageView.setFitHeight(520);

       navBar = createNavBar();
       bottomOverlay = createBottomOverlay();

       rootLayout = new BorderPane();
       rootLayout.setTop(navBar);
       rootLayout.setCenter(imageContainer);
       rootLayout.setBottom(bottomOverlay);
       rootLayout.setStyle("-fx-background-color: black;");

       root = new StackPane(rootLayout);
       root.setStyle("-fx-background-color: black;");

       setupGestures();
   }

  private HBox createNavBar() {
       Circle backCircle = new Circle(15);
       backCircle.setFill(Color.web("#3a3a3c"));

       Label backArrow = new Label("<");
       backArrow.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
       backArrow.setTextFill(Color.WHITE);

       StackPane backIcon = new StackPane(backCircle, backArrow);
       backIcon.setMinSize(32, 32);
       backIcon.setMaxSize(32, 32);

       Button backBtn = new Button();
       backBtn.setGraphic(backIcon);
       backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
       backBtn.setOnAction(e -> close());

       photoDateLabel = new Label("");
       photoDateLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 15));
       photoDateLabel.setTextFill(Color.WHITE);

       timeLabel = new Label("");
       timeLabel.setFont(Font.font("Microsoft YaHei", 11));
       timeLabel.setTextFill(Color.web("#cccccc"));

       VBox datePill = new VBox(1, photoDateLabel, timeLabel);
       datePill.setAlignment(Pos.CENTER);
       datePill.setStyle("-fx-background-color: rgba(58,58,60,0.85); " +
               "-fx-background-radius: 16; -fx-padding: 6 18 6 18;");

       Region spacer = new Region();
       HBox.setHgrow(spacer, Priority.ALWAYS);

       Region spacer2 = new Region();
       HBox.setHgrow(spacer2, Priority.ALWAYS);

       // Three-dot menu button
       Button menuBtn = createCircleIconBtn("\u22ef", 16, e -> showMenu());

       HBox bar = new HBox(backBtn, spacer, datePill, spacer2, menuBtn);
       bar.setAlignment(Pos.CENTER);
       bar.setPadding(new Insets(8, 12, 8, 12));
       bar.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
       bar.setMinHeight(48);
       return bar;
   }

   private HBox createBottomOverlay() {
       HBox bar = new HBox();
       bar.setAlignment(Pos.CENTER);
       bar.setSpacing(28);
       bar.setPadding(new Insets(0, 16, 30, 16));
       bar.setStyle("-fx-background-color: linear-gradient(to top, rgba(0,0,0,0.7), transparent);");
       bar.setMinHeight(80);
       bar.setMaxHeight(80);

       bar.getChildren().addAll(
               createFavoriteBtn(),
               createInfoBtn()
       );

       Region bottomSpacer = new Region();
       HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
       bar.getChildren().addAll(bottomSpacer, createTrashBtn());

       return bar;
   }

   private Button createFavoriteBtn() {
       heartPath = new SVGPath();
       heartPath.setContent("M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 " +
               "2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09 " +
               "C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 " +
               "22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z");
       heartPath.setFill(Color.TRANSPARENT);
       heartPath.setStroke(Color.WHITE);
       heartPath.setStrokeWidth(1.2);

       favoriteBtn = new Button();
       StackPane heartIcon = new StackPane(heartPath);
       heartIcon.setMinSize(26, 26);
       heartIcon.setMaxSize(26, 26);
       favoriteBtn.setGraphic(heartIcon);
       favoriteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;");
       favoriteBtn.setOnAction(e -> toggleFavorite());
       return favoriteBtn;
   }

   private Button createTrashBtn() {
       SVGPath outline = new SVGPath();
       outline.setContent("M9 4v1H5v2h14V5h-4V4H9z" +
               "M6 7v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6z");
       outline.setFill(Color.TRANSPARENT);
       outline.setStroke(Color.WHITE);
       outline.setStrokeWidth(1.2);

      SVGPath lines = new SVGPath();
      lines.setContent("M11.5 9v10M15.5 9v10");
      lines.setStroke(Color.WHITE);
      lines.setStrokeWidth(0.8);

       StackPane trashIcon = new StackPane(outline, lines);
       trashIcon.setMinSize(24, 24);
       trashIcon.setMaxSize(24, 24);
       return makeIconBtn(trashIcon, this::showDeleteConfirm);
   }

   private Button createInfoBtn() {
       Circle circle = new Circle(10.5);
       circle.setFill(Color.TRANSPARENT);
       circle.setStroke(Color.WHITE);
       circle.setStrokeWidth(1.2);

       Label iLabel = new Label("i");
       iLabel.setFont(Font.font("Serif", FontWeight.BOLD, 13));
       iLabel.setTextFill(Color.WHITE);

       StackPane icon = new StackPane(circle, iLabel);
       icon.setMinSize(24, 24);
       icon.setMaxSize(24, 24);

       return makeIconBtn(icon, this::showInfo);
   }

   private Button createEditBtn() {
       SVGPath pencil = new SVGPath();
       pencil.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z" +
               "M20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0" +
               "l-1.83 1.83 3.75 3.75 1.83-1.83z");
       pencil.setFill(Color.WHITE);
       StackPane editIcon = new StackPane(pencil);
       editIcon.setMinSize(24, 24);
       editIcon.setMaxSize(24, 24);
       return makeIconBtn(editIcon, this::showEditOverlay);
   }

   private Button createCircleIconBtn(String text, int fontSize,
                                       javafx.event.EventHandler<javafx.event.ActionEvent> action) {
       Circle circle = new Circle(15);
       circle.setFill(Color.web("#3a3a3c"));
       Label label = new Label(text);
       label.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, fontSize));
       label.setTextFill(Color.WHITE);
       StackPane icon = new StackPane(circle, label);
       icon.setMinSize(32, 32);
       icon.setMaxSize(32, 32);
       Button btn = new Button();
       btn.setGraphic(icon);
       btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
       btn.setOnAction(action);
       return btn;
   }

   private Button makeIconBtn(javafx.scene.Node icon, Runnable action) {
       Button btn = new Button();
       btn.setGraphic(icon);
       btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;");
       btn.setOnAction(e -> action.run());
       return btn;
   }

   private void setupGestures() {
       root.setOnMouseClicked(e -> {
           if (!isDragging && e.getClickCount() == 1) {
               if (slideshowMode) { stopSlideshow(); return; }
               toggleOverlay();
           }
       });

        root.setOnMousePressed(this::onMousePressed);
        root.setOnMouseDragged(this::onMouseDragged);
        root.setOnMouseReleased(this::onMouseReleased);
        root.setOnScroll(this::onScroll);
    }

    private void onMousePressed(MouseEvent e) {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
        dragOffsetX = 0;
        dragOffsetY = 0;
        isDragging = false;
        if (isZoomed) {
            root.setCursor(Cursor.CLOSED_HAND);
        }
    }

    private void onMouseDragged(MouseEvent e) {
        dragOffsetX = e.getSceneX() - dragStartX;
        dragOffsetY = e.getSceneY() - dragStartY;

        if (Math.abs(dragOffsetX) > 3 || Math.abs(dragOffsetY) > 3) {
            isDragging = true;
        }

        if (isZoomed) {
            translateTransform.setX(translateX + dragOffsetX);
            translateTransform.setY(translateY + dragOffsetY);
        } else {
            imageView.setTranslateX(dragOffsetX);
            imageView.setOpacity(1.0 - Math.abs(dragOffsetX) / 400.0);

            if (dragOffsetX > 0 && currentIndex > 0) {
                Image prevImg = PhotoService.getInstance().loadFullImage(
                        photoList.get(currentIndex - 1).getPath());
                prevImageView.setImage(prevImg);
                prevImageView.setOpacity(Math.min(1.0, dragOffsetX / 200.0));
                prevImageView.setVisible(true);
                prevImageView.setTranslateX(dragOffsetX - root.getWidth());
                nextImageView.setVisible(false);
            } else if (dragOffsetX < 0 && currentIndex < photoList.size() - 1) {
                Image nextImg = PhotoService.getInstance().loadFullImage(
                        photoList.get(currentIndex + 1).getPath());
                nextImageView.setImage(nextImg);
                nextImageView.setOpacity(Math.min(1.0, -dragOffsetX / 200.0));
                nextImageView.setVisible(true);
                nextImageView.setTranslateX(dragOffsetX + root.getWidth());
                prevImageView.setVisible(false);
            }
        }
    }

    private void onMouseReleased(MouseEvent e) {
        root.setCursor(isZoomed ? Cursor.OPEN_HAND : Cursor.DEFAULT);

        if (isZoomed) {
            translateX += dragOffsetX;
            translateY += dragOffsetY;
            return;
        }

        if (Math.abs(dragOffsetX) > SWIPE_THRESHOLD) {
            if (dragOffsetX > 0 && currentIndex > 0) {
                animateSwipeOut(1, () -> navigateTo(currentIndex - 1));
                return;
            } else if (dragOffsetX < 0 && currentIndex < photoList.size() - 1) {
                animateSwipeOut(-1, () -> navigateTo(currentIndex + 1));
                return;
            }
        }

        resetImagePosition();
    }

    private void animateSwipeOut(int direction, Runnable onComplete) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), imageView);
        tt.setToX(direction * root.getWidth());
        tt.setOnFinished(e -> Platform.runLater(onComplete));
        tt.play();

        FadeTransition ft = new FadeTransition(Duration.millis(200), imageView);
        ft.setToValue(0);
        ft.play();
    }

    private void resetImagePosition() {
        imageView.setTranslateX(0);
        imageView.setOpacity(1.0);
        prevImageView.setVisible(false);
        nextImageView.setVisible(false);
    }

    private void onScroll(ScrollEvent e) {
        double zoomFactor = e.getDeltaY() > 0 ? 1.1 : 1.0 / 1.1;
        double newScale = scale * zoomFactor;

        if (newScale < MIN_SCALE || newScale > MAX_SCALE) return;

        double mouseX = e.getX() - root.getWidth() / 2;
        double mouseY = e.getY() - root.getHeight() / 2;

        scale = newScale;
        translateX = mouseX - zoomFactor * (mouseX - translateX);
        translateY = mouseY - zoomFactor * (mouseY - translateY);

        applyTransform();

        isZoomed = scale > 1.01;
        root.setCursor(isZoomed ? Cursor.OPEN_HAND : Cursor.DEFAULT);
    }

    private void applyTransform() {
        scaleTransform.setX(scale);
        scaleTransform.setY(scale);
        scaleTransform.setPivotX(0);
        scaleTransform.setPivotY(0);
        translateTransform.setX(translateX);
        translateTransform.setY(translateY);
    }

   private void toggleOverlay() {
       overlayVisible = !overlayVisible;
       double targetOpacity = overlayVisible ? 1.0 : 0.0;

       FadeTransition ft = new FadeTransition(Duration.millis(250), bottomOverlay);
       ft.setToValue(targetOpacity);
       ft.play();
   }

    private void navigateTo(int index) {
        currentIndex = index;
        currentPhoto = photoList.get(index);
        loadCurrentPhoto();
    }

   private void loadCurrentPhoto() {
       Image img = PhotoService.getInstance().loadFullImage(currentPhoto.getPath());
       imageView.setImage(img);

       resetZoom();
       resetImagePosition();
       updateInfo();
       syncFavoriteIcon();
   }

    private void resetZoom() {
        scale = 1.0;
        translateX = 0;
        translateY = 0;
        isZoomed = false;
        applyTransform();
        root.setCursor(Cursor.DEFAULT);
    }

  private void updateInfo() {
       int m = currentPhoto.getDate().getMonthValue();
       int d = currentPhoto.getDate().getDayOfMonth();
       int h = currentPhoto.getDate().getHour();
       int min = currentPhoto.getDate().getMinute();
       photoDateLabel.setText(m + "\u6708" + d + "\u65e5");
       timeLabel.setText(String.format("%02d:%02d", h, min));
  }

    public StackPane getView() {
        return root;
    }

   public void show(Photo photo, List<Photo> photoList) {
       this.photoList = photoList;
       this.currentIndex = photoList.indexOf(photo);
       this.currentPhoto = photo;

       stopSlideshow();
       overlayVisible = true;
       bottomOverlay.setOpacity(1);

       loadCurrentPhoto();
   }

   public void startSlideshow(List<Photo> slides) {
       if (slides.isEmpty()) return;
       this.photoList = new ArrayList<>(slides);
       this.currentIndex = 0;
       this.currentPhoto = photoList.get(0);
       this.slideshowMode = true;

       overlayVisible = true;
       bottomOverlay.setOpacity(0);
       hideDeleteConfirm();
       loadCurrentPhoto();

       stopSlideshow();
       slideshowTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
           currentIndex++;
           if (currentIndex >= photoList.size()) {
               stopSlideshow();
               close();
               return;
           }
           currentPhoto = photoList.get(currentIndex);
           loadCurrentPhoto();
       }));
       slideshowTimer.setCycleCount(Timeline.INDEFINITE);
       slideshowTimer.play();
   }

   private void stopSlideshow() {
       if (slideshowTimer != null) {
           slideshowTimer.stop();
           slideshowTimer = null;
       }
       slideshowMode = false;
       bottomOverlay.setOpacity(1);
   }

    private void close() {
        mainWindow.closeViewer();
    }

  private void toggleFavorite() {
      if (currentPhoto != null) {
          currentPhoto.setFavorite(!currentPhoto.isFavorite());
          heartPath.setFill(currentPhoto.isFavorite() ? Color.WHITE : Color.TRANSPARENT);
          mainWindow.setStatus(currentPhoto.isFavorite() ? "Favorited" : "Unfavorited");
      }
  }

 private void syncFavoriteIcon() {
     if (currentPhoto != null && heartPath != null) {
         heartPath.setFill(currentPhoto.isFavorite() ? Color.WHITE : Color.TRANSPARENT);
     }
 }

  private void showDeleteConfirm() {
      if (currentPhoto == null) return;
      hideDeleteConfirm();

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
       yesBtn.setOnAction(e -> { hideDeleteConfirm(); deleteCurrent(); });

       Button noBtn = new Button("Cancel");
       noBtn.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 10));
       noBtn.setTextFill(Color.web("#1d1d1f"));
       noBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #1d1d1f; " +
               "-fx-border-radius: 12; -fx-background-radius: 12; " +
               "-fx-cursor: hand; -fx-padding: 4 12 4 12;");
       noBtn.setOnAction(e -> hideDeleteConfirm());

       HBox confirmBox = new HBox(10, yesBtn, noBtn);
       confirmBox.setAlignment(Pos.CENTER);

       VBox confirmOverlay = new VBox(8, warning, confirmBox);
       confirmOverlay.setAlignment(Pos.CENTER);
       confirmOverlay.setPadding(new Insets(12, 14, 12, 14));
       confirmOverlay.setStyle("-fx-background-color: rgba(255,255,255,0.96); -fx-background-radius: 14; " +
               "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
       confirmOverlay.setMaxWidth(100);
       confirmOverlay.setMaxHeight(100);
       confirmOverlay.setId("confirmOverlay");

       confirmVisible = true;
       rootLayout.getChildren().add(confirmOverlay);
       StackPane.setAlignment(confirmOverlay, Pos.BOTTOM_CENTER);
       StackPane.setMargin(confirmOverlay, new Insets(0, 0, 60, 0));
   }

   private void hideDeleteConfirm() {
       confirmVisible = false;
       rootLayout.getChildren().removeIf(node -> "confirmOverlay".equals(node.getId()));
   }

   private void deleteCurrent() {
       if (currentPhoto == null) return;

       PhotoService.getInstance().deletePhoto(currentPhoto);
       photoList.remove(currentPhoto);

       if (photoList.isEmpty()) {
           close();
       } else if (currentIndex >= photoList.size()) {
           currentIndex = photoList.size() - 1;
           currentPhoto = photoList.get(currentIndex);
           loadCurrentPhoto();
       } else {
           currentPhoto = photoList.get(currentIndex);
           loadCurrentPhoto();
       }

       mainWindow.refreshCurrentTab();
       mainWindow.setStatus("Deleted");
   }

    private void showInfo() {
        if (currentPhoto == null) return;
        String info = currentPhoto.getName() + "\n\n" +
                "Date: " + currentPhoto.getDateString() + " " + currentPhoto.getTimeString() + "\n" +
                "Size: " + currentPhoto.getFormattedSize() + "\n" +
                "Folder: " + currentPhoto.getParentFolderName();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Photo Info");
        alert.setHeaderText(null);
        alert.setContentText(info);
        alert.initOwner(mainWindow.getStage());
       alert.showAndWait();
   }

  private void showMenu() {
       hideDeleteConfirm();
       VBox menu = new VBox(0);
       menu.setAlignment(Pos.CENTER);
       menu.setPadding(new Insets(4, 6, 4, 6));
       menu.setStyle("-fx-background-color: rgba(44,44,46,0.95); -fx-background-radius: 14;");
       menu.setId("editOverlay");

       SVGPath playIcon = new SVGPath();
       playIcon.setContent("M8 5v14l11-7z");
       playIcon.setFill(Color.WHITE);
       StackPane playPane = new StackPane(playIcon);
       playPane.setMinSize(16, 16);
       playPane.setMaxSize(16, 16);

       Button slideBtn = new Button("\u5e7b\u706f\u7247", playPane);
       slideBtn.setFont(Font.font("Microsoft YaHei", 13));
       slideBtn.setTextFill(Color.WHITE);
       slideBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
       slideBtn.setGraphicTextGap(8);
       slideBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5 10 5 10;");
       slideBtn.setMaxWidth(Double.MAX_VALUE);
       slideBtn.setOnAction(e -> { hideEditOverlay(); startSingleSlideshow(); });

       menu.getChildren().addAll(slideBtn);
       root.getChildren().add(menu);
       StackPane.setAlignment(menu, Pos.TOP_RIGHT);
       StackPane.setMargin(menu, new Insets(8, 10, 0, 0));
   }

  private void showEditOverlay() {
       hideDeleteConfirm();
       showEditToolbar();
   }

  private void showEditToolbar() {
       hideEditToolbar();

       // Top cancel bar
       HBox topBar = new HBox();
       topBar.setPadding(new Insets(8, 14, 0, 14));
       topBar.setId("editTopBar");
       Button cancelBtn = new Button("Cancel");
       cancelBtn.setFont(Font.font("Microsoft YaHei", FontWeight.NORMAL, 14));
       cancelBtn.setTextFill(Color.WHITE);
       cancelBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
       cancelBtn.setOnAction(e -> { hideEditToolbar(); hideEditOverlay(); });
       topBar.getChildren().add(cancelBtn);
       rootLayout.getChildren().add(topBar);
       StackPane.setAlignment(topBar, Pos.TOP_LEFT);
       StackPane.setMargin(topBar, new Insets(0, 0, 0, 0));

       // Sub-panel area (above toolbar)
       HBox subPanel = new HBox();
       subPanel.setAlignment(Pos.CENTER);
       subPanel.setSpacing(10);
       subPanel.setPadding(new Insets(10, 16, 10, 16));
       subPanel.setStyle("-fx-background-color: rgba(0,0,0,0.75);");
       subPanel.setId("editSubPanel");
       rootLayout.getChildren().add(subPanel);
       StackPane.setAlignment(subPanel, Pos.BOTTOM_CENTER);
       StackPane.setMargin(subPanel, new Insets(0, 0, 80, 0));

       // Bottom segmented bar
       HBox bar = new HBox();
       bar.setAlignment(Pos.CENTER);
       bar.setSpacing(0);
       bar.setPadding(new Insets(0, 16, 30, 16));
       bar.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
       bar.setMinHeight(60);
       bar.setMaxHeight(60);
       bar.setId("editToolbar");

       javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();

       javafx.scene.control.ToggleButton cropTb = makeSegBtn("Crop", group);
       javafx.scene.control.ToggleButton adjTb = makeSegBtn("Adjust", group);
       javafx.scene.control.ToggleButton filtTb = makeSegBtn("Filter", group);

       cropTb.setOnAction(e -> hideEditSubPanel());
       adjTb.setOnAction(e -> showAdjustSubPanel());
       filtTb.setOnAction(e -> showFilterSubPanel());
       adjTb.setSelected(true);
       showAdjustSubPanel();

       HBox segCapsule = new HBox(0, cropTb, adjTb, filtTb);
       segCapsule.setStyle("-fx-background-color: rgba(58,58,60,0.85); -fx-background-radius: 16;");
       segCapsule.setAlignment(Pos.CENTER);
       bar.getChildren().add(segCapsule);

       Region doneSpacer = new Region();
       HBox.setHgrow(doneSpacer, Priority.ALWAYS);
       Button doneBtn = new Button("Done");
       doneBtn.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 13));
       doneBtn.setTextFill(Color.WHITE);
       doneBtn.setStyle("-fx-background-color: #007AFF; -fx-background-radius: 14; " +
               "-fx-cursor: hand; -fx-padding: 6 16 6 16;");
       doneBtn.setOnAction(e -> { hideEditToolbar(); hideEditOverlay(); });
       bar.getChildren().addAll(doneSpacer, doneBtn);

       bottomOverlay.setVisible(false);
       bottomOverlay.setManaged(false);
       rootLayout.setBottom(bar);
   }

  private javafx.scene.control.ToggleButton makeSegBtn(String text,
          javafx.scene.control.ToggleGroup group) {
      javafx.scene.control.ToggleButton btn = new javafx.scene.control.ToggleButton(text);
      btn.setFont(Font.font("Microsoft YaHei", 12));
      btn.setTextFill(Color.WHITE);
      btn.setToggleGroup(group);
      String base = "-fx-background-color: rgba(58,58,60,0.85); " +
              "-fx-cursor: hand; -fx-padding: 6 16 6 16; -fx-border-color: transparent; ";
      btn.setStyle(base);
      btn.selectedProperty().addListener((o, ov, nv) -> {
          if (nv) btn.setStyle(base + "-fx-background-color: rgba(58,58,60,1);");
          else btn.setStyle(base);
      });
      return btn;
  }

   private void hideEditSubPanel() {
       rootLayout.getChildren().removeIf(n -> "editSubPanel".equals(n.getId()));
   }

   private void showAdjustSubPanel() {
       hideEditSubPanel();
       HBox panel = new HBox(10);
       panel.setAlignment(Pos.CENTER);
       panel.setPadding(new Insets(8, 16, 8, 16));
       panel.setStyle("-fx-background-color: rgba(0,0,0,0.75);");
       panel.setId("editSubPanel");

       Label label = new Label("Brightness");
       label.setFont(Font.font("Microsoft YaHei", 11));
       label.setTextFill(Color.web("#cccccc"));

       Slider slider = new Slider(-1, 1, 0);
       slider.setPrefWidth(180);
       slider.valueProperty().addListener((o, ov, nv) -> imageView.setOpacity(1 + nv.doubleValue() * 0.5));

       Button resetBtn = new Button("Reset");
       resetBtn.setFont(Font.font("Microsoft YaHei", 11));
       resetBtn.setTextFill(Color.web("#007AFF"));
       resetBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
       resetBtn.setOnAction(e -> { slider.setValue(0); imageView.setOpacity(1); });

       panel.getChildren().addAll(label, slider, resetBtn);
       rootLayout.getChildren().add(panel);
       StackPane.setAlignment(panel, Pos.BOTTOM_CENTER);
       StackPane.setMargin(panel, new Insets(0, 0, 60, 0));
   }

   private void showFilterSubPanel() {
       hideEditSubPanel();
       HBox panel = new HBox(10);
       panel.setAlignment(Pos.CENTER);
       panel.setPadding(new Insets(8, 16, 8, 16));
       panel.setStyle("-fx-background-color: rgba(0,0,0,0.75);");
       panel.setId("editSubPanel");

       String[] filters = {"Original", "Mono", "Sepia", "Cool", "Warm"};
       for (String f : filters) {
           Button fb = new Button(f);
           fb.setFont(Font.font("Microsoft YaHei", 11));
           fb.setTextFill(Color.WHITE);
           fb.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 12; " +
                   "-fx-cursor: hand; -fx-padding: 4 12 4 12;");
           fb.setOnAction(e -> applyFilter(f.toLowerCase()));
           panel.getChildren().add(fb);
       }

       rootLayout.getChildren().add(panel);
       StackPane.setAlignment(panel, Pos.BOTTOM_CENTER);
       StackPane.setMargin(panel, new Insets(0, 0, 60, 0));
   }

  private void hideEditToolbar() {
       rootLayout.getChildren().removeIf(n -> "editToolbar".equals(n.getId()));
       rootLayout.getChildren().removeIf(n -> "editTopBar".equals(n.getId()));
       bottomOverlay.setVisible(true);
       bottomOverlay.setManaged(true);
   }

  private void hideEditOverlay() {
       root.getChildren().removeIf(n -> "editOverlay".equals(n.getId()));
       rootLayout.getChildren().removeIf(n -> "editOverlay".equals(n.getId()));
   }

  private void startSingleSlideshow() {
       List<Photo> slides = new java.util.ArrayList<>(photoList);
       if (slides.isEmpty()) return;
       // Start from current photo
       List<Photo> reordered = new java.util.ArrayList<>();
       reordered.addAll(slides.subList(currentIndex, slides.size()));
       reordered.addAll(slides.subList(0, currentIndex));
       startSlideshow(reordered);
   }

   private void showCropPanel() {
       mainWindow.setStatus("Crop: drag edges to crop (coming soon)");
   }

   private void showAdjustPanel() {
       VBox panel = new VBox(8);
       panel.setAlignment(Pos.CENTER);
       panel.setPadding(new Insets(20));
       panel.setStyle("-fx-background-color: rgba(0,0,0,0.92); -fx-background-radius: 16;");
       panel.setMaxWidth(320);
       panel.setId("editOverlay");

       Label title = new Label("Adjust");
       title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
       title.setTextFill(Color.WHITE);

       Label brightLabel = new Label("Brightness");
       brightLabel.setTextFill(Color.web("#cccccc"));
       javafx.scene.control.Slider brightSlider = new javafx.scene.control.Slider(-1, 1, 0);
       brightSlider.valueProperty().addListener((o, ov, nv) -> imageView.setOpacity(1 + nv.doubleValue() * 0.5));

       Button backBtn = new Button("Back");
       backBtn.setFont(Font.font("Microsoft YaHei", 14));
       backBtn.setTextFill(Color.web("#007AFF"));
       backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
       backBtn.setOnAction(e -> { hideEditOverlay(); showEditToolbar(); });

       Button resetBtn = new Button("Reset");
       resetBtn.setFont(Font.font("Microsoft YaHei", 14));
       resetBtn.setTextFill(Color.web("#FF3B30"));
       resetBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
       resetBtn.setOnAction(e -> { brightSlider.setValue(0); imageView.setOpacity(1); });

       HBox btnRow = new HBox(10, backBtn, resetBtn);
       btnRow.setAlignment(Pos.CENTER);

       panel.getChildren().addAll(title, brightLabel, brightSlider, btnRow);
       rootLayout.getChildren().add(panel);
       StackPane.setAlignment(panel, Pos.CENTER);
   }

   private void showFilterPanel() {
       hideEditOverlay();
       VBox panel = new VBox(8);
       panel.setAlignment(Pos.CENTER);
       panel.setPadding(new Insets(20));
       panel.setStyle("-fx-background-color: rgba(0,0,0,0.92); -fx-background-radius: 16;");
       panel.setMaxWidth(320);
       panel.setId("editOverlay");

       Label title = new Label("Filter");
       title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
       title.setTextFill(Color.WHITE);

       String[] filters = {"Original", "Mono", "Sepia", "Cool", "Warm"};
       for (String f : filters) {
           Button fb = new Button(f);
           fb.setFont(Font.font("Microsoft YaHei", 13));
           fb.setTextFill(Color.WHITE);
           fb.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-background-radius: 10; " +
                   "-fx-cursor: hand; -fx-padding: 8 16 8 16; -fx-min-width: 200;");
           fb.setOnAction(e -> applyFilter(f.toLowerCase()));
           panel.getChildren().add(fb);
       }

       Button backBtn = new Button("Back");
       backBtn.setFont(Font.font("Microsoft YaHei", 14));
       backBtn.setTextFill(Color.web("#007AFF"));
       backBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
       backBtn.setOnAction(e -> { hideEditOverlay(); showEditToolbar(); });

       panel.getChildren().add(backBtn);
       rootLayout.getChildren().add(panel);
       StackPane.setAlignment(panel, Pos.CENTER);
   }

   private void applyFilter(String filter) {
       hideEditOverlay();
       switch (filter) {
           case "mono": imageView.setStyle("-fx-effect: dropshadow(gaussian, black, 0, 0, 0, 0);"); break;
           case "sepia": imageView.setStyle("-fx-effect: sepiatone();"); break;
           case "cool": imageView.setStyle("-fx-effect: null;"); imageView.setOpacity(0.9); break;
           case "warm": imageView.setStyle("-fx-effect: null;"); imageView.setOpacity(1.1); break;
           default: imageView.setStyle(""); imageView.setOpacity(1); break;
       }
       mainWindow.setStatus("Filter: " + filter);
   }
}
