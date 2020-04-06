package com.abstractx1.sensortest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import static android.content.Context.POWER_SERVICE;

public class OnAlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_ALARM_TRIGGERED = "com.abstractx1.sensortest.action.ALARM_TRIGGERED";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Setup WakeLock
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SensorService::WakelockTag");
        // Wake locks are reference counted by default. If a wake lock is reference counted, then each call to acquire() must be balanced by an equal number of calls to release(). If a wake lock is not reference counted, then one call to release() is sufficient to undo the effect of all previous calls to acquire().
        wakeLock.setReferenceCounted(true);
        wakeLock.acquire();

        if(Constants.IN_DEBUG) {
            Log.d(Constants.TAG, "OnAlarmReceiver Acquired WakeLock");
        }

        try {
            // an Intent broadcast.
            String action = intent.getAction();

            if (Constants.IN_DEBUG) {
                Log.d(Constants.TAG, "OnAlarmReceiver received action: " + action.toString());
            }

            context.startService(new Intent(context, SensorService.class));
        } finally {
            if(Constants.IN_DEBUG) {
                Log.d(Constants.TAG, "OnAlarmReceiver releasing WakeLock");
            }
            wakeLock.release();
        }
    }
}

//public class OnAlarmReceiver extends BroadcastReceiver {
//    public static final String ACTION_ALARM_TRIGGERED = "com.abstractx1.sensortest.action.ALARM_TRIGGERED";
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        WakefulIntentService.acquireStaticLock(context);
//
//        // TODO: This method is called when the BroadcastReceiver is receiving
//        // an Intent broadcast.
//        String action = intent.getAction();
//
//        if (Constants.IN_DEBUG) {
//            Log.d(Constants.TAG, "OnAlarmReceiver received action: " + action.toString());
//        }
//
//        Log.d("DemoService", "Received WAKEUP");
//        AlarmUtil.setAlarm(context);
//        context.startService(new Intent(context, DemoService.class));
//    }
//}

//public class OnAlarmReceiver extends BroadcastReceiver {
//    public static final String ACTION_ALARM_TRIGGERED = "com.abstractx1.sensortest.action.ALARM_TRIGGERED";
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        WakefulIntentService.acquireStaticLock(context);
//
//        // TODO: This method is called when the BroadcastReceiver is receiving
//        // an Intent broadcast.
//        String action = intent.getAction();
//
//        if (Constants.IN_DEBUG) {
//            Log.d(Constants.TAG, "OnAlarmReceiver received action: " + action.toString());
//        }
//
//        Log.d("DemoService", "Received WAKEUP");
//        AlarmUtil.setAlarm(context);
//        context.startService(new Intent(context, DemoService.class));
//    }
//}