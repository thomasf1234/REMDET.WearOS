package com.abstractx1.sensortest;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.abstractx1.sensortest.entities.Event;

import org.json.JSONException;

import java.io.IOException;

public class UploadWorker extends Worker {
    private String _serverUrl;
    public UploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this._serverUrl = context.getString(R.string.server_url);
    }

    @Override
    public Result doWork() {
        ServerClient serverClient = new ServerClient(this._serverUrl);

        try {
            DbHelper dbHelper = new DbHelper(getApplicationContext());
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            while (DatabaseUtils.queryNumEntries(db, DbContract.EventEntry.TABLE_NAME) > 0) {
                performBatch(db, serverClient, Constants.UPLOAD_BATCH_SIZE);
            }
        } catch (IOException e) {
            return Result.retry();
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

                Event event = new Event(eventId, measurement, value, recordedAt);
                events[i] = event;
                i++;
                cursor.moveToNext();
            }

            if (!cursor.isClosed()) {
                cursor.close();
            }

            HttpResponse sendBatchUpdateHttpResponse = serverClient.sendBatchUpdate(events);
            if (sendBatchUpdateHttpResponse.getCode() == 202) {
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