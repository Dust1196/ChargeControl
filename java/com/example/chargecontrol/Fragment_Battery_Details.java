package com.example.chargecontrol;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Fragment_Battery_Details extends Fragment
{
    public static final String EXTRA_BATTERY_LEVEL = "extra_battery_level";
    public static final String EXTRA_BATTERY_TEMPERATURE = "extra_battery_temperature";
    public static final String EXTRA_BATTERY_TECHNOLOGY = "extra_battery_technology";
    public static final String EXTRA_BATTERY_HEALTH = "extra_battery_health";
    public static final String EXTRA_BATTERY_VOLTAGE = "extra_battery_voltage";
    public static final String EXTRA_BATTERY_STATUS = "extra_battery_status";


    TextView Text_Battery_Level;
    TextView Text_Battery_Temperature;
    TextView Text_Battery_Technology;
    TextView Text_Battery_Health;
    TextView Text_Battery_Voltage;
    TextView Text_Battery_Current;
    TextView Text_Battery_Status;
    int Battery_Level;
    int Battery_Temperature;
    String Battery_Technology = new String();
    int Battery_Health;
    int Battery_Voltage;
    long Battery_Current;
    int Battery_Status;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_battery_details, container, false);
        Text_Battery_Level = view.findViewById(R.id.Details_Battery_Level);
        Text_Battery_Temperature = view.findViewById(R.id.Details_Battery_Temperature);
        Text_Battery_Technology = view.findViewById(R.id.Details_Battery_Technology);
        Text_Battery_Health = view.findViewById(R.id.Details_Battery_Health);
        Text_Battery_Voltage = view.findViewById(R.id.Details_Battery_Voltage);
        Text_Battery_Current = view.findViewById(R.id.Details_Battery_Current);
        Text_Battery_Status = view.findViewById(R.id.Details_Battery_Status);

        Battery_Level = getArguments().getInt(EXTRA_BATTERY_LEVEL, -1);
        Battery_Temperature = getArguments().getInt(EXTRA_BATTERY_TEMPERATURE, -1);
        Battery_Health = getArguments().getInt(EXTRA_BATTERY_HEALTH, -1);
        Battery_Voltage = getArguments().getInt(EXTRA_BATTERY_VOLTAGE, -1);
        Battery_Technology = getArguments().getString(EXTRA_BATTERY_TECHNOLOGY, "unbekannt");
        Battery_Status = getArguments().getInt(EXTRA_BATTERY_STATUS, -1);

        BatteryManager mBatteryManager = (BatteryManager) getContext().getSystemService(Context.BATTERY_SERVICE);
        Battery_Current = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        show_Values();

        return view;
    }


    void change_Values(Intent intent)
    {
        BatteryManager mBatteryManager = (BatteryManager) getContext().getSystemService(Context.BATTERY_SERVICE);

        Battery_Level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        Battery_Temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        Battery_Technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        Battery_Health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        Battery_Voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        Battery_Status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        Battery_Current = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);


        show_Values();
        Toast.makeText(getContext(), "Aktualisiert", Toast.LENGTH_SHORT).show();
    }
    void show_Values()
    {
        if(Battery_Level != -1)
            Text_Battery_Level.setText(Battery_Level + " %");
        if(Battery_Temperature != -1)
            Text_Battery_Temperature.setText((Battery_Temperature/10) + "," + (Battery_Temperature%10) + " °C");
        if(Battery_Technology != null)
            Text_Battery_Technology.setText(Battery_Technology);
        if(Battery_Health != -1)
        {
            String text = new String();
            if(Battery_Health == BatteryManager.BATTERY_HEALTH_COLD)                text = "Kalt";
            else if(Battery_Health == BatteryManager.BATTERY_HEALTH_DEAD)           text = "Tot";
            else if(Battery_Health == BatteryManager.BATTERY_HEALTH_GOOD)           text = "Gut";
            else if(Battery_Health == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE)   text = "Überspannung";
            else if(Battery_Health == BatteryManager.BATTERY_HEALTH_OVERHEAT)       text = "Überhitzt";
            else if(Battery_Health == BatteryManager.BATTERY_HEALTH_UNKNOWN)        text = "Unbekannt";
            else if(Battery_Health == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE)    text = "Fehler";

            Text_Battery_Health.setText(text);
        }
        if(Battery_Voltage != -1)
        {
            Text_Battery_Voltage.setText((Battery_Voltage)/1000 + "," + (Battery_Voltage%1000) + " V");
        }

        Text_Battery_Current.setText("" + Battery_Current/1000 + " mA");

        if(Battery_Status != -1)
        {
            String text = new String();
            if(Battery_Status == BatteryManager.BATTERY_STATUS_CHARGING)    text = "Lädt";
            else if(Battery_Status == BatteryManager.BATTERY_STATUS_DISCHARGING)    text = "Entlädt";
            else if(Battery_Status == BatteryManager.BATTERY_STATUS_FULL)           text = "Voll geladen";
            else if(Battery_Status == BatteryManager.BATTERY_STATUS_NOT_CHARGING)   text = "Lädt nicht";
            else if(Battery_Status == BatteryManager.BATTERY_STATUS_UNKNOWN)        text = "Unbekannt";

            Text_Battery_Status.setText(text);
        }

        // Verschönerungen
        if(Battery_Health == BatteryManager.BATTERY_HEALTH_COLD)    Text_Battery_Temperature.setTextColor(getResources().getColor(R.color.blue));
        else if(Battery_Health == BatteryManager.BATTERY_HEALTH_OVERHEAT)   Text_Battery_Temperature.setTextColor(getResources().getColor(R.color.red));
        else                                                                Text_Battery_Temperature.setTextColor(getResources().getColor(R.color.black));
    }
}