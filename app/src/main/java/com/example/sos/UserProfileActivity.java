package com.example.sos;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    EditText etName, etDob;
    Spinner spinnerBloodGroup;
    RadioGroup rgDiabetic, rgDisabled;
    Button btnSave;

    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        if (prefs.contains("name")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_user_profile);

        // Request all permissions on first entry
        requestAllPermissions();

        etName = findViewById(R.id.etName);
        etDob = findViewById(R.id.etDob);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        rgDiabetic = findViewById(R.id.rgDiabetic);
        rgDisabled = findViewById(R.id.rgDisabled);
        btnSave = findViewById(R.id.btnSave);

        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bloodGroups);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodGroup.setAdapter(adapter);

        etDob.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
                etDob.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1);
            }, year, month, day);
            datePickerDialog.show();
        });

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Some features may be limited without permissions", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String bloodGroup = spinnerBloodGroup.getSelectedItem().toString();
        
        int diabeticId = rgDiabetic.getCheckedRadioButtonId();
        int disabledId = rgDisabled.getCheckedRadioButtonId();

        if (name.isEmpty() || dob.isEmpty() || diabeticId == -1 || disabledId == -1) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String diabetic = ((RadioButton) findViewById(diabeticId)).getText().toString();
        String disabled = ((RadioButton) findViewById(disabledId)).getText().toString();

        SharedPreferences.Editor editor = getSharedPreferences("UserProfile", MODE_PRIVATE).edit();
        editor.putString("name", name);
        editor.putString("dob", dob);
        editor.putString("bloodGroup", bloodGroup);
        editor.putString("diabetic", diabetic);
        editor.putString("disabled", disabled);
        editor.apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
