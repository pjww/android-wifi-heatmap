package jaxon.bdpp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import jaxon.bdpp.logic.GeographicalCalculator;
import jaxon.bdpp.logic.MainData;
import jaxon.bdpp.logic.WifiNetwork;
import jaxon.bdpp.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener, AdapterView.OnItemSelectedListener {

    // LogCat tag
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private boolean measurementStarted = false;

    private LocationRequest mLocationRequest;

    private WifiManager wifiManager;

    // UI elements
    private TextView lblLocation;
    private Spinner wifiNetworksSpinner;
    private Button measurementButton;
    private Button showMapButton;
    private Button saveButton;
    private Button quitButton;
    private ViewGroup gridViewFrameLayout;

    private GridView gridView;

    private ArrayAdapter<WifiNetwork> wifiNetworksDataAdapter;

    // logic data:
    private MainData mainData = new MainData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblLocation = (TextView) findViewById(R.id.lblLocation);
        wifiNetworksSpinner = (Spinner) findViewById(R.id.wifiNetworksSpinner);
        measurementButton = (Button) findViewById(R.id.measurementButton);
        showMapButton = (Button) findViewById(R.id.showMapButton);
        saveButton = (Button) findViewById(R.id.saveButton);
        quitButton = (Button) findViewById(R.id.quitButton);
        gridViewFrameLayout = (ViewGroup) findViewById(R.id.gridViewFrameLayout);

        // Spinner click listener
        wifiNetworksSpinner.setOnItemSelectedListener(this);

        // Creating adapter for spinner
        wifiNetworksDataAdapter = new ArrayAdapter<WifiNetwork>(
                        this, android.R.layout.simple_spinner_item,
                        new ArrayList<WifiNetwork>());

        // Drop down layout style - list view with radio button
        wifiNetworksDataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        wifiNetworksSpinner.setAdapter(wifiNetworksDataAdapter);

        // First we need to check availability of play services
        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient();

            createLocationRequest();
        }

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        measurementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doMeasurement();
            }
        });
        showMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Intent intent = new Intent(MainActivity.this, MapActivity.class);
                // startActivity(intent);
            }
        });

        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quitApp();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMap();
            }
        });


        gridView = new GridView(this, mainData);

        //tv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        gridViewFrameLayout.addView(gridView);
    }

    private void saveMap() {
        Logger.DumpSignalGrids(mainData);
    }

    private void quitApp() {
        Logger.DumpSignalGrids(mainData);
        ActivityCompat.finishAffinity(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        WifiNetwork wifiNetwork = (WifiNetwork) parent.getItemAtPosition(position);
        gridView.update(wifiNetwork, null);
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mRequestingLocationUpdates = true;
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void doMeasurement() {
        if (!measurementStarted) {
            measurementStarted = true;
            mainData.startMeasurement(mLastLocation);
            measurementButton.setText("Add Measurement");

        } else {
            wifiManager.startScan();
            List<ScanResult> scanResults = wifiManager.getScanResults();

            List<WifiNetwork> discoveredNetworks = new ArrayList<>();
            mainData.addMeasurement(mLastLocation, scanResults, discoveredNetworks);
            updateWifiNetworksSpinner(discoveredNetworks);

            // The rest is for debug only:

            TextView tv = (TextView) findViewById(R.id.textView2);
            tv.setText("");
            String output = "";

            for (final ScanResult scanResult : scanResults) {
                //signalData.put(scanResult.BSSID, new SignalInformation(scanResult.level));

                System.out.println("scanResult.BSSID = " + scanResult.BSSID);
                System.out.println("scanResult.level = " + scanResult.level + "\n");

                output += "scanResult.BSSID = " + scanResult.BSSID + "\n";
                output += "scanResult.SSID = " + scanResult.SSID + "\n";
                output += "scanResult.level = " + scanResult.level + "\n\n";
            }

            tv.setText(output);
            gridView.update(null, null);
        }
    }

    /**
     * Method to display the location on UI (DEBUG ONLY)
     * */
    private void displayLocation() {

        try {
            mLastLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);
        } catch (SecurityException e) {
            e.printStackTrace();
        }


        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            String output = latitude + ", " + longitude;

            if (mainData.getGridInfo() != null) {
                Location centerLocation = mainData.getGridInfo().getCenterLocation();

                output += "\nCenter: " + centerLocation.getLatitude() + ", " + centerLocation.getLongitude();

                output += "\nNorthwardsOffset (meters) = "
                        + GeographicalCalculator.InMeters.getNorthwardsDisplacement(centerLocation, mLastLocation);

                output += "\nEastwardsOffset (meters) = "
                        + GeographicalCalculator.InMeters.getEastwardsDisplacement(centerLocation, mLastLocation);
            }

            lblLocation.setText(output);

        } else {

            lblLocation
                    .setText("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    protected void createLocationRequest() {
        // Location updates intervals in sec
        final int UPDATE_INTERVAL = 2000; // 2 sec
        final int FASTEST_INTERVAL = 1000; // 1 sec
        final int DISPLACEMENT = 2; // 2 meters

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected void startLocationUpdates() {

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        displayLocation();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;
        displayLocation();
        gridView.update(null, mLastLocation);
    }

    private void updateWifiNetworksSpinner(List<WifiNetwork> discoveredNetworks) {
        for (WifiNetwork discoveredNetwork : discoveredNetworks) {
            wifiNetworksDataAdapter.add(discoveredNetwork);
        }

        wifiNetworksDataAdapter.notifyDataSetChanged();
    }
}