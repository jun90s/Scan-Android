package com.jun90.projects.scan.android;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.lang.ref.WeakReference;

public class SplashActivity extends AppCompatActivity {

    private static class InitTask extends AsyncTask {

        private final ScanApplication mApplication;
        private final WeakReference<SplashActivity> mActivity;

        public InitTask(SplashActivity activity) {
            mApplication = (ScanApplication) activity.getApplication();
            mActivity = new WeakReference<SplashActivity>(activity);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            mApplication.clear();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) { }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            SplashActivity activity = mActivity.get();
            if(activity != null) activity.startActivity(new Intent(activity, MainActivity.class));
            activity.finish();
        }
    }

    private ScanApplication mApplication;
    private AsyncTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mApplication = (ScanApplication) getApplication();
        mTask = new InitTask(this);
        try {
            String apiKey = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData.getString("apikey");
            if(apiKey != null && apiKey.length() == 32)
                mApplication.setAPIKey(apiKey);
        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
            showTerminatorAlertDialog("APIKEY不合法，请联系开发者");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            mTask.execute();
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }

    private void showTerminatorAlertDialog(String msg) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("警告");
        dialogBuilder.setMessage(msg);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SplashActivity.this.finish();
            }
        });
        dialogBuilder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    mTask.execute();
                else
                    showTerminatorAlertDialog("缺少必要的权限");
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

}
