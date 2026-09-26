package com.safesphere.android.hardware;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * High-Decibel Modulating Acoustic Siren Generator.
 * Synthesizes a piercing 900Hz - 2200Hz emergency distress siren using raw AudioTrack PCM streaming.
 */
public class SirenManager {
    private static final int SAMPLE_RATE = 44100;
    private AudioTrack audioTrack;
    private Thread audioThread;
    private volatile boolean isPlaying = false;

    public synchronized void startSiren() {
        if (isPlaying) return;
        isPlaying = true;

        audioThread = new Thread(() -> {
            int bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );

            audioTrack = new AudioTrack(
                    AudioManager.STREAM_ALARM,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
            );

            audioTrack.play();

            short[] buffer = new short[bufferSize];
            double phase = 0.0;
            double modPhase = 0.0;

            while (isPlaying) {
                for (int i = 0; i < buffer.length; i++) {
                    // Modulate frequency between 900Hz and 2200Hz at a 1.5Hz sweep cycle
                    double currentFreq = 1550.0 + 650.0 * Math.sin(modPhase);
                    modPhase += 2.0 * Math.PI * 1.5 / SAMPLE_RATE;
                    if (modPhase > 2.0 * Math.PI) modPhase -= 2.0 * Math.PI;

                    buffer[i] = (short) (Math.sin(phase) * 32767.0);
                    phase += 2.0 * Math.PI * currentFreq / SAMPLE_RATE;
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI;
                }
                audioTrack.write(buffer, 0, buffer.length);
            }

            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
        }, "SafeSphere-Siren-Thread");

        audioThread.start();
    }

    public synchronized void stopSiren() {
        isPlaying = false;
        if (audioThread != null) {
            audioThread.interrupt();
            audioThread = null;
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
