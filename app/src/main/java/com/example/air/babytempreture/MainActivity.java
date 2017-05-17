package com.example.air.babytempreture;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableArrayList;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Handler;

import com.example.air.babytempreture.databinding.ActivityMainBinding;

import static android.content.ContentValues.TAG;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BleManager bleManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if(!isLocationEnable(this)) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Open GPS");
            alertDialog.setMessage("You need open GPS to support BLE");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            setLocationService();
                        }
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        initBluetooth();
        bleManager = new BleManager(this);

        bleManager.infos = new DevicesInfoList();
        binding.setInfos(bleManager.infos);
        //mHandler = new Handler();

        Button scanBtn = (Button) findViewById(R.id.scan_btn);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bleManager.scanLeDevice(true);

            }
        });
        mayRequestLocation();

        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(BleManager.ACTION_DATA_AVAILABLE);
        this.registerReceiver(mGattUpdateReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bleManager.mBluetoothAdapter != null && bleManager.mBluetoothAdapter.isEnabled()) {
            bleManager.scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (bleManager.mGatt == null) {
            return;
        }
        bleManager.mGatt.close();
        bleManager.mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
//            case REQUEST_ENABLE_BT:
//                if (resultCode == Activity.RESULT_CANCELED) {
//                    //Bluetooth not enabled.
//                    Log.i(TAG,"REQUEST_ENABLE_BT is RESULT_CANCELED");
//                }
//                break;
            case REQUEST_CODE_LOCATION_SETTINGS:
                if (isLocationEnable(this)) {
                    //定位已打开的处理
                    Log.i(TAG,"Location is enabled");
                } else {
                    //定位依然没有打开的处理
                    Log.i(TAG,"Location is disabled");
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }



    private static final int REQUEST_FINE_LOCATION=0;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private void mayRequestLocation() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                //判断是否需要 向用户解释，为什么要申请该权限
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION));
                    //Toast.makeText(this,"Geo permission not allow", Toast.LENGTH_SHORT).show();

                //ActivityCompat.requestPermissions(this ,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_FINE_LOCATION);
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
                return;
            }else{

            }
        } else {

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "REQUEST_FINE_LOCATION Permission is granted");
                    // The requested permission is granted.
                    //if (mScanning == false) {
                     //   scanLeDevice(true);
                    //}
                } else{
                    // The user disallowed the requested permission.
                    Log.i(TAG, "REQUEST_FINE_LOCATION Permission is denied");
                }
                break;
            case REQUEST_CODE_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "REQUEST_CODE_ACCESS_COARSE_LOCATION Permission is granted");

                } else{
                    // The user disallowed the requested permission.
                    Log.i(TAG, "REQUEST_CODE_ACCESS_COARSE_LOCATION Permission is denied");
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void initBluetooth() {
        BluetoothManager mBluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();  //打开蓝牙
                }
            }
        }
    }

    private static final int REQUEST_CODE_LOCATION_SETTINGS = 2;
    private void setLocationService() {
        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        this.startActivityForResult(locationIntent, REQUEST_CODE_LOCATION_SETTINGS);
    }

    public static final boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BleManager.ACTION_GATT_CONNECTED.equals(action)) {
//                mConnected = true;
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
            } else if (BleManager.ACTION_GATT_DISCONNECTED.equals(action)) {
//                mConnected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
            } else if (BleManager.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BleManager.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                  Log.i("BroadcastReceiver", intent.getStringExtra(BleManager.EXTRA_DATA));
            }
        }
    };

}

