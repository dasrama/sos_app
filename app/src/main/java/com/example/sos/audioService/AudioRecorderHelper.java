package com.example.sos.audioService;

import android.content.Context;
import android.media.MediaRecorder;
import java.io.IOException;

public class AudioRecorderHelper {
    private MediaRecorder recorder;
    private String audioPath;

    public AudioRecorderHelper(Context context) {
        // Updated extension to .m4a (High quality, compatible with everything)
        audioPath = context.getExternalCacheDir().getAbsolutePath() + "/emergency_voice.m4a";
    }

    public void start() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        
        // Use MPEG_4 container and AAC encoder for better quality
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44100);
        
        recorder.setOutputFile(audioPath);
        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public String stop() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException stopException) {
                // handle cleanup
            }
            recorder.release();
            recorder = null;
        }
        return audioPath;
    }
}
