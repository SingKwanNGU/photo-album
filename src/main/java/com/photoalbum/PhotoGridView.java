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

import java.util.List;
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
                mainWindow.openViewer(photo, allPhotos);
            }
        });

        // Hover effect
        cell.setOnMouseEntered(e -> cell.setOpacity(0.85));
        cell.setOnMouseExited(e -> cell.setOpacity(1.0));

        return cell;
    }
}
