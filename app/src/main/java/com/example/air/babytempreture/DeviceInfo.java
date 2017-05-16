package com.example.air.babytempreture;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import static android.content.ContentValues.TAG;

/**
 * Created by air on 17年5月16日.
 */

public class DeviceInfo {
    public String Mac;
    public String Name;
    private BluetoothDevice Device;

    public DeviceInfo(BluetoothDevice device) {
        Device = device;
        Mac = device.getAddress();
        Name = device.getName();
    }

    public void onSelectDevice(View v){
        v.setBackgroundColor(Color.RED);
        Log.i(TAG, "onSelectDevice"+Device.getName());
    }

}
