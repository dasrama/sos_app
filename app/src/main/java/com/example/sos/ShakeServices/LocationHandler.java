package com.example.sos.ShakeServices;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationHandler {

    public interface OnLocationResultListener {
        void onLocationResult(String message);
    }

    private final Context context;
    private final OnLocationResultListener listener;

    public LocationHandler(Context context, OnLocationResultListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void getUserLocation() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            listener.onLocationResult("DANGER! I need help immediately. (Location permission denied)");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                String message;
                if (location != null) {
                    message = "I am in DANGER! Help me at: http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                } else {
                    message = "I am in DANGER! Help me immediately. GPS location currently unavailable.";
                }
                listener.onLocationResult(message);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                listener.onLocationResult("DANGER! Emergency SOS triggered. GPS failed to fetch location.");
            }
        });
    }
}
