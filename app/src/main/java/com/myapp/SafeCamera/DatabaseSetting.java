package com.myapp.SafeCamera;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.io.File;

public final class DatabaseSetting {
        // To prevent someone from accidentally instantiating the contract class,
        // make the constructor private.
        private DatabaseSetting() {
        }

        /* Inner class that defines the table contents */
        public static class FeedEntry implements BaseColumns {
            public static final String TABLE_NAME = "entry";
            public static final String COLUMN_NAME_TITLE = "title";
            public static final String COLUMN_NAME_SUBTITLE = "subtitle";
            public static final File MY_KEY = null;
        }

    protected static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedEntry.COLUMN_NAME_TITLE + " TEXT," +
                    FeedEntry.COLUMN_NAME_SUBTITLE + " TEXT" +
                    FeedEntry.MY_KEY + "PATH)";

    protected static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;

}