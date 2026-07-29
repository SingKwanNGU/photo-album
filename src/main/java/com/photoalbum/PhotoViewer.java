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
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
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
    private final StackPane imageContainer;
    private final ImageView imageView;
    private final VBox topOverlay;
    private final HBox bottomOverlay;
   private Label photoNameLabel;
   private Label photoDateLabel;

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

       imageView.fitWidthProperty().bind(imageContainer.widthProperty());
       imageView.fitHeightProperty().bind(imageContainer.heightProperty());

       topOverlay = createTopOverlay();
        bottomOverlay = createBottomOverlay();

        root = new StackPane(imageContainer, topOverlay, bottomOverlay);
        root.setStyle("-fx-background-color: black;");

        StackPane.setAlignment(topOverlay, Pos.TOP_LEFT);
        StackPane.setAlignment(bottomOverlay, Pos.BOTTOM_CENTER);

        setupGestures();
    }

  private VBox createTopOverlay() {
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

       // Date pill: iOS-style rounded capsule at top center
       photoDateLabel = new Label("");
       photoDateLabel.setFont(Font.font("Microsoft YaHei", 12));
       photoDateLabel.setTextFill(Color.WHITE);
       photoDateLabel.setStyle("-fx-background-color: rgba(58,58,60,0.85); " +
               "-fx-background-radius: 14; -fx-padding: 4 14 4 14;");

       HBox dateRow = new HBox(photoDateLabel);
       dateRow.setAlignment(Pos.CENTER);
       dateRow.setPadding(new Insets(6, 0, 0, 0));

       // Back button pinned top-left
       HBox backRow = new HBox(backBtn);
       backRow.setPadding(new Insets(8, 12, 0, 12));
       backRow.setAlignment(Pos.CENTER_LEFT);

       // Stack: back button left, date pill center
       StackPane topPane = new StackPane();
       topPane.getChildren().addAll(dateRow, backRow);
       StackPane.setAlignment(backRow, Pos.TOP_LEFT);
       StackPane.setAlignment(dateRow, Pos.TOP_CENTER);

       VBox top = new VBox(0, topPane);
       top.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.7), transparent);");
       top.setMinHeight(120);
       top.setMaxHeight(120);
       return top;
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
               createTrashBtn(),
               createInfoBtn()
       );

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

       FadeTransition t1 = new FadeTransition(Duration.millis(250), topOverlay);
       t1.setToValue(targetOpacity);
       t1.play();
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
       photoDateLabel.setText(currentPhoto.getDateString() + "  " +
               currentPhoto.getTimeString() + "  " +
               currentPhoto.getFormattedSize());
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
       topOverlay.setOpacity(1);
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
       topOverlay.setOpacity(1);
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

       Label warning = new Label("Delete this photo?");
       warning.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));
       warning.setTextFill(Color.WHITE);

       Button yesBtn = new Button("Delete");
       yesBtn.setFont(Font.font("Microsoft YaHei", 12));
       yesBtn.setTextFill(Color.WHITE);
       yesBtn.setStyle("-fx-background-color: #FF3B30; -fx-background-radius: 14; " +
               "-fx-cursor: hand; -fx-padding: 6 16 6 16;");
       yesBtn.setOnAction(e -> { hideDeleteConfirm(); deleteCurrent(); });

       Button noBtn = new Button("Cancel");
       noBtn.setFont(Font.font("Microsoft YaHei", 12));
       noBtn.setTextFill(Color.web("#007AFF"));
       noBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 14; " +
               "-fx-cursor: hand; -fx-padding: 6 16 6 16;");
       noBtn.setOnAction(e -> hideDeleteConfirm());

       HBox confirmBox = new HBox(10, yesBtn, noBtn);
       confirmBox.setAlignment(Pos.CENTER);

       VBox confirmOverlay = new VBox(8, warning, confirmBox);
       confirmOverlay.setAlignment(Pos.CENTER);
       confirmOverlay.setPadding(new Insets(14, 20, 14, 20));
       confirmOverlay.setStyle("-fx-background-color: rgba(200,0,0,0.92); -fx-background-radius: 14;");
       confirmOverlay.setMaxWidth(260);
       confirmOverlay.setId("confirmOverlay");

       confirmVisible = true;
       root.getChildren().add(confirmOverlay);
       StackPane.setAlignment(confirmOverlay, Pos.BOTTOM_RIGHT);
       StackPane.setMargin(confirmOverlay, new Insets(0, 16, 90, 0));
   }

   private void hideDeleteConfirm() {
       confirmVisible = false;
       root.getChildren().removeIf(node -> "confirmOverlay".equals(node.getId()));
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
}
