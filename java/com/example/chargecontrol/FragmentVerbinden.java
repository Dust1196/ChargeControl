package com.example.chargecontrol;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


// Hier soll der Hauptbildschirm sien, wenn das Gerät über Bluetooth verbunden ist
public class FragmentVerbinden extends Fragment implements Button.OnClickListener, AdapterView.OnItemClickListener
{
    public static final String EXTRA_BATTERY_LEVEL = "extra_battery_level";

    enum BluetoothStatus
    {
        OFF,
        TURNING_OFF,
        ON,
        TURNING_ON,
        CONNECTED
    }
    /* Interfacevariable, in der die Funktion ist, mit der der Activity Daten übergeben werden können */
    Interface_BluetoothVerbunden_Listener mCallback;

    // Funktion, mit der die Activity das Interface deklarieren kann
    public void set_Interface_BluetoothVerbunden(Activity activity)
    {
        mCallback = (Interface_BluetoothVerbunden_Listener) activity;
    }

    // Interface. Muss in der Activity eingebunden werden (implements)
    public interface Interface_BluetoothVerbunden_Listener
    {
        // Folgende Methode wird in der Activity programmiert und kann von dem Fragment aufgerufen werden
        //void VerbindungAufgebaut(BluetoothDevice device, BluetoothSocket socket); // Alte Funktion

        boolean User_Request_Connect(BluetoothDevice device);
        void start_BLE_Scann(BluetoothAdapter adapter);

        // ... weitere Methoden ...
    }

    // -----------------------------------------------------------------------------

    boolean BLE_vorhanden = false;
    int Battery_Level = -1;

    BluetoothStatus mBluetoothStatus;

    TextView Text_Bluetooth_Status;
    public TextView Text_Battery_Level;
    Button Button_Scan;

    BluetoothAdapter mBluetoothAdapter;

   // UUID Bluetooth_uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");      // Eventuell andere UUID verwenden

    ListView List_Bluetooth;
    List<String> Name_Bluetooth;        // Für die Namen der Gescannten Geräte
    List<BluetoothDevice> Device_Bluetooth;

    ArrayAdapter<String> Name_Bluetooth_Adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_verbinden, container, false);

        Text_Bluetooth_Status = view.findViewById(R.id.Text_Bluetooth_Status);
        Text_Battery_Level = view.findViewById(R.id.Text_Battery_Level);

        List_Bluetooth = view.findViewById(R.id.List_Bluetooth);
        Button_Scan = view.findViewById(R.id.Button_Scan);

        Button_Scan.setOnClickListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        check_bluetooth_at_Startup();          // checkt die Bluetooth Verfügbarkeit (auch LowEnergy)


        // --------------------- Battery --------------------------------------------
        Battery_Level = getArguments().getInt(EXTRA_BATTERY_LEVEL, -1);
        changeBatteryLevel(Battery_Level);

        // ------------- List View anlegen -----------------------------------------------------------------

        List_Bluetooth = view.findViewById(R.id.List_Bluetooth);

        List_Bluetooth.setOnItemClickListener(this);
        Name_Bluetooth = new ArrayList<>();
        Device_Bluetooth = new ArrayList<>();

        return view;
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
    }
// Funktionen, die von außen aufgerufen werden ---------------------------------------------------------
    // -------------------------------------------------------------------------------------------------
    void changeBatteryLevel(int level)
    {
        Text_Battery_Level.setText("Akku: " + level + "%");
    }
    void Bluetooth_State_Changed(final int state)
    {
        switch (state)
        {
            case BluetoothAdapter.STATE_OFF:
                mBluetoothStatus = FragmentVerbinden.BluetoothStatus.OFF;     // Setzt die Variable auf Bluetooth Off
                Text_Bluetooth_Status.setText("Bluetooth Status: AUS");
                Button_Scan.setText("Bluetooth\neinschalten");
                Button_Scan.setTextSize(20);
                break;

            case BluetoothAdapter.STATE_TURNING_OFF:
                mBluetoothStatus = FragmentVerbinden.BluetoothStatus.TURNING_OFF;
                clear_List();
                Text_Bluetooth_Status.setText("Bluetooth Status: Schaltet aus...");
                break;

            case BluetoothAdapter.STATE_ON:
                mBluetoothStatus = FragmentVerbinden.BluetoothStatus.ON;          // Setzt die Variable auf Bluetooth On
                Text_Bluetooth_Status.setText("Bluetooth Status: AN");
                Button_Scan.setText("Scannen");
                Button_Scan.setTextSize(30);
                break;

            case BluetoothAdapter.STATE_TURNING_ON:
                mBluetoothStatus = FragmentVerbinden.BluetoothStatus.TURNING_ON;
                Text_Bluetooth_Status.setText("Bluetooth Status: Schaltet an...");
                break;
        }
    }

    void Bluetooth_Device_Found(BluetoothDevice device)
    {
        if(device.getName() != null)
        {
            Name_Bluetooth.add(device.getName());       // Namen in die Liste übernehmen

            Device_Bluetooth.add(device); // Gerät in Liste übernehmen

            Name_Bluetooth_Adapter = new ArrayAdapter<>(getContext(), R.layout.bluetooth_list, R.id.TextView_Name, Name_Bluetooth);      // context, Layout, Objekte

            List_Bluetooth.setAdapter(Name_Bluetooth_Adapter);
        }
    }
    // -------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------


    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.Button_Scan:      // Wenn gescannt werden soll
                if((mBluetoothAdapter != null) && (BLE_vorhanden == true))
                {
                    if(mBluetoothStatus == BluetoothStatus.OFF || mBluetoothStatus == BluetoothStatus.TURNING_OFF)      // Bluetooth ist aus
                    {
                        check_enable_Bluotooth();
                    }
                    else        // Bluetooth ist an -> scannen
                    {
                        checkBTPermissions();
                        clear_List();

                        mCallback.start_BLE_Scann(mBluetoothAdapter);
                    }
                }
                else
                {
                    Toast.makeText(getContext(), "Kein Bluetoothadapter vorhanden", Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    // ------------------List view on Item Click----------------------------------
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        mBluetoothAdapter.cancelDiscovery();        // Suche stoppen
        boolean success;

        success = mCallback.User_Request_Connect(Device_Bluetooth.get(position));

        if(!success)
        {
            Toast.makeText(getContext(), "Da ist etwas schiefgelaufen...", Toast.LENGTH_SHORT).show();
        }
        clear_List();       // Bluetootliste leeren
    }

    private void check_bluetooth_at_Startup()
    {
        if(mBluetoothAdapter == null)
        {
            Text_Bluetooth_Status.setText("Kein Bluetoothadapter gefunden");
            //Toast.makeText(getContext(), "Kein Bluetoothadapter vorhanden", Toast.LENGTH_LONG).show();
        }
        else if(!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(getContext(), "Kein Bluetooth LE verfügbar", Toast.LENGTH_LONG).show();
        }
        else
        {
            BLE_vorhanden = true;                 // Variable für BT verfügbar setzen
            if(mBluetoothAdapter.isEnabled())
            {
                Text_Bluetooth_Status.setText("Bluetooth Status: AN");
                mBluetoothStatus = BluetoothStatus.ON;
            }
            else
            {
                Text_Bluetooth_Status.setText("Bluetooth Status: AUS");
                Button_Scan.setText("Bluetooth\neinschalten");
                Button_Scan.setTextSize(20);
                mBluetoothStatus = BluetoothStatus.OFF;
            }
        }
    }

    // ----------------- Bluetooth -------------------------------

    private boolean check_enable_Bluotooth()
    {
        if(mBluetoothAdapter == null)      return false;        // Gerät hat kein Bluetoothadapter
        if(!mBluetoothAdapter.isEnabled())                      // Bluetooth ist aus
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);        // Wird aktiviert (bzw. User wird gefragt)
            startActivity(enableBluetooth);                                                     // Activity ausführen
        }

        return true;
    }

    // check Permissions -----------------------------------------------------
    private void checkBTPermissions()
    {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck = getActivity().checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += getActivity().checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0)
            {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }

    // ----------------  Liste der Gescannten Geräte -------------
    private void clear_List()
    {
        Name_Bluetooth.clear();
        Device_Bluetooth.clear();
        Name_Bluetooth_Adapter = new ArrayAdapter<>(getContext(), R.layout.bluetooth_list, R.id.TextView_Name, Name_Bluetooth);      // context, Layout, Objekte
        List_Bluetooth.setAdapter(Name_Bluetooth_Adapter);
    }
}

