package com.photoalbum;

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
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Map;

public class AlbumListView {

    private final MainWindow mainWindow;
    private final VBox albumContainer;
    private final ScrollPane scrollPane;
    private final Label emptyLabel;

    public AlbumListView(MainWindow mainWindow) {
        this.mainWindow = mainWindow;

        albumContainer = new VBox(12);
        albumContainer.setPadding(new Insets(12, 8, 12, 8));
        albumContainer.setStyle("-fx-background-color: #f2f2f7;");

        emptyLabel = new Label("没有相簿");
        emptyLabel.setFont(Font.font("Microsoft YaHei", 15));
        emptyLabel.setTextFill(Color.web("#8e8e93"));
        emptyLabel.setTextAlignment(TextAlignment.CENTER);
        emptyLabel.setAlignment(Pos.CENTER);
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setMaxHeight(Double.MAX_VALUE);

        StackPane content = new StackPane(albumContainer, emptyLabel);
        content.setAlignment(Pos.CENTER);

        scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f2f2f7; -fx-background-color: #f2f2f7;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    public ScrollPane getView() {
        return scrollPane;
    }

    public void refresh() {
        albumContainer.getChildren().clear();
        Map<String, List<Photo>> albums = PhotoService.getInstance().getAlbums();

        if (albums.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        for (Map.Entry<String, List<Photo>> entry : albums.entrySet()) {
            HBox albumRow = createAlbumRow(entry.getKey(), entry.getValue());
            albumContainer.getChildren().add(albumRow);
        }
    }

    private HBox createAlbumRow(String name, List<Photo> photos) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-cursor: hand;");
        row.setMinHeight(70);

        // Cover thumbnail
        StackPane coverBox = new StackPane();
        coverBox.setMinSize(54, 54);
        coverBox.setMaxSize(54, 54);
        coverBox.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 6;");

        Rectangle clip = new Rectangle(54, 54);
        clip.setArcWidth(6);
        clip.setArcHeight(6);
        coverBox.setClip(clip);

        if (!photos.isEmpty()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(54);
            iv.setFitHeight(54);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);

            Image thumb = PhotoService.getInstance().getThumbnail(photos.get(0), 108);
            if (thumb != null) {
                iv.setImage(thumb);
            }
            coverBox.getChildren().add(iv);
        }

        // Album info
        VBox info = new VBox(4);
        info.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 15));
        nameLabel.setTextFill(Color.web("#1d1d1f"));

        Label countLabel = new Label(photos.size() + " 张照片");
        countLabel.setFont(Font.font("Microsoft YaHei", 12));
        countLabel.setTextFill(Color.web("#8e8e93"));

        info.getChildren().addAll(nameLabel, countLabel);

        // Disclosure arrow
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label arrow = new Label(">");
        arrow.setFont(Font.font(16));
        arrow.setTextFill(Color.web("#c7c7cc"));

        row.getChildren().addAll(coverBox, info, spacer, arrow);

        // Click to open album photos
        row.setOnMouseClicked(e -> {
            mainWindow.openViewer(photos.get(0), photos);
        });

        // Hover effect
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #f0f0f5; " +
                "-fx-background-radius: 10; -fx-cursor: hand;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: white; " +
                "-fx-background-radius: 10; -fx-cursor: hand;"));

        return row;
    }
}
