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
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.view.ViewGroup.LayoutParams;
import android.widget.ToggleButton;

import com.example.air.babytempreture.databinding.ActivityMainBinding;

import static android.content.ContentValues.TAG;
import static android.graphics.Color.YELLOW;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BleManager bleManager;
    private boolean mReceiverTag = false;

    private Button connectBtn;
    private Button ledBtn;
    private TextView powerLevelTxt;
    private LinearLayout mainContainer;
    private ArrayList<Button> controlBtns;

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

        mainContainer = (LinearLayout) findViewById(R.id.main_container);
        connectBtn = (Button) findViewById(R.id.scan_btn);
        ledBtn = (Button) findViewById(R.id.led_on);
        powerLevelTxt = (TextView) findViewById(R.id.power_level);
        controlBtns = new ArrayList<>();

        for (int j=0; j<3; j++) {
            LinearLayout linLayout = new LinearLayout(this);
            linLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams linLayoutParam = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            linLayoutParam.gravity=Gravity.CENTER;
            linLayout.setPadding(0,30,0,0);

            final TextView deviceIdText = new TextView(this);
            deviceIdText.setText("No."+j+" ");
            deviceIdText.setTextColor(getResources().getColor(R.color.soft_opaque));
            linLayout.addView(deviceIdText);
            for (int i = 0; i < 3; i++) {
                LayoutParams lpView = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

                final Button btn = new Button(this);
                //btn.setEnabled(false);
                btn.setLayoutParams(lpView);
                btn.setText("OFF");
                btn.setTextColor(getResources().getColor(R.color.white));
                btn.setGravity(Gravity.CENTER);
                btn.setId(j*3+i);
                switch (i) {
                    case 0:
                        btn.setBackgroundColor(getResources().getColor(R.color.red));
                        break;
                    case 1:
                        btn.setBackgroundColor(getResources().getColor(R.color.mediumspringgreen));
                        break;
                    case 2:
                        btn.setBackgroundColor(getResources().getColor(R.color.dodgerblue));
                        break;
                }

                btn.setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View buttonView) {

                        //bleManager.sendCommand("device: "+ btn.getId()+ ",color:" +btn.getBackground().toString());
                        byte[] data=new byte[6];
                        data[0]=0x00;
                        data[1]=(byte)btn.getId();
                        data[2]=0x00;
                        data[3]=0x02;
                        if (btn.getText().equals("ON")) {
                            // The toggle is enabled
                            data[4]=0x01;
                            data[5]=0x00;
                        } else if(btn.getText().equals("OFF")) {
                            // The toggle is disabled
                            data[4]=0x00;
                            data[5]=0x00;
                        }
                        Log.i("ledBtn onClick", "device: "+ btn.getId()+ ",data:" + Arrays.toString(data));
                        bleManager.sendCommand(data);
                    }
                });
                controlBtns.add(btn);
                linLayout.addView(btn,lpView);
            }
            mainContainer.addView(linLayout,linLayoutParam);
        }

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

        ledBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("setOnClickListener", "lenOn");
                bleManager.sendCommand("LED_ON");
            }
        });

        LayoutInflater inflater = LayoutInflater.from(this);

        mayRequestLocation();

        broadcastRegister();

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
        mReceiverTag = true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (bleManager.mBluetoothAdapter != null && bleManager.mBluetoothAdapter.isEnabled()) {
            bleManager.scanLeDevice(false);
        }
        this.unregisterReceiver(mGattUpdateReceiver);
        mReceiverTag = false;
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

                    if(controlBtns.get(deviceIdLow).getText().equals("ON")){
                        Log.i("btn click", "OFF");
                        controlBtns.get(deviceIdLow).setText("OFF");
                    }else if(controlBtns.get(deviceIdLow).getText().equals("OFF")){
                        Log.i("btn click", "ON");
                        controlBtns.get(deviceIdLow).setText("ON");
                    }
                }
                Log.i("BroadcastReceiver", Arrays.toString(intent.getByteArrayExtra(BleManager.EXTRA_DATA)));


            } else if(BleManager.ACTION_LESCAN_TIMEOUT.equals(action)) {
                if(!connectBtn.isEnabled()){
                    connectBtn.setText("CONNECT");
                    connectBtn.setEnabled(true);
                }

                Log.i("BroadcastReceiver", action);
            }
        }
    };

}

