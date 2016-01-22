package cl.hxl.smartbike.fragments;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import cl.hxl.smartbike.R;
import cl.hxl.smartbike.connection.BluetoothClientConn;

public class RealtimeChartFragment extends Fragment {
    private LineChart mchart;
    private int datoBT;

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;

    final byte delimiter = 33;  //This is the ASCII code for a newline character
    int readBufferPosition = 0;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Handler handler = new Handler();

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

        AdapterBT();
        setData();

        return mainView;
    }

    private void setData() {
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

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
            //data.addEntry(new Entry((float) (Math.random() * 40) + 30f, set.getEntryCount()), 0);
            data.addEntry(new Entry(datoBT, set.getEntryCount()), 0);

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
                            (new Thread(new workerThread("1"))).start();
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

    public void sendBtMsg(String msg2send) {
        String msg = msg2send;
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID

        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()) {
                mmSocket.connect();
            }
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes()); //Método getBytes para algún string, lo convierte en una secuencia de bytes

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class workerThread implements Runnable {
        private String btMsg;

        public workerThread(String msg) {
            btMsg = msg;
        }

        public void run() {
            sendBtMsg(btMsg);
            while(!Thread.currentThread().isInterrupted()) {
                int bytesAvailable;
                boolean workDone = false;

                try {
                    InputStream mmInputStream;
                    mmInputStream = mmSocket.getInputStream();
                    bytesAvailable = mmInputStream.available();

                    if(bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        Log.e("SmartBike recv bt","bytes available");
                        byte[] readBuffer = new byte[1024];
                        mmInputStream.read(packetBytes);

                        for(int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if(b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                //The variable data now contains our full command
                                handler.post(new Runnable() {
                                    public void run() {
                                        //myLabel.setText(data);
                                        datoBT = Integer.parseInt(data);
                                    }
                                });

                                workDone = true;
                                break;
                            }
                            else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }

                        if (workDone == true){
                            mmSocket.close();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void AdapterBT(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        //Si el bluetooth no está habilitado, pide al usuario habilitarlo
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        //Si existen dispositivos vinculados
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("SmartBike"))
                {
                    Log.e("Conectado a", device.getName());
                    mmDevice = device;
                    break;
                }
            }
        }
    }

}
