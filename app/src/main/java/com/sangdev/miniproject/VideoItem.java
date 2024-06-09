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
    private boolean selected;

    public VideoItem(String id, String title, String resolution, String duration, String size, String angle) {
        this.id = id;
        this.title = title;
        this.resolution = resolution;
        this.duration = duration;
        this.size = size;
        this.angle = angle;
        this.selected = false;
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
