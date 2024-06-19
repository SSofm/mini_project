package com.sangdev.miniproject;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;


import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class VideoMerger {

    private static int mVideoTrackIndex1 = -1;
    private static int mAudioTrackIndex1 = -1;
    private static int mVideoTrackIndex2 = -1;
    private static int mAudioTrackIndex2 = -1;

    private static final String TAG = "VideoMerger";
    private static final Logger logger = Logger.getLogger(VideoMerger.class.getName());

    public static void mergeVideos(String filePath1, String filePath2, String outputFile) throws IOException {
        MediaMuxer mediaMuxer = null;
        MediaExtractor videoExtractor1 = new MediaExtractor();
        MediaExtractor videoExtractor2 = new MediaExtractor();

        try {
            mediaMuxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Trích xuất và ghi track video từ video đầu tiên
            videoExtractor1.setDataSource(filePath1);
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
            }else{
                muxerAudioTrackIndex = addSilentAudioTrack(filePath1, videoFormat1);
            }

            // Trích xuất và ghi track video từ video thứ hai
            videoExtractor2.setDataSource(filePath2);
            int videoTrackIndex2 = selectTrack(videoExtractor2, "video/");
            MediaFormat videoFormat2 = videoExtractor2.getTrackFormat(videoTrackIndex2);
            videoExtractor2.selectTrack(videoTrackIndex2);

            // Trích xuất và ghi track audio từ video thứ hai
            int audioTrackIndex2 = selectTrack(videoExtractor2, "audio/");
            int muxerAudioTrackIndex2 = -1;
            if (audioTrackIndex2 >= 0) {
                MediaFormat audioFormat2 = videoExtractor2.getTrackFormat(audioTrackIndex2);
                videoExtractor2.selectTrack(audioTrackIndex2);
                muxerAudioTrackIndex2 = mediaMuxer.addTrack(audioFormat2);
            }

            mediaMuxer.start();

            // Ghi dữ liệu video từ video đầu tiên
            mVideoTrackIndex1 = videoTrackIndex1;

            long lastPresentationTimeUs = writeSampleData(videoExtractor1, mediaMuxer, muxerVideoTrackIndex, muxerAudioTrackIndex, 0);


            // Ghi dữ liệu video từ video thứ hai
            mVideoTrackIndex2 = videoTrackIndex2;
            if (muxerAudioTrackIndex != -1) {
                muxerAudioTrackIndex2 -= 1;
            }
            writeSampleData(videoExtractor2, mediaMuxer, muxerVideoTrackIndex, muxerAudioTrackIndex2, lastPresentationTimeUs);

        } finally {
            if (mediaMuxer != null) {
                try {

                    mediaMuxer.stop();

                    mediaMuxer.release();
                } catch (Exception e) {
                    logger.severe(e.toString());
                }
            }
            videoExtractor1.release();
            videoExtractor2.release();
        }
    }

    private static int addSilentAudioTrack(String outputPath, MediaFormat videoFormat) {
        MediaMuxer muxer = null;
        try {
            muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int sampleRate = 44100;
            int channelCount = 1;
            int bitRate = 64000;

            MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            int audioTrackIndex = muxer.addTrack(audioFormat);

            muxer.start();

            int bufferSize = 1024;
            ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                long durationUs = videoFormat.getLong(MediaFormat.KEY_DURATION);
                long presentationTimeUs = 0;

                while (presentationTimeUs < durationUs) {
                    bufferInfo.offset = 0;
                    bufferInfo.size = bufferSize;
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                    bufferInfo.presentationTimeUs = presentationTimeUs;
                    muxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo);
                    presentationTimeUs += (1000000L * bufferSize / (sampleRate * channelCount * 2)); // Assuming 16-bit PCM
                }
            }

            return audioTrackIndex;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (muxer != null) {
                try {
                    muxer.stop();
                    muxer.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static int selectTrack(MediaExtractor extractor, String mimePrefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            assert mime != null;
            if (mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }


    public static void removeAudioTrack(String inputFilePath, String outputFilePath) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        try {
            extractor.setDataSource(inputFilePath);
            int trackCount = extractor.getTrackCount();
            muxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            MediaFormat videoFormat = null;
            int videoTrackIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoFormat = format;
                    videoTrackIndex = muxer.addTrack(format);
                    extractor.selectTrack(i);
                    break;
                }
            }

            if (videoTrackIndex == -1) {
                throw new RuntimeException("No video track found in " + inputFilePath);
            }

            muxer.start();

            ByteBuffer buffer = ByteBuffer.allocate(determineBufferSizeUsingMediaFormat(videoFormat));
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (true) {
                bufferInfo.offset = 0;
                bufferInfo.size = extractor.readSampleData(buffer, 0);
                if (bufferInfo.size < 0) {
                    Log.d(TAG, "End of video file");
                    break;
                }

                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bufferInfo.flags = mapExtractorFlagsToCodecFlags(extractor.getSampleFlags());
                }
                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                extractor.advance();
            }

        } finally {
            if (muxer != null) {
                try {
                    muxer.stop();
                    muxer.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing muxer", e);
                }
            }
            extractor.release();
        }
    }


    private static long writeSampleData(MediaExtractor extractor, MediaMuxer muxer, int videoTrackIndex, int audioTrackIndex, long startPresentationTimeUs) {
        Log.d(TAG, "writeSampleData: check video track index " + videoTrackIndex);
        Log.d(TAG, "writeSampleData: check audio track index " + audioTrackIndex);


        int bufferSize = determineBufferSize(extractor, videoTrackIndex);
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long lastPresentationTimeUs = startPresentationTimeUs;

        while (true) {
            bufferInfo.offset = 0;
            bufferInfo.size = extractor.readSampleData(buffer, 0);
            if (bufferInfo.size < 0) {
                Log.d(TAG, "No more samples to read");
                bufferInfo.size = 0;
                break;
            }
            bufferInfo.presentationTimeUs = extractor.getSampleTime() + startPresentationTimeUs;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bufferInfo.flags = mapExtractorFlagsToCodecFlags(extractor.getSampleFlags());
            }
            int trackIndex = extractor.getSampleTrackIndex();
            Log.d(TAG, "writeSampleData: trackindex " + trackIndex);
            if (trackIndex == 0 && videoTrackIndex % 2 == 0) {
                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
            } else if (trackIndex == 1 && audioTrackIndex % 2 != 0) {
                muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo);
            }
            lastPresentationTimeUs = bufferInfo.presentationTimeUs;
            extractor.advance();
        }
        return lastPresentationTimeUs;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static int mapExtractorFlagsToCodecFlags(int extractorFlags) {
        int codecFlags = 0;
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            codecFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
        }
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            codecFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
        }
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_ENCRYPTED) != 0) {
            codecFlags |= MediaCodec.BUFFER_FLAG_CODEC_CONFIG; // Best guess for encrypted
        }
        return codecFlags;
    }

    private static int determineBufferSizeUsingMediaFormat(MediaFormat format) {
        int width = format.getInteger(MediaFormat.KEY_WIDTH);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);
        int pixelCount = width * height;
        int bufferSize;

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

    private static int determineBufferSize(MediaExtractor extractor, int trackIndex) {
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        int width = format.getInteger(MediaFormat.KEY_WIDTH);
        int height = format.getInteger(MediaFormat.KEY_HEIGHT);
        int pixelCount = width * height;
        int bufferSize;

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

