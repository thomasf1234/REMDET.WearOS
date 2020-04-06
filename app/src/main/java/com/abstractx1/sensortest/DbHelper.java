package com.abstractx1.sensortest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SensorEvent.db";

    private static final String SQL_CREATE_EVENT =
            "CREATE TABLE " + DbContract.EventEntry.TABLE_NAME + " (" +
                    DbContract.EventEntry.COLUMN_NAME_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    DbContract.EventEntry.COLUMN_NAME_MEASUREMENT + " TEXT NOT NULL," +
                    DbContract.EventEntry.COLUMN_NAME_VALUE + " REAL NOT NULL," +
                    DbContract.EventEntry.COLUMN_NAME_RECORDED_AT + " TIMESTAMP NOT NULL," +
                    DbContract.EventEntry.COLUMN_NAME_CREATED_AT + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL);";

    private static final String SQL_DROP_EVENT =
            "DROP TABLE IF EXISTS " + DbContract.EventEntry.TABLE_NAME;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        setup(db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        dropAll(db);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void setup(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_EVENT);
        Log.d(Constants.TAG, "Created all tables");
    }

    public void dropAll(SQLiteDatabase db) {
        db.execSQL(SQL_DROP_EVENT);
        Log.d(Constants.TAG, "Dropped all tables");
    }

    public void reset(SQLiteDatabase db) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        dropAll(db);
        onCreate(db);
    }
}
