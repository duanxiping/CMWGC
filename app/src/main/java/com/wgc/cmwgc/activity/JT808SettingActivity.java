package com.wgc.cmwgc.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.wgc.cmwgc.R;
import com.wgc.cmwgc.app.Config;
import com.wgc.cmwgc.db.NetStateDB;
import com.wgc.cmwgc.service.BeiDouService;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 功能： descriable
 * 作者： Administrator
 * 日期： 2017/4/13 11:48
 * 邮箱： descriable
 */
public class JT808SettingActivity extends AppCompatActivity {

    private String TAG = "JT808SettingActivity";

    @Bind(R.id.edit_ip)
    TextInputEditText editIp;
    @Bind(R.id.edit_port)
    TextInputEditText editPort;

    @Bind(R.id.progressBar1)
    ProgressBar pgBar;

    private String ip;
    private String port;

    private SharedPreferences spf;
    private SharedPreferences.Editor editor;

    private NetStateDB netStateDB = new NetStateDB(this);
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jt808_setting);
        ButterKnife.bind(this);
        initSP();
        initBorcast();
    }

    private void initSP(){

        spf = getSharedPreferences(Config.SPF_MY,MODE_PRIVATE);
        editor = spf.edit();

//        db = netStateDB.getReadableDatabase();
//        Cursor cursor = db.query("table_jt808", new String[]{"jt808_id,jt808_ip, jt808_port"}, null, null, null, null, null, null);
//        while (cursor.moveToNext()) {
//            int id = cursor.getInt(0); //获取id
//            ip = cursor.getString(1);//获取ip
//            port = cursor.getString(2);//获取网络
//        }

//        ip = spf.getString( Config.SP_SERVICE_IP, ip);
//        port = spf.getString( Config.SP_SERVICE_PORT, port);

//        if (TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)){
//            editPort.setText(getResources().getString(R.string.default_port));
//            editIp.setText(getResources().getString(R.string.default_ip));
//        }else{
//            editIp.setText(ip);
//            editPort.setText(port);
//        }
    }
    /**
     * @param context
     */
    public static void startAction(Activity context) {
        Intent intent = new Intent(context, JT808SettingActivity.class);
        context.startActivity(intent);
    }


    @OnClick(R.id.btn_jt_enable)
    public void onClick() {
        saveAddress();

    }

    private int times =1000;
    private void saveAddress(){
        ip = editIp.getText().toString().trim();
        port = editPort.getText().toString().trim();
        Log.e("---------ip地址-------" ,ip+"");
        Log.e("---------port端口号-------" ,ip+"");
        if(TextUtils.isEmpty(ip) || TextUtils.isEmpty(port)){
            Toast.makeText(this,"IP或者端口不能为空",Toast.LENGTH_LONG).show();
            return;
        }

//        db = netStateDB.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put("jt808_ip" , ip);
//        values.put("jt808_port" , port);

        editor.putString(Config.SP_SERVICE_IP,ip);
        editor.putString(Config.SP_SERVICE_PORT,port);
        editor.commit();
        Toast.makeText(this,"开始启动部标",Toast.LENGTH_LONG).show();
        Intent intent = new Intent("my_bro_is_enable_jt");
        intent.putExtra("ip",ip);
        intent.putExtra("port",port);
        sendBroadcast(intent);

        pgBar.setVisibility(View.VISIBLE);


    }
    private boolean client;
    private boolean jtNum;
    private void initBorcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction("MY_JT808_HEARTbeat");
        filter.addAction("MY_Websocket_HEARTbeat_erro");
        registerReceiver(receiver, filter);
    }

    /**
     * 广播接收器
     */
    private int clent;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("MY_JT808_HEARTbeat")) {

                client = intent.getBooleanExtra("jt_heart",false);
                Log.e(TAG, "接收到的广播是：===="+client);

                clent ++;
                    if (client = true) {
//                        Toast.makeText(context, "开始启动部标", Toast.LENGTH_LONG).show();
                        if (clent == 1) {
                            pgBar.setVisibility(View.GONE);
//                            editor.commit();
                            Intent intent1 = new Intent();
                            intent1.setClass(context, DeviceActivity.class);
                            intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent1);
//                        finish();
                        }
                    }
            }

            if (intent.getAction().equals("MY_Websocket_HEARTbeat_erro")){

                jtNum = intent.getBooleanExtra("websocket_heart_erro",false);
                Log.e(TAG, "接收到的广播是：===="+jtNum);
                clent++;
//                if (jtNum){
                    if (clent == 1){
                        Toast.makeText(context,"部标连接异常或部标不正确！" , Toast.LENGTH_LONG).show();
                        pgBar.setVisibility(View.GONE);
//                    }

                }
            }

        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (receiver != null){
            unregisterReceiver(receiver);
        }
    }
}
