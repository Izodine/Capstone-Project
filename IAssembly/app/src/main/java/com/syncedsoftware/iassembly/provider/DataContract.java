package com.syncedsoftware.iassembly.provider;

import android.net.Uri;

/**
 * Created by Anthony M. Santiago on 2/11/16.
 */
public final class DataContract {
    public static final String AUTHORITY = "com.syncedsoftware.iassembly.provider.DataProvider";
    public static final String PATH = "main";
    public static final String CONTENT_URL = "content://" + AUTHORITY +"/"+ PATH;
    public static final Uri CONTENT_URI = Uri.parse(CONTENT_URL);

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PROGRAM = "program";

    public static final String DATABASE_NAME = "dataDb";
    public static final String TABLE_NAME = "program";
    public static final int VERSION = 1;
    public static final String CREATE_DB = "CREATE TABLE " + TABLE_NAME + "(" + COLUMN_ID +
            " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_NAME
            + " TEXT NOT NULL, "+COLUMN_PROGRAM +" TEXT NOT NULL);";

    public static final int DATA_TABLE_1 = 1;
}
