package com.example.air.babytempreture;

import android.databinding.ObservableArrayList;
import android.view.View;

/**
 * Created by air on 17年5月16日.
 */

public class DevicesInfoList {
    public ObservableArrayList<DeviceInfo> list = new ObservableArrayList<>();
    private int mTotalCount;

    public DevicesInfoList() {
    }

    public void add(DeviceInfo info) {
        list.add(info);
    }
}
