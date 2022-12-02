package com.example.ruffle;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_LINEAR_ACCELERATION;
import static android.hardware.SensorManager.SENSOR_DELAY_FASTEST;
import static android.os.Environment.DIRECTORY_DOWNLOADS;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

class Reading {
    final Float ax, ay, az;

    public Reading(float ax, float ay, float az) {
        this.ax = ax;
        this.ay = ay;
        this.az = az;
    }

    @Override
    public String toString() {
        return String.join(",", ax.toString(), ay.toString(), az.toString());
    }

    public String[] toArray() {
        return new String[]{ax.toString(), ay.toString(), az.toString()};
    }
}

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "PLS_HALP";
    private static final long EXPORT_SIZE = 256 * 1024; // TODO - set to 128K finally
    private static final long READING_SIZE = 4 * 3;
    private static final long NUM_READINGS = EXPORT_SIZE / READING_SIZE;

    private boolean exported = false;
    private final List<Reading> accelReadings = new LinkedList<>();
    private final List<Reading> linearAccelReadings = new LinkedList<>();

    private TextView textView_exported;
    private TextView textView_recordingsSize;

    private TextView textView_accelReadings;
    private TextView textView_accelReliability;

    private TextView textView_linearAccelReadings;
    private TextView textView_linearAccelReliability;

    private SensorManager sensorManager;
    private Sensor sensor_accel;
    private Sensor sensor_linearAccel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView_exported = findViewById(R.id.text_exported);
        textView_recordingsSize = findViewById(R.id.text_recordingsSize);

        textView_accelReadings = findViewById(R.id.text_accelReadings);
        textView_accelReliability = findViewById(R.id.text_accelReliability);

        textView_linearAccelReadings = findViewById(R.id.text_linearAccelReadings);
        textView_linearAccelReliability = findViewById(R.id.text_linearAccelReliability);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor_accel = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        sensor_linearAccel = sensorManager.getDefaultSensor(TYPE_LINEAR_ACCELERATION);

        sensorManager.registerListener(MainActivity.this, sensor_accel, SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(MainActivity.this, sensor_linearAccel, SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        TextView textView_readings, textView_reliability;

        if(sensorEvent.sensor == this.sensor_accel) {
            textView_readings = textView_accelReadings;
            textView_reliability = textView_accelReliability;
        } else if(sensorEvent.sensor == this.sensor_linearAccel) {
            textView_readings = textView_linearAccelReadings;
            textView_reliability = textView_linearAccelReliability;
        } else return;

        Reading reading = new Reading(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        textView_readings.setText(reading.toString());
        switch (sensorEvent.accuracy) {
            case 0: textView_reliability.setText("Unreliable"); break;
            case 1: textView_reliability.setText("Low Accuracy"); break;
            case 2: textView_reliability.setText("Medium Accuracy"); break;
            case 3: textView_reliability.setText("High Accuracy"); break;
        }

        textView_recordingsSize.setText(Integer.toString(accelReadings.size()));

        if(!exported){
            if(sensorEvent.sensor == this.sensor_accel)
                accelReadings.add(reading);
            else
                linearAccelReadings.add(reading);

            if(accelReadings.size() >= NUM_READINGS){
                Log.d(TAG, "EXPORTING DATA!");
                File downloadDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
                long time = 0;

                File accelFile = new File(downloadDir + "/accel-reading-" + time + ".csv");
                File linearAccelFile = new File(downloadDir + "/linear-accel-reading-" + time + ".csv");

                try (FileWriter writer = new FileWriter(accelFile);
                     CSVWriter csvWriter = new CSVWriter(writer)){
                    accelReadings.forEach(r -> csvWriter.writeNext(r.toArray()));
                } catch (IOException e){
                    throw new RuntimeException(e);
                }

                try (FileWriter writer = new FileWriter(linearAccelFile);
                     CSVWriter csvWriter = new CSVWriter(writer)){
                    linearAccelReadings.forEach(r -> csvWriter.writeNext(r.toArray()));
                } catch (IOException e){
                    throw new RuntimeException(e);
                }

                textView_exported.setText("Exported at : " + time);
                exported = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}