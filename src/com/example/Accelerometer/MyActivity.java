package com.example.Accelerometer;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MyActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    Button btnStart;
    Button btnStop;
    TextView tvRepeatCount;


    /**
     * Called when the activity is first created.
     */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Inditas ota ennyi ido telt el
        //elapsedSinceStart = System.currentTimeMillis();

        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enabled = true;
                Log.w("Button pressed", "Start measuring...");

            }
        });

        btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enabled = false;
                repeatCount = 0;
                Log.w("Button pressed", "Stop measuring...");

            }
        });


        tvRepeatCount = (TextView) findViewById(R.id.tvRepeat);

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private long lastUpdate = 0;

    //Azert kell, mert fel is kell venni a sulyt a kezdo pozicioba
    private long elapsedSinceStart = 0;


    private float prev_last_x = 0.0f, prev_last_y = 0.0f, prev_last_z = 0.0f; // utolso elotti meres
    private float last_x = 0.0f, last_y = 0.0f, last_z = 0.0f; // utolso meres
    private float current_x = 0.0f, current_y = 0.0f, current_z = 0.0f; // utolso meres

    private float ppAcc = 0.0f;
    private float pAcc = 0.0f;
    private float acc = 0.0f;

    private int repeatCount = 0;
    private int phase = 0;
    private boolean enabled = false;
    private boolean lock = false;

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor detectedSensor = event.sensor;

        if (detectedSensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            //current time
            long cTime = System.currentTimeMillis();

            //FILTER----(High-low)----------------------------------------------------------------------
            //0.8-cal a legpontosabb
            float alpha = (1.0f / 0.8f ) / ( 1.0f / 0.8f + 1.0f / 3.0f);

            // Isolate the force of gravity with the low-pass filter / a vegen
            float[] gravity = new float[3];
            gravity[0] = SensorManager.GRAVITY_EARTH;
            gravity[1] = SensorManager.GRAVITY_EARTH;
            gravity[2] = SensorManager.GRAVITY_EARTH;

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            float[] linear_acceleration = new float[3];
            linear_acceleration[0] = event.values[0] -  gravity[0];
            linear_acceleration[1] = event.values[1] -  gravity[1];
            linear_acceleration[2] = event.values[2] -  gravity[2];

            //Gyorsulasvektor nagysaga / low pass filter = gravity minusz
            float mAccelCurrent = FloatMath.sqrt( linear_acceleration[1] *  linear_acceleration[1]
            +linear_acceleration[2] * linear_acceleration[2] +
            linear_acceleration[0] * linear_acceleration[0])  - SensorManager.GRAVITY_EARTH;
            //---------------------------------------------------------------------------------

            //Nagyon erzekeny, ezert 333 ms elteltevel mintavetelezunk / 1 s egy minta
            if (cTime - lastUpdate > 333) {
                if (lock == false) {
                    //long diffTime = cTime - lastUpdate;
                    lastUpdate = cTime;

                    Log.w("Acceleration", "Phase: "  + phase +  ", a: " + mAccelCurrent ) ;
                    if (phase == 2) {
                        Log.w("Measurement ready", "Values: " + ppAcc + "; " + pAcc + "; " + acc);
                    }
                    switch (phase) {
                        case 0:
                            ppAcc=mAccelCurrent;
                            /*prev_last_x = x;
                            prev_last_y = y;
                            prev_last_z = z;*/
                            phase = 1;
                            break;
                        case 1:
                            pAcc=mAccelCurrent;
                            /*last_x = x;
                            last_y = y;
                            last_z = z;*/
                            phase = 2;
                            break;
                        case 2:
                            acc = mAccelCurrent;

                           /* current_x = x;
                            current_y = y;
                            current_z = z;*/

                            phase = 0;

                            break;
                    }

                    lock = true;
                    evaluate();
                    lock = false;
                }
            }
        }
    }

    private void evaluate() {
        if (enabled) {
            Log.w("Evaluate", "Measures completed...");
            //Nagy érzékenység miatt küszönbérték számítása
            if (ppAcc != 0.0f && pAcc != 0.0f && acc != 0.0f) {
                if (pAcc - ppAcc > 0.7f && pAcc - acc > 0.7f) {
                    if (ppAcc < pAcc && acc < pAcc) {
                        Log.w("Detection:", "Repeat detected");
                        repeatCount++;
                        tvRepeatCount.setText("Ismétlésszám: " + repeatCount);

                        ppAcc = 0.0f;
                        pAcc = 0.0f;
                        acc = 0.0f;
                    }
                } else {
                    Log.w("Threshold", "Threshold less than 0.06");
                }
            } else {
                Log.w("Error", "Data contains zero value");
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
