package com.abstractx1.sensortest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;

public class MainActivity extends WearableActivity implements AdapterCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();

        // Enables Always-on
        setAmbientEnabled();

        WearableRecyclerView wearableRecyclerView = findViewById(R.id.main_menu_view);

        wearableRecyclerView.setEdgeItemsCenteringEnabled(true);
        WearableLinearLayoutManager wearableLinearLayoutManager = new WearableLinearLayoutManager(this);
        wearableRecyclerView.setLayoutManager(wearableLinearLayoutManager);

        ArrayList<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(new MenuItem(R.drawable.ic_cc_checkmark, "Start Tracking"));
        menuItems.add(new MenuItem(R.drawable.ic_cc_clear, "Stop Tracking"));
        menuItems.add(new MenuItem(R.drawable.ic_full_sad, "Sync Data"));

        if (Constants.IN_DEBUG) {
            menuItems.add(new MenuItem(R.drawable.ic_full_cancel, "*WIPE"));
        }

        MainMenuAdapter mainMenuAdapter = new MainMenuAdapter(this, menuItems, this);

        wearableRecyclerView.setAdapter(mainMenuAdapter);
        wearableLinearLayoutManager.scrollToPosition(2);

    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        int permissionRequestId = 100;
        ArrayList<String> permissionsNotGranted = new ArrayList<String>();

        String[] permissions =  new String[]{Manifest.permission.BODY_SENSORS};

        for (String permission: permissions) {
            if(!hasPermission(permission))
                permissionsNotGranted.add(permission);
        }

        if(!permissionsNotGranted.isEmpty()) {
            String [] permissionsToRequest = permissionsNotGranted.toArray(new String[permissionsNotGranted.size()]);
            ActivityCompat.requestPermissions(this, permissionsToRequest, permissionRequestId);
        }
    }

    @Override
    public void onItemClicked(Integer menuPosition) {
        switch (menuPosition){
            case 0:
                Toast.makeText(this, "Starting SensorService", Toast.LENGTH_SHORT).show();
                startService();
                break;
            case 1:
                Toast.makeText(this, "Stopping SensorService", Toast.LENGTH_SHORT).show();
                AlarmUtil.cancelAlarm(this);
                stopService();
                break;
            case 2:
                Toast.makeText(this, "Uploading data to server", Toast.LENGTH_SHORT).show();
                OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class).build();
                WorkManager.getInstance(this).enqueue(uploadWorkRequest);
                Log.d(Constants.TAG, "Enqueued and upload work request to the Work Manager");
                break;
            case 3:
                Log.d(Constants.TAG, "Wiping database");
                Toast.makeText(this, "Wiping database", Toast.LENGTH_SHORT).show();
                DbHelper dbHelper = new DbHelper(getApplicationContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                dbHelper.reset(db);
                break;
            default : ;
        }
    }
    private void startService() {
        Intent serviceIntent = new Intent(this, SensorService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, SensorService.class);
        stopService(serviceIntent);
    }
}
