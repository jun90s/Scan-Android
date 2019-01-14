package com.jun90.projects.scan.android;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static class SettingItem {

        private String mText;
        private View.OnClickListener mOnClickListener;

        public String getText() {
            return mText;
        }

        public void setText(String text) {
            mText = text;
        }

        public View.OnClickListener getOnClickListener() {
            return mOnClickListener;
        }

        public void setOnClickListener(View.OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
        }

    }

    public static class SettingCategory {

        private String mName;
        private List<SettingItem> mItems = new LinkedList<SettingItem>();

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public int getCount() {
            return mItems.size();
        }

        public SettingItem getItem(int index) {
            return mItems.get(index);
        }

        public void addItem(SettingItem item) {
            mItems.add(item);
        }

        public void clear() {
            mItems.clear();
        }

        public Iterator<SettingItem> iterator() {
            return mItems.iterator();
        }

    }

    public static class SettingAdapter extends RecyclerView.Adapter<SettingAdapter.SettingViewHolder> {

        public static class SettingViewHolder extends RecyclerView.ViewHolder {

            private View mItemView;
            private TextView mTextView;

            public SettingViewHolder(View itemView) {
                super(itemView);
                mItemView = itemView;
                mTextView = itemView.findViewById(R.id.textView);
                mTextView.findViewById(R.id.textView);
            }

            public void setText(String text) {
                mTextView.setText(text);
            }

            public void setOnClickListener(View.OnClickListener listener) {
                mItemView.setOnClickListener(listener);
            }

        }

        private List<Object> mSettings;

        public SettingAdapter(List<SettingCategory> settings) {
            updateData(settings, false);
        }

        public void updateData(List<SettingCategory> settings) {
            updateData(settings, true);
        }

        public void updateData(List<SettingCategory> settings, boolean notify) {
            List<Object> newList =  new LinkedList<Object>();
            for(SettingCategory category : settings) {
                newList.add(category);
                Iterator<SettingItem> i = category.iterator();
                while(i.hasNext()) {
                    newList.add(i.next());
                }
            }
            mSettings = newList;
            if(notify) notifyDataSetChanged();
        }

        @Override
        public SettingViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = null;
            switch (viewType) {
                case 1:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.category_setting, viewGroup, false);
                    break;
                case 2:
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_setting, viewGroup, false);
                    break;
            }
            return new SettingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SettingViewHolder settingViewHolder, int i) {
            if(mSettings.get(i) instanceof SettingItem) {
                SettingItem item = (SettingItem) mSettings.get(i);
                settingViewHolder.setText(item.getText());
                settingViewHolder.setOnClickListener(item.getOnClickListener());
            } else if(mSettings.get(i) instanceof SettingCategory) {
                SettingCategory category = (SettingCategory) mSettings.get(i);
                settingViewHolder.setText(category.getName());
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(mSettings.get(position) instanceof SettingCategory)
                return 1;
            else if(mSettings.get(position) instanceof SettingItem)
                return 2;
            else
                return 0;
        }

        @Override
        public int getItemCount() {
            return mSettings.size();
        }
    }

    private static class SettingsActivityHandler extends Handler {

        private final ScanApplication mApplication;
        private final WeakReference<SettingsActivity> mActivity;

        public SettingsActivityHandler(SettingsActivity activity) {
            mApplication = (ScanApplication) activity.getApplication();
            mActivity = new WeakReference<SettingsActivity>(activity);
        }

        private void showMessageDialog(SettingsActivity activity, String msg) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            dialogBuilder.setTitle("登录");
            dialogBuilder.setMessage(msg);
            dialogBuilder.setCancelable(false);
            dialogBuilder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialogBuilder.show();
        }

        @Override
        public void handleMessage(Message msg) {
            final SettingsActivity activity = mActivity.get();
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
                        View inputView = LayoutInflater.from(activity).inflate(R.layout.dialog_input,null, false);
                        final EditText editText = inputView.findViewById(R.id.editText);
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                        dialogBuilder.setTitle("登录");
                        dialogBuilder.setMessage("请输入您收到的验证码");
                        dialogBuilder.setView(inputView);
                        dialogBuilder.setCancelable(false);
                        dialogBuilder.setPositiveButton("登录", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.showWaitingDialog("正在登录", APIHelper.login(mApplication, SettingsActivityHandler.this, 2, bundle.getString("phone_number"), editText.getText().toString()));
                                dialog.dismiss();
                            }
                        });
                        dialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        dialogBuilder.show();
                    } else {
                        showMessageDialog(activity, result == null ? "请检查网络连接" : result.get("msg").getAsString());
                    }
                    activity.dismissWaitingDialog();
                    break;
                case 2:
                    if(result != null && result.get("code").getAsInt() == 200) {
                        mApplication.setToken(result.get("token").getAsString());
                        activity.recreate();
                    } else {
                        showMessageDialog(activity, result == null ? "请检查网络连接" : result.get("msg").getAsString());
                    }
                    activity.dismissWaitingDialog();
                    break;
                case 3:
                    if(result != null && result.get("code").getAsInt() != 500) {
                        mApplication.setToken("");
                        activity.recreate();
                    } else {
                        showMessageDialog(activity, result == null ? "请检查网络连接" : result.get("msg").getAsString());
                    }
                    activity.dismissWaitingDialog();
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private ScanApplication mApplication;
    private SettingsActivityHandler mHandler;
    private RecyclerView mRecyclerView;
    private SettingAdapter mAdapter;
    private List<SettingCategory> mSettings;
    private Dialog mWaitingDialog;
    private Thread mWaitingThread;

    protected void showWaitingDialog(String msg, Thread thread) {
        mWaitingThread = thread;
        View waitingView = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.dialog_waiting,null, false);
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
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
        mWaitingDialog = dialogBuilder.create();
        mWaitingDialog.show();
    }

    protected void dismissWaitingDialog() {
        if(mWaitingDialog != null) {
            mWaitingDialog.dismiss();
            mWaitingDialog = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mApplication = (ScanApplication) getApplication();
        mSettings = new LinkedList<SettingCategory>();
        mHandler = new SettingsActivityHandler(this);
        mRecyclerView = findViewById(R.id.recyclerView);
        SettingCategory accountSettingCategory = new SettingCategory();
        accountSettingCategory.setName("账户");
        mSettings.add(accountSettingCategory);
        SettingCategory aboutSettingCategory = new SettingCategory();
        aboutSettingCategory.setName("关于");
        SettingItem thanksItem = new SettingItem();
        thanksItem.setText("感谢");
        thanksItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LongTextActivity.class);
                intent.putExtra("text", getString(R.string.thanks));
                startActivity(intent);
            }
        });
        aboutSettingCategory.addItem(thanksItem);
        SettingItem licenseItem = new SettingItem();
        licenseItem.setText("许可证");
        licenseItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LongTextActivity.class);
                intent.putExtra("text", getString(R.string.license));
                startActivity(intent);
            }
        });
        aboutSettingCategory.addItem(licenseItem);
        mAdapter = new SettingAdapter(mSettings);
        mSettings.add(aboutSettingCategory);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        for(SettingCategory category : mSettings) {
            if(category.getName().equals("账户")) {
                category.clear();
                SettingItem item = new SettingItem();
                if(mApplication.isLogin()) {
                    item.setText("注销");
                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showWaitingDialog("正在注销", APIHelper.logout(mApplication, mHandler, 3));
                        }
                    });
                } else {
                    item.setText("使用手机号登录");
                    item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            View inputView = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.dialog_input,null, false);
                            final EditText editText = inputView.findViewById(R.id.editText);
                            editText.setInputType(InputType.TYPE_CLASS_PHONE);
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
                            dialogBuilder.setTitle("登录");
                            dialogBuilder.setMessage("请输入您的手机号码");
                            dialogBuilder.setView(inputView);
                            dialogBuilder.setCancelable(false);
                            dialogBuilder.setPositiveButton("登录", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    showWaitingDialog("正在登录", APIHelper.readyLogin(mApplication, mHandler, 1, editText.getText().toString()));
                                    dialog.dismiss();
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
                    });
                }
                category.addItem(item);
            }
        }
        mAdapter.updateData(mSettings);
    }

}
