package com.syncedsoftware.iassembly.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

/**
 * Created by Anthony M. Santiago on 2/11/16.
 */
public class DataProvider extends ContentProvider {

    private static HashMap<String, String> values;
    private static final UriMatcher sUriMatcher;

    static{
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(DataContract.AUTHORITY, DataContract.PATH, DataContract.DATA_TABLE_1);
    }

    private SQLiteDatabase sqlDb;


    @Override
    public boolean onCreate() {
        DatabaseHelper helper = new DatabaseHelper(getContext());

        sqlDb = helper.getWritableDatabase();

        return sqlDb != null;

    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        builder.setTables(DataContract.TABLE_NAME);

        switch (sUriMatcher.match(uri)) {

            case DataContract.DATA_TABLE_1:
                builder.setProjectionMap(values);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI");

        }

        Cursor cursor = builder.query(sqlDb, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;

    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case DataContract.DATA_TABLE_1:
                return "vnd.android.cursor.dir/" + DataContract.PATH;

            default:
                throw new IllegalArgumentException("Unknown URI");

        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowId = sqlDb.insert(DataContract.TABLE_NAME, null, values);

        if(rowId > 0){
            Uri fUri = ContentUris.withAppendedId(DataContract.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(fUri, null);
            return fUri;
        }
        else{
            Toast.makeText(getContext(), "FAILED", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted = 0;
        switch (sUriMatcher.match(uri)) {

            case DataContract.DATA_TABLE_1:
                rowsDeleted = sqlDb.delete(DataContract.TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI");

        }

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int rowsUpdated = 0;
        switch (sUriMatcher.match(uri)) {

            case DataContract.DATA_TABLE_1:
                rowsUpdated = sqlDb.update(DataContract.TABLE_NAME, values, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI");

        }

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
    }

    protected static final class DatabaseHelper extends SQLiteOpenHelper {

        private static final String SQL_CREATE_MAIN = DataContract.CREATE_DB;

        DatabaseHelper(Context context) {
            super(context, DataContract.DATABASE_NAME, null, 1);
        }

        /*
         * Creates the data repository. This is called when the provider attempts to open the
         * repository and SQLite reports that it doesn't exist.
         */
        public void onCreate(SQLiteDatabase db) {

            // Creates the main table
            db.execSQL(SQL_CREATE_MAIN);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + DataContract.TABLE_NAME);
            onCreate(db);
        }
    }
}
