package com.photoalbum;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoService {

    private static PhotoService instance;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
    );

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<Path, Image> thumbnailCache = new ConcurrentHashMap<>();
    private List<Photo> photos = new CopyOnWriteArrayList<>();
    private Path currentDirectory;
    private String statusMessage = "";

    public static PhotoService getInstance() {
        if (instance == null) {
            instance = new PhotoService();
        }
        return instance;
    }

    private PhotoService() {
        String userHome = System.getProperty("user.home");
        currentDirectory = Paths.get(userHome, "Pictures");
        if (!Files.exists(currentDirectory)) {
            currentDirectory = Paths.get(userHome);
        }
    }

    public Path getCurrentDirectory() { return currentDirectory; }
    public void setCurrentDirectory(Path dir) { this.currentDirectory = dir; }
    public List<Photo> getPhotos() { return photos; }
    public String getStatusMessage() { return statusMessage; }

    public void scanPhotos(Runnable onComplete) {
        statusMessage = "Scanning photos...";
        executor.submit(() -> {
            try {
                List<Photo> found = new ArrayList<>();
                Files.walkFileTree(currentDirectory,
                        EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                        Integer.MAX_VALUE,
                        new SimpleFileVisitor<Path>() {
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                String ext = getExtension(file).toLowerCase();
                                if (IMAGE_EXTENSIONS.contains(ext)) {
                                    found.add(new Photo(file));
                                }
                                return FileVisitResult.CONTINUE;
                            }
                            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        });
                Collections.sort(found);
                photos = new CopyOnWriteArrayList<>(found);
                statusMessage = "Found " + found.size() + " photos";
            } catch (Exception e) {
                statusMessage = "Scan failed: " + e.getMessage();
                photos = new CopyOnWriteArrayList<>();
            } finally {
                if (onComplete != null) {
                    javafx.application.Platform.runLater(onComplete);
                }
            }
        });
    }

    public Image getThumbnail(Photo photo, int size) {
        Path key = photo.getPath();
        Image cached = thumbnailCache.get(key);
        if (cached != null) return cached;
        executor.submit(() -> {
            try {
                BufferedImage buf = loadAndCropSquare(photo.getPath(), size);
                if (buf != null) {
                    Image img = SwingFXUtils.toFXImage(buf, null);
                    thumbnailCache.put(key, img);
                }
            } catch (Exception ignored) {}
        });
        return null;
    }

    public Image loadFullImage(Path path) {
        try { return new Image(path.toUri().toString(), true); }
        catch (Exception e) { return null; }
    }

    private BufferedImage loadAndCropSquare(Path path, int targetSize) {
        try {
            BufferedImage original = ImageIO.read(path.toFile());
            if (original == null) return null;
            int w = original.getWidth(), h = original.getHeight();
            int cropSize = Math.min(w, h);
            int x = (w - cropSize) / 2, y = (h - cropSize) / 2;
            BufferedImage cropped = original.getSubimage(x, y, cropSize, cropSize);
            BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(cropped, 0, 0, targetSize, targetSize, null);
            g.dispose();
            return scaled;
        } catch (Exception e) { return null; }
    }

    public Map<String, List<Photo>> getAlbums() {
        Map<String, List<Photo>> albums = new LinkedHashMap<>();
        for (Photo p : photos) {
            albums.computeIfAbsent(p.getParentFolderName(), k -> new ArrayList<>()).add(p);
        }
        return albums;
    }

    public void deletePhoto(Photo photo) {
        photos.remove(photo);
        thumbnailCache.remove(photo.getPath());
        try { Files.deleteIfExists(photo.getPath()); } catch (IOException ignored) {}
    }

    public void shutdown() { executor.shutdown(); }

    private static String getExtension(Path path) {
        String name = path.getFileName().toString();
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : "";
    }
}