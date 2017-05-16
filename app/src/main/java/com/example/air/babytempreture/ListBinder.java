package com.example.air.babytempreture;

import android.databinding.BindingAdapter;
import android.databinding.ObservableArrayList;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * Created by air on 17年5月16日.
 */

public class ListBinder {

    @BindingAdapter("bind:imageRes")
    public static void bindImage(ImageView view, int r) {
        view.setImageResource(r);
    }

    @BindingAdapter("bind:items")
    public static void bindList(ListView view, ObservableArrayList<DeviceInfo> list) {
        ListAdapter adapter = new ListAdapter(list);
        view.setAdapter(adapter);
    }
}
