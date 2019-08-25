package com.example.chargecontrol;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

public class Orientation implements SensorEventListener
{
    String TAG = "TAG_Orientation";

    Context context;

    float[] gravity;
    private SensorManager sensorManager;
    private Sensor sensor;

    float angle_to_ground = 0;      // normal Halten = 0 -> positiv gegen Uhrzeigersinn
    float angle_to_ground_old = 0;

    boolean active;

    /* Interfacevariable, in der die Funktion ist, mit der der Activity Daten übergeben werden können */
    Interface_Orientation_Callback mCallback;

    // Interface. Muss in der Activity eingebunden werden (implements)
    public interface Interface_Orientation_Callback
    {
        void change_Orientation(float angle);
        // ... weitere Methoden ...
    }

    Orientation(Context _context, Activity activity)
    {
        mCallback = (Interface_Orientation_Callback) activity;
        active = false;
        context = _context;
        gravity = new float[3];
        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    void activate()
    {
        if(!active)
        {
            active = true;
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    void deaktivate()
    {
        if(active)
        {
            active = false;
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        //Log.i(TAG, "onSensorChanged:");
        if(event.sensor.getType() == Sensor.TYPE_GRAVITY)
        {
            gravity = event.values;
            if(gravity != null)
            {
                for(int i = 0; i < 2; i++)
                {
                    if(Math.abs(gravity[i]) < 0.1f) gravity[i] = 0;
                }
                //Log.i(TAG, "berrechne jetzt: " + gravity[0] + "  " + gravity[1] + "  " + gravity[2]);
                berechne_winkel();
                Log.i(TAG, "berrechnet: " + (angle_to_ground * (180/(float)Math.PI) + " Grad"));
               // if(Math.abs(angle_to_ground - angle_to_ground_old) > 0.01f)      // Wenn der Unterschied größer als 1° ist
               // {
                    angle_to_ground_old = angle_to_ground;
                    //Log.i(TAG, "und schicke");
                    mCallback.change_Orientation(angle_to_ground);
                //}
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
    }

    void berechne_winkel()
    {
        if(gravity[0] == 0)
        {
            if(gravity[1] > 0)  angle_to_ground = 0;
            else                angle_to_ground = (float)Math.PI;
        }
        else if(gravity[1] == 0)
        {
            if(gravity[0] > 0)  angle_to_ground = (float)Math.PI / 2;
            else                angle_to_ground = -(float)Math.PI / 2;
        }
        else
        {
            angle_to_ground = (float)Math.atan(Math.abs(gravity[0]) / Math.abs(gravity[1]));   // Winkel zu senktrcht runter
            if(gravity[1] < 0)
                if(angle_to_ground < 0) angle_to_ground -= (2 * ((float)Math.PI / 2 + angle_to_ground));
                else                    angle_to_ground += (2 * ((float)Math.PI / 2 - angle_to_ground));
            if(gravity[0] < 0)  angle_to_ground = -angle_to_ground;                             // Richtiges Vorzeichen
        }
    }
}
