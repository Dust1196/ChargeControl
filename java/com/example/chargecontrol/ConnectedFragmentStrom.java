package com.example.chargecontrol;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


// Hier soll dann der Strom angezeigt werden, sobald er vom Gerät gesendet wurde
public class ConnectedFragmentStrom extends Fragment
{
    static final String TAG = "Fragment_Strom";
    public static final String AKTUELLER_STROM = "aktueller_strom";
    public static final String ARRAY_STROM = "ARRAY_STROM_VERGANGENHEIT";
    public static final String ARRAY_STROM_TIMESTAMP = "ARRAY_STROM_TIMESTAMP";

    GraphView graph_Strom;
    TextView Text_aktueller_Strom;

    int aktueller_strom = 0;

    ArrayList<Integer> StromListe;
    ArrayList<String> StromListe_Time;

    int max_strom;

    DataPoint[] dataPoints;

    SimpleDateFormat dateFormat;

    LineGraphSeries<DataPoint> series;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_strom, container, false);
        graph_Strom = view.findViewById(R.id.GraphView_Strom);
        Text_aktueller_Strom = view.findViewById(R.id.TextView_Strom_aktuellerStrom);

        StromListe_Time = null;
        StromListe = null;

        StromListe = getArguments().getIntegerArrayList(ARRAY_STROM);
        if(StromListe == null)
            StromListe = new ArrayList<>();
        StromListe_Time = getArguments().getStringArrayList(ARRAY_STROM_TIMESTAMP);
        if(StromListe_Time == null)
            StromListe_Time = new ArrayList<>();

        find_max_strom();

        dateFormat = new SimpleDateFormat("HH:mm:ss yyyy");

        series = new LineGraphSeries<>();

        Text_aktueller_Strom.setText(aktueller_strom + " mA");

        dataPoints = new DataPoint[StromListe.size()];

        set_up_Graph();

        return view;
    }

    void set_up_Graph()
    {
        graph_Strom.getViewport().setScalable(true);

       /* Calendar calendar = Calendar.getInstance();
        Date d1 = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        Date d2 = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        Date d3 = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        Date d4 = calendar.getTime();
        calendar.add(Calendar.MINUTE, 1);
        Date d5 = calendar.getTime();


        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]
                {
                        new DataPoint(d1, 0),
                        new DataPoint(d2, 2),
                        new DataPoint(d3, 2),
                        new DataPoint(d4, 1),
                        new DataPoint(d5, 4)
                });*/

       if(!StromListe.isEmpty())
       {
           setup_series();

           series.resetData(dataPoints);

           series.setDrawDataPoints(true);
           series.setDataPointsRadius(10);
           series.setDrawBackground(true);
           graph_Strom.addSeries(series);

           // set date label formatter
           //graph_Strom.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity()));

           graph_Strom.getGridLabelRenderer().setHumanRounding(false);
           //graph_Strom.getViewport().setYAxisBoundsManual(true);
           graph_Strom.getViewport().setMinY(0);
           graph_Strom.getViewport().setMaxY(max_strom);

           graph_Strom.getViewport().setMinX(0);
           graph_Strom.getViewport().setMaxX(StromListe.size() - 1);
       }
    }

    void setup_series()
    {
        // ToDo: bei vielen Werten Mittelwert aus 10 oder so berechnen und anzeigen

        for (int i = 0; i < StromListe.size(); i++) {
            try {
                Date date = dateFormat.parse(StromListe_Time.get(i));
                //Log.i(TAG, "set_up_Graph: forschleife durchgang: " + i);
                //Log.i(TAG, "set_up_Graph: Strom: " + StromListe.get(i) + " Date: " + date);
                dataPoints[i] = new DataPoint(i, StromListe.get(i));
            } catch (ParseException e) {
                Log.i(TAG, "set_up_Graph_test: ERROR String -> Date: " + e.getMessage());
                Log.i(TAG, "set_up_Graph_test: nochmal: " + e.getLocalizedMessage());
            }
        }
    }

    void find_max_strom()
    {
        max_strom = 0;
        if(!StromListe.isEmpty())
        {
            for(int i = 0; i < StromListe.size(); i++)
            {
                if(max_strom < StromListe.get(i))   max_strom = StromListe.get(i);
            }
        }
    }

    public void set_aktueller_Strom(int strom_in_mA)
    {
        aktueller_strom = strom_in_mA;
        Text_aktueller_Strom.setText(aktueller_strom + " mA");

        if(aktueller_strom > max_strom) max_strom = aktueller_strom;    // Neuer max. Strom

        // StromListe und StromListe_Time wird von MainActivity gesetzt (Ist zeiger)

        set_up_Graph();
    }
}
