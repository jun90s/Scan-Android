package com.jun90.projects.scan.android;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GalleryDataHandler {

    public static class Image {

        private String mId, mName, mFilename;
        private Date mCreated;
        private Bitmap mThumbnail, mImage;
        private File mFile;

        public String getId() {
            return mId;
        }

        public void setId(String id) {
            mId = id;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getFilename() {
            return mFilename;
        }

        public void setFilename(String filename) {
            mFilename = filename;
        }

        public Date getCreated() {
            return mCreated;
        }

        public void setCreated(Date created) {
            mCreated = created;
        }

        public Bitmap getThumbnail() {
            return mThumbnail;
        }

        public void setThumbnail(Bitmap thumbnail) {
            mThumbnail = thumbnail;
        }

        public Bitmap getImage() {
            return mImage;
        }

        public void setImage(Bitmap image) {
            mImage = image;
        }

        public File getFile() {
            return mFile;
        }

        public void setFile(File file) {
            mFile = file;
        }

    }

    public static class ImageAdapter extends RecyclerView.Adapter<MainActivity.ImageAdapter.CardHolder> {

        public static class CardHolder extends RecyclerView.ViewHolder {

            private View mView;
            private TextView mTextView;
            private ImageView mImageView, mDeleteImageView;

            public CardHolder(View itemView) {
                super(itemView);
                mView = itemView;
                mTextView = itemView.findViewById(R.id.textView);
                mImageView = itemView.findViewById(R.id.imageView);
                mDeleteImageView = itemView.findViewById(R.id.deleteImageView);
            }

            public void setTitle(String title) {
                mTextView.setText(title);
            }

            public void setImage(Bitmap bitmap) {
                mImageView.setImageBitmap(bitmap);
            }

            public void setImageViewOnClickListener(View.OnClickListener onClickListener) {
                mImageView.setOnClickListener(onClickListener);
            }

            public void setDeleteButtonOnClickListener(View.OnClickListener onClickListener) {
                mDeleteImageView.setOnClickListener(onClickListener);
            }

        }

        private ScanApplication mApplication;
        private List<Image> mImages;
        private MainActivity mActivity;
        private Handler mHandler;

        public ImageAdapter(MainActivity activity, Handler handler, List<Image> images) {
            mApplication = (ScanApplication) activity.getApplication();
            mActivity = activity;
            mHandler = handler;
            mImages = images;
        }

        @Override
        public CardHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new CardHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_image, viewGroup, false));
        }

        public void onBindViewHolder(CardHolder cardHolder, int i) {
            final Image image = mImages.get(i);
            cardHolder.setTitle(image.getName());
            cardHolder.setImage(image.getThumbnail());
            cardHolder.setImageViewOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(image.getFile() != null) {
                        mActivity.openImage(image.getFile());
                    } else if(!mActivity.isRefreshing()) {
                        mActivity.downloadImage(image);
                    }
                }
            });
            cardHolder.setDeleteButtonOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mActivity.isRefreshing()) return;
                    if(image.getFile() != null) {
                        image.getFile().delete();
                        mActivity.showMessageDialog("删除", "本地图像已删除");
                    }
                    if(!image.getId().isEmpty())
                        mActivity.showWaitingDialog("正在删除云端图像", APIHelper.deleteImage(mApplication, mHandler, 3, image.getId()));
                    else
                        mActivity.refresh();
                }
            });
        }

        public int getItemCount() {
            return mImages.size();
        }

    }

    public static class MainActivityHandler extends Handler {

        private final ScanApplication mApplication;
        private final WeakReference<MainActivity> mActivity;

        public MainActivityHandler(MainActivity activity) {
            mApplication = (ScanApplication) activity.getApplication();
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = mActivity.get();
            if (activity == null) return;
            JsonObject result = null;
            final Bundle bundle = msg.getData();
            if(bundle != null) {
                String s = bundle.getString("result");
                if(s != null) result = new JsonParser().parse(s).getAsJsonObject();
            }
            switch (msg.what) {
                case 1:
                    activity.refreshLocal();
                    break;
                case 2:
                    activity.setRefreshing(false);
                    break;
                case 3:
                    if(result != null && result.get("code").getAsInt() == 200) {
                        activity.showMessageDialog("删除", "云端图像已删除");
                        activity.refresh();
                    } else {
                        activity.showMessageDialog("删除", result == null ? "请检查网络连接" : result.get("msg").getAsString());
                    }
                    activity.dismissWaitingDialog();
                    break;
                case 4:
                    activity.dismissWaitingDialog();
                    if(bundle == null || bundle.getString("filename") == null || bundle.getString("id") == null) {
                        activity.showMessageDialog("下载", "下载失败");
                    } else {
                        File imageFile = new File(mApplication.getScanDir() + "/" + bundle.getString("filename"));
                        ScanSQLiteOpenHelper helper = new ScanSQLiteOpenHelper(mApplication);
                        SQLiteDatabase db = helper.getWritableDatabase();
                        db.beginTransaction();
                        try {
                            db.execSQL("UPDATE `image` SET `filename` = ? WHERE `id` = ?",
                                    new Object[]{bundle.getString("filename"), bundle.getString("id")});
                            db.setTransactionSuccessful();
                        } catch (Exception e) {
                            activity.showMessageDialog("下载", "程序异常");
                            imageFile.delete();
                            activity.finish();
                            return;
                        } finally {
                            db.endTransaction();
                            helper.close();
                        }
                        activity.openImage(imageFile);
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    }

    public static class CanProhibitScrollingLinearLayoutManager extends LinearLayoutManager {

        private boolean mProhibitScroll = false;

        public CanProhibitScrollingLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        @Override
        public boolean canScrollVertically() {
            return !mProhibitScroll && super.canScrollVertically();
        }

        @Override
        public boolean canScrollHorizontally() {
            return !mProhibitScroll && super.canScrollHorizontally();
        }

        public void prohibitScroll(boolean prohibit) {
            mProhibitScroll = prohibit;
        }

    }

    private ScanApplication mApplication;
    private MainActivityHandler mHandler;
    private Toolbar mToolbar;
    private ActionBar mActionBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private CanProhibitScrollingLinearLayoutManager mRecyclerViewLayoutManager;
    private ImageAdapter mAdapter;
    private Dialog mWaitingDialog;
    private Thread mWaitingThread;
    private Map<String, Bitmap> mRemoteThumbnails;
    private List<Image> mImages;
    private long mDownloadTaskId = -1;
    private DownloadManager mDownloadManager;

    public void cancelDownloadImage() {
        mDownloadManager.remove(mDownloadTaskId);
    }

    public void downloadImage(final Image image) {
        final File file = mApplication.getAvailableFile(image.getName());
        DownloadManager.Request request = new DownloadManager.Request(APIHelper.getImageDownloadUri(mApplication, image.getId()));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setMimeType("image/jpeg");
        request.setDestinationUri(Uri.fromFile(file));
        mDownloadTaskId = mDownloadManager.enqueue(request);
        Thread t = new Thread() {
            private int status = DownloadManager.STATUS_RUNNING;
            @Override
            public void run() {
                while(true) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(mDownloadTaskId);
                    Cursor c = mDownloadManager.query(query);
                    if(c.moveToNext()) {
                        status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    }
                    c.close();
                    if(status == DownloadManager.STATUS_SUCCESSFUL) {
                        Message message = new Message();
                        message.what = 4;
                        Bundle bundle = new Bundle();
                        bundle.putString("id", image.getId());
                        bundle.putString("filename", file.getName());
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                        image.setFile(file);
                        return;
                    }
                    if(status == DownloadManager.STATUS_FAILED) {
                        file.delete();
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                mHandler.sendEmptyMessage(4);
            }
        };
        t.start();
        showWaitingDialog("正在下载", t);
    }

    protected void openImage(File image) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(image), "image/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApplication = (ScanApplication) getApplication();
        mHandler = new MainActivityHandler(this);
        mDownloadManager = (DownloadManager) mApplication.getSystemService(DOWNLOAD_SERVICE);
        mRemoteThumbnails = new HashMap<String, Bitmap>();
        mImages = new LinkedList<Image>();
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_settings);
        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });
        mAdapter = new ImageAdapter(this, mHandler, mImages);
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerViewLayoutManager = new CanProhibitScrollingLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mRecyclerViewLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void interruptWaitingThread() {
        if(mWaitingThread != null && mWaitingThread.isAlive()) {
            try {
                mWaitingThread.interrupt();
            } catch (SecurityException e) {}
            mWaitingThread = null;
        }
    }

    @Override
    protected void onPause() {
        interruptWaitingThread();
        dismissWaitingDialog();
        setRefreshing(false);
        super.onPause();
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
                cancelDownloadImage();
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

    protected boolean isRefreshing() {
        return mSwipeRefreshLayout.isRefreshing();
    }

    protected void setRefreshing(boolean refreshing) {
        mRecyclerViewLayoutManager.prohibitScroll(refreshing);
        if(!refreshing) mAdapter.notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    protected void refresh() {
        setRefreshing(true);
        mApplication.clear();
        mWaitingThread = APIHelper.listImages(mApplication, mHandler, 1, MainActivity.this);
    }

    protected void refreshLocal() {
        mWaitingThread = new Thread() {
            @Override
            public void run() {
                ScanSQLiteOpenHelper helper = new ScanSQLiteOpenHelper(getApplicationContext());
                SQLiteDatabase db = helper.getWritableDatabase();
                mImages.clear();
                Cursor c = db.rawQuery("SELECT * FROM `image` ORDER BY `Created` DESC", null);
                while(c.moveToNext()) {
                    Image image = new Image();
                    try {
                        image.setId(c.getString(c.getColumnIndex("id")));
                        image.setName(c.getString(c.getColumnIndex("name")));
                        image.setFilename(c.getString(c.getColumnIndex("filename")));
                        image.setCreated(mApplication.getDateFormat().parse(c.getString(c.getColumnIndex("created"))));
                    } catch (ParseException e) {
                        continue;
                    }
                    if(mRemoteThumbnails.containsKey(image.getId())) image.setThumbnail(mRemoteThumbnails.get(image.getId()));
                    if(!image.getFilename().isEmpty()) {
                        image.setFile(new File(mApplication.getScanDir() + "/" + image.getFilename()));
                        image.setImage(BitmapFactory.decodeFile(image.getFile().getAbsolutePath()));
                        image.setThumbnail(EditImageView.makeThumbnail(image.getImage(), 1000, 1000));
                    }
                    mImages.add(image);
                }
                helper.close();
                mHandler.sendEmptyMessage(2);
            }
        };
        mWaitingThread.start();
    }

    @Override
    public void handleGalleryData(String data) {
        if(data == null) return;
        JsonObject jsonObject = new JsonParser().parse(data).getAsJsonObject();
        if(jsonObject.get("code").getAsInt() != 200) return;
        mRemoteThumbnails.clear();
        JsonArray imageArray = jsonObject.get("images").getAsJsonArray();
        Iterator<JsonElement> i = imageArray.iterator();
        while(i.hasNext()) {
            JsonObject imageObject = i.next().getAsJsonObject();
            Image image = new Image();
            image.setId(imageObject.get("id").getAsString());
            image.setName(imageObject.get("name").getAsString());
            try {
                image.setCreated(mApplication.getDateFormat().parse(imageObject.get("created").getAsString()));
            } catch (ParseException e) {
                continue;
            }
            byte[] thumbnailData = Base64.decode(imageObject.get("image").getAsString().split("base64,", 2)[1], Base64.DEFAULT);
            if(thumbnailData == null) continue;
            image.setThumbnail(BitmapFactory.decodeByteArray(thumbnailData, 0, thumbnailData.length));

            ScanSQLiteOpenHelper helper = new ScanSQLiteOpenHelper(getApplicationContext());
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                Cursor c = db.rawQuery("SELECT * FROM `image` WHERE `id` = ?", new String[] {image.getId()});
                if(c.moveToNext()) {
                    db.execSQL("UPDATE `image` SET `name` = ?, `created` = ? WHERE `id` = ?",
                            new Object[]{image.getName(), mApplication.getDateFormat().format(image.getCreated()), image.getId()});
                } else {
                    db.execSQL("INSERT INTO `image` (`id`, `name`, `created`) VALUES (?, ?, ?)",
                            new Object[]{image.getId(), image.getName(), mApplication.getDateFormat().format(image.getCreated())});
                }
                db.setTransactionSuccessful();
            } catch (SQLException e) {
                continue;
            } finally {
                db.endTransaction();
                helper.close();
            }
            mRemoteThumbnails.put(image.getId(), image.getThumbnail());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.new_from_camera:
                interruptWaitingThread();
                setRefreshing(false);
                intent = new Intent(getApplicationContext(), WorkbenchActivity.class);
                intent.putExtra("from", 1);
                startActivityForResult(intent, 1);
                return true;
            case R.id.new_from_gallery:
                interruptWaitingThread();
                setRefreshing(false);
                intent = new Intent(getApplicationContext(), WorkbenchActivity.class);
                intent.putExtra("from", 2);
                startActivityForResult(intent, 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1 && resultCode == RESULT_OK)
            refresh();
        else
            super.onActivityResult(requestCode, resultCode, data);
    }
}
