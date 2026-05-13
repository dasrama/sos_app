package com.example.sos.audioService;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.Map;

public class CloudinaryAudioUploader {

    private static final String CLOUDINARY_UPLOAD_PRESET = "sos-app";

    public interface OnUploadListener {
        void onSuccess(String url);
        void onFailure(String error);
    }

    public static void upload(String localPath, OnUploadListener listener) {
        MediaManager.get().upload(localPath)
                .unsigned(CLOUDINARY_UPLOAD_PRESET)
                .option("resource_type", "auto") // FIX: Tells Cloudinary to accept audio/video files
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Success! Get the secure URL
                        String audioUrl = (String) resultData.get("secure_url");
                        listener.onSuccess(audioUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        listener.onFailure(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        listener.onFailure("Rescheduled: " + error.getDescription());
                    }
                }).dispatch();
    }
}
