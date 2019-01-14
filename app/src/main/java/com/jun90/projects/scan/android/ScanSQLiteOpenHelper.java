package com.jun90.projects.scan.android;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ScanSQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "scan";
    private static final int VERSION = 1;

    public ScanSQLiteOpenHelper(Context context) {
        this(context, null);
    }

    public ScanSQLiteOpenHelper(Context context, DatabaseErrorHandler errorHandler) {
        super(context, DB_NAME, null, VERSION, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE `image` (\n" +
                "`id` CHAR(32) NOT NULL DEFAULT ''," +
                "`name` VARCHAR(255) NOT NULL DEFAULT ''," +
                "`filename` TEXT NOT NULL DEFAULT ''," +
                "`created` DATETIME NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX index_id ON `image` (`id`)");
        db.execSQL("CREATE INDEX name_id ON `image` (`name`)");
        db.execSQL("CREATE INDEX created ON `image` (`created`)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
