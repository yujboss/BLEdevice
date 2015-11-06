/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.widget.SeekBar;
import android.widget.Toast;

import com.example.android.bluetoothlegatt.model.Weather;

import org.json.JSONException;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private TextView switchStatus;
    private Switch mySwitch;

    Timer timer, timer2;
    TimerTask timer_humid, timer_temp;
    String date = "";
    SimpleDateFormat sdf;
    //////////////
    private LinearLayout layoutAnalog;
    private TextView inDoor, outDoor;
    private ProgressBar vProgressBar;
// ///////

    private TextView cityText, temp2, fara2_text, hum, celcius_text, fara_text, date_text, clock_text, humid_text;
    private boolean flag = true;
    private LinearLayout ll,layoutDigital;
    int count = 0;
    FileWriter writer;
    private int[] RGBFrame = {0, 0, 0};
    private String mDeviceAddress;
    String city = "Torino,IT";
    private String mDeviceName;
    float temp = 0;
    float humid = 0;
    //  private ExpandableListView mGattServicesList;
    private WeatherHttpClient.BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    private boolean humidity = true;
    private boolean sleep = true;

    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    //HUMIDITY SERVICE
    private static final UUID HUMIDITY_SERVICE = UUID.fromString("f000aa20-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_DATA_CHAR = UUID.fromString("f000aa21-0451-4000-b000-000000000000");
    private static final UUID HUMIDITY_CONFIG_CHAR = UUID.fromString("f000aa22-0451-4000-b000-000000000000");

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    final Handler handler = new Handler();

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((WeatherHttpClient.BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            //mBluetoothLeService.connect("54:4A:16:25:8E:76");
            // mBluetoothLeService.connect("54:4A:16:25:A7:FF");
            Log.e(TAG, "Unable to initialize Bluetooth");
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (WeatherHttpClient.BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (WeatherHttpClient.BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (WeatherHttpClient.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.d("new", WeatherHttpClient.BluetoothLeService.UUID_HM_RX_TX.toString());
                Log.d("new", "service is connected");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (WeatherHttpClient.BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String sensedData = intent.getStringExtra(mBluetoothLeService.EXTRA_DATA);
                appendDataToBuffer(sensedData);
                //displayData(sensedData);
                Log.d("sensor", "data recieved: " + sensedData);
            }
        }
    };

    private void clearUI() {
        // mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics_layout);
        layoutDigital = (LinearLayout) findViewById(R.id.layoutDigital);
        /////////////////////////////////////////////////////////////////
        layoutAnalog = (LinearLayout) findViewById(R.id.layoutAnalog);
        inDoor = (TextView) findViewById(R.id.inDoorText);
        outDoor = (TextView) findViewById(R.id.outDoorText);
        vProgressBar = (ProgressBar) findViewById(R.id.vprogressbar);
        ////////////////////////////////////////////////////////////////////
        initSwitch();
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

        File root = Environment.getExternalStorageDirectory();
        File gpxfile = new File(root, "sensor_data.csv");
        try {
            writer = new FileWriter(gpxfile);
            writeCsvHeader("Date", "Temperature", "Humidity");
        } catch (IOException e) {
            e.printStackTrace();
        }

        celcius_text = (TextView) findViewById(R.id.celcius_text);
        humid_text = (TextView) findViewById(R.id.humidity_text);
        clock_text = (TextView) findViewById(R.id.clock_text);
        fara_text = (TextView) findViewById(R.id.fara_text);
        date_text = (TextView) findViewById(R.id.date_text);

        Intent gattServiceIntent = new Intent(this, WeatherHttpClient.BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startTimer();
        updateTimeThread();
        cityText = (TextView) findViewById(R.id.cityText);
        temp2 = (TextView) findViewById(R.id.celcius2_text);
        hum = (TextView) findViewById(R.id.humidity2_text);
        fara2_text = (TextView) findViewById(R.id.fara2_text);
    }


    private void writeCsvHeader(String h1, String h2, String h3) throws IOException {
        String line = String.format("%s,%s,%s\n", h1, h2, h3);
        writer.write(line);
    }

    private void writeCsvData(String date, float e, float f) throws IOException {
        String line = String.format("%s,%.0f,%.0f\n", date, e, f);
        writer.write(line);
    }

    private void updateTimeThread() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                java.util.Date noteTS = Calendar.getInstance().getTime();
                                String time = "hh:mm aa"; // 12:00
                                clock_text.setText(DateFormat.format(time, noteTS));
                                String date = "EEE, MMM d"; // 01 January 2013
                                date_text.setText(DateFormat.format(date, noteTS));
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        JSONWeatherTask task;
        task = new JSONWeatherTask();
        task.execute(new String[]{city});
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        //task.cancel(true);
        flag = false;
        stoptimertask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        flag = false;
    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private List<Byte> serialbuffer = new ArrayList<Byte>();

    private void appendDataToBuffer(String data) {
        byte[] rawArray = data.getBytes(Charset.forName("ISO-8859-1")); // Latin1
        for (int i = 0; i < rawArray.length; i++)
            serialbuffer.add(rawArray[i]);
        boolean finished = false;
        while (!finished) {
            // Remove until start

            while ((serialbuffer.size() > 0) && ((serialbuffer.get(0) & 0xFF) != 0xC0)) // UTF8: C3 80
                serialbuffer.remove(0);
            if (serialbuffer.size() <= 0)
                break;
            int indexofstop = -1;

            for (int i = 1; i < serialbuffer.size(); i++) {
                if ((serialbuffer.get(i) & 0xFF) == 0xD8) // UTF8: C3 98
                {
                    indexofstop = i;
                    break;
                }
            }

            if (indexofstop < 0) { // No more stop bytes inside buffer
                finished = true;
            } else {
                if (indexofstop > 2) {
                    byte[] packet = new byte[indexofstop - 1];
                    for (int i = 0; i < indexofstop - 1; i++)
                        packet[i] = serialbuffer.get(i + 1);
                    displayData(packet);
                    for (int i = 0; i < indexofstop + 1; i++)
                        serialbuffer.remove(0);
                }
            }
        }

    }

    private void displayData(byte[] rawArray) {

        if (rawArray != null) {
            //byte[] rawArray = data.clone();
            int len = rawArray.length;
            if (len >= 4) {
                int t = (((int) rawArray[0]) & 0xFF) << 9;  //Take the first byte and shift it of 8
                t |= (((int) rawArray[1]) & 0xFF) << 2;    //Add a second byte. In total 14 bit

                int h = (((int) rawArray[2]) & 0xFF) << 9;
                h |= (((int) rawArray[3]) & 0xFF) << 2;

                float tc = Math.round((-46.85 + (175.72 / 65536.0) * (float) t));
                celcius_text.setText(String.format("%.1f\u00B0C", tc));
                float farangeit = (tc) * (9 / 5) + 32;
                fara_text.setText(String.format("%.1f\u00B0F", farangeit));
                temp = tc;

                float RH = Math.round((-6 + (125.0 / 65536.0) * (float) h));//Return the humidity
                // float RH = ((-6f) + 125f * ((float)h / 65535f));
                humid_text.setText("HUMIDITY:" + " " + String.format("%.0f%%", RH));
                humid = RH;

                try {
                    date = sdf.format(new Date());
                    writeCsvData(date, temp, humid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //unbindService(mServiceConnection);
        //mBluetoothLeService = null;
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
            if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                // isSerial.setText("  Yes, serial connection");
            } else {
                //isSerial.setText("  No, serial connection");
            }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(WeatherHttpClient.BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(WeatherHttpClient.BluetoothLeService.UUID_HM_RX_TX);

        }
        timer.schedule(timer_humid, 5000, 15000); //
        timer2.schedule(timer_temp, 1000, 15000); //
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WeatherHttpClient.BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(WeatherHttpClient.BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(WeatherHttpClient.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(WeatherHttpClient.BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void readSeek(SeekBar seekBar, final int pos) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                RGBFrame[pos] = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                //   makeChange();
            }
        });
    }

    public void temp_update_timer_function(View view) {
        humidity = false;
        characteristicTX.setValue("D");
        mBluetoothLeService.writeCharacteristic(characteristicTX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
    }

    public void humid_update_timer_funtion(View view) {
        humidity = true;
    }

    private static Integer shortSignedAtOffset(BluetoothGattCharacteristic characteristicRX, int offset) {
        Integer lowerByte = characteristicRX.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = characteristicRX.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.

        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic characteristicRX, int offset) {
        Integer lowerByte = characteristicRX.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset);
        Integer upperByte = characteristicRX.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.

        return (upperByte << 8) + lowerByte;
    }

    public static int[] extractCalibrationCoefficients(BluetoothGattCharacteristic characteristicRX) {
        int[] coefficients = new int[8];

        coefficients[0] = shortUnsignedAtOffset(characteristicRX, 0);
        coefficients[1] = shortUnsignedAtOffset(characteristicRX, 2);
        coefficients[2] = shortUnsignedAtOffset(characteristicRX, 4);
        coefficients[3] = shortUnsignedAtOffset(characteristicRX, 6);
        coefficients[4] = shortSignedAtOffset(characteristicRX, 8);
        coefficients[5] = shortSignedAtOffset(characteristicRX, 10);
        coefficients[6] = shortSignedAtOffset(characteristicRX, 12);
        coefficients[7] = shortSignedAtOffset(characteristicRX, 14);

        return coefficients;
    }


    public void startTimer() {
        timer = new Timer();
        timer2 = new Timer();
        initializeTimerTask();
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (timer2 != null) {
            timer2.cancel();
            timer2 = null;
        }
    }

    public void initializeTimerTask() {
        timer_humid = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        //get the current timeStamp
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
                        final String strDate = simpleDateFormat.format(calendar.getTime());

                        //show the toast
                        int duration = Toast.LENGTH_SHORT;
                        //Toast toast = Toast.makeText(getApplicationContext(), strDate, duration);
                        //toast.show();

                        humid_update_timer_funtion(null);

                    }
                });
            }
        };

        timer_temp = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
                        final String strDate = simpleDateFormat.format(calendar.getTime());
                        int duration = Toast.LENGTH_SHORT;
                        temp_update_timer_function(null);
                    }
                });
            }
        };
    }


    private class JSONWeatherTask extends AsyncTask<String, Weather, Weather> {
        @Override
        protected Weather doInBackground(String... params) {
            Weather weather = new Weather();
            flag = true;
            while (flag == true && !isCancelled()) {
                while (isNetworkAvailable() == true && flag == true && !isCancelled()) {
                    String data = ((new WeatherHttpClient()).getWeatherData(params[0]));
                    try {
                        weather = JSONWeatherParser.getWeather(data);
                        weather.iconData = ((new WeatherHttpClient()).getImage(weather.currentCondition.getIcon()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    publishProgress(weather);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return weather;
        }


        @Override
        protected void onProgressUpdate(Weather... weather2) {
            super.onProgressUpdate(weather2);
            Weather weather = weather2[0];
            if (weather.iconData != null && weather.iconData.length > 0) {
                //Bitmap img = BitmapFactory.decodeByteArray(weather.iconData, 0, weather.iconData.length);
                //imgView.setImageBitmap(img);
            }
            ll = (LinearLayout) findViewById(R.id.internet_data);
            ll.setVisibility(View.VISIBLE);
            //cityText.setVisibility(View.VISIBLE);
            cityText.setText(weather.location.getCity() + "," + weather.location.getCountry());
            temp2.setText("" + Math.round((weather.temperature.getTemp() - 273.15)) + (char) 0x00B0 + "C");
            float farangeit2 = (Math.round((weather.temperature.getTemp() - 273.15))) * (9 / 5) + 32;
            fara2_text.setText(String.format("%.1f\u00B0F", farangeit2));
            hum.setText("HUMIDITY: " + weather.currentCondition.getHumidity() + "%");
            count++;
            Context context = getApplicationContext();
            CharSequence text = "Times ";
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, text + " " + count, duration);
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void initSwitch() {
        switchStatus = (TextView) findViewById(R.id.switchStatus);
        mySwitch = (Switch) findViewById(R.id.mySwitch);
        mySwitch.setChecked(true);
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) switchDigital();
                else switchAnalog();
            }
        });
        if (mySwitch.isChecked()) switchDigital();
        else switchAnalog();
    }

    private void switchDigital() {
        layoutDigital.setVisibility(View.VISIBLE);
        layoutAnalog.setVisibility(View.INVISIBLE);
        inDoor.setVisibility(View.INVISIBLE);
        outDoor.setVisibility(View.INVISIBLE);
        clock_text.setVisibility(View.VISIBLE);
        date_text.setVisibility(View.VISIBLE);
        switchStatus.setText("Digital");
    }

    private void switchAnalog() {
        layoutDigital.setVisibility(View.INVISIBLE);
        layoutAnalog.setVisibility(View.VISIBLE);
        inDoor.setVisibility(View.VISIBLE);
        outDoor.setVisibility(View.VISIBLE);
        clock_text.setVisibility(View.INVISIBLE);
        date_text.setVisibility(View.INVISIBLE);
        switchStatus.setText("Analog");
        new asyncTaskUpdateProgress().execute();
    }


    /////////////


    public class asyncTaskUpdateProgress extends AsyncTask<Void, Integer, Void> {

        int progress;

        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            //buttonStart.setClickable(true);
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            progress = 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            vProgressBar.setProgress(values[0]);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            while (progress < 100) {
                progress++;
                publishProgress(progress);
                SystemClock.sleep(100);
            }
            return null;
        }
        /////////////
    }
}