package nabu.iris.keyboard.latin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public final class KeySoundSynthesizer {

    public static final int SOUNDPACK_CHERRY = 1;
    public static final int SOUNDPACK_TYPEWRITER = 2;
    public static final int SOUNDPACK_BUBBLE = 3;
    public static final int SOUNDPACK_SCIFI = 4;

    public static short[] synthesize(int soundpack, String keyType) {
        int sampleRate = 44100;
        int length;
        
        if (soundpack == SOUNDPACK_TYPEWRITER && keyType.equals("return")) {
            length = 15000; // ~340ms carriage return slide
        } else if (keyType.equals("spacebar")) {
            length = 5292; // ~120ms
        } else if (keyType.equals("return") || keyType.equals("delete")) {
            length = 4410; // ~100ms
        } else {
            length = 3528; // ~80ms
        }

        short[] pcm = new short[length];
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            double t = (double) i / (double) sampleRate;
            double val = 0;

            switch (soundpack) {
                case SOUNDPACK_CHERRY:
                    // 1. Tactile Bump Click (sharp transient at 0-15ms)
                    if (i < 660) {
                        double clickFreq = 4200.0;
                        double envelope = Math.exp(-t * 350.0);
                        val += Math.sin(2.0 * Math.PI * clickFreq * t) * envelope * 0.45;
                    }
                    // 2. Heavy bottoming out contact thump (sine wave + noise)
                    double thumpDelay = 0.008; // 8ms delay
                    if (t > thumpDelay) {
                        double tThump = t - thumpDelay;
                        double thumpFreq = keyType.equals("spacebar") ? 170.0 : 250.0;
                        double envelope = Math.exp(-tThump * 80.0);
                        val += Math.sin(2.0 * Math.PI * thumpFreq * tThump) * envelope * 0.40;
                        
                        // Add organic plastic contact noise
                        double noiseEnv = Math.exp(-tThump * 180.0);
                        val += (random.nextDouble() * 2.0 - 1.0) * noiseEnv * 0.07;
                    }
                    break;

                case SOUNDPACK_TYPEWRITER:
                    if (keyType.equals("return")) {
                        // Striker sound
                        double strikeEnv = Math.exp(-t * 110.0);
                        val += (random.nextDouble() * 2.0 - 1.0) * strikeEnv * 0.25;
                        val += Math.sin(2.0 * Math.PI * 300.0 * t) * strikeEnv * 0.20;

                        // Typewriter Carriage Return Slide ("Ziiippp-clack!")
                        double slideStart = 0.08;
                        if (t > slideStart) {
                            double tSlide = t - slideStart;
                            if (tSlide < 0.20) {
                                // Slide zipp frequency sweep
                                double slideFreq = 80.0 + 350.0 * Math.sin(2.0 * Math.PI * 20.0 * tSlide);
                                val += Math.sin(2.0 * Math.PI * slideFreq * tSlide) * 0.15;
                            } else {
                                // Carriage Clack End
                                double tClack = tSlide - 0.20;
                                double clackEnv = Math.exp(-tClack * 130.0);
                                val += (random.nextDouble() * 2.0 - 1.0) * clackEnv * 0.30;
                                val += Math.sin(2.0 * Math.PI * 200.0 * tClack) * clackEnv * 0.20;
                            }
                        }
                    } else {
                        // Standard striker impact click
                        double envClick = Math.exp(-t * 220.0);
                        val += (random.nextDouble() * 2.0 - 1.0) * envClick * 0.25;

                        // Resonant metallic bell ring decay
                        double bellFreq1 = 3900.0;
                        double bellFreq2 = 4600.0;
                        double envBell = Math.exp(-t * 45.0);
                        val += (Math.sin(2.0 * Math.PI * bellFreq1 * t) + Math.sin(2.0 * Math.PI * bellFreq2 * t)) * envBell * 0.10;
                    }
                    break;

                case SOUNDPACK_BUBBLE:
                    // Rapid bubble pressure sweep (pitch drops from high to low)
                    double fStart = keyType.equals("spacebar") ? 950.0 : 1350.0;
                    double fEnd = keyType.equals("spacebar") ? 480.0 : 680.0;
                    double sweep = fStart + (fEnd - fStart) * (t / 0.022); // sweep in first 22ms
                    
                    if (t < 0.022) {
                        double rising = t / 0.004; // 4ms attack
                        if (rising > 1.0) rising = 1.0;
                        double env = rising * Math.exp(-(t - 0.004) * 160.0);
                        val += Math.sin(2.0 * Math.PI * sweep * t) * env * 0.60;
                    } else {
                        double env = Math.exp(-(t - 0.022) * 85.0);
                        val += Math.sin(2.0 * Math.PI * fEnd * t) * env * 0.18;
                    }
                    break;

                case SOUNDPACK_SCIFI:
                    double baseFreq = keyType.equals("spacebar") ? 580.0 : (keyType.equals("delete") ? 780.0 : 980.0);
                    // Sine sweep + triangular LFO vibrato
                    double lfo = Math.sin(2.0 * Math.PI * 26.0 * t) * 75.0;
                    double freq = baseFreq * Math.exp(-t * 7.5) + lfo;

                    double env = Math.exp(-t * 22.0); // synth decay
                    val += Math.sin(2.0 * Math.PI * freq * t) * env * 0.50;
                    break;
            }

            pcm[i] = (short) (Math.max(-1.0, Math.min(1.0, val)) * 32767);
        }

        return pcm;
    }

    public static void writeWavFile(File file, short[] pcmData, int sampleRate) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        int totalAudioLen = pcmData.length * 2;
        int totalDataLen = totalAudioLen + 36;
        int channels = 1;
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
        header[32] = 2; // block align
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
        
        // Write PCM data
        byte[] pcmBytes = new byte[pcmData.length * 2];
        for (int i = 0; i < pcmData.length; i++) {
            pcmBytes[i * 2] = (byte) (pcmData[i] & 0xff);
            pcmBytes[i * 2 + 1] = (byte) ((pcmData[i] >> 8) & 0xff);
        }
        fos.write(pcmBytes);
        fos.close();
    }
}
