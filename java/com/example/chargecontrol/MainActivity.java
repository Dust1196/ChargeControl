package com.example.chargecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FragmentVerbinden.Interface_BluetoothVerbunden_Listener,
        ConnectedFragmentMain.Interface_VerbundenMain_Listener, Fragment_Settings.Interface_Settings_Listener, Orientation.Interface_Orientation_Callback,
        BLE_Scann.Interface_BLE_Scann_Listener
{

    enum Fragmente
    {
        FRAGMENT_VERBINDEN,
        CONNECTED_FRAGMENT_MAIN,
        CONNECTED_FRAGMENT_STROM,
        SETTINGS,
        BATTERY_DETAILS,
        LOADING_SCREEN
    }
    enum BluetoothStatus
    {
        OFF,
        TURNING_OFF,
        ON,
        TURNING_ON,
    }
    enum Characteristicen
    {
        CHARACTERISTIC_LADE_EIN_AUS,
        CHARACTERISTIC_LED_AN_AUS,
        CHARACTERISTIC_NAME,
        CHARACTERISTIC_STROM,
        CHARACTERISTIC_AKKU_LEVEL
    }

    Fragmente AktivesFragment = Fragmente.FRAGMENT_VERBINDEN;
    BluetoothStatus mBluetoothStatus;
    Characteristicen gesendete_characteristic;

    public static final String TAG = "MainActivity";
    static final String SETTINGS_SHARED_PREFERENCES = "SETTINGS_SHARED_PREFERENCES";
    static final String SETTINGS_BT_DEVICE_LED_ON = "bt_device_led_on";
    static final String SETTINGS_BACKGROUND_ANIMATION_ON_Shared_PREFERENCES = "settings_background_animation_on_shared_prefs";
    static final String SETTINGS_MAX_BATTERY_LEVEL_SHARED_PREFERENCES = "settings_max_battery_level";
    static final String SETTINGS_MAX_BATTERY_LEVEL_HYSTERESE = "settings_battery_hysterese";

    Custom_BLE_Services Services;       // Hier sind alle UUIDs, Services und Characteristics drin

    BluetoothGattCharacteristic BLE_Characteristic_temp;
    boolean BLE_Characteristic_richtig_gesendet = false;
    boolean BLE_Characteristic_erneut_gesendet = false;

    private boolean verbunden = false;

    int time_timeout_verbindung_herstellen = 5000;

    private DrawerLayout drawer;
    NavigationView navigationView;

    BLE_Scann ble_scann;

    BluetoothDevice mBluetoothDevice = null;
    BluetoothGatt mBluetoothGatt = null;

    Timer Timer_verivication_delay;
    Timer Timer_timeout;            // Für alle Timeouts, die ich so brauche

    String Name_BT_Device;
    Boolean BT_Device_LED_an;

    int Battery_Level = -1;
    int Battery_Level_alt = Battery_Level;
    int Battery_Temperature;
    String Battery_Technology = new String();
    int Battery_Health;
    int Battery_Voltage;
    int Battery_Status;

    byte Max_Battery_Level;
    byte Max_Battery_Level_Hysterese;
    static final byte Min_Max_Battery_Level = 30;
    boolean ladezustand_aktuell_laden_an = false;
    boolean Entwickleroptionen_enable = false;

    int aktueller_strom = 0;

    ArrayList<Integer> StromListe;
    ArrayList<String> StromListe_Time;
    DateFormat dateFormat;
    String aktuelle_Zeit;

    Orientation orientation;
    Boolean Background_Animation_Battery;

    // ----------------------------------- Callback Gatt ----------------------------------------
    // ------------------------------------------------------------------------------------------

    public final BluetoothGattCallback BT_GATT_Callback = new BluetoothGattCallback()       // Callback für GATT Server (Microkontroller)
    {// Was passieren soll, wenn irgendetwas ankommt

        // Todo: Unhandled exception in callback: java.util.UUID android.bluetooth.BluetoothGattCharacteristic.getUuid()' on a null object reference
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if(newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        Verbindung_Verloren_reset();
                        Toast.makeText(getApplicationContext(), "Verbindung verloren", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else if(newState == BluetoothProfile.STATE_CONNECTED)
            {
                Log.i(TAG, "Verbunden");
                Log.i(TAG, "Starte discovery");
                if(!mBluetoothGatt.discoverServices())
                {
                    Log.i(TAG, "onConnectionStateChange: ERROR: Fehler bei discoverServices()");
                    Toast.makeText(getApplicationContext(), "Fehler bei discoverServices()", Toast.LENGTH_SHORT).show();
                }
                // noch irgendwas
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)        // Services entdeckt
        {
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                Services.on_Services_Discovered(gatt);      // Holt sich alle Services und Charakteristiken mit den UUIDs raus.
                Log.i(TAG, "Service Discover fertig");
                Log.i(TAG, "onServicesDiscovered: activate_Notification_hallo()");
                Services.activate_Notification_hallo(mBluetoothGatt);

                Timer_verivication_delay.schedule(new TimerTask()
                {
                    @Override
                    public void run() {
                        Services.send_verification(mBluetoothGatt);
                        Log.i(TAG, "Verifikationsanfrage gesendet");

                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Services.activate_Notification_current(mBluetoothGatt);
                                Log.i(TAG, "onServicesDiscovered: activate_Notification_current");
                            }
                        }, 200);    // Nicht weniger!!!!
                    }
                }, 30); // 30 ms delay
            }
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            Log.i(TAG, "onCharacteristicChanged: aufgerufen");
            if(characteristic.getUuid().equals(Services.UUID_Characteristic_hallo))      // Verification
            {
                //Log.i(TAG, "Richtige UUID");
                byte[] empfangen = characteristic.getValue();
                if(Services.check_verification(empfangen[0]))
                {
                    Log.i(TAG, "Verifikation OK -> Verbunden");
                    Timer_timeout.cancel();
                    Timer_timeout = null;
                    Timer_timeout = new Timer();    // Da nach cancel() der TImer nichtmehr verwendet werden kann...
                    Log.i(TAG, "Timer VerbindungsTimeout: Timer angehalten");

                   // Services.deactivate_Notification_hallo(mBluetoothGatt);
                   // Log.i(TAG, "onCharacteristicChanged: deaktiviere Notification für Hallo-Nachricht");

                    verbunden = true;                               // Verbindung aufgebaut und veriviziert
                    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);  // Gebe Drawer wieder frei
                    wechsle_fragment_zu_ConnectedFragmentMain();
                    sende_einstellungen_bei_verbunden();            // Sendet LED, später vllt mehr
                }
                else
                {
                    Log.i(TAG, "Verifikation nicht OK -> Verbindung wird getrennt");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Verbindung_Verloren_reset();
                        }
                    });
                }
            }
            else if(characteristic.getUuid().equals(Services.UUID_Characteristic_Current))      // Strom erhalten
            {
                Log.i(TAG, "onCharacteristicChanged: Strom erhalten");
                byte[] value = characteristic.getValue();

                int strom = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                Log.i(TAG, "onCharacteristicChanged: uint16 strom = " + strom);

                strom = (int)((float)strom * 1.63);


                /*int strom =  value[0] << 8;  // Höherwertige Bits
                int strom = (0 | value[0]);
                Log.i(TAG, "onCharacteristicChanged: strom = " + strom);
                strom = strom << 8;
                Log.i(TAG, "onCharacteristicChanged: strom << 8 = " + strom);

                try
                {
                    strom = strom | value[1];   // Niederwertige Bits
                    Log.i(TAG, "onCharacteristicChanged: strom | value[1] = " + strom);
                }catch (Exception e)
                {
                    Log.i(TAG, "onCharacteristicChanged: value[1] ERROR: " + e.getMessage());
                }

                strom *= 4; // Umrechnen*/
                aktueller_strom = strom;    // Zuweisen

                Date date = Calendar.getInstance().getTime();

                StromListe.add(aktueller_strom);

                aktuelle_Zeit = dateFormat.format(date);
                StromListe_Time.add(aktuelle_Zeit);   // Format HH:mm:ss yyyy

                if(AktivesFragment == Fragmente.CONNECTED_FRAGMENT_STROM)
                {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                            try {
                                Log.i(TAG, "onCharacteristicChanged: sende Zeit und Strom an Fragment Strom");
                                ((ConnectedFragmentStrom) fragment).set_aktueller_Strom(aktueller_strom);
                            } catch (Exception e) {
                                Log.i(TAG, "onCharacteristicChanged: ERROR:  " + e.getMessage());
                            }
                        }
                    });
                }
            }
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            //super.onCharacteristicWrite(gatt, characteristic, status);

            if((status == BluetoothGatt.GATT_SUCCESS) && (characteristic.getUuid() == BLE_Characteristic_temp.getUuid()) && (characteristic.getValue() == BLE_Characteristic_temp.getValue()))
            {
                BLE_Characteristic_richtig_gesendet = true;
                BLE_Characteristic_erneut_gesendet = false;
                Log.i(TAG, "onCharacteristicWrite: Characteristic richtig empfangen ( " + gesendete_characteristic + " )");
                mBluetoothGatt.executeReliableWrite();  // Service wurde richtig übertragen

                Timer_timeout.cancel();
                Timer_timeout = null;
                Timer_timeout = new Timer();    // Da nach cancel() der Timer nichtmehr verwendet werden kann...

                switch (gesendete_characteristic)
                {
                    case CHARACTERISTIC_LADE_EIN_AUS:
                    {
                        byte[] bekommen = characteristic.getValue();

                        break;
                    }
                    case CHARACTERISTIC_LED_AN_AUS:     // Schalter in den einstellungen
                    {
                        byte [] bekommen = characteristic.getValue();
                        if(bekommen[0] == 1)    BT_Device_LED_an = true;
                        else                    BT_Device_LED_an = false;

                        // Speichern
                        SharedPreferences shared_preferences = getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
                        final SharedPreferences.Editor editor = shared_preferences.edit();
                        editor.putBoolean(SETTINGS_BT_DEVICE_LED_ON, BT_Device_LED_an);
                        editor.apply();

                        if(AktivesFragment == Fragmente.SETTINGS) {
                            // schalter Visuell umschalten
                            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                            try {
                                ((Fragment_Settings) fragment).change_switch_LED(BT_Device_LED_an);
                            } catch (Exception e) {
                                Log.i(TAG, "change_switch_LED (onCharacteristicWrite) : ERROR: " + e.getMessage());
                            }
                        }
                        break;
                    }
                    case CHARACTERISTIC_NAME:
                    {
                        Name_BT_Device = characteristic.getStringValue(0);  // Name übernehmen
                        break;
                    }
                    case CHARACTERISTIC_STROM:
                    {
                        Log.i(TAG, "onCharacteristicWrite: Anmeldung / Abmeldung Verifiziert (" + gesendete_characteristic + ")");
                        break;
                    }
                    case CHARACTERISTIC_AKKU_LEVEL:
                        if(Battery_Level != characteristic.getValue()[0])
                        {
                            Log.i(TAG, "onCharacteristicWrite: Falschen Akkustand übermittelt:\nAkkustand = " + Battery_Level + ", Von µC bekommen = " + characteristic.getValue()[0]);
                        }
                        break;
                    default: break;
                }
            }
            else
            {
                Log.i(TAG, "onCharacteristicWrite: UUID oder Value nicht OK");
                Log.i(TAG, "onCharacteristicWrite: sollte " + BLE_Characteristic_temp.getUuid() + " sein");
                Log.i(TAG, "onCharacteristicWrite: ist    " + characteristic.getUuid());
                sende_characteristic_erneut();
            }
        }
    };
// ------------------------------------------- Callback Gatt ENDE ------------------------------------------
// ---------------------------------------------------------------------------------------------------------

    // Fehler beim senden---------------------------------------------------------------------------------------------------------

    void sende_characteristic_erneut()
    {
        if(!BLE_Characteristic_richtig_gesendet)
        {
            if(BLE_Characteristic_erneut_gesendet)          // Wenn das 2. mal aufgerufen
            {
                Log.i(TAG, "sende_characteristic_erneut: 2. mal nicht gesendet");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Verbindung_Verloren_reset();
                    }
                });

            }
            else
            {
                mBluetoothGatt.abortReliableWrite();        // Daten nicht am µC übernehmen

                Log.i(TAG, "sende_characteristic_fehlgeschlagen: sende erneut");
                mBluetoothGatt.beginReliableWrite();
                mBluetoothGatt.writeCharacteristic(BLE_Characteristic_temp);            // Sende characterietic erneut
                BLE_Characteristic_erneut_gesendet = true;

                Timer_timeout.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        // Wenn auch beim 2. mal falsch ist -> abbruch
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run()
                            {
                                Verbindung_Verloren_reset();
                                Toast.makeText(getApplicationContext(), "Verbindung verloren", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }, 750);             // Nochmals mit 0,75 sec Timer starten
            }
        }
        else
        {
            Log.i(TAG, "sende_characteristic_fehlgeschlagen: alles ok... Wieso wurde das aufgerufen=");
        }
    }

    // onAttachFragment---------------------------------------------------------------------------------------------------------
    @Override
    public void onAttachFragment(Fragment fragment)
    {
        if(fragment instanceof FragmentVerbinden)       // wenn es ein FragmentVerbinden ist
        {
            ((FragmentVerbinden)fragment).set_Interface_BluetoothVerbunden(this);  // Zeiger auf diese Activity übergeben, dass Fragment Daten schicken kann
        }
        if(fragment instanceof ConnectedFragmentMain)
        {
            ((ConnectedFragmentMain) fragment).set_Interface_VerbundenMain_Listener(this);
        }
        if(fragment instanceof Fragment_Settings)
        {
            ((Fragment_Settings)fragment).set_Interface_Settings_Listener(this);
        }
        super.onAttachFragment(fragment);
    }

/* ------------------------ On Create -----------------------------------------------------------------------
* -----------------------------------------------------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_bar_open, R.string.navigation_bar_close);
        drawer.addDrawerListener(toggle);           // Für die 3 Striche oben links um das Menü zu öffnen
        toggle.syncState();        

        orientation = new Orientation(getApplicationContext(), this);     // Für die Ausrichtung des Handys

        StromListe = new ArrayList<>();
        StromListe_Time = new ArrayList<>();
        dateFormat = new SimpleDateFormat("HH:mm:ss yyyy");

        Timer_verivication_delay = new Timer();
        Timer_timeout = new Timer();

        ble_scann = new BLE_Scann();

        // ----------------- Broadcast Registrierung -------------------------------
        IntentFilter FilterBTStatus = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);  // Filter für Broadcast anlegen
        registerReceiver(mBluetoothStateChanged, FilterBTStatus);                               // Broadcast registrieren

        IntentFilter Bat_Filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(mBatteryReceiver, Bat_Filter);      // Dieser musst *NICHT* unregistriert werden!!

        Services = new Custom_BLE_Services();

        Name_BT_Device = "Name Unbekannt";
        Battery_Level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        Battery_Temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        Battery_Technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        Battery_Health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        Battery_Voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        Battery_Status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        load_data_from_storage();   // Lädt alles gespeicherte aus dem Speicher

        if(Background_Animation_Battery)
        {
            orientation.activate();
        }

        if(BluetoothAdapter.getDefaultAdapter() != null)
        {
            if(BluetoothAdapter.getDefaultAdapter().isEnabled())    mBluetoothStatus = BluetoothStatus.ON;
        }



        // Startbildschirm anzeigen
        if(savedInstanceState == null)  // Wenn die App neu gestartet wurde
        {
            wechsle_fragment_zu_Fragment_Verbinden();   // Welchsel zum ersten Fragment und übergibt Batterielevel
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_Battery_Image, new Fragment_Battery_Image()).commit();

    }

    @Override
    protected void onDestroy()
    {

        Verbindung_Verloren_reset();
        alles_beenden();
        super.onDestroy();
        System.exit(0); // Bleibt jetzt so
    }

    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause: ");
        orientation.deaktivate();
        super.onPause();
    }

    @Override
    protected void onResume() 
    {
        Log.i(TAG, "onResume: ");
        if(Background_Animation_Battery)
            orientation.activate();
        super.onResume();
    }

    void alles_beenden()
    {
        if(mBluetoothGatt != null)
            mBluetoothGatt.close();

        orientation.deaktivate();

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if(fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();

        try
        {
            unregisterReceiver(mBluetoothStateChanged);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void onBackPressed()
    {
       if(drawer.isDrawerOpen(GravityCompat.START))
       {
           drawer.closeDrawer(GravityCompat.START);        // Schließt den drawer
       }
       else
       {
           if(AktivesFragment == Fragmente.FRAGMENT_VERBINDEN)
           {
               onDestroy();
               super.onBackPressed();
           }
           else if(AktivesFragment == Fragmente.CONNECTED_FRAGMENT_MAIN)
           {
               try
               {
                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           Verbindung_Verloren_reset();
                       }
                   });
               }catch (Exception e)
               {
                   Log.i(TAG, "onBackPressed error: " + e.getLocalizedMessage());
               }
           }
           else if(AktivesFragment == Fragmente.CONNECTED_FRAGMENT_STROM)
           {
               if(verbunden)
               {
                   byte[] sende_byte = new byte[1]; // Für Strom abmelden
                   sende_byte[0] = 0;
                   Services.Characteristic_Current.setValue(sende_byte);
                   BLE_Characteristic_temp = Services.Characteristic_Current;  // Zur verifikation
                   gesendete_characteristic = Characteristicen.CHARACTERISTIC_STROM;
                   mBluetoothGatt.writeCharacteristic(Services.Characteristic_Current);
                   Log.i(TAG, "für Strom abgemeldet");
               }
               else
               {
                   Toast.makeText(getApplicationContext(), "Nicht verbunden", Toast.LENGTH_SHORT).show();
               }

               wechsle_fragment_zu_ConnectedFragmentMain();

               try
               {
                   navigationView.getMenu().getItem(0).setChecked(true);
               }
               catch (Exception e)
               {
                   Log.i(TAG, "onBackPressed: ERROR:  "  +e.getMessage());
               }
           }
           else if(AktivesFragment == Fragmente.BATTERY_DETAILS || AktivesFragment == Fragmente.SETTINGS)
           {
               if(verbunden)
               {
                   wechsle_fragment_zu_ConnectedFragmentMain();
                   try
                   {
                       navigationView.getMenu().getItem(0).setChecked(true);
                   }
                   catch (Exception e)
                   {
                       Log.i(TAG, "onBackPressed: ERROR:  "  +e.getMessage());
                   }

               }

               else
               {
                   wechsle_fragment_zu_Fragment_Verbinden();
               }

           }
       }
    }

    void load_data_from_storage()
    {
        SharedPreferences shared_preferences = getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
        try
        {
            BT_Device_LED_an = shared_preferences.getBoolean(SETTINGS_BT_DEVICE_LED_ON, true);
        }
        catch (ClassCastException e)
        {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        try
        {
            Background_Animation_Battery = shared_preferences.getBoolean(SETTINGS_BACKGROUND_ANIMATION_ON_Shared_PREFERENCES, true);
            Background_Animation_Battery = false;
        }
        catch (ClassCastException e)
        {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        try
        {
            Max_Battery_Level = (byte)shared_preferences.getInt(SETTINGS_MAX_BATTERY_LEVEL_SHARED_PREFERENCES, 80);
        }
        catch (Exception e)
        {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        try
        {
            Max_Battery_Level_Hysterese = (byte)shared_preferences.getInt(SETTINGS_MAX_BATTERY_LEVEL_HYSTERESE, 5);
        }
        catch (Exception e)
        {
            Log.i(TAG, "load_data_from_storage: Hysterese konnte nicht geladen werden, ERROR: " + e.getMessage());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
    {
        boolean success = false;

        if(AktivesFragment != Fragmente.LOADING_SCREEN)
        {
            switch (menuItem.getItemId()) {
                case R.id.nav_connected_main:
                    if (verbunden || true)           // todo: Das muss raus!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    {
                        wechsle_fragment_zu_ConnectedFragmentMain();
                        success = true;
                    } else {
                        Toast.makeText(getApplicationContext(), "Bitte mit Gerät verbinden", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.nav_connected_strom:
                    if (verbunden)
                    {
                        wechsle_fragment_zu_ConnectedFragmentStrom();
                        success = true;
                    } else {
                        Toast.makeText(getApplicationContext(), "Bitte mit Gerät verbinden", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.nav_settings:
                    wechsle_fragment_zu_settings();
                    success = true;
                    break;

                case R.id.Battery_Details:
                    wechsle_fragment_zu_Battery_Details();
                    success = true;
                    break;
            }
        }

        drawer.closeDrawer(GravityCompat.START);        // Schließt den drawer nach links

        return success;
    }
    // ---------------- Methoden für Fragmente ---------------------------------------
    // -------------------------------------------------------------------------------------------

    // Wird vom Fragment aufgerufen, wenn eine Bluetoothferbindung hergestellt wurde

    @Override
    public void found_Device(BluetoothDevice device)        // Aus BLE_Scann
    {
        if(AktivesFragment == Fragmente.FRAGMENT_VERBINDEN)
        {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            ((FragmentVerbinden) fragment).Bluetooth_Device_Found(device);
        }
    }

    @Override       // Aus Fragment Verbinden
    public void start_BLE_Scann(BluetoothAdapter adapter)
    {
        ble_scann.initialisiere(this, getApplicationContext(), adapter);
        Log.i(TAG, "start_BLE_Scann: ");

        if(ble_scann.mScanning)
            ble_scann.scanLeDevice(false);      // stoppen, wenns gerade läuft
        ble_scann.scanLeDevice(true);   // starte scannen
    }

    // Sendet die Characteristic mit ReliableWrite, speichert sie in "BLE_Characteristic_temp" und startet den Timer
    public boolean sende_BLE_Characteristic_ReliableWrite(BluetoothGattCharacteristic characteristic)
    {
        boolean success;
        try
        {
            mBluetoothGatt.beginReliableWrite();
            BLE_Characteristic_temp = characteristic;
            mBluetoothGatt.writeCharacteristic(characteristic);
            success = true;
        }
        catch (Exception e)
        {
            success = false;
            Log.i(TAG, "sende_BLE_Characteristic_ReliableWrite: Fehler: " + e.getMessage().toString());
        }

        // Timeout ---------------------------------------------
        BLE_Characteristic_richtig_gesendet = false;    // reset der Variable
        BLE_Characteristic_erneut_gesendet = false;
        if(Timer_timeout == null)
            Timer_timeout = new Timer();
        Timer_timeout.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                sende_characteristic_erneut();
            }
        }, 750);       // 0,75 sec timeout

        return success;
    }
    
    @Override       // Aus FragmentVerbinden
    public boolean User_Request_Connect(BluetoothDevice device)
    {
        // Stoppe scannen
        ble_scann.scanLeDevice(false);
        Log.i(TAG, "User_Request_Connect: stoppe scannen");

        boolean success;
        BluetoothGatt temp = null;
        mBluetoothDevice = device;      // Device übernehmen

        Log.i(TAG, "Starte Timer Timeout");      // Timer starten
        // Starte Timerout Timer

        Timer_timeout.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if(!verbunden)
                {
                    Log.i(TAG, "timeout_abgelaufen: Abbruch ( Verifizierung nicht erhalten )");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            Verbindung_Verloren_reset();
                            Toast.makeText(getApplicationContext(), "Timeout\nBitte erneut versuchen", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }, time_timeout_verbindung_herstellen);

        try
        {
            temp = mBluetoothDevice.connectGatt(getApplicationContext(), true, BT_GATT_Callback);
            success = true;
            Log.i(TAG, "User_Request_Connect: connectGatt ausgeführt");
        }
        catch (IllegalArgumentException e)
        {
            success = false;
            Log.i(TAG, "User_Request_Connect: ERROR: " + e.getMessage());
        }
        mBluetoothGatt = temp;      // Übernehmen

        if(success)
        {
            //verbunden = true;     // Später erst
            Name_BT_Device = device.getName();
            wechsle_fragment_zu_loadingScreen();
            navigationView.getMenu().getItem(0).setChecked(true);   // Für nach dem Laden
        }

        return success;
    }

    // aus Connected Main -------------------------------------------------------------

    @Override
    public void Max_Battery_Level_Changed(byte max_level)
    {

        if(max_level != Max_Battery_Level)      // Wenn neuer Wert
        {
            if(max_level < Min_Max_Battery_Level)      // Wenn zu klein ( unter 30 )
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                ((ConnectedFragmentMain)fragment).SeekBar_max_Battery.setProgress(Min_Max_Battery_Level);   // Auf minimum setztn
            }
            else        // Wenns passt -> Speichern
            {
                Max_Battery_Level = max_level;

                if((Max_Battery_Level > Battery_Level) || (Max_Battery_Level == 100 && (Battery_Status == BatteryManager.BATTERY_STATUS_FULL)))
                                                            sende_laden_ein_ausschalten(true);      // Laden ein
                else                                        sende_laden_ein_ausschalten(false);     // Laden aus

                SharedPreferences shared_preferences = getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
                final SharedPreferences.Editor editor = shared_preferences.edit();
                editor.putInt(SETTINGS_MAX_BATTERY_LEVEL_SHARED_PREFERENCES, (int)Max_Battery_Level);
                editor.apply();
            }
        }
    }

    @Override
    public void Change_Hysterese(int hysterese)
    {
        Max_Battery_Level_Hysterese = (byte)hysterese;
        Log.i(TAG, "Change_Hysterese: " + hysterese);
        SharedPreferences shared_preferences = getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
        final SharedPreferences.Editor editor = shared_preferences.edit();
        editor.putInt(SETTINGS_MAX_BATTERY_LEVEL_HYSTERESE, hysterese);
        editor.apply();
    }

    // aus Fragment_Settings -----------------------------------------------------

    @Override
    public void Change_Name(String name)
    {
        if(verbunden)
        {
           Log.i(TAG, "Change_Name: sende Name: " + name + " Länge: " + name.length());
           byte[] sende_name = name.getBytes();

           Services.Characteristic_Name.setValue(sende_name);
           gesendete_characteristic = Characteristicen.CHARACTERISTIC_NAME; // Zum prüfen, welche Characteristic gesendet wurde
           if(sende_BLE_Characteristic_ReliableWrite(Services.Characteristic_Name))
           {
               Log.i(TAG, "Change_Name: gesendet");
           }
           else
           {
               Log.i(TAG, "Change_Name: Fehler beim senden");
               Toast.makeText(getApplicationContext(), "Fehler beim senden", Toast.LENGTH_SHORT).show();
           }
        }
    }

    @Override
    public void change_switch_LED(boolean LED_on)
    {
        if(BT_Device_LED_an != LED_on)      // Wenn unterschiedlich
        {
            if(verbunden)
            {
                byte[] sendebyte = new byte[1];
                sendebyte[0] = (byte)(LED_on == true ? 1 : 0);
                Services.Characteristic_LED.setValue(sendebyte);
                gesendete_characteristic = Characteristicen.CHARACTERISTIC_LED_AN_AUS;
                if(sende_BLE_Characteristic_ReliableWrite(Services.Characteristic_LED))
                {
                    Log.i(TAG, "change_switch_LED: gesendet");
                }
                else
                {
                    Log.i(TAG, "change_switch_LED: Fehler beim senden");
                }
            }
            else // nicht verbunden -> einfach speichern
            {
                BT_Device_LED_an = LED_on;
                // Speichern
                SharedPreferences shared_preferences = getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
                final SharedPreferences.Editor editor = shared_preferences.edit();
                editor.putBoolean(SETTINGS_BT_DEVICE_LED_ON, BT_Device_LED_an);
                editor.apply();
            }
        }
    }

    @Override
    public void Change_Entwickleroptionen(boolean enable)
    {
        Entwickleroptionen_enable = enable;
    }

    @Override
    public void Change_Background_Animation_state(boolean Hintergrundanimation)
    {

        if(Hintergrundanimation != Background_Animation_Battery)
        {
            if(Hintergrundanimation)
            {
                orientation.activate();
            }
            else
            {
                change_Orientation(0);  // gerade Machen
                orientation.deaktivate();
            }

            Background_Animation_Battery = Hintergrundanimation;
            SharedPreferences shared_preferences = getApplicationContext().getSharedPreferences(SETTINGS_SHARED_PREFERENCES, MODE_PRIVATE);
            final SharedPreferences.Editor editor = shared_preferences.edit();
            editor.putBoolean(SETTINGS_BACKGROUND_ANIMATION_ON_Shared_PREFERENCES, Hintergrundanimation);
            editor.apply();
        }
    }

    // --------- aus Orientation -------------------------

    @Override
    public void change_Orientation(float angle)
    {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_Battery_Image);
        if(fragment != null)
        {
            ((Fragment_Battery_Image)fragment).onOrientationChanged(angle);     // Winkel im Bogenmaß
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------------

    // ----------------------------------------------------

    void wechsle_fragment_zu_Fragment_Verbinden()
    {
        if(!isFinishing() && !isDestroyed())
        {
            Log.i(TAG, "wechsle_fragment_zu_Fragment_Verbinden: ");
            FragmentVerbinden fragment = new FragmentVerbinden();       // Neues Fragment erstellen

            Bundle args = new Bundle();
            args.putInt(FragmentVerbinden.EXTRA_BATTERY_LEVEL, Battery_Level);      // BatterieLevel Übergeben
            fragment.setArguments(args);                                            // und hinzufügen

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, fragment);  // Ansicht wechseln
            transaction.addToBackStack(null);
            transaction.commit();
            AktivesFragment = Fragmente.FRAGMENT_VERBINDEN;

            Log.i(TAG, "wechsle_fragment_zu_Fragment_Verbinden: fertig");
            try
            {
                navigationView.getCheckedItem().setChecked(false);
            }
            catch (Exception e)
            {
                Log.i(TAG, "wechsle_fragment_zu_Fragment_Verbinden: Fehler: " + e.getMessage().toString());
            }
        }
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    void wechsle_fragment_zu_ConnectedFragmentMain()
    {
        try
        {
            ConnectedFragmentMain fragment = new ConnectedFragmentMain();       // Neues Fragment erstellen

            Bundle args = new Bundle();
            args.putString(ConnectedFragmentMain.NAME_BT_DEVICE, Name_BT_Device);      // Name des BT Device übergeben
            args.putByte(ConnectedFragmentMain.MAX_BATTERY_LEVEL, Max_Battery_Level);
            args.putBoolean(ConnectedFragmentMain.Entwickleroptionen, Entwickleroptionen_enable);
            args.putBoolean(ConnectedFragmentMain.Battery_Charging_Status, ladezustand_aktuell_laden_an);
            fragment.setArguments(args);                                            // und hinzufügen

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, fragment);  // Ansicht wechseln
            transaction.addToBackStack(null);
            transaction.commit();
            AktivesFragment = Fragmente.CONNECTED_FRAGMENT_MAIN;

           // navigationView.getMenu().getItem(0).setChecked(true);

        }
        catch (Exception e)
        {
            Log.i(TAG, "wechsle_fragment_zu_ConnectedFragmentMain ERROR: " + e.getMessage());
        }
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

    }

    void wechsle_fragment_zu_ConnectedFragmentStrom()
    {
        if(!isFinishing() && !isDestroyed())
        {
            ConnectedFragmentStrom fragment = new ConnectedFragmentStrom();       // Neues Fragment erstellen

            Bundle args = new Bundle();
            args.putIntegerArrayList(ConnectedFragmentStrom.ARRAY_STROM, StromListe);
            args.putStringArrayList(ConnectedFragmentStrom.ARRAY_STROM_TIMESTAMP, StromListe_Time); // Format HH:mm:ss
            fragment.setArguments(args);                                            // und hinzufügen

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, fragment);  // Ansicht wechseln
            transaction.addToBackStack(null);
            transaction.commit();
            AktivesFragment = Fragmente.CONNECTED_FRAGMENT_STROM;
            
            try
            {
                navigationView.getMenu().getItem(1).setChecked(true);
            }catch (Exception e)
            {
                Log.i(TAG, "wechsle_fragment_zu_ConnectedFragmentStrom: ERROR:  " + e.getMessage());
            }

            if(verbunden)
            {
                Timer_verivication_delay.schedule(new TimerTask()
                {
                    @Override
                    public void run() {
                        // ToDo: Strom anfragen -> Frequenziell und passend iwan beenden
                        byte[] sende_byte = new byte[1];            // Für Strom anmelden
                        sende_byte[0] = 2;      // Frequenziell
                        Services.Characteristic_Current.setValue(sende_byte);
                        BLE_Characteristic_temp = Services.Characteristic_Current;  // Zur verifikation
                        gesendete_characteristic = Characteristicen.CHARACTERISTIC_STROM;
                        mBluetoothGatt.writeCharacteristic(Services.Characteristic_Current);
                        Log.i(TAG, "Für Strom angemeldet");
                    }
                }, 30); // 30 ms delay

            }

            Log.i(TAG, "wechsle_fragment_zu_ConnectedFragmentStrom: fertig");
        }
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    void wechsle_fragment_zu_Battery_Details()
    {
        if(!isFinishing() && !isDestroyed())
        {
            Fragment_Battery_Details fragment = new Fragment_Battery_Details();       // Neues Fragment erstellen

            Bundle args = new Bundle();
            args.putInt(Fragment_Battery_Details.EXTRA_BATTERY_LEVEL, Battery_Level);      // BatterieLevel Übergeben
            args.putInt(Fragment_Battery_Details.EXTRA_BATTERY_TEMPERATURE, Battery_Temperature);
            args.putInt(Fragment_Battery_Details.EXTRA_BATTERY_HEALTH, Battery_Health);
            args.putInt(Fragment_Battery_Details.EXTRA_BATTERY_VOLTAGE, Battery_Voltage);
            args.putString(Fragment_Battery_Details.EXTRA_BATTERY_TECHNOLOGY, Battery_Technology);
            args.putInt(Fragment_Battery_Details.EXTRA_BATTERY_STATUS, Battery_Status);
            fragment.setArguments(args);                                            // und hinzufügen

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, fragment);  // Ansicht wechseln
            transaction.addToBackStack(null);
            transaction.commit();
            AktivesFragment = Fragmente.BATTERY_DETAILS;
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    void wechsle_fragment_zu_settings()
    {
        Fragment_Settings fragment = new Fragment_Settings();       // Neues Fragment erstellen

        //Name_BT_Device = NameAuslesen oder so();      // Name des Bluetoothdevices auslesen

        Bundle args = new Bundle();
        args.putString(Fragment_Settings.NAME_BT_DEVICE, Name_BT_Device);      // Name des BT Device übergeben
        args.putBoolean(Fragment_Settings.SETTINGS_LED_ON, BT_Device_LED_an);
        args.putBoolean(Fragment_Settings.SETTINGS_BACKGROUND_ANIMATION_ON, Background_Animation_Battery);
        args.putBoolean(Fragment_Settings.STATUS_VERBUNDEN, verbunden);
        args.putBoolean(Fragment_Settings.ENTWICKLEROPTIONEN, Entwickleroptionen_enable);
        args.putByte(Fragment_Settings.HYSTERESE, Max_Battery_Level_Hysterese);
        fragment.setArguments(args);                                            // und hinzufügen

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment_container, fragment);  // Ansicht wechseln
        transaction.addToBackStack(null);
        transaction.commit();
        AktivesFragment = Fragmente.SETTINGS;

        navigationView.getMenu().getItem(2).setChecked(true);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    void wechsle_fragment_zu_loadingScreen()
    {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Fragment_Loading_Screen()).commit();
        AktivesFragment = Fragmente.LOADING_SCREEN;
    }

    void Verbindung_Verloren_reset()      // Wechsle wieder zum Startbildschirm
    {
        Log.i(TAG, "Verbindung_Verloren_reset: starte reset");
        if(mBluetoothGatt != null)
            mBluetoothGatt.close();         // Gatt schließen
        mBluetoothGatt = null;
        mBluetoothDevice = null;
        verbunden = false;
        Name_BT_Device = "Name Unbekannt";
        
        Services.reset();

        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);        // Schließt den drawer
        }
        wechsle_fragment_zu_Fragment_Verbinden();
    }

    /*
    Bei "false" sedet die Funktion das Ausschalten-Signal, bei "true" das Einschalten-Signal
    Sendet nur, wenn auch nötig
     */
    public void sende_laden_ein_ausschalten(boolean laden_ein)
    {
        Log.i(TAG, "sende_laden_ein_ausschalten: " + laden_ein);
        if(verbunden)
        {
           // if(ladezustand_aktuell_laden_an != laden_ein)       // Wenn der gewünschte Zustand nich schon da ist  // TODO: erkennung, ob strom auch aus/an geschaltet wurde. (Wenn die Platine fertig ist)
            {
                Log.i(TAG, "sende_laden_ein_ausschalten: schaltet um: " + laden_ein);
                byte[] sende_byte = new byte[1];
                if(laden_ein)   sende_byte[0] = 1;                      // Einschalten
                else            sende_byte[0] = 0;                      // Ausschalten

                Services.Characteristic_Charge_State.setValue(sende_byte);
                gesendete_characteristic = Characteristicen.CHARACTERISTIC_LADE_EIN_AUS;
                if(sende_BLE_Characteristic_ReliableWrite(Services.Characteristic_Charge_State))
                {
                    Log.i(TAG, "sende_laden_ein_ausschalten: gesendet");
                }
                else
                {
                    Log.i(TAG, "sende_laden_ein_ausschalten: Fehler beim senden");
                }
            }
        }
        else
        {
            Log.i(TAG, "sende_laden_ein_ausschalten: Nicht mit Gerät verbunden");
            Toast.makeText(getApplicationContext(), "Nicht mit Gerät verbunden", Toast.LENGTH_SHORT).show();
        }
    }

    public void sende_Battery_Level(byte Level)     // Für tests public -> kann aus ConnectedFragmentMain aufgerufen werden
    {
        if(verbunden)
        {
            byte Level_255 = (byte)(Level * 2.55);
            Log.i(TAG, "sende_Battery_Level: " + Level + " ( " + Level_255 + " wird gesendet )");
            byte[] sende_byte = new byte[1];
            sende_byte[0] = Level;
            Services.Characteristic_Akku_Level.setValue(sende_byte);
            gesendete_characteristic = Characteristicen.CHARACTERISTIC_AKKU_LEVEL;

            if(sende_BLE_Characteristic_ReliableWrite(Services.Characteristic_Akku_Level)) {
                Log.i(TAG, "onReceive: Akkustand gesendet");
            }
            else {
                Log.i(TAG, "onReceive: Akkustand nicht gesendet. ERROR");
            }
        }
    }

    void sende_einstellungen_bei_verbunden()
    {
        if(verbunden)
        {
            Log.i(TAG, "sende_einstellungen_bei_verbunden");
            // LED -----------------------------
            byte[] sende_byte = new byte[1];
            sende_byte[0] = (byte)(BT_Device_LED_an == true ? 1 : 0);
            Services.Characteristic_LED.setValue(sende_byte);

            gesendete_characteristic = Characteristicen.CHARACTERISTIC_LED_AN_AUS;
            sende_BLE_Characteristic_ReliableWrite(Services.Characteristic_LED);


            if(Max_Battery_Level < Battery_Level)
            {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sende_laden_ein_ausschalten(false); // Aus
                    }
                }, 400);
            }

            Timer timer2 = new Timer();
            timer2.schedule(new TimerTask() {
                @Override
                public void run() {
                    sende_Battery_Level((byte)Battery_Level);
                }
            }, 600);
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Ein Fehler ist aufgetreten", Toast.LENGTH_SHORT).show();
        }
    }


    // --------------------------------------- BroadcastReceiver -------------------------------------------
    // -----------------------------------------------------------------------------------------------------
    // Create a BroadcastReceiver for STATE_CHANGED.
    private final BroadcastReceiver mBluetoothStateChanged = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))   // Wenn sich der Status geändert hat
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);     // in state wird der Status des BluetoothAdapters gespeichert

                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if(AktivesFragment == Fragmente.FRAGMENT_VERBINDEN)                                                 // An aktives Fragment übergeben
                {
                    try{((FragmentVerbinden)fragment).Bluetooth_State_Changed(state);}
                    catch(Exception e) {Toast.makeText(getApplicationContext(), "Es ist ein Fehler aufgetreten", Toast.LENGTH_SHORT).show();}
                }
                else if(verbunden)              // Wenn sich der Bluetoothstatus ändert, wärend vrebunden ist. -> trennen (Bluetooth wird deaktiviert, also wird trennen nicht klappen)
                {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            Verbindung_Verloren_reset();
                        }
                    });
                }
                switch(state)
                {
                    case BluetoothAdapter.STATE_OFF:
                            mBluetoothStatus = BluetoothStatus.OFF;
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mBluetoothStatus = BluetoothStatus.TURNING_OFF;
                        break;

                    case BluetoothAdapter.STATE_ON:
                        mBluetoothStatus = BluetoothStatus.ON;
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        mBluetoothStatus = BluetoothStatus.TURNING_ON;
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int Battery_Status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            Battery_Level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

            if(fragment instanceof Fragment_Battery_Details)
            {
                ((Fragment_Battery_Details)fragment).change_Values(intent);   // Übergibt den Intent an Battery_Details
            }

            if(Battery_Status == BatteryManager.BATTERY_STATUS_DISCHARGING)        // Wenn Akku entlädt
            {
                Log.i(TAG, "onReceive: Akku entlädt");
                ladezustand_aktuell_laden_an = false;

            }
            else if(Battery_Status == BatteryManager.BATTERY_STATUS_CHARGING)   // Wenn Akku lädt
            {
                Log.i(TAG, "onReceive: Akku lädt");
                ladezustand_aktuell_laden_an = true;
            }

            if(fragment instanceof ConnectedFragmentMain)
            {
                ((ConnectedFragmentMain)fragment).setBattery_charging_status(ladezustand_aktuell_laden_an);      // Ladezustand übergeben
            }

            if((Battery_Level != -1) && (Battery_Level != Battery_Level_alt))   // Akkustand verändert
            {
                if(verbunden)
                {
                    if((Battery_Level > Max_Battery_Level) || ( (Max_Battery_Level == 100) && (Battery_Status == BatteryManager.BATTERY_STATUS_FULL) )) // Ein Prozent über Max_Level
                    {                                                       // Dass volle [Max_Level] % (außer bei 100)
                        Log.i(TAG, "onReceive: Laden aus");
                        sende_laden_ein_ausschalten(false);     // Laden aus
                    }
                    else if((Battery_Level <= (Max_Battery_Level - Max_Battery_Level_Hysterese)))
                    {                                                       // Nach Hysterese wieder an
                        Log.i(TAG, "onReceive: Laden an");
                        sende_laden_ein_ausschalten(true);      // Laden an
                    }
                    // Sende Akkustand an Microcontroller
                    Log.i(TAG, "onReceive: Sende Akku Level");
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            sende_Battery_Level((byte)Battery_Level);
                        }
                    }, 200);
                }

                if(fragment instanceof FragmentVerbinden)
                {
                    ((FragmentVerbinden) fragment).changeBatteryLevel(Battery_Level);
                }

                // Hintergrund
                fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container_Battery_Image);     // fragment gewechselt !!!
                if(fragment instanceof Fragment_Battery_Image)
                {
                    ((Fragment_Battery_Image)fragment).onBatteryLevelChange(Battery_Level);
                }

                Battery_Level_alt = Battery_Level;
            }
        }
    };
}