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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;


public class Fragment_Settings extends Fragment implements View.OnClickListener {

    static final String TAG = "Fragment_Settings";

    public static final String NAME_BT_DEVICE = "name_bt_device";
    public static final String SETTINGS_LED_ON = "settings_led_on";
    public static final String SETTINGS_BACKGROUND_ANIMATION_ON = "settings_background_animation_on";
    public static final String STATUS_VERBUNDEN = "status_verbunden";
    public static final String ENTWICKLEROPTIONEN = "ENTWICKLEROPTIONEN_ENABLE";
    public static final String HYSTERESE = "Hysterese";

    EditText Edit_Text_Name;
    Switch Switch_LED_AN;
    Button Button_change_Name;
    Switch Switch_Entwickleroptionen;
    EditText EditText_Hysterese;
    //Switch Switch_BackgroundAnimation;

    String Name_BT_Device;
    Boolean LED_an;
    Boolean Background_Animation_on;
    boolean verbunden = false;
    int hysterese = 0;

    Interface_Settings_Listener mCallback;      //Interfacevariable, in der die Funktion ist, mit der der Activity Daten übergeben werden können

    public void set_Interface_Settings_Listener(Activity activity)   // Funktion, mit der die Activity das Interface deklarieren kann
    {
        mCallback = (Interface_Settings_Listener) activity;
    }

    public interface Interface_Settings_Listener
    {
        // in der Activity implementieren
        void Change_Name(String name);
        void change_switch_LED(boolean LED_on);
        void Change_Background_Animation_state(boolean Hintergrundanimation);
        void Change_Entwickleroptionen(boolean enable);
        void Change_Hysterese(int hysterese);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Edit_Text_Name = view.findViewById(R.id.Edit_Text_Name);
        Switch_LED_AN = view.findViewById(R.id.Settings_switch_LED);
        Switch_Entwickleroptionen = view.findViewById(R.id.Settings_switch_Entwickleroptionen);
        //Switch_BackgroundAnimation = view.findViewById(R.id.Settings_Switch_BackgroundAnimation);
        Button_change_Name = view.findViewById(R.id.Button_Edit_Name);
        EditText_Hysterese = view.findViewById(R.id.EditText_Hysterese);

        Switch_Entwickleroptionen.setChecked(getArguments().getBoolean(ENTWICKLEROPTIONEN, false));
        Switch_LED_AN.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
               // if(LED_an != isChecked)
                {
                    LED_an = isChecked;
                    mCallback.change_switch_LED(isChecked);
                }
            }
        });

        Switch_Entwickleroptionen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                mCallback.Change_Entwickleroptionen(isChecked);
            }
        });
     /*   Switch_BackgroundAnimation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(Background_Animation_on != isChecked)
                {
                    Background_Animation_on = isChecked;
                    mCallback.Change_Background_Animation_state(isChecked);
                }
            }
        }); */
        Button_change_Name.setOnClickListener(this);

        Name_BT_Device = getArguments().getString(NAME_BT_DEVICE, "Name Unbekannt");
        Edit_Text_Name.setText(Name_BT_Device);

        LED_an = getArguments().getBoolean(SETTINGS_LED_ON, true);
        Switch_LED_AN.setChecked(LED_an);
        Background_Animation_on = getArguments().getBoolean(SETTINGS_BACKGROUND_ANIMATION_ON, true);
      //  Switch_BackgroundAnimation.setChecked(Background_Animation_on);


        verbunden = getArguments().getBoolean(STATUS_VERBUNDEN, false);
        Button_change_Name.setClickable(verbunden); // Name ändern ausgrauen, wenn nicht verbunden
        if(!verbunden)
        {
            Button_change_Name.setTextColor(getResources().getColor(R.color.light_grey));
        }

        hysterese = getArguments().getByte(HYSTERESE, (byte)5);
        EditText_Hysterese.setText("" + hysterese);

        return view;
    }

    @Override
    public void onDestroyView()
    {
        try
        {
            hysterese = Integer.parseInt(EditText_Hysterese.getText().toString());
        }
        catch (Exception e)
        {
            Log.i(TAG, "onDestroyView: ERROR bei lesen Hysterese:  " + e.getMessage());
        }
        Log.i(TAG, "onDestroyView: hysterese = " + hysterese);
        mCallback.Change_Hysterese(hysterese);

        super.onDestroyView();
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.Button_Edit_Name:
                if(!Name_BT_Device.equals(Edit_Text_Name.getText().toString()))     // Wenn sich auch was geändert hat
                {
                    if(verbunden)
                    {
                        Name_BT_Device = Edit_Text_Name.getText().toString();
                        mCallback.Change_Name(Name_BT_Device);
                        Toast.makeText(getContext(), "Name geändert", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public void change_switch_LED(boolean state)    // Wird von ausen aufgerufen
    {
        LED_an = state;
        Switch_LED_AN.setChecked(state);
    }
}
