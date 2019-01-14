package com.jun90.projects.scan.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class WorkbenchActivity extends AppCompatActivity {

    private static class WorkbenchActivityHandler extends Handler {

        private final ScanApplication mApplication;
        private final WeakReference<WorkbenchActivity> mActivity;

        public WorkbenchActivityHandler(WorkbenchActivity activity) {
            mApplication = (ScanApplication) activity.getApplication();
            mActivity = new WeakReference<WorkbenchActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final WorkbenchActivity activity = mActivity.get();
            if(activity == null) return;
            JsonObject result = null;
            final Bundle bundle = msg.getData();
            if(bundle != null) {
                String s = bundle.getString("result");
                if(s != null) result = new JsonParser().parse(s).getAsJsonObject();
            }
            switch (msg.what) {
                case 1:
                    if(result != null && result.get("code").getAsInt() == 200) {
                        activity.showWaitingDialog("正在云端处理", APIHelper.saveProject(mApplication, WorkbenchActivityHandler.this, 2, result.get("project").getAsString(), activity.getImageName()));
                    } else {
                        activity.showMessageDialog("保存", result == null ? "上传失败" : result.get("msg").getAsString());
                        activity.dismissWaitingDialog();
                    }
                    break;
                case 2:
                    if(result != null && result.get("code").getAsInt() == 200) {
                        activity.showMessageDialog("保存", "保存到云成功", true);
                        activity.setResult(RESULT_OK);
                    } else {
                        activity.showMessageDialog("保存", result == null ? "上传失败" : result.get("msg").getAsString());
                    }
                    activity.dismissWaitingDialog();
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private ScanApplication mApplication;
    private WorkbenchActivityHandler mHandler;
    private Bitmap mSourceBitmap;
    private TextView mResetTextView, mSaveTextView;
    private TextView mirrorXTextView, mirrorYTextView, syncTextView;
    private int defaultMirrorXTextViewColor, defaultMirrorYTextViewColor, defaultSyncTextViewColor;
    private TextView mContrastValueTextView, mBrightnessValueTextView;
    private SeekBar mContrastSeekBar, mBrightnessSeekBar;
    private EditImageView mEditImageView;
    private boolean mMirrorX, mMirrorY, mSync;
    private Dialog mWaitingDialog;
    private Thread mWaitingThread;
    private String mImageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workbench);
        mApplication = (ScanApplication) getApplication();
        mHandler = new WorkbenchActivityHandler(this);

        mEditImageView = findViewById(R.id.editImageView);

        mResetTextView = findViewById(R.id.resetTextView);

        mSaveTextView = findViewById(R.id.saveTextView);

        mirrorXTextView = findViewById(R.id.mirrorXTextView);
        defaultMirrorXTextViewColor = mirrorXTextView.getCurrentTextColor();

        mirrorYTextView = findViewById(R.id.mirrorYTextView);
        defaultMirrorYTextViewColor = mirrorXTextView.getCurrentTextColor();

        syncTextView = findViewById(R.id.syncTextView);
        defaultSyncTextViewColor = syncTextView.getCurrentTextColor();

        mContrastValueTextView = findViewById(R.id.contrastValueTextView);
        mContrastSeekBar = findViewById(R.id.contrastSeekBar);
        mContrastSeekBar.setMax(200);
        mContrastSeekBar.setProgress(100);

        mBrightnessValueTextView = findViewById(R.id.brightnessValueTextView);
        mBrightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        mBrightnessSeekBar.setMax(200);
        mBrightnessSeekBar.setProgress(100);

        switch (getIntent().getIntExtra("from", 0)) {
            case 1:
                startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), 1);
                break;
            case 2:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,2);
                break;
            default:
                finish();
                break;
        }

    }

    public String getImageName() {
        return mImageName;
    }

    public void setImageName(String imageName) {
        mImageName = imageName;
    }

    public void setMirrorX(boolean mirrorX) {
        setMirrorX(mirrorX, true);
    }

    private void setMirrorX(boolean mirrorX, boolean preview) {
        mMirrorX = mirrorX;
        if(mMirrorX)
            mirrorXTextView.setTextColor(WorkbenchActivity.this.getResources().getColor(R.color.colorAccent));
        else
            mirrorXTextView.setTextColor(defaultMirrorXTextViewColor);
        mEditImageView.setMirrorX(mMirrorX, preview);
    }

    public void setMirrorY(boolean mirrorY) {
        setMirrorY(mirrorY, true);
    }

    private void setMirrorY(boolean mirrorY, boolean preview) {
        mMirrorY = mirrorY;
        if(mMirrorY)
            mirrorYTextView.setTextColor(WorkbenchActivity.this.getResources().getColor(R.color.colorAccent));
        else
            mirrorYTextView.setTextColor(defaultMirrorYTextViewColor);
        mEditImageView.setMirrorY(mirrorY, preview);
    }

    private void setSync(boolean sync) {
        if(!mApplication.isLogin()) return;
        mSync = sync;
        if(mSync)
            syncTextView.setTextColor(WorkbenchActivity.this.getResources().getColor(R.color.colorAccent));
        else
            syncTextView.setTextColor(defaultSyncTextViewColor);
    }

    public void setAngle(int angle) {
        setAngle(angle, true);
    }

    private void setAngle(int angle, boolean preview) {
        mEditImageView.setAngle(angle, preview);
    }

    public void setContrast(int contrast) {
        setContrast(contrast, true);
    }

    private void setContrast(int contrast, boolean preview) {
        mContrastSeekBar.setProgress(100 + contrast);
        mContrastValueTextView.setText(String.valueOf(contrast));
        mEditImageView.setContrast(contrast / 100.0f, preview);
    }

    private void setBrightness(int brightness) {
        setBrightness(brightness, true);
    }

    private void setBrightness(int brightness, boolean preview) {
        mBrightnessSeekBar.setProgress(100 + brightness);
        mBrightnessValueTextView.setText(String.valueOf(brightness));
        mEditImageView.setBrightness(brightness / 100.0f, preview);
    }

    private void reset() {
        setMirrorX(false, false);
        setMirrorY(false, false);
        setContrast(0, false);
        setBrightness(0, false);
        setAngle(0, false);
        mEditImageView.preview();
    }

    protected void showSaveDialog() {
        View inputView = LayoutInflater.from(this).inflate(R.layout.dialog_input,null, false);
        final EditText editText = inputView.findViewById(R.id.editText);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(WorkbenchActivity.this);
        dialogBuilder.setTitle("保存");
        dialogBuilder.setMessage("请输入图像名称");
        dialogBuilder.setView(inputView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton("保存", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setImageName(editText.getText().toString().trim());
                if(getImageName().isEmpty()) {
                    showMessageDialog("保存", "文件名不能为空");
                    dialog.dismiss();
                    return;
                }
                if(mSync) {
                    showWaitingDialog("正在上传图片", APIHelper.newProject(mApplication, mHandler, 1,
                            mEditImageView.getImageScanner(true).toJSON(), mSourceBitmap));
                } else {
                    Bitmap bitmap = mEditImageView.getImageScanner(true).run(mSourceBitmap);
                    String filename = mApplication.saveImage(bitmap, getImageName());
                    if(filename == null) {
                        showMessageDialog("保存", "保存失败");
                    } else {
                        showMessageDialog("保存", "已保存到文件：" + filename, true);
                        setResult(RESULT_OK);
                    }
                    dialog.dismiss();
                }
            }
        });
        dialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.show();
    }

    protected void showMessageDialog(String title, String msg) {
        showMessageDialog(title, msg, false);
    }

    protected void showMessageDialog(String title, String msg, final boolean finish) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(title);
        dialogBuilder.setMessage(msg);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(finish) finish();
            }
        });
        dialogBuilder.show();
    }

    protected void dismissWaitingDialog() {
        if(mWaitingDialog != null) {
            mWaitingDialog.dismiss();
            mWaitingDialog = null;
        }
    }

    protected void showWaitingDialog(String msg, Thread thread) {
        mWaitingThread = thread;
        View waitingView = LayoutInflater.from(this).inflate(R.layout.dialog_waiting,null, false);
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("请稍候");
        dialogBuilder.setMessage(msg);
        dialogBuilder.setView(waitingView);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mWaitingThread != null && mWaitingThread.isAlive()) {
                    try {
                        mWaitingThread.interrupt();
                    } catch (SecurityException e) {}
                    mWaitingThread = null;
                }
                dialog.dismiss();
            }
        });
        if(mWaitingDialog != null) mWaitingDialog.dismiss();
        mWaitingDialog = dialogBuilder.create();
        mWaitingDialog.show();
    }

    private void init() {
        if(mSourceBitmap == null) return;

        mEditImageView.setImageBitmap(mSourceBitmap);
        mResetTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });
        mSaveTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSaveDialog();
            }
        });
        mirrorXTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMirrorX(!mMirrorX);
            }
        });
        mirrorYTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMirrorY(!mMirrorY);
            }
        });
        syncTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSync(!mSync);
            }
        });
        mContrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setContrast(progress - 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mBrightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setBrightness(progress - 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) {
            try {
                if(resultCode != RESULT_OK) throw new FileNotFoundException();
                mSourceBitmap = (Bitmap) data.getExtras().get("data");
                init();
            } catch (IOException e) {
                finish();
            }
        } else if(requestCode == 2) {
            try {
                if(resultCode != RESULT_OK) throw new FileNotFoundException();
                InputStream in = getContentResolver().openInputStream(data.getData());
                mSourceBitmap = BitmapFactory.decodeStream(in);
                in.close();
                init();
            } catch (IOException e) {
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
