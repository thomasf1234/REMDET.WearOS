package com.abstractx1.sensortest;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.abstractx1.sensortest.entities.Event;

import org.json.JSONException;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

public class UploadWorker extends Worker {

    public UploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        ServerClient serverClient = new ServerClient(Constants.SERVER_BASE_URL);

        try {
            HttpResponse readyHttpResponse = serverClient.ready();

            if (readyHttpResponse.getCode() == 200) {

                Log.d(Constants.TAG, String.format("Server healthcheck returned %d", readyHttpResponse.getCode()));

                DbHelper dbHelper = new DbHelper(getApplicationContext());
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                while (DatabaseUtils.queryNumEntries(db, DbContract.EventEntry.TABLE_NAME) > 0) {
                    performBatch(db, serverClient, 300);
                }
            } else {
                // TODO : Throw error
                return Result.retry();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            return Result.failure();
        }

        // Indicate whether the task finished successfully with the Result
        return Result.success();
    }

    private boolean performBatch(SQLiteDatabase db, ServerClient serverClient, int batchSize) throws IOException, JSONException {
        String[] columns = new String[] { DbContract.EventEntry.COLUMN_NAME_EVENT_ID, DbContract.EventEntry.COLUMN_NAME_MEASUREMENT, DbContract.EventEntry.COLUMN_NAME_VALUE, DbContract.EventEntry.COLUMN_NAME_RECORDED_AT };
        Cursor cursor = db.query(DbContract.EventEntry.TABLE_NAME, columns, null, null, null, null, null, Integer.toString(batchSize));

        if (cursor != null) {
            Event events[] = new Event[cursor.getCount()];
            cursor.moveToFirst();

            int i = 0;
            while (!cursor.isAfterLast()) {
                int eventId = cursor.getInt(0);
                String measurement = cursor.getString(1);
                double value = cursor.getDouble(2);
                String recordedAt = cursor.getString(3);

//                Log.d(Constants.TAG, String.format("Returned event (EventId, SampleId, Metric, Value, RecordedAt) (%d, %d, %s, %f, %s)", eventId, Constants.SAMPLE_ID, metric, value, recordedAt));
                Event event = new Event(eventId, measurement, value, recordedAt);
                events[i] = event;
                i++;
                cursor.moveToNext();
            }

            if (!cursor.isClosed()) {
                cursor.close();
            }

            HttpResponse sendBatchUpdateHttpResponse = serverClient.sendBatchUpdate(events);
            if (sendBatchUpdateHttpResponse.getCode() == 200) {
                Log.d(Constants.TAG, "Batch update was successful");
                Log.d(Constants.TAG, "Deleting events");
                String[] eventIds = new String[events.length]; //Array of Ids you wish to delete.
                String[] questionMarks = new String[events.length];

                for (int j=0; j<events.length; ++j) {
                    eventIds[j] = Integer.toString(events[j].getEventId());
                    questionMarks[j] = "?";
                }

                String whereClause = String.format("%s IN (%s)", DbContract.EventEntry.COLUMN_NAME_EVENT_ID, String.join(",", questionMarks));
                Log.d(Constants.TAG, String.format("Where clause %s", whereClause));

                int deletedCount = db.delete(DbContract.EventEntry.TABLE_NAME, whereClause, eventIds);
                Log.d(Constants.TAG, String.format("Deleted %d records", deletedCount));
                return true;
            } else {
                Log.e(Constants.TAG, "Batch update failed");
            }
        }

        return false;
    }
}