package com.abstractx1.sensortest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/*
Batched sensor events
To better manage device power, the SensorManager APIs now allow you to specify the frequency at which you'd like the system to deliver batches of sensor events to your app. This doesn't reduce the number of actual sensor events available to your app for a given period of time, but instead reduces the frequency at which the system calls your SensorEventListener with sensor updates. That is, instead of delivering each event to your app the moment it occurs, the system saves up all the events that occur over a period of time, then delivers them to your app all at once.

To provide batching, the SensorManager class adds two new versions of the registerListener() method that allow you to specify the "maximum report latency." This new parameter specifies the maximum delay that your SensorEventListener will tolerate for delivery of new sensor events. For example, if you specify a batch latency of one minute, the system will deliver the recent set of batched events at an interval no longer than one minute by making consecutive calls to your onSensorChanged() method—once for each event that was batched. The sensor events will never be delayed longer than your maximum report latency value, but may arrive sooner if other apps have requested a shorter latency for the same sensor.

However, be aware that the sensor will deliver your app the batched events based on your report latency only while the CPU is awake. Although a hardware sensor that supports batching will continue to collect sensor events while the CPU is asleep, it will not wake the CPU to deliver your app the batched events. When the sensor eventually runs out of its memory for events, it will begin dropping the oldest events in order to save the newest events. You can avoid losing events by waking the device before the sensor fills its memory then call flush() to capture the latest batch of events. To estimate when the memory will be full and should be flushed, call getFifoMaxEventCount() to get the maximum number of sensor events it can save, and divide that number by the rate at which your app desires each event. Use that calculation to set wake alarms with AlarmManager that invoke your Service (which implements the SensorEventListener) to flush the sensor.
 */
public class SensorService extends Service implements SensorEventListener2 {
    public static final String CHANNEL_ID = "sensor_service_0";
    public static final String CHANNEL_NAME = "SensorService";

    private PowerManager.WakeLock wakeLock;

    private SensorManager sensorManager;
    private Sensor linearAccelerationSensor;
    private Sensor heartRateSensor;

    private BlockingQueue<String> saveBufferQueue;

    private long bootMs;

    private SQLiteDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();
        long uptimeMs = SystemClock.elapsedRealtime();
        long nowMs = System.currentTimeMillis();
        this.bootMs = nowMs - uptimeMs;
        this.saveBufferQueue = new ArrayBlockingQueue<String>(Constants.EVENT_BUFFER_SIZE);

        registerSensorListeners();

        DbHelper dbHelper = new DbHelper(getApplicationContext());
        this.db = dbHelper.getWritableDatabase();

        Log.d(Constants.TAG, "Creating SensorService");

        createNotificationChannel(CHANNEL_ID, CHANNEL_NAME);
        Intent actionIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("REM Tracker")
                .setContentText("Stop from the app to halt tracker")
                .setSmallIcon(R.drawable.ic_add_white_24dp)
                .setContentIntent(pendingIntent)
                .build();

        // Does not actually start the service
        startForeground(1, notification);
    }

    private void createNotificationChannel(String channelId, String channelName)
    {
        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        notificationChannel.setLightColor(Color.BLUE);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "Starting SensorService");

        // Setup WakeLock
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        if (this.wakeLock == null) {
            this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SensorService::WakelockTag");
            this.wakeLock.acquire();
            Log.d(Constants.TAG, "SensorService acquired new WakeLock");
        } else {
            if (this.wakeLock.isHeld()) {
                Log.e(Constants.TAG, "SensorService WakeLock was still held at start. This should be released in onFlushCompleted but re-using now");
            } else {
                this.wakeLock.acquire();
                Log.d(Constants.TAG, "SensorService acquired existing WakeLock");
            }
        }

        try {
            Log.d(Constants.TAG, "Setting alarm for next time");
            AlarmUtil.setAlarm(this.getApplicationContext());

            Log.d(Constants.TAG, "SensorService flushing sensorManager");
            this.sensorManager.flush(this);
        } catch (RuntimeException re){
            Log.e(Constants.TAG, "Error occurred during service run", re);
            Toast.makeText(this, "Error occurred during service run", Toast.LENGTH_LONG).show();
        }

        // IF we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "Stopping SensorService");
        Log.d(Constants.TAG, "Flushing save buffer to database");
        flushSaveBufferToDatabase();
        Log.d(Constants.TAG, "Unregistering sensor listeners");
        this.sensorManager.unregisterListener(this);
        Log.d(Constants.TAG, "Stopped SensorService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor _sensor = event.sensor;

        if (this.saveBufferQueue.remainingCapacity() == 0) {
            Log.i(Constants.TAG, String.format("BUFFER FULL - REACHED %d", this.saveBufferQueue.size()));
            flushSaveBufferToDatabase();
        }

        // TODO move to separate method
        long uptimeAtEventMs = Math.round((double) event.timestamp / 1000000L); // ns to ms
        long eventMs = bootMs + uptimeAtEventMs;
        Date recordedAt = new Date(eventMs);

        if (_sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            double aX = event.values[0];
            double aY = event.values[1];
            double aZ = event.values[2];
            double acceleration = Math.sqrt(Math.pow(aX, 2) + Math.pow(aY, 2) + Math.pow(aZ, 2));

            String measurement = "linear_acceleration";
            double value = acceleration;

            appendEventToSaveBuffer(measurement, value, recordedAt);
        } else if (_sensor.getType() == Sensor.TYPE_HEART_RATE)
        {
            double bpm = event.values[0];

            String measurement = "heart_rate";
            double value = bpm;

            appendEventToSaveBuffer(measurement, value, recordedAt);
        }
    }

    private void appendEventToSaveBuffer(String measurement, double value, Date recordedAt) {
        // Sqlite expects "YYYY-MM-DD HH:MM:SS.SSS"?
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String formattedRecordedAt = dateFormat.format(recordedAt);

        String sql = "INSERT INTO " + DbContract.EventEntry.TABLE_NAME + " ("
                + DbContract.EventEntry.COLUMN_NAME_MEASUREMENT + ", "
                + DbContract.EventEntry.COLUMN_NAME_VALUE + ", "
                + DbContract.EventEntry.COLUMN_NAME_RECORDED_AT + ") "
                + String.format("VALUES ('%s',%f,'%s')", measurement, value, formattedRecordedAt);

        this.saveBufferQueue.add(sql);

        if (Constants.IN_DEBUG) {
            Log.d(Constants.TAG, String.format("Appended to SQL batch insert: %s", sql));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerSensorListeners() {
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.linearAccelerationSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        this.heartRateSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (this.linearAccelerationSensor == null) {
            Log.e(Constants.TAG, "No Sensor Sensor.TYPE_LINEAR_ACCELERATION");
        } else {
            // TODO : Extract into config
            int linearAccelerationSamplingPeriodUs = 500 * 1000; // 0.5 seconds
            int linearAccelerationMaxReportLatencyUs = 10 * 60 * 1000 * 1000; // 10 minutes
            this.sensorManager.registerListener(this, this.linearAccelerationSensor, linearAccelerationSamplingPeriodUs, linearAccelerationMaxReportLatencyUs);

            Log.d(Constants.TAG, "Registered Sensor.TYPE_LINEAR_ACCELERATION");
        }

        if (this.heartRateSensor == null) {
            Log.e(Constants.TAG, "No Sensor Sensor.TYPE_HEART_RATE");
        } else {
            int heartRateMaxReportLatencyUs = 1 * 60 * 1000000; // 10 minutes
            this.sensorManager.registerListener(this, this.heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST, heartRateMaxReportLatencyUs);

            Log.d(Constants.TAG, "Registered Sensor.TYPE_HEART_RATE");
        }
    }

    private void flushSaveBufferToDatabase() {
        this.db.beginTransaction();
        try {
            int entriesCount = this.saveBufferQueue.size();
            for (int i=0; i<entriesCount; ++i) {
                String sql = this.saveBufferQueue.take();
                Log.d(Constants.TAG, String.format("Executing SQL %s", sql));
                this.db.execSQL(sql);
            }
            this.db.setTransactionSuccessful();
            this.saveBufferQueue.clear();
            Log.d(Constants.TAG, "Flushed buffer to database");
        } catch(SQLException | InterruptedException e) {
            // TODO : Effectively handle interrupt exception
            //  You do not need to explicitly rollback. If you call db.endTransaction() without .setTransactionSuccessful() it will roll back automatically.
            Log.e(Constants.TAG, "Error occurred insert rows", e);
        } finally {
            this.db.endTransaction();
        }
    }

    @Override
    public void onFlushCompleted(Sensor sensor) {
        Log.d(Constants.TAG, "Flush completed");
        // TODO : This is called more than once but we remove the wakelock on the first call, we should only do it after the last
        if (this.wakeLock != null && this.wakeLock.isHeld()) {
            this.wakeLock.release();
            this.wakeLock = null;
            Log.d(Constants.TAG, "WakeLock released");
        }
    }
}