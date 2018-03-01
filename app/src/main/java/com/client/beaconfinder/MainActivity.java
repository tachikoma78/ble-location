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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

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
    private Map<String, String> mScanResults;
    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private List<String> mDeviceList = new ArrayList<>();
    private ArrayAdapter<String> mListAdapter;
    private static final int SCAN_PERIOD = 5000;
    private static final String TAG = "MainActivity";
    private ListView mDeviceLV;
    private ProgressBar mProgress;
    private Button scanBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mProgress = findViewById(R.id.progress);

        // list
        mDeviceLV = findViewById(R.id.deviceList);

        mListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mDeviceList);
        mDeviceLV.setAdapter(mListAdapter);

        // ble adapter
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        scanBtn = findViewById(R.id.scan);
        scanBtn.setOnClickListener(view -> startScan());
    }

    // is  the device is BLE capable ?
    @Override
    protected void onResume() {
        super.onResume();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish(); // close activity
        }
    }

    // ensure we are not already scanning and permissions are granted
    private void startScan(){

        // hide list show progress
        mProgress.setVisibility(View.VISIBLE);
        scanBtn.setVisibility(View.INVISIBLE);
        mDeviceLV.setVisibility(View.GONE);

        if(!hasPermissions() || mScanning){
            return;
        }
        // start the scan
        mScanResults = new HashMap<>();
        // mScanResults = new ArrayList<>();
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

    // Print
    private void scanComplete(){

        // hide list show progress
        mProgress.setVisibility(View.GONE);
        mDeviceLV.setVisibility(View.VISIBLE);
        scanBtn.setVisibility(View.VISIBLE);
        if(mScanResults.isEmpty()){
            // TODO inform user that no results were found
            return;
        }

        for(String deviceInfo : mScanResults.values()){
            Log.d(TAG, mScanResults.toString() + "result: " + deviceInfo);
            mListAdapter.add(deviceInfo);
            mListAdapter.notifyDataSetChanged();
        }
    }

    private void stopScan(){
        if(mScanning
                && mBluetoothAdapter != null
                && mBluetoothAdapter.isEnabled()
                && mBluetoothLeScanner != null)
        {
            mListAdapter.clear(); // first empty adapter
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }

        mScanCallback = null;
        mScanning = false;
        mHandler = null;
    }

    // callbacks
    private class BtleScanCallback extends ScanCallback {

        BtleScanCallback(Map<String, String> scanResult){
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
        String rssi = String.valueOf(result.getRssi());

        String name = result.getDevice().getName();
        Log.d(TAG, " >>>>"+result.toString());
        if(result.getDevice().getName() == null){
          name = "";
        }
        StringBuilder sb= new StringBuilder();
        sb.append(name);
        sb.append("   ");
        sb.append(deviceAdress);
        sb.append("   ");
        sb.append(rssi);

            Log.d(TAG, " >>>>"+sb.toString());
        mScanResults.put(deviceAdress, sb.toString());

        }
    }


}
