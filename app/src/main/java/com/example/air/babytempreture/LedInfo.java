package com.example.air.babytempreture;

/**
 * Created by air on 17-7-13.
 */
import android.util.Log;

import static com.example.air.babytempreture.R.color.red;

public class LedInfo {
    public char Id;
    public boolean isOn;
    public char ColorR;
    public char ColorG;
    public char ColorB;
    public int  Rgb;

    public LedInfo(char id){
        Id = id;
        isOn = false;
        ColorR = 0;
        ColorG = 0;
        ColorB = 0;
        Rgb = 0;
    }

    public void updateColor(char r, char g, char b) {
        if(!isOn) return;

        ColorR = r;
        ColorG = g;
        ColorB = b;
        Rgb = (((r<<8)+g)<<8)+b;
        Log.i("LedInfo", "R:"+ (int)r+" G:"+(int)g+" B:"+(int)b+" Color:"+Rgb);

    }


}
