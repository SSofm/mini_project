package com.sangdev.miniproject;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoMerger {

    private static final String TAG = "VideoMerger";

    public void mergeVideos(AssetFileDescriptor afd1, AssetFileDescriptor afd2, String outputFile) throws IOException {
        MediaMuxer mediaMuxer = null;
        MediaExtractor videoExtractor1 = new MediaExtractor();
        MediaExtractor videoExtractor2 = new MediaExtractor();
        try {
            mediaMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Trích xuất và ghi track video từ video đầu tiên
            videoExtractor1.setDataSource(afd1.getFileDescriptor(), afd1.getStartOffset(), afd1.getLength());
            int videoTrackIndex1 = selectTrack(videoExtractor1, "video/");
            MediaFormat videoFormat1 = videoExtractor1.getTrackFormat(videoTrackIndex1);
            videoExtractor1.selectTrack(videoTrackIndex1);
            int muxerVideoTrackIndex = mediaMuxer.addTrack(videoFormat1);

            // Tuỳ chọn, trích xuất và ghi track audio từ video đầu tiên
            int audioTrackIndex1 = selectTrack(videoExtractor1, "audio/");
            int muxerAudioTrackIndex = -1;
            if (audioTrackIndex1 >= 0) {
                MediaFormat audioFormat1 = videoExtractor1.getTrackFormat(audioTrackIndex1);
                videoExtractor1.selectTrack(audioTrackIndex1);
                muxerAudioTrackIndex = mediaMuxer.addTrack(audioFormat1);
            }

            // Trích xuất và ghi track audio từ video thứ hai
            int audioTrackIndex2 = selectTrack(videoExtractor2, "audio/");
            int muxerAudioTrackIndex2 = -1;
            if (audioTrackIndex2 >= 0) {
                MediaFormat audioFormat2 = videoExtractor2.getTrackFormat(audioTrackIndex2);
                videoExtractor2.selectTrack(audioTrackIndex2);
                muxerAudioTrackIndex2 = mediaMuxer.addTrack(audioFormat2);
            }

            // Trích xuất và ghi track video từ video thứ hai
            videoExtractor2.setDataSource(afd2.getFileDescriptor(), afd2.getStartOffset(), afd2.getLength());
            int videoTrackIndex2 = selectTrack(videoExtractor2, "video/");
            MediaFormat videoFormat2 = videoExtractor2.getTrackFormat(videoTrackIndex2);
            videoExtractor2.selectTrack(videoTrackIndex2);


            mediaMuxer.start();

            // Ghi dữ liệu video từ video đầu tiên
            writeSampleData(videoExtractor1, mediaMuxer, muxerVideoTrackIndex, muxerAudioTrackIndex);




            // Ghi dữ liệu video từ video thứ hai
            writeSampleData(videoExtractor2, mediaMuxer, muxerVideoTrackIndex, muxerAudioTrackIndex2);

        } finally {
            if (mediaMuxer != null) {
                try {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            videoExtractor1.release();
            videoExtractor2.release();
            afd1.close();
            afd2.close();
        }
    }

    private int selectTrack(MediaExtractor extractor, String mimePrefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }

    private void writeSampleData(MediaExtractor extractor, MediaMuxer muxer, int videoTrackIndex, int audioTrackIndex) {

        ByteBuffer buffer = ByteBuffer.allocate(256 * 1024);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true) {
            bufferInfo.offset = 0;
            bufferInfo.size = extractor.readSampleData(buffer, 0);
            if (bufferInfo.size < 0) {
                Log.d(TAG, "No more samples to read");
                bufferInfo.size = 0;
                break;
            }
            bufferInfo.presentationTimeUs = extractor.getSampleTime();
            bufferInfo.flags = mapExtractorFlagsToCodecFlags(extractor.getSampleFlags());
            int trackIndex = extractor.getSampleTrackIndex();
            if (trackIndex == videoTrackIndex) {
                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
            } else if (trackIndex == audioTrackIndex) {
                muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo);
            }
            extractor.advance();
        }
    }

    private int mapExtractorFlagsToCodecFlags(int extractorFlags) {
        int codecFlags = 0;
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            codecFlags |= MediaCodec.BUFFER_FLAG_SYNC_FRAME;
        }
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            codecFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
        }
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_ENCRYPTED) != 0) {
            codecFlags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG; // Best guess for encrypted
        }
        return codecFlags;
    }

    private int determineBufferSize(MediaExtractor extractor, int trackIndex) {
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        int width = format.getInteger(MediaFormat.KEY_WIDTH);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);
        int pixelCount = width * height;
        int bufferSize = 0;

        if (pixelCount <= 1280 * 720) { // 720p
            bufferSize = 256 * 1024; // 256 KB
        } else if (pixelCount <= 1920 * 1080) { // 1080p
            bufferSize = 512 * 1024; // 512 KB
        } else if (pixelCount <= 3840 * 2160) { // 4K
            bufferSize = 1024 * 1024; // 1 MB
        } else {
            bufferSize = 2048 * 1024; // 2 MB
        }

        return bufferSize;
    }
}

