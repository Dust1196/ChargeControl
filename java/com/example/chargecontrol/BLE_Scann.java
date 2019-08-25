
package com.example.chargecontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


import java.util.ArrayList;

public class BLE_Scan
{
    static final String TAG = "BLE_SCAN";

    /* Interfacevariable, in der die Funktion ist, mit der der Activity Daten übergeben werden können */
    Interface_BLE_Scan_Listener mCallback;

    // Funktion, mit der die Activity das Interface deklarieren kann
    public void set_Interface_BLE_Scan_Listener(Activity activity)
    {
        mCallback = (Interface_BLE_Scan_Listener) activity;
    }

    // Interface. Muss in der Activity eingebunden werden (implements)
    public interface Interface_BLE_Scan_Listener
    {
        void found_Device(BluetoothDevice device);
    }


    private ArrayList<BluetoothDevice> BT_device;       // Liste mit den gefundenen geräten

    final Handler mHandler = new Handler();
    private Thread mUiThread;
    Context context;

    private BluetoothAdapter bluetoothAdapter;
    public boolean mScanning;
    private Handler handler;
    private Runnable r;

    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;

    void initialisiere(Activity activity, Context _context, BluetoothAdapter adapter)
    {
        mCallback = (Interface_BLE_Scan_Listener) activity;
        bluetoothAdapter = adapter;
        context = _context;
        handler = new Handler();
        mUiThread = new Thread();
        BT_device = new ArrayList<>();

        r = new Runnable() {
            @Override
            public void run() {
                if(mScanning || true)   // Wenns läuft oder immmer
                {
                    Log.i(TAG, "stoppe scannen");
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    Toast.makeText(context, "stoppe Scannen", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            // Stops scanning after a pre-defined scan period.

            handler.postDelayed(r, SCAN_PERIOD);        // Macht das oben nach 10 sec

            Log.i(TAG, "scanne");
            Toast.makeText(context, "Scanne...\nGPS an?", Toast.LENGTH_SHORT).show();
            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);   // Startet Scannen
        }
        else
        {
            Log.i(TAG, "stoppe scannen");

            bluetoothAdapter.stopLeScan(leScanCallback);

            if(mScanning)
            {
                handler.removeCallbacks(r);
                Log.i(TAG, "Remove Callback");
            }
            mScanning = false;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!BT_device.contains(device))     // Wenn nicht schon drin
                    {
                        Log.i(TAG, "run: neues Gerät gefunden");
                        BT_device.add(device);          // hinzufügen
                        mCallback.found_Device(device); // An MainActivity übergeben
                    }
                }
            });
        }
    };

    public final void runOnUiThread(Runnable action)
    {
        if (Thread.currentThread() != mUiThread)
        {
            mHandler.post(action);
        }
        else
            {
            action.run();
        }
    }
}
