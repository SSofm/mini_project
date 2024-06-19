package com.sangdev.miniproject;

import static com.sangdev.miniproject.VideoUtils.getOutputRemoveSoundFilePath;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.sangdev.miniproject.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements VideoAdapter.OnVideoSelectedListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private ActivityMainBinding binding;
    private VideoAdapter videoAdapter;
    private List<VideoItem> videoList;

    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate layout using ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Setup RecyclerView
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        videoList = new ArrayList<>();
        videoAdapter = new VideoAdapter(this, videoList, this);
        binding.recyclerView.setAdapter(videoAdapter);

        // Request storage permissions if necessary
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            loadVideos();
        }

        // Initialize merge button
        binding.mergeButtonId.setEnabled(false);
        binding.mergeButtonId.setOnClickListener(v -> {

            ArrayList<String> inputPaths = new ArrayList<>();
            for (VideoItem item : videoList) {
                if (item.isSelected()) {
                    inputPaths.add(item.getPath());
                }
            }
            String outputPath = VideoUtils.getOutputMergeFilePath(inputPaths.get(0));
            if (inputPaths.size() == 2) {
                Log.d(TAG, "onCreate: file path1 " + inputPaths.get(0));
                Log.d(TAG, "onCreate: file path2 " + inputPaths.get(1));
                try {
//                    VideoMerger.removeAudioTrack(inputPaths.get(0), getOutputRemoveSoundFilePath(inputPaths.get(0)));
                    VideoMerger.mergeVideos(inputPaths.get(0), inputPaths.get(1), outputPath);
                    showShortToast("Merged video...");
                    playVideo(outputPath);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }

    private void showShortToast(String content) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    private void loadVideos() {
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.RESOLUTION,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.ORIENTATION,
                MediaStore.Video.Media.DATA,
        };

        Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                String resolution = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                String size = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                String angle = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ORIENTATION));

                // Đường dẫn tệp dạng tuyệt đối
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));

                if(duration!=null){
                    duration = convertDuration(Long.parseLong(duration));
                }
                VideoItem videoItem = new VideoItem(id, title, resolution, duration, size, angle, path);
                videoList.add(videoItem);
                videoAdapter.notifyItemInserted(videoList.size() - 1);
            }
            cursor.close();

        }
    }

    public static String convertDuration(long durationInMs) {
        // Tính toán số giờ
        long hours = durationInMs / (1000 * 60 * 60);
        durationInMs %= (1000 * 60 * 60);

        // Tính toán số phút
        long minutes = durationInMs / (1000 * 60);
        durationInMs %= (1000 * 60);

        // Tính toán số giây
        long seconds = durationInMs / 1000;

        // Số mili giây còn lại
        long milliseconds = durationInMs % 1000;

        // Định dạng kết quả thành h:m:s.millisecond
        return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadVideos();
            } else {
                Toast.makeText(this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onVideoSelected(int count) {
        binding.mergeButtonId.setEnabled(count <= 2);
    }

    private void playVideo(String videoPath) {

        Uri uri = Uri.parse(videoPath);

        // Cấu hình MediaController để điều khiển video (play, pause, seek, ...)
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(binding.videoView);

        // Đặt MediaController vào VideoView
        binding.videoView.setMediaController(mediaController);

        // Đặt đường dẫn video cho VideoView và bắt đầu phát video
        binding.videoView.setVideoURI(uri);
        binding.videoView.start();
    }
}
