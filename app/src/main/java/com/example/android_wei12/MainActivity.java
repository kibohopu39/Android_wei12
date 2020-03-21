package com.example.android_wei12;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.util.prefs.PreferenceChangeEvent;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class MainActivity extends AppCompatActivity {
    private ConnectivityManager cmgr;
    private MyReceiver myReceiver;
    private TextView mesg;
    private ImageView img;
    private boolean isAllowSDCard;
    private  File downloadDir;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //確認授權狀況
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //沒有的話，跳出詢問
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE},123);
        }
        else{
            //有的話
            isAllowSDCard=true;
            init();
        }
    }

    private void init(){
        if(isAllowSDCard) {
            downloadDir= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
        progressDialog=new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("downloading...");

        //回報
        cmgr=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        myReceiver=new MyReceiver();
        IntentFilter filter=new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);//過濾 Action
        //如果有多個過濾，可以透過 addAction 加入: filter.addAction();
        filter.addAction("brad");
        registerReceiver(myReceiver,filter);//過濾廣播

        img=findViewById(R.id.img);
        mesg=findViewById(R.id.mesg);
    }

    //權限回報
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAllowSDCard=true;
        }else{
            isAllowSDCard=false;
        }


    }

    //生命週期設定廣播存活時段
    @Override
    public void finish() {
        unregisterReceiver(myReceiver);
        super.finish();
    }
    private boolean isConnectNetwork(){
        //沒聯網-->傳回 null
        NetworkInfo networkInfo=cmgr.getActiveNetworkInfo();
        //有無運作
        return networkInfo!=null && networkInfo.isConnectedOrConnecting();
    }
    private boolean isWifiConnected(){
        NetworkInfo network=cmgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return network!=null && network.isConnected();
    }
    private class MyReceiver extends BroadcastReceiver{
        //廣播接收器
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("brad")){
                String data=intent.getStringExtra("data");
                mesg.setText(data);
            }else if(intent.getAction().equals(ConnectivityManager.EXTRA_CAPTIVE_PORTAL)){
                test1(null);
            }


        }
    }
    public void test1(View view){
        Log.v("wei","isNetwork="+isConnectNetwork());
    }
    public void test2(View view){
        Log.v("wei","isWifi="+isWifiConnected());
    }
    public void test3(View view){//抓網頁原始碼
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url=new URL("https://bradchao.com/wp");
                    HttpsURLConnection conn=(HttpsURLConnection) url.openConnection();
                    conn.setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });

                    conn.connect();
                    //明碼傳送 http => Android 8 後才改成 https
                    //要用明碼傳送，在 manifest 裡多加一條 android:usesCleartextTraffic="true"
                    //conn.getInputStream();//取得最底層串流
                    BufferedReader reader=
                            new BufferedReader(
                                    new InputStreamReader(conn.getInputStream())
                            );
                    String line;StringBuffer sb=new StringBuffer();
                    while ((line=reader.readLine())!=null){
                        //Log.v("wei",line);
                        sb.append(line+"\n");
                    }

                    reader.close();

                    Intent intent=new Intent("brad");
                    intent.putExtra("data",sb.toString());
                    sendBroadcast(intent);//Context ==> Activity,Service,Application,這些可以發送
                }catch(Exception e){
                    Log.v("wei",e.toString());
                }
            }
        }.start();
    }
    public void test4(View view){//抓網頁圖片
        new Thread(){
            @Override
            public void run() {
                super.run();
                fetch();
            }
        }.start();
    }
    private Bitmap bitmap;
    private void fetch(){
        try {
            URL url=new URL("https://www.bradchao.com/wp/wp-content/uploads/2019/09/%E5%B0%81%E9%9D%A21129-1-01-757x1024.jpg");
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.connect();

            bitmap=BitmapFactory.decodeStream(conn.getInputStream());
            uihandler.sendEmptyMessage(0);

        } catch (Exception e) {
            Log.v("wei",e.toString());
        }
    }
    private uihandler uihandler=new uihandler();
    private class uihandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) img.setImageBitmap(bitmap);
            if (msg.what==1) progressDialog.dismiss();
            if (msg.what == 2 ) showPDF();
        }
    }

    public void test5(View view){
        if(!isAllowSDCard) return;//假設沒有同意權限
        new Thread(){
            @Override
            public void run() {
                super.run();
                fetchPDF();
            }
        }.start();
    }
    private void fetchPDF(){
        try {
            URL url=new URL("https://pdfmyurl.com/?url=https://www.bradchao.com/wp/%e9%97%9c%e6%96%bc%e6%88%91%e5%80%91");
            HttpURLConnection conn=(HttpURLConnection) url.openConnection();


            conn.connect();
            //輸入
            File downloadFile=new File(downloadDir,"picture1.pdf");
            //輸出
            FileOutputStream fout=new FileOutputStream(downloadFile);
            byte[]buf=new byte[4096*1024];
            BufferedInputStream bin=
                    new BufferedInputStream(conn.getInputStream());
            int len=-1;
            while((len=bin.read(buf))!=-1){
                fout.write(buf,0,len);
            }
            bin.close();
            fout.flush();
            fout.close();
            Log.v("wei","OK");
        }catch (Exception e){
            Log.v("wei",e.toString());
        }finally {
            uihandler.sendEmptyMessage(1);
        }
    }

    private void showPDF(){
        File file=new File(downloadDir,"google.pdf");
        Uri pdfuri= FileProvider.getUriForFile(this,getPackageName()+".provider",file);

        //顯示檔案的權限要給予，否則對象應用程式無法開啟
        Intent intent = new Intent(Intent.ACTION_VIEW);//有符合可以打開 Action_View 都會來你選擇開
        intent.setDataAndType(pdfuri,"application/pdf");
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);//把額外的資訊給intent，特定應用程式才可以開
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);
    }

    public void test6(View view){
        Intent intent=new Intent(Intent.ACTION_SEND);
        intent.setType();
    }
}
