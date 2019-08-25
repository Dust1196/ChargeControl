package com.example.chargecontrol;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class Fragment_Battery_Image extends Fragment
{
    Battery_View mBattery_View;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_battery_image, container, false);

        mBattery_View = view.findViewById(R.id.View_Battery_Image);

        return view;
    }

    void onBatteryLevelChange(int Battery_Level)
    {
        mBattery_View.draw_Battery_Level(Battery_Level);
    }

    void onOrientationChanged(float angle)
    {
        mBattery_View.change_orientation(angle);
    }
}
