package com.photoalbum;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PhotoGridView {

    private final MainWindow mainWindow;
    private final ScrollPane scrollPane;
    private final FlowPane grid;
    private final Label emptyLabel;

   private static final double SPACING = 2;
   private double cellSize = 120;

   private boolean selectMode = false;
   private final Set<Photo> selectedPhotos = new HashSet<>();

   public PhotoGridView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        grid = new FlowPane(SPACING, SPACING);
        grid.setPadding(new Insets(0));
        grid.setStyle("-fx-background-color: #f2f2f7;");

        emptyLabel = new Label("没有照片\n点击下方选择文件夹开始");
        emptyLabel.setFont(Font.font("Microsoft YaHei", 15));
        emptyLabel.setTextFill(Color.web("#8e8e93"));
        emptyLabel.setTextAlignment(TextAlignment.CENTER);
        emptyLabel.setAlignment(Pos.CENTER);
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setMaxHeight(Double.MAX_VALUE);

        StackPane content = new StackPane(grid, emptyLabel);
        content.setAlignment(Pos.CENTER);

        scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #f2f2f7; -fx-background-color: #f2f2f7;");
        scrollPane.setPannable(true);

        // Update cell size when width changes
        scrollPane.widthProperty().addListener((obs, old, w) -> {
            int cols = w.doubleValue() > 500 ? 4 : 3;
            cellSize = (w.doubleValue() - SPACING * (cols + 1)) / cols;
            refreshGrid();
        });
    }

    public ScrollPane getView() {
        return scrollPane;
    }

   public void refresh() {
       refreshGrid();
   }

   public void enterSelectMode() {
       selectMode = true;
       selectedPhotos.clear();
       refreshGrid();
   }

   public void exitSelectMode() {
       selectMode = false;
       selectedPhotos.clear();
       refreshGrid();
   }

   public Set<Photo> getSelectedPhotos() { return selectedPhotos; }

   private void refreshGrid() {
        List<Photo> photos = PhotoService.getInstance().getPhotos();
        grid.getChildren().clear();

        if (photos.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        double size = cellSize;
        for (int i = 0; i < photos.size(); i++) {
            Photo photo = photos.get(i);
           StackPane cell = createPhotoCell(photo, size, i, photos);
           grid.getChildren().add(cell);
       }

   }

   private StackPane createPhotoCell(Photo photo, double size, int index, List<Photo> allPhotos) {
       StackPane cell = new StackPane();
       cell.setMinSize(size, size);
       cell.setMaxSize(size, size);
       cell.setPrefSize(size, size);
       cell.setStyle("-fx-background-color: #e0e0e0; -fx-cursor: hand;");

       // Clip to rounded rectangle
       Rectangle clip = new Rectangle(size, size);
       clip.setArcWidth(4);
       clip.setArcHeight(4);
       cell.setClip(clip);

       // Placeholder / actual thumbnail
       ImageView imageView = new ImageView();
       imageView.setFitWidth(size);
       imageView.setFitHeight(size);
       imageView.setPreserveRatio(false);
       imageView.setSmooth(true);
       imageView.setStyle("-fx-background-color: #d0d0d0;");

       cell.getChildren().add(imageView);

       // Load thumbnail synchronously for immediate display
       PhotoService service = PhotoService.getInstance();
       Image thumb = service.getThumbnailSync(photo, (int) size * 2);
       if (thumb != null) {
           imageView.setImage(thumb);
       }

      // Click to open viewer
      cell.setOnMouseClicked(e -> {
          if (e.getClickCount() == 1) {
              if (selectMode) {
                  toggleSelection(photo, cell);
                  return;
              }
              mainWindow.openViewer(photo, allPhotos);
          }
      });

      // Selection checkmark overlay
      if (selectMode) {
          StackPane checkmark = createCheckmark(size, selectedPhotos.contains(photo));
          cell.getChildren().add(checkmark);
          StackPane.setAlignment(checkmark, Pos.BOTTOM_RIGHT);
          StackPane.setMargin(checkmark, new Insets(0, 5, 5, 0));
      }

      // Favorite heart overlay
      if (photo.isFavorite()) {
          Label heart = new Label("\u2665");
          heart.setFont(Font.font(size * 0.18));
          heart.setTextFill(Color.WHITE);
          heart.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 2, 0, 0, 1);");
          cell.getChildren().add(heart);
          StackPane.setAlignment(heart, Pos.BOTTOM_LEFT);
          StackPane.setMargin(heart, new Insets(0, 0, 4, 5));
      }

      // Hover effect
      cell.setOnMouseEntered(e -> cell.setOpacity(0.85));
      cell.setOnMouseExited(e -> cell.setOpacity(1.0));

      return cell;
  }

  private void toggleSelection(Photo photo, StackPane cell) {
      if (selectedPhotos.contains(photo)) {
          selectedPhotos.remove(photo);
      } else {
          selectedPhotos.add(photo);
      }
      int index = PhotoService.getInstance().getPhotos().indexOf(photo);
      StackPane newCell = createPhotoCell(photo, cellSize, index,
              PhotoService.getInstance().getPhotos());
      int gridIndex = grid.getChildren().indexOf(cell);
      if (gridIndex >= 0) {
          grid.getChildren().set(gridIndex, newCell);
      }
      mainWindow.updateBatchCount(selectedPhotos.size());
  }

  private StackPane createCheckmark(double cellSize, boolean selected) {
      double r = cellSize * 0.22;
      StackPane circle = new StackPane();
      circle.setMinSize(r, r);
      circle.setMaxSize(r, r);
      if (selected) {
          circle.setStyle("-fx-background-color: #007AFF; -fx-background-radius: 50%;");
          Label check = new Label("\u2713");
          check.setFont(Font.font(10));
          check.setTextFill(Color.WHITE);
          circle.getChildren().add(check);
      } else {
          circle.setStyle("-fx-background-color: rgba(0,0,0,0.15); -fx-background-radius: 50%; " +
                  "-fx-border-color: rgba(255,255,255,0.8); -fx-border-radius: 50%; -fx-border-width: 1.5;");
      }
      return circle;
  }
}
