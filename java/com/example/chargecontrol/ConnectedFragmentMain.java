package com.example.chargecontrol;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


// Hier soll der Hauptbildschirm sien, wenn das Gerät über Bluetooth verbunden ist
public class ConnectedFragmentMain extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener
{
    public final static String TAG = "ConnectedFragmentMain";
    
    Interface_VerbundenMain_Listener mCallback; //Interfacevariable, in der die Funktion ist, mit der der Activity Daten übergeben werden können


    public void set_Interface_VerbundenMain_Listener(Activity activity)     // Funktion, mit der die Activity das Interface deklarieren kann
    {
        mCallback = (Interface_VerbundenMain_Listener) activity;
    }

    public interface Interface_VerbundenMain_Listener       // Interface. Muss in der Activity eingebunden werden (implements)
    {
        // Folgende Methode wird in der Activity programmiert und kann von dem Fragment aufgerufen werden
        void sende_laden_ein_ausschalten(boolean strom_an);
        void Max_Battery_Level_Changed(byte max_level);
        void sende_Battery_Level(byte Level);           // Für Testzwecke

        // ... weitere Methoden ...
    }

    // -----------------------------------------------------------------------------

    Button Button_Strom_an;
    Button Button_Strom_aus;
    TextView Text_Name;

    SeekBar SeekBar_max_Battery;
    TextView TextView_max_Battery;

    ImageView ImageView_Battery;

    boolean Battery_is_charging = false;


    public static final String NAME_BT_DEVICE = "name_bt_device";
    public static final String MAX_BATTERY_LEVEL = "max_battery_level";
    public static final String Entwickleroptionen = "ENTWICKLEROPTIONEN_ENABLE";
    public static final String Battery_Charging_Status = "Battery_Charging_Status";

    String Name_Bluetooth;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        Button_Strom_an = view.findViewById(R.id.Button_Strom_an);
        Button_Strom_an.setOnClickListener(this);
        Button_Strom_aus = view.findViewById(R.id.Button_Strom_aus);
        Button_Strom_aus.setOnClickListener(this);

        ImageView_Battery = view.findViewById(R.id.ImageView_Batterie);


        SeekBar_max_Battery = view.findViewById(R.id.SeekBar_Akku_Level);
        SeekBar_max_Battery.setOnSeekBarChangeListener(this);

        Text_Name = view.findViewById(R.id.Text_Name);
        TextView_max_Battery = view.findViewById(R.id.TextView_max_Level);

        Name_Bluetooth = getArguments().getString(NAME_BT_DEVICE);
        Battery_is_charging = getArguments().getBoolean(Battery_Charging_Status, false);

        set_Battery_Image();

        boolean Entwickleroptionen_enable = getArguments().getBoolean(Entwickleroptionen);
        if(Entwickleroptionen_enable)
        {
            Log.i(TAG, "onCreateView: Buttons visible");
            Button_Strom_an.setVisibility(View.VISIBLE);
            Button_Strom_aus.setVisibility(View.VISIBLE);
        }
        else
        {
            Log.i(TAG, "onCreateView: Buttons Invisible");
            Button_Strom_an.setVisibility(View.INVISIBLE);
            Button_Strom_aus.setVisibility(View.INVISIBLE);
        }


        byte max_level = getArguments().getByte(MAX_BATTERY_LEVEL, (byte) 0);
        if(max_level != 0)  SeekBar_max_Battery.setProgress((int)max_level);

        if(Name_Bluetooth == null)
        {
            Name_Bluetooth = "Unbekannt";
        }
        Text_Name.setText("Verbunden mit: " + Name_Bluetooth);

        return view;
    }

    // ----------------------- onClick ------------------------
    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.Button_Strom_an:
                mCallback.sende_laden_ein_ausschalten(true);
                break;
            case R.id.Button_Strom_aus:
                mCallback.sende_laden_ein_ausschalten(false);
                break;
        }
    }

    // SeekBar ------------------------------------------------
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        TextView_max_Battery.setText("" + progress);

       // mCallback.sende_Battery_Level((byte)progress);  // Für Testzwecke
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
        Log.i(TAG, "onStartTrackingTouch: ");
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar)        // setOnSeekBarChangeListener
    {
        Log.i(TAG, "onStopTrackingTouch: ");
        if(SeekBar_max_Battery.getProgress() <= 100)
        {
            mCallback.Max_Battery_Level_Changed((byte)SeekBar_max_Battery.getProgress());
        }
        else
        {
            Toast.makeText(getContext(), "Es ist ein Fehler aufgetreten", Toast.LENGTH_LONG).show();
            Log.i(TAG, "onStopTrackingTouch: Fehler! mehr als 100 von Seekbar bekommen");
            SeekBar_max_Battery.setProgress(100);
        }
    }

    public void setBattery_charging_status(boolean is_charging)
    {
        Battery_is_charging = is_charging;
        set_Battery_Image();
    }

    void set_Battery_Image()
    {
        if(Battery_is_charging)
        {
            ImageView_Battery.setImageResource(R.drawable.ic_battery_charging);
        }
        else
        {
            ImageView_Battery.setImageResource(R.drawable.ic_battery_discharging);
        }
    }
}