package com.photoalbum;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Photo implements Comparable<Photo> {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Path path;
    private final String name;
    private final long sizeBytes;
    private final LocalDateTime date;
    private final String parentFolderName;
    private boolean favorite;

    public Photo(Path path) {
        this.path = path;
        this.name = path.getFileName().toString();
        this.sizeBytes = path.toFile().length();
        this.parentFolderName = path.getParent().getFileName().toString();

        LocalDateTime d = LocalDateTime.now();
        try {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime ft = attr.lastModifiedTime();
            d = ft.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {
        }
        this.date = d;
        this.favorite = false;
    }

    public Path getPath() { return path; }
    public String getName() { return name; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getDate() { return date; }
    public String getParentFolderName() { return parentFolderName; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean f) { this.favorite = f; }

    public String getDateString() { return date.format(DATE_FMT); }
    public String getTimeString() { return date.format(TIME_FMT); }

    public String getFormattedSize() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
        return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }

    @Override
    public int compareTo(Photo o) {
        return o.date.compareTo(this.date);
    }

    @Override
    public String toString() {
        return name + " (" + getDateString() + ")";
    }
}
