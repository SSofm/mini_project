package com.sangdev.miniproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sangdev.miniproject.databinding.ItemVideoBinding;

import java.io.IOException;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final Context context;
    private final List<VideoItem> videoList;
    private final OnVideoSelectedListener listener;
    private int selectedCount = 0;

    public interface OnVideoSelectedListener {
        void onVideoSelected(int selectedCount);
    }

    public VideoAdapter(Context context, List<VideoItem> videoList, OnVideoSelectedListener listener) {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemVideoBinding binding = ItemVideoBinding.inflate(inflater, parent, false);
        return new VideoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        int firstIndex = position * 2;
        int secondIndex = firstIndex + 1;

        try {
            holder.bindFirstVideo(videoList.get(firstIndex));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (secondIndex < videoList.size()) {
            try {

                holder.bindSecondVideo(videoList.get(secondIndex));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            holder.clearSecondVideo();
        }
    }



    @Override
    public int getItemCount() {
        return (int) Math.ceil(videoList.size() / 2.0);
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {

        private final ItemVideoBinding binding;

        public VideoViewHolder(ItemVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindFirstVideo(VideoItem videoItem) throws IOException {
            binding.videoTitle1.setText(videoItem.getTitle());
            binding.videoResolution1.setText(videoItem.getResolution());
            binding.videoDuration1.setText(videoItem.getDuration());
            binding.videoSize1.setText(videoItem.getSize());
            binding.videoAngle1.setText(videoItem.getAngle());
            binding.videoCheckbox1.setChecked(videoItem.isSelected());

            Uri videoUri = videoItem.getThumbnailUri();
            Bitmap thumbnail = getVideoThumbnail(videoUri);
            if (thumbnail != null) {
                binding.videoThumbnail1.setImageBitmap(thumbnail);
            } else {
                binding.videoThumbnail1.setImageResource(R.drawable.placeholder); // Placeholder image if thumbnail not available
            }

            binding.videoThumbnail1.setOnClickListener(v -> {
                Uri contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoItem.getId());
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("video_uri", contentUri.toString());
                context.startActivity(intent);
            });

            binding.videoCheckbox1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && selectedCount >= 2) {
                    buttonView.setChecked(false); // Uncheck the checkbox if already 2 videos selected
                    return;
                }

                videoItem.setSelected(isChecked);

                if (isChecked) {
                    selectedCount++;
                } else {
                    selectedCount--;
                }

                listener.onVideoSelected(selectedCount);
            });

        }

        private Bitmap getVideoThumbnail(Uri uri) throws IOException {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, uri);
                return retriever.getFrameAtTime(0);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } finally {
                retriever.release();
            }
            return null;
        }

        public void bindSecondVideo(VideoItem videoItem) throws IOException {
            binding.videoTitle2.setText(videoItem.getTitle());
            binding.videoResolution2.setText(videoItem.getResolution());
            binding.videoDuration2.setText(videoItem.getDuration());
            binding.videoSize2.setText(videoItem.getSize());
            binding.videoAngle2.setText(videoItem.getAngle());
            binding.videoCheckbox2.setChecked(videoItem.isSelected());
            Uri videoUri = videoItem.getThumbnailUri();
            Bitmap thumbnail = getVideoThumbnail(videoUri);
            if (thumbnail != null) {
                binding.videoThumbnail2.setImageBitmap(thumbnail);
            } else {
                binding.videoThumbnail2.setImageResource(R.drawable.placeholder); // Placeholder image if thumbnail not available
            }


            binding.videoThumbnail2.setOnClickListener(v -> {
                Uri contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoItem.getId());
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("video_uri", contentUri.toString());
                context.startActivity(intent);
            });


            binding.videoCheckbox2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && selectedCount >= 2) {
                    buttonView.setChecked(false); // Uncheck the checkbox if already 2 videos selected
                    return;
                }

                videoItem.setSelected(isChecked);

                if (isChecked) {
                    selectedCount++;
                } else {
                    selectedCount--;
                }

                listener.onVideoSelected(selectedCount);
            });
        }

        public void clearSecondVideo() {
            binding.videoTitle2.setText("");
            binding.videoResolution2.setText("");
            binding.videoDuration2.setText("");
            binding.videoSize2.setText("");
            binding.videoAngle2.setText("");
            binding.videoThumbnail2.setImageResource(R.drawable.placeholder);
            binding.videoThumbnail2.setOnClickListener(null);
        }
    }
}
