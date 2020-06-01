package com.abstractx1.sensortest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.text.SimpleDateFormat;

/*
To move into Doze mode must

$ adb shell dumpsys battery unplug
$ adb shell dumpsys deviceidle enable

To enter light doze
$ adb shell dumpsys deviceidle step light

To enter deep doze
$ adb shell dumpsys deviceidle step deep

Can get device details such a display
$ adb shell dumpsys display

Is screen off
$ adb shell dumpsys deviceidle get screen

To return must reset the battery, disable deviceidle and reboot the device
$ adb shell dumpsys battery reset
$ adb shell dumpsys deviceidle disable
$ adb reboot

If adb hangs after this will need to restart the server
$ adb kill-server
$ adb start-server
 */
public class AlarmUtil {
    public static void setAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, OnAlarmReceiver.class);
        int reminderiId = 0;
        intent.setAction(OnAlarmReceiver.ACTION_ALARM_TRIGGERED);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminderiId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 23)
        {
            // https://medium.com/@sauge16/how-to-handle-alarm-in-all-android-version-e7aca16ae885
            // Alarm time in SystemClock.elapsedRealtime() (time since boot, including sleep), which will wake up the device when it goes off.
            long uptimeMs = SystemClock.elapsedRealtime();
            long triggerAtMillis = uptimeMs + Constants.ALARM_INTERVAL_MS;

            if (Constants.IN_DEBUG) {
                long nowMs = System.currentTimeMillis();
                String triggerAtDateString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(nowMs + Constants.ALARM_INTERVAL_MS);
                Log.d(Constants.TAG, String.format("Setting alarm at %s", triggerAtDateString));
            }

            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, OnAlarmReceiver.class);
        int reminderiId = 0;
        intent.setAction(OnAlarmReceiver.ACTION_ALARM_TRIGGERED);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminderiId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d(Constants.TAG, "Cancelling pending intent");
        alarmManager.cancel(pendingIntent);
    }
}
