package com.example.air.babytempreture;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.StringPrepParseException;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Handler;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static android.R.attr.action;
import static android.R.attr.data;
import static android.R.attr.duration;
import static android.content.ContentValues.TAG;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.view.View.Z;

/**
 * Created by air on 17年5月15日.
 */

public class BleManager {

    public BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    public BluetoothGatt mGatt;
    public BluetoothDevice btDevice;
    public DevicesInfoList infos;
    public BluetoothGattCharacteristic mReadWriteCharacteristic;

    private static final UUID CUSTOM_SERVICE_UUID  =UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CUSTOM_CHARATERISTIC_WRITE_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    //private static final UUID CUSTOM_CHARATERISTIC_READ_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID NRF_CUSTOM_SERVICE_UUID  =UUID.fromString("0000fee4-0000-1000-8000-00805f9b34fb");
    private static final UUID NRF_CUSTOM_CHARATERISTIC_READ_WRITE_UUID  =UUID.fromString("2a1e0005-fd51-d882-8ba8-b98c0000cd1e");
    private static final UUID NRF_CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public final static String ACTION_LESCAN_TIMEOUT =
            "com.example.bluetooth.le.ACTION_LESCAN_TIMEOUT";
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private Context mContext;

    public BleManager(Context context) {
        this.mContext = context;
        mHandler = new Handler();

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(TAG, "BLE not supported");
            return;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);  //BluetoothManager只在android4.3以上有
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return;
        }

        Log.i(TAG, "new BleManager Ok!");
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (Build.VERSION.SDK_INT >= 21) {
            settings = new ScanSettings.Builder()
                    //.setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                    .build();
            filters = new ArrayList<ScanFilter>();
            //ScanFilter filter = new ScanFilter.Builder().setServiceUuid( new ParcelUuid(NRF_CUSTOM_SERVICE_UUID)).build();
            //ScanFilter filter = new ScanFilter.Builder().setDeviceName("AVNET Smart Thermometer").build();

            //ScanFilter filter = new ScanFilter.Builder().setDeviceName(Pattern.compile("rbc_mesh #\\d*").pattern()).build();
            ScanFilter filter = new ScanFilter.Builder().setDeviceName("rbc_mesh #57055").build();
            //filters.add(filter);
        }


    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        Log.i(TAG, "mHandler.postDelayed");
                        mLEScanner.stopScan(mScanCallback);
                        broadcastUpdate(ACTION_LESCAN_TIMEOUT);
                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                Log.i(TAG, "mLEScanner.startScan");
                //mLEScanner.startScan(filters, settings, mScanCallback);
                mLEScanner.startScan(filters, settings, mScanCallback);
                //mLEScanner.startScan(mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            btDevice = result.getDevice();
            if(btDevice.getName()!=null && btDevice.getName().matches("rbc_mesh #\\d*")) {
                DeviceInfo device = new DeviceInfo(btDevice);
                Log.i(TAG, infos.list+"");
                if(!infos.list.contains(device))
                    infos.add(device);
                connectToDevice(btDevice);
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.i("onBatchScanResults", results.toString());
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.i("onLeScan", device.toString());
//                            connectToDevice(device);
//                        }
//                    });
                    Log.i("onLeScan", device.toString());
                    connectToDevice(device);
                }
            };


    public void connectToDevice(BluetoothDevice device) {
        Log.i("connectToDevice", "true");
        if (mGatt == null) {
            mGatt = device.connectGatt(mContext, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    public void disconnectToDevice() {
        if(mGatt != null){
            mGatt.disconnect();

        }
        infos.deleteAll();
    }

    public void sendCommand(String cmd) {
        if(mGatt != null) {
            mReadWriteCharacteristic.setValue(cmd);
            mGatt.writeCharacteristic(mReadWriteCharacteristic);
        }
    }

    public void sendCommand(byte[] cmd) {
        if(mGatt != null) {
            mReadWriteCharacteristic.setValue(cmd);
            mGatt.writeCharacteristic(mReadWriteCharacteristic);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    broadcastUpdate(ACTION_GATT_CONNECTED);

                    gatt.discoverServices();

                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            mReadWriteCharacteristic = gatt.getService(NRF_CUSTOM_SERVICE_UUID).getCharacteristic(NRF_CUSTOM_CHARATERISTIC_READ_WRITE_UUID);
            //gatt.readCharacteristic(services.get(2).getCharacteristics().get(0));
            gatt.setCharacteristicNotification(mReadWriteCharacteristic, true);

            BluetoothGattDescriptor descriptor = mReadWriteCharacteristic.getDescriptor(NRF_CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            //gatt.readCharacteristic(mReadWriteCharacteristic);
        }

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt,
                                       BluetoothGattDescriptor descriptor,
                                       int status) {
            if(status==BluetoothGatt.GATT_SUCCESS) {
                Log.i("onDescriptorWrite", "Success!");

            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }

        @Override
        public void onCharacteristicChanged (BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic){
            Log.i("onCharacteristicChanged", characteristic.getStringValue(0));

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        Log.i("broadcastUpdate", action);
        final Intent intent = new Intent(action);
        String data = "";
        intent.putExtra(EXTRA_DATA,data);
        mContext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        //String data = characteristic.getStringValue(0);
        final byte[] data = characteristic.getValue();
        Log.i("broadcastUpdate", Arrays.toString(data));
        intent.putExtra(EXTRA_DATA, data);
//        if(characteristic!=null) {
//            final byte[] data = characteristic.getValue();
//            final byte[] temp = new byte[3];
//            temp[0]=data[0];
//            temp[1]=data[1];
//            temp[2]=data[2];
//            //intent.putExtra(EXTRA_DATA, new String(temp));
//            intent.putExtra(EXTRA_DATA, new String(data));
//
//        }
        mContext.sendBroadcast(intent);
    }
}