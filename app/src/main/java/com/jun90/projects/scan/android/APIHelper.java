package com.jun90.projects.scan.android;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class APIHelper {

    private static byte[] paramsToData(Map<String, String> params) {
        StringBuffer buffer = new StringBuffer();
        for(Map.Entry<String, String> e : params.entrySet()) {
            if(buffer.length() > 0) buffer.append("&");
            try {
                buffer.append(URLEncoder.encode(e.getKey(), "utf-8") + "=" + URLEncoder.encode(e.getValue(), "utf-8"));
            } catch (UnsupportedEncodingException err) { }
        }
        return buffer.toString().getBytes();
    }

    private static String postURLEncodedData(String url, Map<String, String> params) {
        HttpURLConnection connection = null;
        String result = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            byte[] data = paramsToData(params);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(data.length));
            OutputStream out = connection.getOutputStream();
            out.write(data);
            out.flush();
            out.close();
            if(connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuffer buffer = new StringBuffer();
                String line;
                while((line = reader.readLine()) != null)
                    buffer.append(line);
                reader.close();
                result = buffer.toString();
            }
        } catch (IOException e) {

        } finally {
            if(connection != null) connection.disconnect();
        }
        return result;
    }

    public static Thread readyLogin(final ScanApplication application, final Handler handler, final int what, final String phoneNumber) {
        if(!application.isAPIEnable()) {
            handler.sendEmptyMessage(what);
            return null;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("apikey", application.getAPIKey());
                params.put("token", application.getAPIKey());
                params.put("phonenumber", phoneNumber);
                String result = postURLEncodedData("http://localhost/web/login/ready", params);
                if(result != null) {
                    Message msg = new Message();
                    msg.what = what;
                    Bundle bundle = new Bundle();
                    bundle.putString("phone_number", phoneNumber);
                    bundle.putString("result", result);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(what);
                }
            }
        });
        t.start();
        return t;
    }

    public static Thread login(final ScanApplication application, final Handler handler, final int what, final String phoneNumber, final String code) {
        if(!application.isAPIEnable()) {
            handler.sendEmptyMessage(what);
            return null;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("apikey", application.getAPIKey());
                params.put("token", application.getAPIKey());
                params.put("phonenumber", phoneNumber);
                params.put("code", code);
                String result = postURLEncodedData("http://localhost/web/login", params);
                if(result != null) {
                    Message msg = new Message();
                    msg.what = what;
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(what);
                }
            }
        });
        t.start();
        return t;
    }

    public static Thread logout(final ScanApplication application, final Handler handler, final int what) {
        if(!application.isAPIEnable() || !application.isLogin()) {
            handler.sendEmptyMessage(what);
            return null;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("apikey", application.getAPIKey());
                params.put("token", application.getToken());
                String result = postURLEncodedData("http://localhost/web/logout", params);
                if(result != null) {
                    Message msg = new Message();
                    msg.what = what;
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(what);
                }
            }
        });
        t.start();
        return t;
    }

    public static Thread newProject(final ScanApplication application, final Handler handler, final int what, final String tasks, final Bitmap bitmap) {
        if(!application.isAPIEnable() || !application.isLogin()) {
            handler.sendEmptyMessage(what);
            return null;
        }
        final byte[] image = application.bitmapToByteArray(bitmap);
        if(image == null) {
            handler.sendEmptyMessage(what);
            return null;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                String result = null;
                try {
                    connection = (HttpURLConnection) new URL("http://localhost/web/project").openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setRequestMethod("POST");
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=-com.jun90.projects.scan.android");
                    OutputStream out = connection.getOutputStream();
                    out.write(("---com.jun90.projects.scan.android\r\n").getBytes());
                    out.write(("Content-Disposition: form-data; name=\"apikey\"\r\n\r\n").getBytes());
                    out.write(application.getAPIKey().getBytes());
                    out.write("\r\n".getBytes());
                    out.write(("---com.jun90.projects.scan.android\r\n").getBytes());
                    out.write(("Content-Disposition: form-data; name=\"token\"\r\n\r\n").getBytes());
                    out.write(application.getToken().getBytes());
                    out.write("\r\n".getBytes());
                    out.write(("---com.jun90.projects.scan.android\r\n").getBytes());
                    out.write(("Content-Disposition: form-data; name=\"tasks\"\r\n\r\n").getBytes());
                    out.write(tasks.getBytes());
                    out.write("\r\n".getBytes());
                    out.write(("---com.jun90.projects.scan.android\r\n").getBytes());
                    out.write(("Content-Disposition: form-data; name=\"image\"; filename=\".jpg\"\r\n").getBytes());
                    out.write(("Content-Type: image/jpeg\r\n\r\n").getBytes());
                    out.write(image);
                    out.write("\r\n".getBytes());
                    out.write(("---com.jun90.projects.scan.android--\r\n").getBytes());
                    out.flush();
                    out.close();
                    if(connection.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuffer buffer = new StringBuffer();
                        String line;
                        while((line = reader.readLine()) != null)
                            buffer.append(line);
                        reader.close();
                        result = buffer.toString();
                    }
                } catch (IOException e) {

                } finally {
                    if(connection != null) connection.disconnect();
                }
                if(result != null) {
                    Message msg = new Message();
                    msg.what = what;
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(what);
                }
            }
        });
        t.start();
        return t;
    }

    public static Thread saveProject(final ScanApplication application, final Handler handler, final int what, final String project, final String name) {
        if(!application.isAPIEnable() || !application.isLogin()) {
            handler.sendEmptyMessage(what);
            return null;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("apikey", application.getAPIKey());
                params.put("token", application.getToken());
                params.put("project", project);
                params.put("name", name);
                String result = postURLEncodedData("http://localhost/web/project", params);
                if(result != null) {
                    Message msg = new Message();
                    msg.what = what;
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(what);
                }
            }
        });
        t.start();
        return t;
    }

    public static Thread listImages(final ScanApplication application, final Handler handler, final int what, final GalleryDataHandler galleryDataHandler) {
        if(!application.isAPIEnable() || !application.isLogin()) {
            handler.sendEmptyMessage(what);
            return null;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("apikey", application.getAPIKey());
                params.put("token", application.getToken());
                String result = postURLEncodedData("http://localhost/web/gallery", params);
                if(result != null) galleryDataHandler.handleGalleryData(result);
                handler.sendEmptyMessage(what);
            }
        });
        t.start();
        return t;
    }

    public static Thread deleteImage(final ScanApplication application, final Handler handler, final int what, final String image) {
        if(!application.isAPIEnable() || !application.isLogin()) {
            handler.sendEmptyMessage(what);
            return null;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("apikey", application.getAPIKey());
                params.put("token", application.getToken());
                params.put("image", image);
                params.put("action", "delete");
                String result = postURLEncodedData("http://localhost/web/gallery", params);
                if(result != null) {
                    Message msg = new Message();
                    msg.what = what;
                    Bundle bundle = new Bundle();
                    bundle.putString("result", result);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                } else {
                    handler.sendEmptyMessage(what);
                }
            }
        });
        t.start();
        return t;
    }

    public static Uri getImageDownloadUri(ScanApplication application, String image) {
        if(!application.isAPIEnable() || !application.isLogin()) return null;
        Map<String, String> params = new HashMap<String, String>();
        params.put("apikey", application.getAPIKey());
        params.put("token", application.getToken());
        params.put("image", image);
        params.put("action", "get");
        return Uri.parse("http://localhost/web/gallery?" + new String(paramsToData(params)));
    }

}
