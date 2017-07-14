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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.view.ViewGroup.LayoutParams;
import android.widget.ToggleButton;

import com.example.air.babytempreture.databinding.ActivityMainBinding;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;

import static android.content.ContentValues.TAG;
import static android.graphics.Color.YELLOW;
import static android.graphics.Color.green;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BleManager bleManager;
    private LedInfo currentCntrlLed;
    private LedInfo led1 = new LedInfo((char)1);
    private LedInfo led2 = new LedInfo((char)2);
    private LedInfo led3 = new LedInfo((char)3);
    private LedInfo led4 = new LedInfo((char)4);
    private Button connectBtn, led1Btn, led2Btn, led3Btn, led4Btn;
    private ColorPickerView colorPicker;

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

        connectBtn = (Button) findViewById(R.id.scan_btn);
        led1Btn = (Button) findViewById(R.id.btnLed1);
        led2Btn = (Button) findViewById(R.id.btnLed2);
        led3Btn = (Button) findViewById(R.id.btnLed3);
        led4Btn = (Button) findViewById(R.id.btnLed4);
        currentCntrlLed = led1;
        colorPicker = (ColorPickerView) findViewById(R.id.color_picker_view);

        colorPicker.addOnColorSelectedListener(new OnColorSelectedListener() {
            @Override
            public void onColorSelected(int i) {
                //if(!currentCntrlLed.isOn) return;

                char b = (char)(i&0xFF);
                char g = (char)((i>>8)&0xFF);
                char r = (char)((i>>16)&0xFF);
                Log.i("onColorSelected", "currentLed: "+ (int)(currentCntrlLed.Id)+ " int: "+ Integer.toHexString(i)+" r:"+(int)(r)+" g:"+(int)(g)+" b:"+(int)(b));
                byte[] data = new byte[7];
                data[0] = 0x00;
                data[1] = (byte)currentCntrlLed.Id;
                data[2] = 0x00;
                data[3] = 0x03;
                data[4] = (byte)r;
                data[5] = (byte)g;
                data[6] = (byte)b;
                bleManager.sendCommand(data);
                currentCntrlLed.updateColor(r, g, b);
            }
        });
        connectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("button text", connectBtn.getText().toString());
                if(connectBtn.getText().toString().equals("CONNECT")) {
                    Log.i("equals(\"CONNECT\")", "true");
                    bleManager.scanLeDevice(true);
                    connectBtn.setText("SCANNING...");
                    connectBtn.setEnabled(false);

                }else bleManager.disconnectToDevice();

            }
        });

        led1Btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("led1", "clicked");
                ledClick(led1);
            }
        });

        led2Btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("led2", "clicked");
                ledClick(led2);
            }
        });

        led3Btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("led3", "clicked");
                ledClick(led3);
            }
        });

        led4Btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("led4", "clicked");
                ledClick(led4);
            }
        });

        mayRequestLocation();

        broadcastRegister();

    }

    public void onRadioButtonClicked(View view) {

        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.led1_pick_color:
                if (checked){
                    currentCntrlLed = led1;
                }
                break;
            case R.id.led2_pick_color:
                if (checked){
                    currentCntrlLed = led2;
                }
                break;
            case R.id.led3_pick_color:
                if(checked){
                    currentCntrlLed = led3;
                }
                break;
            case R.id.led4_pick_color:
                if(checked){
                    currentCntrlLed = led4;
                }
                break;
        }
    }

    private void ledClick(LedInfo led) {
        if(led.isOn){
            byte[] data = new byte[7];
            data[0] = 0x00;
            data[1] = (byte)led.Id;
            data[2] = 0x00;
            data[3] = 0x03;
            data[4] = 0x00;
            data[5] = 0x00;
            data[6] = 0x00;
            bleManager.sendCommand(data);
            switch (led.Id){
                case 0x01:
                    led1Btn.setBackgroundColor(0x80000000 | led.Rgb);
                    break;
                case 0x02:
                    led2Btn.setBackgroundColor(0x80000000 | led.Rgb);
                    break;
                case 0x03:
                    led3Btn.setBackgroundColor(0x80000000 | led.Rgb);
                    break;
                case 0x04:
                    led4Btn.setBackgroundColor(0x80000000 | led.Rgb);
                    break;
            }
            led.isOn = false;
        }else {
            byte[] data = new byte[7];
            data[0] = 0x00;
            data[1] = (byte)led.Id;
            data[2] = 0x00;
            data[3] = 0x03;
            data[4] = (byte)led.ColorR;
            data[5] = (byte)led.ColorG;
            data[6] = (byte)led.ColorB;
            bleManager.sendCommand(data);
            switch (led.Id){
                case 0x01:
                    led1Btn.setBackgroundColor(led.Rgb);
                    break;
                case 0x02:
                    led2Btn.setBackgroundColor(led.Rgb);
                    break;
                case 0x03:
                    led3Btn.setBackgroundColor(led.Rgb);
                    break;
                case 0x04:
                    led4Btn.setBackgroundColor(led.Rgb);
                    break;
            }
            led.isOn = true;
        }
    }
    protected void broadcastRegister() {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(BleManager.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleManager.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleManager.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BleManager.ACTION_LESCAN_TIMEOUT);

        this.registerReceiver(mGattUpdateReceiver, intentFilter);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (bleManager.mBluetoothAdapter != null && bleManager.mBluetoothAdapter.isEnabled()) {
            bleManager.scanLeDevice(false);
        }
        this.unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(TAG, "App stopped");
//        if (bleManager.mGatt == null) {
//            return;
//        }
//        bleManager.mGatt.close();
//        bleManager.mGatt = null;

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w(TAG, "app resumed");
        broadcastRegister();

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "App destroy");
        if (bleManager.mGatt == null) {
            return;
        }
        bleManager.mGatt.close();
        bleManager.mGatt = null;
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
            Log.i("BroadCast_onReceive", action);
            if (BleManager.ACTION_GATT_CONNECTED.equals(action)) {
//                mConnected = true;
//                updateConnectionState(R.string.connected);
//                invalidateOptionsMenu();
                Log.i("Receive Broadcast", "ACTION_GATT_CONNECTED");

                if(!connectBtn.getText().toString().equals("DISCONNECT")){
                    connectBtn.setText("DISCONNECT");
                    connectBtn.setBackgroundColor(getResources().getColor(R.color.grassgreen));
                    connectBtn.setEnabled(true);
                }
            } else if (BleManager.ACTION_GATT_DISCONNECTED.equals(action)) {
//                mConnected = false;
//                updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
                Log.i("Receive Broadcast", "ACTION_GATT_DISCONNECTED");
                if(!connectBtn.getText().toString().equals("CONNECT")){
                    connectBtn.setText("CONNECT");
                    connectBtn.setBackgroundResource(android.R.drawable.btn_default);
                    connectBtn.setEnabled(true);
                    bleManager.infos.deleteAll();
                }
                bleManager.mGatt = null;
            } else if (BleManager.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                Log.i("Receive Broadcast", "ACTION_GATT_SERVICES_DISCOVERED");
            } else if (BleManager.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] echo = intent.getByteArrayExtra(BleManager.EXTRA_DATA);
                byte cmdRep = echo[0];
                byte opcode = echo[1];
                byte result = echo[2];
                byte deviceIdLow = echo[3];
                //byte deviceIdHigh = echo[4];
                Log.i("BroadcastReceiveraaa", Byte.toString(cmdRep)+Byte.toString(opcode)+Byte.toString(result));
                if(cmdRep == 17 && opcode == 0x00 && result == -128) {

//                    if(controlBtns.get(deviceIdLow).getText().equals("ON")){
//                        Log.i("btn click", "OFF");
//                        controlBtns.get(deviceIdLow).setText("OFF");
//                    }else if(controlBtns.get(deviceIdLow).getText().equals("OFF")){
//                        Log.i("btn click", "ON");
//                        controlBtns.get(deviceIdLow).setText("ON");
//                    }
                }
                Log.i("BroadcastReceiver", Arrays.toString(intent.getByteArrayExtra(BleManager.EXTRA_DATA)));


            } else if(BleManager.ACTION_LESCAN_TIMEOUT.equals(action)) {
                if(!connectBtn.isEnabled()){
                    connectBtn.setText("CONNECT");
                    connectBtn.setBackgroundResource(android.R.drawable.btn_default);
                    connectBtn.setEnabled(true);
                }

                Log.i("BroadcastReceiver", action);
            }
        }
    };

}

