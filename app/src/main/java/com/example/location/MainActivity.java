package com.example.location;

import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    MapView mapView;
    FloatingActionButton floatingActionButton;
    FloatingActionButton satellite;
    FloatingActionButton street;
    FloatingActionButton hybrid;
    TextView textViewSpeed;


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;


    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean o) {
            if(o){
                Toast.makeText(MainActivity.this, "permission granted !", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    });
    private final OnIndicatorBearingChangedListener onIndicatorBearingChangedListener = new OnIndicatorBearingChangedListener() {
        @Override
        public void onIndicatorBearingChanged(double v) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().bearing(v).build());
        }
    };
    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = new OnIndicatorPositionChangedListener() {
        @Override
        public void onIndicatorPositionChanged(@NonNull Point point) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().center(point).zoom(14.0).build());
            getGestures(mapView).setFocalPoint(mapView.getMapboxMap().pixelForCoordinate(point));
        }
    };
    private final OnMoveListener onMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {
            getLocationComponent(mapView).removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
            getLocationComponent(mapView).removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
            getGestures(mapView).removeOnMoveListener(onMoveListener);
            floatingActionButton.show();
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector moveGestureDetector) {
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {

        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        floatingActionButton = findViewById(R.id.focusLocation);
        satellite = findViewById(R.id.satellite);
        street = findViewById(R.id.street);
        hybrid = findViewById(R.id.hybrid);
        textViewSpeed = findViewById(R.id.textViewSpeed);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }


        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder().zoom(14.0).build());
                LocationComponentPlugin locationComponentPlugin = getLocationComponent(mapView);
                locationComponentPlugin.setEnabled(true);
                locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
                locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
                getGestures(mapView).addOnMoveListener(onMoveListener);

                floatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
                        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
                        getGestures(mapView).addOnMoveListener(onMoveListener);

                    }
                });
                satellite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mapView.getMapboxMap().loadStyle(Style.SATELLITE);
                    }
                });
                street.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mapView.getMapboxMap().loadStyle(Style.MAPBOX_STREETS);
                    }
                });
                hybrid.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mapView.getMapboxMap().loadStyle(Style.SATELLITE_STREETS);
                    }
                });


            }
        });


    }


    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000) // Update every second
                .setFastestInterval(500);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            float speed = location.getSpeed(); // Speed in meters/second
                            textViewSpeed.setText(String.format("%.2f km/h", speed * 3.6)); // Convert to km/h
                        }
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }


}