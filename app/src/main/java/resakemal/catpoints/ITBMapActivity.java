package resakemal.catpoints;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import resakemal.catpoints.Controller.DatabaseManager;
import resakemal.catpoints.Controller.FactManager;

import static java.lang.Thread.sleep;

public class ITBMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private Context context = this;

    private GoogleMap mMap;

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;
    static final int DEFAULT_ZOOM = 17;
    static final int MAX_ZOOM = 19;
    static final LatLng mDefaultLocation = new LatLng(-6.890334, 107.610092);

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Location mLastKnownLocation;

    boolean mLocationPermissionGranted;

    private AlertDialog alert_gps;

    DatabaseManager db;

    FactManager fcts;
    ArrayList<String> factList;

    String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itbmap);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        db = new DatabaseManager(mMap);
        db.setupListener();
        fcts = new FactManager();

        // Add bounds for ITB area
        final LatLngBounds ITB_BOUNDS = new LatLngBounds(
                new LatLng(-6.892738, 107.607946), new LatLng(-6.887338, 107.612463));

        // Set zoom level
        mMap.setMinZoomPreference(DEFAULT_ZOOM);
        mMap.setMaxZoomPreference(MAX_ZOOM);

        //mMap.setMyLocationEnabled(true);

//        // Add markers
//        mMap.addMarker(new MarkerOptions().position(itb).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_kucing)));

        // Set camera to ITB
        mMap.setLatLngBoundsForCameraTarget(ITB_BOUNDS);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mDefaultLocation));
        //mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(ITB_BOUNDS, 0));

        Button button1 = (Button) findViewById(R.id.cam_button);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { createCatPoint(); }
        });

        getLocationPermission();
        setupUserLocation();
        updateLocationUI();
        //getDeviceLocation();

        final ImageView cat_view = (ImageView) findViewById(R.id.cam_result);
        cat_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cat_view.setVisibility(view.INVISIBLE);
                Log.d("marker_photo","reset");
            }
        });

        final TextView facts = (TextView) findViewById(R.id.fact_bar);
        InputStream is = context.getResources().openRawResource(R.raw.cat_facts);
        factList = fcts.readFactFile(is, facts);

        factTaskParams param = new factTaskParams(facts,factList);
        showFactTask fact_task = new showFactTask();
        fact_task.execute(param);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (alert_gps != null)
        alert_gps.dismiss();
    }

    private void setupUserLocation() {
        int off = 0;
        try {
            off = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if(off==0){
            showGPSDisabledAlertToUser();
        }
    }

    private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
//                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                    new LatLng(mLastKnownLocation.getLatitude(),
//                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            Log.d("locate","success");
                        } else {
                            Log.d("locInfo", "Current location is null. Using defaults.");
                            Log.e("locInfo", "Exception: %s", task.getException());
                            mLastKnownLocation = new Location("");
                            mLastKnownLocation.setLatitude(mDefaultLocation.latitude);
                            mLastKnownLocation.setLongitude(mDefaultLocation.longitude);
//                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
//                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            Log.d("locate","fail");
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void showGPSDisabledAlertToUser(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage("GPS is disabled in your device. Would you like to enable it?");
        builder.setPositiveButton("Go to settings to enable GPS", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert_gps = builder.create();
        alert_gps.show();
    }

    private void createCatPoint() {
        dispatchTakePictureIntent();
        getDeviceLocation();
//        Log.d("point_lat",Double.toString(mLastKnownLocation.getLatitude()));
//        Log.d("point_lng",Double.toString(mLastKnownLocation.getLongitude()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            db.createNewEntry(mLastKnownLocation,mCurrentPhotoPath);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d("marker_photo","entry");
        ImageView cat_view = (ImageView) findViewById(R.id.cam_result);
        db.getCatPhoto(context, marker, cat_view);
        cat_view.setVisibility(View.VISIBLE);
        return true;
    }

    private static class factTaskParams {
        TextView fact_bar;
        ArrayList<String> fact_list;

        factTaskParams(TextView _fact_bar, ArrayList<String> _fact_list) {
            this.fact_bar = _fact_bar;
            this.fact_list = _fact_list;
        }
    }

    private class showFactTask extends AsyncTask<factTaskParams, Void, Void> {
        @Override
        protected Void doInBackground(factTaskParams... factTaskParams) {
            final TextView fact_bar = factTaskParams[0].fact_bar;
            final ArrayList<String> fact_list = factTaskParams[0].fact_list;

            Thread factThread = new Thread() {
                public void run() {
                    try {
                        while(true) {
                            Random r = new Random();
                            int rnd = r.nextInt(fact_list.size() - 1);
                            final int finalRnd = rnd;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    fact_bar.setText(fact_list.get(finalRnd));
                                }
                            });
                            sleep(10000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            factThread.start();
            return null;
        }
    }

    public void gotoCredits (View v) {
        startActivity(new Intent(this, AboutActivity.class));
    }
}