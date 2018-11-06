package com.acrelec.completedemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.acrelec.completesdk.CompleteEngine;
import com.acrelec.completesdk.DistanceResponse;
import com.acrelec.completesdk.DistanceResponseStatus;
import com.acrelec.completesdk.RegisterRequest;
import com.acrelec.completesdk.Site;
import com.acrelec.completesdk.Units;
import com.acrelec.completesdk.UserInfo;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "YOUR API KEY";
    private static final String TAG = MainActivity.class.getSimpleName();
    private List<Site> siteList;
    private TextView latitude;
    private TextView longitude;
    private TextView accuracy;
    private TextView duration;
    private EditText trackingId;
    private EditText customerName;
    private Spinner siteId;
    private Button startButton;
    private Button cancelButton;
    private final String uniqueIdentifier = UUID.randomUUID().toString();
    private boolean scanning = false;
    private static final float ALPHA = (float)0.5;
    private static final float NO_ALPHA = (float)1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latitude = findViewById(R.id.latitude);
        longitude = findViewById(R.id.longitude);
        accuracy = findViewById(R.id.accuracy);
        duration = findViewById(R.id.duration);
        trackingId = findViewById(R.id.trackingId);
        customerName = findViewById(R.id.customerName);
        siteId = findViewById(R.id.siteId);
        startButton = findViewById(R.id.start_button);
        cancelButton = findViewById(R.id.cancel_button);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        CompleteEngine.getInstance().init(this, API_KEY, Locale.getDefault().toString(), Units.imperial);

        CompleteEngine.onSitesRequestedListener listener = new CompleteEngine.onSitesRequestedListener() {
            @Override
            public void onSitesRequested(List<Site> sites) {
                siteList = sites;
                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                for(int i = 0; i < siteList.size(); i++) {
                    adapter.add(siteList.get(i).getSiteName());
                }
                siteId.setAdapter(adapter);
            }
        };

        CompleteEngine.onRegisterTrackingListener registerTrackingListener = new CompleteEngine.onRegisterTrackingListener() {
            @Override
            public void onTrackingRegistered(boolean response) {
                Log.i(TAG, Boolean.toString(response));
                if(response) {
                    startLocationRequest();
                }
                else {
                    registerError();
                }
            }
        };

        CompleteEngine.onLocationUpdatedListener locationUpdatedListener = new CompleteEngine.onLocationUpdatedListener() {
            @Override
            public void onLocationUpdated(DistanceResponse response) {
                if(response != null) {
                    latitude.setText(getString(R.string.lat, Double.toString(response.getOrigin().getLatitude())));
                    longitude.setText(getString(R.string.lng, Double.toString(response.getOrigin().getLongitude())));
                    accuracy.setText(getString(R.string.accuracy, Double.toString(response.getAccuracy())));
                    if(response.getStatus() != DistanceResponseStatus.Cancelled) {
                        duration.setText(getResources().getString(R.string.estimate, response.getDuration().getText(), Boolean.toString(response.isArrived()), Boolean.toString(response.isParked()), response.getParkingSpot()));
                    }
                    else {
                        duration.setText(R.string.left_spot);
                        stopLocationRequest();
                    }
                }
            }
        };

        CompleteEngine.onTripCancelledListener tripCancelledListener = new CompleteEngine.onTripCancelledListener() {
            @Override
            public void onTripCancelled(boolean response) {
                if(response) {
                    stopLocationRequest();
                }
                else {
                    cancelError();
                }
            }
        };

        CompleteEngine.getInstance().setSitesRequestedListener(listener);
        CompleteEngine.getInstance().setRegisterTrackingListener(registerTrackingListener);
        CompleteEngine.getInstance().setLocationUpdatedListener(locationUpdatedListener);
        CompleteEngine.getInstance().setTripCancelledListener(tripCancelledListener);

        CompleteEngine.getInstance().getSites();

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!scanning) {
                    if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1337);
                    }
                    else {
                        registerTracking();
                    }
                }
                else {
                    stopLocationRequest();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CompleteEngine.getInstance().cancelTrip();
            }
        });

        cancelButton.setAlpha(ALPHA);
        startButton.setAlpha(NO_ALPHA);
    }

    private void registerTracking() {
        RegisterRequest request = new RegisterRequest(uniqueIdentifier, trackingId.getText().toString(), siteList.get(siteId.getSelectedItemPosition()).getSiteId(), 0, new UserInfo(customerName.getText().toString()));
        CompleteEngine.getInstance().registerTrackingIdentifier(request);
    }

    private void startLocationRequest() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            CompleteEngine.getInstance().startTracking();
            scanning = true;
            startButton.setText(R.string.button_stop);
            cancelButton.setEnabled(true);
            cancelButton.setAlpha(NO_ALPHA);
        }
    }

    private void stopLocationRequest() {
        CompleteEngine.getInstance().stopTracking();
        startButton.setText(R.string.button_start);
        cancelButton.setEnabled(false);
        cancelButton.setAlpha(ALPHA);
        latitude.setText(R.string.lat_label);
        longitude.setText(R.string.lng_label);
        accuracy.setText(R.string.acc_label);
        duration.setText(R.string.duration);
        scanning = false;
    }

    private void registerError() {
        Toast.makeText(this, "Error registering tracking id, please try again.", Toast.LENGTH_LONG).show();
    }

    private void cancelError() {
        Toast.makeText(this, "Error cancelling trip, please try again.", Toast.LENGTH_LONG).show();
    }
}
