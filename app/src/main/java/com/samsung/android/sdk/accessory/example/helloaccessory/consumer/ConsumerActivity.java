/*
 * Copyright (c) 2015 Samsung Electronics Co., Ltd. All rights reserved. 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that 
 * the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice, 
 *       this list of conditions and the following disclaimer. 
 *     * Redistributions in binary form must reproduce the above copyright notice, 
 *       this list of conditions and the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution. 
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse or 
 *       promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.samsung.android.sdk.accessory.example.helloaccessory.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

public class ConsumerActivity extends AppCompatActivity implements OnChartGestureListener {
    private static TextView mTextView;
    private static MessageAdapter mMessageAdapter;
    private boolean mIsBound = false;
    private ListView mMessageListView;
    private ConsumerService mConsumerService = null;

    Toolbar toolbar;

    private PieChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTextView = (TextView) findViewById(R.id.tvStatus);
        mMessageListView = (ListView) findViewById(R.id.lvMessage);
        mMessageAdapter = new MessageAdapter();
        mMessageListView.setAdapter(mMessageAdapter);
        // Bind service
        mIsBound = bindService(new Intent(ConsumerActivity.this, ConsumerService.class), mConnection, Context.BIND_AUTO_CREATE);

        //Open Shared Pref for saving data
        SharedPreferences sharedPrefStore = getPreferences(Context.MODE_PRIVATE);

        //Draw Pie Chart
        mChart = (PieChart) findViewById(R.id.chart1);
        mChart.setHardwareAccelerationEnabled(true);
        mChart.setUsePercentValues(true);
        mChart.setDescription("");
        mChart.setExtraOffsets(5, 10, 5, 5);
        //Disable the hole
        mChart.setDrawHoleEnabled(false);
        //Align slices correctly
        mChart.setRotationAngle(270);
        // enable rotation of the chart by touch
        mChart.setRotationEnabled(false);
        mChart.setHighlightPerTapEnabled(false);
        // add a selection listener
        //mChart.setOnChartValueSelectedListener(this);
        // add a gesture listener
        mChart.setOnChartGestureListener(this);
        //Set data for chart
        setChartData(sharedPrefStore);
        //Cool spinny thing when loading
        mChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);
        //Disable Legend
        Legend l = mChart.getLegend();
        l.setEnabled(false);

    }

    @Override
    protected void onDestroy() {
        // Clean up connections
        if (mIsBound == true && mConsumerService != null) {
            if (mConsumerService.closeConnection() == false) {
                //updateTextView("Disconnected");
                Toast.makeText(getApplicationContext(), "onDestroy cleaning up", Toast.LENGTH_LONG).show();
                mMessageAdapter.clear();
            }
        }
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
            //Toast.makeText(getApplicationContext(), "onDestroy un-bind service", Toast.LENGTH_LONG).show();
        }
        super.onDestroy();
    }

    public class SavedData {
        public int healthy = 0;
        public int unhealthy = 0;
        //constructor
        public SavedData(int a, int b) {
            healthy = a;
            unhealthy = b;
        }
    }

    private SavedData getValues (){
        //Open Share Pref
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        //Should be default or updated if received info from gear or if previous run of app
        int defaultHealthyValue = getResources().getInteger(R.integer.saved_healthy_default);
        int defaultUnhealthyValue = getResources().getInteger(R.integer.saved_unhealthy_default);
        int healthyValue = sharedPref.getInt(getString(R.string.saved_healthy_value), defaultHealthyValue);
        int unHealthyValue = sharedPref.getInt(getString(R.string.saved_unhealthy_value), defaultUnhealthyValue);
        Log.i("healthyValue", "healthyValue(default): " + healthyValue);
        Log.i("unHealthyValue", "unHealthyValue(default): " + unHealthyValue);

        //Instantiate SavedData object
        SavedData savedValues = new SavedData(healthyValue,unHealthyValue);
        return  savedValues;

    }
    public void mOnClick(View v) {
        SavedData currentData;
        //Open Share Pref
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        switch (v.getId()) {
            case R.id.buttonIncHe: {
                if (mIsBound == true && mConsumerService != null) {

                    //Get Saved Values
                    currentData = getValues();
                    //Pass result to setChartData
                    setChartData( sharedPref, currentData.healthy + 1, currentData.unhealthy);

                    //send update to gear
                    sendData(sharedPref);
                }
                break;
            }
            case R.id.buttonIncUn: {
                if (mIsBound == true && mConsumerService != null) {

                    //Get Saved Values
                    currentData = getValues();
                    //Pass result to setChartData
                    setChartData( sharedPref, currentData.healthy, currentData.unhealthy + 1);

                    //send update to gear
                    sendData(sharedPref);
                }
                break;
            }
            default:
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConsumerService = ((ConsumerService.LocalBinder) service).getService();
            //updateTextView("onServiceConnected");
            //Toast.makeText(getApplicationContext(), "Connected to Service", Toast.LENGTH_LONG).show();

            //Auto connect to device when app is launched.
            //mConsumerService.findPeers();
            //Toast.makeText(getApplicationContext(), "Auto Connecting", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConsumerService = null;
            mIsBound = false;
            //updateTextView("onServiceDisconnected");
            Toast.makeText(getApplicationContext(), "Disonnected from Service", Toast.LENGTH_LONG).show();
        }
    };

    public static void addMessage(String data) {

        //Need to pass to Message Adapter because of static method
        mMessageAdapter.addMessage(new Message(data));
    }

    public static void updateTextView(final String str) {
        mTextView.setText(str);
    }

    private class MessageAdapter extends BaseAdapter {
        private static final int MAX_MESSAGES_TO_DISPLAY = 20;
        private List<Message> mMessages;

        public MessageAdapter() {
            mMessages = Collections.synchronizedList(new ArrayList<Message>());
        }

        void addMessage(final Message msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    String data = msg.data;
                    //Parse "Received" message from Gear and call setChartData
                    Log.i("receivedGear", "Received(string): " + data);
                    //Received:Healthy=9,Unhealthy=1

                    String delims = "[:]";
                    String[] tokens = data.split(delims);
                    delims = "[,]";
                    tokens = tokens[1].split(delims);
                    delims = "[=]";
                    for (int i=0; i<tokens.length; i++) {
                        String[] bucket = tokens[i].split(delims);
                        tokens[i] = bucket[1];
                    }
                    for (int i=0; i<tokens.length; i++) {
                        System.out.println("Number = " + tokens[i]);
                    }

                    //Open Shared Pref for saving data
                    SharedPreferences sharedPrefsStore = getPreferences(Context.MODE_PRIVATE);

                    //Pass result to setChartData
                    setChartData( sharedPrefsStore, Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]) );


                    //this added messages to message list
                    /*if (mMessages.size() == MAX_MESSAGES_TO_DISPLAY) {
                        mMessages.remove(0);
                        mMessages.add(msg);
                    } else {
                        mMessages.add(msg);
                    }
                    notifyDataSetChanged();
                    mMessageListView.setSelection(getCount() - 1);*/
                }
            });
        }

        void clear() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessages.clear();
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getCount() {
            return mMessages.size();
        }

        @Override
        public Object getItem(int position) {
            return mMessages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View messageRecordView = null;
            if (inflator != null) {
                messageRecordView = inflator.inflate(R.layout.message, null);
                TextView tvData = (TextView) messageRecordView.findViewById(R.id.tvData);
                Message message = (Message) getItem(position);
                tvData.setText(message.data);
            }
            return messageRecordView;
        }
    }

    private static final class Message {
        String data;

        public Message(String data) {
            super();
            this.data = data;
        }
    }

    private void setChartData(SharedPreferences sharedPref, int healthy, int unhealthy) {

        //Toast.makeText(getApplicationContext(), "I am getting called", Toast.LENGTH_LONG).show();

        //Add Values to shared preferences
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.saved_healthy_value), healthy);
        editor.putInt(getString(R.string.saved_unhealthy_value), unhealthy);
        editor.apply();

        Log.i("healthyValue", "healthy: " + healthy);
        Log.i("unHealthyValue", "unHealthy: " + unhealthy);

        //Call to update data in chart
        setChartData(sharedPref);
    }

    private void setChartData(SharedPreferences sharedPref) {
        SavedData currentData;
        //Get Saved Values
        currentData = getValues();


        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        yVals1.add(new Entry(currentData.healthy, 1));
        yVals1.add(new Entry(currentData.unhealthy, 2));

        ArrayList<String> xVals = new ArrayList<String>();
        xVals.add("Healthy");
        xVals.add("Unhealthy");

        PieDataSet dataSet = new PieDataSet(yVals1, "Election Results");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);


        //Add colors for the chart
        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(Color.GREEN);
        colors.add(Color.RED);
        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(20f);
        data.setValueTextColor(Color.BLACK);
        //data.setValueTypeface(tf);
        mChart.setData(data);

        // undo all highlights
        mChart.highlightValues(null);

        mChart.invalidate();
    }


    // INTERFACE OnChartGestureListener Implementation
    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.i("Gesture", "START, x: " + me.getX() + ", y: " + me.getY());
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.i("Gesture", "END, lastGesture: " + lastPerformedGesture);

        // un-highlight values after the gesture is finished and no single-tap
        if(lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP)
            mChart.highlightValues(null); // or highlightTouch(null) for callback to onNothingSelected(...)
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
        Log.i("LongPress", "Chart longpressed.");
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

        Log.i("SingleTap", "Chart single-tapped.");
        Log.i("SingleTap", "Me:" + me);
    }



    // NOT USED BUT HAVE TO IMPLEMENT PER INTERFACE OnChartGestureListener
    @Override
    public void onChartDoubleTapped(MotionEvent me) {
        Log.i("DoubleTap", "Chart double-tapped.");
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        Log.i("Fling", "Chart flinged. VeloX: " + velocityX + ", VeloY: " + velocityY);
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        Log.i("Scale / Zoom", "ScaleX: " + scaleX + ", ScaleY: " + scaleY);
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        Log.i("Translate / Move", "dX: " + dX + ", dY: " + dY);
    }

    private void connect(SharedPreferences sharedPref) {
        if (mIsBound == true && mConsumerService != null) {
            Toast.makeText(getApplicationContext(), "Connecting", Toast.LENGTH_SHORT).show();
            mConsumerService.findPeers();
        }
    }
    private void disconnect(SharedPreferences sharedPref) {
        if (mIsBound == true && mConsumerService != null) {
            if (mConsumerService.closeConnection() == false) {
                Toast.makeText(getApplicationContext(), R.string.ConnectionAlreadyDisconnected, Toast.LENGTH_LONG).show();
                mMessageAdapter.clear();
            } else {
                Toast.makeText(getApplicationContext(), "Disconnecting", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void sendData(SharedPreferences sharedPref) {
        if (mIsBound == true && mConsumerService != null) {
            SavedData currentData;
            //Get Saved Values
            currentData = getValues();
            //Convert to string to send
            String data = "Healthy=" + currentData.healthy + ",Unhealthy=" + currentData.unhealthy;

            if (mConsumerService.sendData(data)) {
                //break;
            } else {
                Toast.makeText(getApplicationContext(), "Not connected: unable to send. Reconnecting...", Toast.LENGTH_LONG).show();
                //Reconnect to gear
                mConsumerService.findPeers();
            }
        }
    }

    private void decHe(SharedPreferences sharedPref) {
        if (mIsBound == true && mConsumerService != null) {
            SavedData currentData;
            //Get Saved Values
            currentData = getValues();

            //Pass result to setChartData
            setChartData( sharedPref, currentData.healthy - 1, currentData.unhealthy);
        }
    }
    private void decUn(SharedPreferences sharedPref) {
        if (mIsBound == true && mConsumerService != null) {
            SavedData currentData;
            //Get Saved Values
            currentData = getValues();

            //Pass result to setChartData
            setChartData( sharedPref, currentData.healthy, currentData.unhealthy - 1);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //Open Share Pref
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        int id = item.getItemId();
        switch (id) {
            case R.id.action_connect:
                //Toast.makeText(getApplicationContext(), "Clicked Connect", Toast.LENGTH_SHORT).show();
                connect(sharedPref);
                break;
            case R.id.action_disconnect:
                //Toast.makeText(getApplicationContext(), "Clicked Disconnect", Toast.LENGTH_SHORT).show();
                disconnect(sharedPref);
                break;
            case R.id.action_send:
                //Toast.makeText(getApplicationContext(), "Clicked Send", Toast.LENGTH_SHORT).show();
                sendData(sharedPref);
                break;
            case R.id.action_negHealthy:
                //Toast.makeText(getApplicationContext(), "Clicked Send", Toast.LENGTH_SHORT).show();
                decHe(sharedPref);
                sendData(sharedPref);
                break;
            case R.id.action_negUnhealthy:
                //Toast.makeText(getApplicationContext(), "Clicked Send", Toast.LENGTH_SHORT).show();
                decUn(sharedPref);
                sendData(sharedPref);
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

}
