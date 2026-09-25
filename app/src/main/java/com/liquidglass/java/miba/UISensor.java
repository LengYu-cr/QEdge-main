package com.liquidglass.java.miba;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** Direct Android port of catalog/utils/UISensor.kt + androidMain/UISensor.kt. */
public final class UISensor implements SensorEventListener {
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private float gravityAngle = 45f;
    private float gravityX;
    private float gravityY;
    private boolean started;

    public UISensor(Context context) {
        if (context == null) throw new IllegalArgumentException("context == null");
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public float getGravityAngle() { return gravityAngle; }
    public float getGravityX() { return gravityX; }
    public float getGravityY() { return gravityY; }

    /** Lua-friendly replacement for Compose Offset gravity. outXY[0]=x, outXY[1]=y. */
    public float[] getGravity() { return new float[] { gravityX, gravityY }; }
    public void getGravity(float[] outXY) {
        if (outXY == null || outXY.length < 2) return;
        outXY[0] = gravityX;
        outXY[1] = gravityY;
    }

    public boolean isStarted() { return started; }
    public boolean isAvailable() { return sensorManager != null && accelerometer != null; }

    public void start() {
        if (started || sensorManager == null || accelerometer == null) return;
        started = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        started = false;
    }

    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0];
        float y = event.values[1];
        float norm = (float) Math.sqrt(x * x + y * y + 9.81f * 9.81f);
        if (norm <= 0f) return;
        float alpha = 0.5f;
        float angle = (float) (Math.atan2(y, x) * (180.0 / Math.PI));
        gravityAngle = gravityAngle * (1f - alpha) + angle * alpha;
        gravityX = gravityX * (1f - alpha) + (x / norm) * alpha;
        gravityY = gravityY * (1f - alpha) + (y / norm) * alpha;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
