package com.sangdev.miniproject;

import android.net.Uri;
import android.provider.MediaStore;

public class VideoItem {
    private String id;
    private String title;
    private String resolution;
    private String duration;
    private String size;
    private String angle;

    private String path;
    private boolean selected;

    public VideoItem(String id, String title, String resolution, String duration, String size, String angle, String path) {
        this.id = id;
        this.title = title;
        this.resolution = resolution;
        this.duration = duration;
        this.size = size;
        this.angle = angle;
        this.path = path;
        this.selected = false;
    }

    public String getPath() {
        return path;
    }


    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getResolution() {
        return resolution;
    }

    public String getDuration() {
        return duration;
    }

    public String getSize() {
        return size;
    }

    public String getAngle() {
        return angle;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Uri getThumbnailUri() {
        return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
    }
}
