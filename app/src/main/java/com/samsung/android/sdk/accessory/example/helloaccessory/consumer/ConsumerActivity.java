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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.github.mikephil.charting.components.ComponentBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

public class ConsumerActivity extends Activity implements OnChartGestureListener {
    private static TextView mTextView;
    private static MessageAdapter mMessageAdapter;
    private boolean mIsBound = false;
    private ListView mMessageListView;
    private ConsumerService mConsumerService = null;

    private PieChart mChart;
    //private PieChart mChart2;
    //private Typeface tf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.tvStatus);
        mMessageListView = (ListView) findViewById(R.id.lvMessage);
        mMessageAdapter = new MessageAdapter();
        mMessageListView.setAdapter(mMessageAdapter);
        // Bind service
        mIsBound = bindService(new Intent(ConsumerActivity.this, ConsumerService.class), mConnection, Context.BIND_AUTO_CREATE);

        //Pie Chart
        mChart = (PieChart) findViewById(R.id.chart1);
        mChart.setHardwareAccelerationEnabled(true);
        mChart.setUsePercentValues(true);
        mChart.setDescription("");
        mChart.setExtraOffsets(5, 10, 5, 5);

        //mChart.setDragDecelerationFrictionCoef(0.95f);

        //tf = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");

        //mChart.setCenterTextTypeface(Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf"));
        //mChart.setCenterText(generateCenterSpannableText());

        mChart.setDrawHoleEnabled(false);
        //mChart.setHoleColor(Color.WHITE);

        //mChart.setTransparentCircleColor(Color.WHITE);
        //mChart.setTransparentCircleAlpha(110);

        //mChart.setHoleRadius(58f);
        //mChart.setTransparentCircleRadius(61f);

        //mChart.setDrawCenterText(true);

        mChart.setRotationAngle(180);
        // enable rotation of the chart by touch
        mChart.setRotationEnabled(false);
        mChart.setHighlightPerTapEnabled(false);

        // mChart.setUnit(" €");
        // mChart.setDrawUnitsInChart(true);

        // add a selection listener
        //mChart.setOnChartValueSelectedListener(this);

        // add a gesture listener
        mChart.setOnChartGestureListener(this);

        setData(3, 100);

        mChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);
        // mChart.spin(2000, 0, 360);

        Legend l = mChart.getLegend();
        /*l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);*/
        l.setEnabled(false);

    }

    @Override
    protected void onDestroy() {
        // Clean up connections
        if (mIsBound == true && mConsumerService != null) {
            if (mConsumerService.closeConnection() == false) {
                updateTextView("Disconnected");
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

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.buttonConnect: {
                if (mIsBound == true && mConsumerService != null) {
                    mConsumerService.findPeers();
                }
                break;
            }
            case R.id.buttonDisconnect: {
                if (mIsBound == true && mConsumerService != null) {
                    if (mConsumerService.closeConnection() == false) {
                        updateTextView("Disconnected");
                        Toast.makeText(getApplicationContext(), R.string.ConnectionAlreadyDisconnected, Toast.LENGTH_LONG).show();
                        mMessageAdapter.clear();
                    } else {
                        Toast.makeText(getApplicationContext(), "Disconnecting", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
            case R.id.buttonSend: {
                if (mIsBound == true && mConsumerService != null) {
                    if (mConsumerService.sendData("Hello Accessory!")) {
                    } else {
                        Toast.makeText(getApplicationContext(), "Not connected: unable to send", Toast.LENGTH_LONG).show();
                    }
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
            updateTextView("onServiceConnected");
            Toast.makeText(getApplicationContext(), "Connected to Service", Toast.LENGTH_LONG).show();

            //Auto connect to device when app is launched.
            mConsumerService.findPeers();
            Toast.makeText(getApplicationContext(), "Auto Connecting", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConsumerService = null;
            mIsBound = false;
            updateTextView("onServiceDisconnected");
            Toast.makeText(getApplicationContext(), "Disonnected from Service", Toast.LENGTH_LONG).show();
        }
    };

    public static void addMessage(String data) {
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
                    if (mMessages.size() == MAX_MESSAGES_TO_DISPLAY) {
                        mMessages.remove(0);
                        mMessages.add(msg);
                    } else {
                        mMessages.add(msg);
                    }
                    notifyDataSetChanged();
                    mMessageListView.setSelection(getCount() - 1);
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

    private void setData(int count, float range) {

        float mult = range;

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();

        // IMPORTANT: In a PieChart, no values (Entry) should have the same
        // xIndex (even if from different DataSets), since no values can be
        // drawn above each other.
        /*for (int i = 0; i < count + 1; i++) {
            yVals1.add(new Entry((float) (Math.random() * mult) + mult / 5, i));
        }*/
        yVals1.add(new Entry((float) 20, 1));
        yVals1.add(new Entry((float) 80, 2));


        ArrayList<String> xVals = new ArrayList<String>();

        /*for (int i = 0; i < count + 1; i++)
            xVals.add(mParties[i % mParties.length]);*/
        xVals.add("Unhealthy");
        xVals.add("Healthy");

        PieDataSet dataSet = new PieDataSet(yVals1, "Election Results");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // add a lot of colors

        ArrayList<Integer> colors = new ArrayList<Integer>();

        /*for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());*/
        colors.add(Color.RED);
        colors.add(Color.GREEN);

        dataSet.setColors(colors);
        //dataSet.setSelectionShift(0f);

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

}
