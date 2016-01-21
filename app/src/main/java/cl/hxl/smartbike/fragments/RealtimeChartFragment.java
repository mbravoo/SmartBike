package cl.hxl.smartbike.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

import cl.hxl.smartbike.R;
import cl.hxl.smartbike.connection.BluetoothClientConn;

public class RealtimeChartFragment extends Fragment {
    LineChart mchart;

    public RealtimeChartFragment() {
        // Required empty public constructor
    }

    public static RealtimeChartFragment newInstance() {
        RealtimeChartFragment fragment = new RealtimeChartFragment();
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mainView = inflater.inflate(R.layout.fragment_realtime_chart, null);

        mchart = (LineChart) mainView.findViewById(R.id.realtime_chart);

        setData();

        return mainView;
    }

    private void setData() {
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // set data
        mchart.setData(data);
        YAxis rightAxis = mchart.getAxisRight();
        rightAxis.setEnabled(false);

        feedMultiple();
    }

    private void addEntry() {

        LineData data = mchart.getData();

        if (data != null) {

            LineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            // add a new x-value first
            data.addXValue("T");
            data.addEntry(new Entry((float) (Math.random() * 40) + 30f, set.getEntryCount()), 0);

            // let the chart know it's data has changed
            mchart.notifyDataSetChanged();

            // limit the number of visible entries
            mchart.setVisibleXRangeMaximum(30);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mchart.moveViewToX(data.getXValCount() - 30);

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Potencia utilizada");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private void feedMultiple() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(getActivity() == null)
                        return;

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry();
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
