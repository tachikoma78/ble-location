package com.client.beaconfinder;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    boolean mScanning = false;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
    private Map<String, BluetoothDevice> mScanResults;
    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private static final int SCAN_PERIOD = 5000;

    private static final String TAG = "MainActivity"; // constant

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView text = (TextView) findViewById(R.id.description);

        // ble adapter
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan(view);
            }
        });


    }


    // is  the device is BLE capable ?
    @Override
    protected void onResume() {
        super.onResume();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }
    }

    // ensure we are not already scanning and permissions are granted
    private void startScan(View view){
        if(!hasPermissions() || mScanning){
            Snackbar.make(view, "inform user", Snackbar.LENGTH_LONG)
            .setAction("Bluetooth scan not possible", null).show();
            return;
        }
        // start the scan
       mScanResults = new HashMap<>();
       mScanCallback = new BtleScanCallback(mScanResults);

       //TODO filter unwanted BLE signals from other sources
        filters = new ArrayList<>();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
        mScanning = true;

        mHandler = new Handler();
        mHandler.postDelayed(this::stopScan, SCAN_PERIOD);
    }


    // Check permissions for Bluetooth and location
    private boolean hasPermissions(){
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            requestBluetoothEnabled();
            return false;
        } else if(!hasLocationPermissions()){
            requestLocationPermission();
            return false;
        }
        return true;
    }

    private void requestBluetoothEnabled(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //action shown to user
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again");
    }

    private boolean hasLocationPermissions(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission(){
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION) ;
    }



    private void stopScan(){
        if(mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null){
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }

        mScanCallback = null;
        mScanning = false;
        mHandler = null;
    }


    private void scanComplete(){
        if(mScanResults.isEmpty()){
            return;
        }

        for(BluetoothDevice device : mScanResults.values()){
            String name = device.getName();
            String adress = device.getAddress();
            String toStr = device.toString();
            Log.d(TAG, "name: "+ name + ", adress: "+adress + ", toStr: " +toStr);
        }

        /*
        for(String deviceAdress : mScanResults.keySet()){
            if(deviceAdress.equals("5A:74:FC:6A:A6:8C")){
                Log.d(TAG, "Beacon A ");
            }
            if(deviceAdress.equals("7D:E5:40:FE:17:74")){
                Log.d(TAG, "Gas meter "+ deviceAdress);
            }
            if(deviceAdress.equals("F2:23:75:D7:3C:34")){
                Log.d(TAG, "Room 3 "+ deviceAdress);
            }
            //Log.d(TAG, "Found device "+ deviceAdress);
        }*/
    }

    // callbacks
    private class BtleScanCallback extends ScanCallback {

        private Map<String, BluetoothDevice> mScanResults;

        BtleScanCallback(Map<String, BluetoothDevice> scanResult){
            mScanResults = scanResult;
        }


        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResults(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for(ScanResult result: results){
                addScanResults(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed with code " + errorCode);
        }

        private void addScanResults(ScanResult result){
            BluetoothDevice device = result.getDevice();
            String deviceAdress = device.getAddress();
            mScanResults.put(deviceAdress, device);
        }
    }


}
