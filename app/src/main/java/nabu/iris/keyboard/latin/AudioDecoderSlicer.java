package nabu.iris.keyboard.latin;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class AudioDecoderSlicer {
    private static final String TAG = "AudioDecoderSlicer";

    public static class DecodedAudio {
        public final short[] pcmShorts;
        public final int sampleRate;
        public final int channelCount;

        public DecodedAudio(short[] pcmShorts, int sampleRate, int channelCount) {
            this.pcmShorts = pcmShorts;
            this.sampleRate = sampleRate;
            this.channelCount = channelCount;
        }
    }

    public static boolean sliceAudio(File audioFile, File destFile, int startMs, int durationMs) {
        DecodedAudio decoded = decodeAudio(audioFile);
        if (decoded == null) return false;
        return sliceAudioFromDecoded(decoded, destFile, startMs, durationMs);
    }

    public static DecodedAudio decodeAudio(File audioFile) {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec decoder = null;
        ByteArrayOutputStream pcmStream = new ByteArrayOutputStream();

        try {
            extractor.setDataSource(audioFile.getAbsolutePath());
            int trackIndex = -1;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    trackIndex = i;
                    break;
                }
            }

            if (trackIndex == -1) {
                Log.e(TAG, "No audio track found in: " + audioFile.getName());
                return null;
            }

            extractor.selectTrack(trackIndex);
            MediaFormat format = extractor.getTrackFormat(trackIndex);
            String mime = format.getString(MediaFormat.KEY_MIME);

            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, null, null, 0);
            decoder.start();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isInputEOS = false;
            boolean isOutputEOS = false;

            int noProgressCount = 0;
            while (!isOutputEOS) {
                boolean progressMade = false;
                if (!isInputEOS) {
                    int inputBufferIndex = decoder.dequeueInputBuffer(10000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            inputBuffer = decoder.getInputBuffer(inputBufferIndex);
                        } else {
                            inputBuffer = decoder.getInputBuffers()[inputBufferIndex];
                        }

                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isInputEOS = true;
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                        progressMade = true;
                    }
                }

                int outputBufferIndex = decoder.dequeueOutputBuffer(info, 10000);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = decoder.getOutputBuffer(outputBufferIndex);
                    } else {
                        outputBuffer = decoder.getOutputBuffers()[outputBufferIndex];
                    }

                    if (info.size > 0) {
                        byte[] chunk = new byte[info.size];
                        outputBuffer.position(info.offset);
                        outputBuffer.get(chunk);
                        pcmStream.write(chunk);
                    }

                    decoder.releaseOutputBuffer(outputBufferIndex, false);
                    progressMade = true;

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true;
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    progressMade = true;
                }

                if (progressMade) {
                    noProgressCount = 0;
                } else {
                    noProgressCount++;
                    if (noProgressCount > 500) {
                        Log.e(TAG, "MediaCodec decode loop stuck - breaking to prevent freeze");
                        break;
                    }
                }
            }

            byte[] fullPcmBytes = pcmStream.toByteArray();
            short[] fullPcmShorts = new short[fullPcmBytes.length / 2];
            ByteBuffer.wrap(fullPcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(fullPcmShorts);

            return new DecodedAudio(fullPcmShorts, sampleRate, channelCount);

        } catch (Exception e) {
            Log.e(TAG, "Error decoding audio", e);
            return null;
        } finally {
            try {
                extractor.release();
            } catch (Exception e) {
                // Ignore
            }
            if (decoder != null) {
                try {
                    decoder.stop();
                    decoder.release();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    public static boolean sliceAudioFromDecoded(DecodedAudio decoded, File destFile, int startMs, int durationMs) {
        if (decoded == null || decoded.pcmShorts == null) {
            Log.e(TAG, "Decoded audio is null");
            return false;
        }
        try {
            short[] fullPcmShorts = decoded.pcmShorts;
            int sampleRate = decoded.sampleRate;
            int channelCount = decoded.channelCount;

            // Calculate start and end sample positions based on startMs and durationMs
            int startSample = (int) (((long) startMs * sampleRate * channelCount) / 1000);
            int durationSamples = (int) (((long) durationMs * sampleRate * channelCount) / 1000);

            if (startSample >= fullPcmShorts.length) {
                startSample = fullPcmShorts.length - 1;
            }
            if (startSample < 0) {
                startSample = 0;
            }

            int endSample = startSample + durationSamples;
            if (endSample > fullPcmShorts.length) {
                endSample = fullPcmShorts.length;
            }

            int slicedLength = endSample - startSample;
            if (slicedLength <= 0) {
                Log.e(TAG, "Sliced audio length is zero or negative");
                return false;
            }

            short[] slicedPcmShorts = new short[slicedLength];
            System.arraycopy(fullPcmShorts, startSample, slicedPcmShorts, 0, slicedLength);

            // Write as standard mono or stereo WAV file
            writeWavFile(destFile, slicedPcmShorts, sampleRate, channelCount);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error slicing decoded audio", e);
            return false;
        }
    }

    private static void writeWavFile(File file, short[] pcmData, int sampleRate, int channels) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        int totalAudioLen = pcmData.length * 2;
        int totalDataLen = totalAudioLen + 36;
        long byteRate = sampleRate * channels * 2;

        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; // WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1 (PCM)
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd'; // 'data' chunk
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        fos.write(header);

        byte[] pcmBytes = new byte[pcmData.length * 2];
        for (int i = 0; i < pcmData.length; i++) {
            pcmBytes[i * 2] = (byte) (pcmData[i] & 0xff);
            pcmBytes[i * 2 + 1] = (byte) ((pcmData[i] >> 8) & 0xff);
        }
        fos.write(pcmBytes);
        fos.close();
    }
}
