package com.example.chargecontrol;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.Random;
import java.util.UUID;

public class Custom_BLE_Services
{
    static final String TAG = "Custom_BLE_Services";

    public UUID UUID_Service_Verification = UUID.fromString("C0DE1D00-0815-DEAD-BEEF-C0DEDCAB1E00");
    public UUID UUID_Characteristic_hallo = UUID.fromString("C0DE1D01-0815-DEAD-BEEF-C0DEDCAB1E00");

    public UUID UUID_Service_Main = UUID.fromString("C0DE1D10-0815-DEAD-BEEF-C0DEDCAB1E00");
    public UUID UUID_Characteristic_Charge_State = UUID.fromString("C0DE1D11-0815-DEAD-BEEF-C0DEDCAB1E00");
    public UUID UUID_Characteristic_Current = UUID.fromString("C0DE1D12-0815-DEAD-BEEF-C0DEDCAB1E00");

    public UUID UUID_Service_Settings = UUID.fromString("C0DE1D20-0815-DEAD-BEEF-C0DEDCAB1E00");
    public UUID UUID_Characteristic_LED = UUID.fromString("C0DE1D21-0815-DEAD-BEEF-C0DEDCAB1E00");
    public UUID UUID_Characteristic_Name = UUID.fromString("C0DE1D22-0815-DEAD-BEEF-C0DEDCAB1E00");

    public UUID UUID_Service_Special = UUID.fromString("C0DE1D30-0815-DEAD-BEEF-C0DEDCAB1E00");
    public UUID UUID_Characteristic_Akku_Level = UUID.fromString("C0DE1D31-0815-DEAD-BEEF-C0DEDCAB1E00");


    public BluetoothGattService Service_Verification;
    public BluetoothGattService Service_Main;
    public BluetoothGattService Service_Settings;
    public BluetoothGattService Service_Special;


    public BluetoothGattCharacteristic Characteristic_hallo;
    public BluetoothGattCharacteristic Characteristic_Charge_State;
    public BluetoothGattCharacteristic Characteristic_Current;
    public BluetoothGattCharacteristic Characteristic_LED;
    public BluetoothGattCharacteristic Characteristic_Name;
    public BluetoothGattCharacteristic Characteristic_Akku_Level;

    Random random;
    byte key;

    Custom_BLE_Services()
    {
        random = new Random();
        key = (byte)random.nextInt(82);

    }

    public boolean on_Services_Discovered(BluetoothGatt gatt)
    {
        boolean success = true;

        Service_Verification = gatt.getService(UUID_Service_Verification);
        Service_Main = gatt.getService(UUID_Service_Main);
        Service_Settings = gatt.getService(UUID_Service_Settings);
        Service_Special = gatt.getService(UUID_Service_Special);

        Characteristic_hallo = Service_Verification.getCharacteristic(UUID_Characteristic_hallo);

        Characteristic_Charge_State = Service_Main.getCharacteristic(UUID_Characteristic_Charge_State);
        Characteristic_Current = Service_Main.getCharacteristic(UUID_Characteristic_Current);

        Characteristic_LED = Service_Settings.getCharacteristic(UUID_Characteristic_LED);
        Characteristic_Name = Service_Settings.getCharacteristic(UUID_Characteristic_Name);

        Characteristic_Akku_Level = Service_Special.getCharacteristic(UUID_Characteristic_Akku_Level);

        return success;
    }

    public boolean activate_Notification_hallo(BluetoothGatt gatt)
    {
        boolean success = false;
        BluetoothGattDescriptor descriptor;
        Log.i(TAG, "activate_Notification_hallo: starte");
        boolean temp = gatt.setCharacteristicNotification(Characteristic_hallo, true);        // Anmelden für Verification
        Log.i(TAG, "activate_Notification_hallo: gatt.setCharacteristicNotification() = " + temp);
        descriptor = Characteristic_hallo.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));   // Ist eine Standard UUID
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        success = true;
        Log.i(TAG, "activate_Notification_hallo: fertig");
        return success;
    }

    /*public boolean deactivate_Notification_hallo(BluetoothGatt gatt)
    {
        boolean temp;
        boolean success = false;
        BluetoothGattDescriptor descriptor;
        Log.i(TAG, "deactivate_Notification_hallo: starte");
        temp = gatt.setCharacteristicNotification(Characteristic_hallo, false);        // Anmelden für Verification
        Log.i(TAG, "deactivate_Notification_hallo: gatt.setCharacteristicNotification() = " + temp);
        descriptor = Characteristic_hallo.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));   // Ist eine Standard UUID
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        success = true;
        Log.i(TAG, "deactivate_Notification_hallo: fertig");
        return success;
    }*/

    public boolean activate_Notification_current(BluetoothGatt gatt)
    {
        boolean success = true;
        BluetoothGattDescriptor descriptor;

        Log.i(TAG, "activate_Notification_current: starte");
        gatt.setCharacteristicNotification(Characteristic_Current, true);        // Anmelden für Strom

        descriptor = Characteristic_Current.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));   // Ist eine Standard UUID
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        Log.i(TAG, "activate_Notification_current: fertig");

        return success;
    }


    void send_verification(BluetoothGatt gatt)
    {
        Log.i(TAG, "send_verification: starte");
        byte[] sende = new byte[1];
        sende[0] = key;
        try
        {
            Characteristic_hallo.setValue(sende);
            gatt.writeCharacteristic(Characteristic_hallo);
        }catch(Exception e)
        {
            Log.i(TAG, "send_verification: " + e.getMessage().toString());
        }
        key += 3;
        key *= 3;
        Log.i(TAG, "send_verification: fertig");
    }

    boolean check_verification(byte answer)
    {
        return (key == answer);
    }

    void reset()
    {
        Service_Verification = null;
        Service_Main = null;
        Service_Settings = null;
        Service_Special = null;

        Characteristic_hallo = null;
        Characteristic_Charge_State = null;
        Characteristic_Current = null;
        Characteristic_LED = null;
        Characteristic_Name = null;
        Characteristic_Akku_Level = null;

        key = (byte)random.nextInt(82);     // Neuen Key erzeugen
    }

}
