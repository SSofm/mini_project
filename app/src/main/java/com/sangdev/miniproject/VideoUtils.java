package com.sangdev.miniproject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoUtils {

    @SuppressLint("WrongConstant")
    public static void removeAudioFromVideo(Context context, Uri inputUri, String outputFilePath) throws IOException {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;

        try (FileInputStream fis = new FileInputStream(new File(String.valueOf(context.getContentResolver().openFileDescriptor(inputUri, "r").getFileDescriptor())))) {
            FileDescriptor fd = fis.getFD();
            videoExtractor.setDataSource(fd);
            int videoTrackIndex = -1;

            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                MediaFormat format = videoExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);

                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i);
                    mediaMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    videoTrackIndex = mediaMuxer.addTrack(format);
                    mediaMuxer.start();
                    break;
                }
            }

            if (videoTrackIndex == -1) {
                throw new IOException("No video track found in " + inputUri);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);

            while (true) {
                bufferInfo.offset = 0;
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0);

                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0;
                    break;
                }

                bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                bufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                videoExtractor.advance();
            }
        } finally {
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
            }
            videoExtractor.release();
        }
    }

    public static String getOutputMergeFilePath(String inputFilePath) {
        File inputFile = new File(inputFilePath);
        File parentDir = inputFile.getParentFile(); // Lấy thư mục cha của input file
        String outputFileName = "merged_video_" + System.currentTimeMillis() + ".mp4";
        File outputFile = new File(parentDir, outputFileName);

        return outputFile.getAbsolutePath();
    }

    public static String getOutputRemoveSoundFilePath(String inputFilePath) {
        File inputFile = new File(inputFilePath);
        File parentDir = inputFile.getParentFile(); // Lấy thư mục cha của input file
        String outputFileName = "remove_sound_" + System.currentTimeMillis() + ".mp4";
        File outputFile = new File(parentDir, outputFileName);

        return outputFile.getAbsolutePath();
    }

}
