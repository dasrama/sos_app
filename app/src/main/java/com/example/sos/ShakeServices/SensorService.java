package com.example.sos.ShakeServices;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.sos.R;
import com.example.sos.SosAlertActivity;
import com.example.sos.audioService.CloudinaryAudioUploader;
import com.example.sos.model.ContactModel;
import com.example.sos.dbHelper.DbHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorService extends Service {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private static final String CHANNEL_ID = "SensorServiceChannel";

    public SensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "doojw9oxi");
        try {
            com.cloudinary.android.MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // Already initialized
        }

        createNotificationChannel();
        startMyOwnForeground();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();

        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onShake(int count) {
                if (count == 3) {
                    vibrate();

                    // Message 1: Send User Profile Details first
                    sendUserProfileSms();

                    // Message 2: Send GPS location (Decoupled via LocationHandler)
                    LocationHandler locationHandler = new LocationHandler(getApplicationContext(), message -> {
                        sendSmsToAll(message);
                    });
                    locationHandler.getUserLocation();

                    // Message 3: Start recording and send audio link later
                    startAudioRecordingSequence();
                }
            }
        });

        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    private void sendUserProfileSms() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        String name = prefs.getString("name", "N/A");
        String dob = prefs.getString("dob", "N/A");
        String bloodGroup = prefs.getString("bloodGroup", "N/A");
        String diabetic = prefs.getString("diabetic", "N/A");
        String disabled = prefs.getString("disabled", "N/A");

        int age = calculateAge(dob);
        String ageStr = (age == -1) ? "N/A" : String.valueOf(age);

        String message = "Victim Details:\nName: " + name + "\nAge: " + ageStr + " years\nBlood Group: " + bloodGroup + 
                         "\nDiabetic: " + diabetic + "\nPhysically Disabled: " + disabled;

        sendSmsToAll(message);
    }

    private int calculateAge(String dob) {
        if (dob == null || dob.equals("N/A") || !dob.contains("/")) return -1;
        try {
            String[] parts = dob.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            Calendar dobCal = Calendar.getInstance();
            dobCal.set(year, month - 1, day);

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);

            if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            return -1;
        }
    }

    private void startAudioRecordingSequence() {
        com.example.sos.audioService.AudioRecorderHelper audioHelper =
                new com.example.sos.audioService.AudioRecorderHelper(getApplicationContext());

        audioHelper.start();
        Log.d("SOS_APP", "Recording started...");

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            String path = audioHelper.stop();
            CloudinaryAudioUploader.upload(path, new CloudinaryAudioUploader.OnUploadListener(){
                @Override
                public void onSuccess(String url) {
                    sendSmsToAll("SOS Audio Evidence: " + url);
                    showSosPopup();
                }

                @Override
                public void onFailure(String error) {
                    Log.e("SOS_APP", "Audio Upload Failed: " + error);
                }
            });
        }, 30000);
    }

    private void showSosPopup() {
        Intent intent = new Intent(this, SosAlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void sendSmsToAll(String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            DbHelper db = new DbHelper(SensorService.this);
            List<ContactModel> list = db.getAllContacts();

            for (ContactModel c : list) {
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(c.getPhoneNo(), null, parts, null, null);
                Log.d("SOS_APP", "SMS sent to: " + c.getPhoneNo());
            }
        } catch (Exception e) {
            Log.e("SOS_APP", "SMS sending failed", e);
        }
    }

    public void vibrate() {
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK));
        } else {
            vibrator.vibrate(500);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sensor Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Swift-Rescue Protection Active")
                .setContentText("Your safety is our priority")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(mShakeDetector);
        super.onDestroy();
    }
}
