package com.jun90.projects.scan.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScanApplication extends Application {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String mAPIKey = "", mToken;
    private SharedPreferences mUserSharedPreferences;

    @Override
    public void onCreate() {
        System.loadLibrary("opencv_java4");
        mUserSharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        super.onCreate();
    }

    public File getScanDir() {
        File scanDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Scan");
        if(!scanDir.exists() && !scanDir.mkdir()) return null;
        return scanDir;
    }

    public void udpateImageRecord(String id, String name, String filename, Date created) {
        ScanSQLiteOpenHelper helper = new ScanSQLiteOpenHelper(getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT * FROM `image` WHERE `id` = ?", new String[] {"object"});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            helper.close();
        }

    }

    public void clear() {
        List<String> lostFiles = new ArrayList<String>();
        ScanSQLiteOpenHelper helper = new ScanSQLiteOpenHelper(getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM `image`", null);
        while(c.moveToNext()) {
            String filename = c.getString(c.getColumnIndex("filename"));
            if(filename.isEmpty() || !new File(getScanDir().getAbsolutePath() + "/" + filename).exists())
                lostFiles.add(filename);
        }
        if(lostFiles.isEmpty()) return;
        db.beginTransaction();
        try {
            for(String filename : lostFiles)
                db.execSQL("DELETE FROM `image` WHERE `filename` = ?", new Object[] {filename});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            helper.close();
        }
    }

    public File getAvailableFile(String name) {
        if(name == null) return null;
        int id = 1;
        File file = new File(getScanDir().getAbsolutePath() + "/" + name + ".jpg");
        while(file.exists())
            file = new File(getScanDir().getAbsolutePath() + "/" + name + "_" + (id++) + ".jpg");
        return file;
    }

    public byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = null;
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            data = out.toByteArray();
            out.close();
        } catch (IOException e) { }
        return data;
    }

    public String saveImage(Bitmap bitmap, String name) {
        File file = getAvailableFile(name);
        if(bitmap == null || file == null) return null;
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            out.close();
            ScanSQLiteOpenHelper helper = new ScanSQLiteOpenHelper(getApplicationContext());
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                db.execSQL("INSERT INTO `image` (`name`, `filename`, `created`) VALUES (?, ?, ?)",
                        new Object[]{name, file.getName(), getDateFormat().format(new Date())});
                db.setTransactionSuccessful();
                return file.getName();
            } catch(SQLException e) {
                file.delete();
            } finally {
                db.endTransaction();
                helper.close();
            }
        } catch (IOException e) { }
        return null;
    }

    public DateFormat getDateFormat() {
        return DATE_FORMAT;
    }

    public boolean isAPIEnable() {
        return !(getAPIKey().isEmpty());
    }

    public boolean isLogin() {
        return !(getToken().isEmpty());
    }

    public String getAPIKey() {
        return mAPIKey;
    }

    public void setAPIKey(String APIKey) {
        mAPIKey = APIKey;
    }

    public String getToken() {
        if(mToken == null)
            mToken = mUserSharedPreferences.getString("token", "");
        return mToken;
    }

    public void setToken(String token) {
        mToken = token;
        SharedPreferences.Editor editor = mUserSharedPreferences.edit();
        editor.putString("token", token);
        editor.commit();
    }

}
