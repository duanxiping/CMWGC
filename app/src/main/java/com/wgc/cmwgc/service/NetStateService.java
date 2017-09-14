package com.wgc.cmwgc.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

import com.wgc.cmwgc.Until.SystemTools;
import com.wgc.cmwgc.app.Config;
import com.wgc.cmwgc.db.NetStateDB;
import com.wicare.wistorm.WiStormApi;

public class NetStateService extends Service {

    private final String TAG = NetStateService.class.getName();

    private boolean isServiceRunning;
    private boolean isNetworkAvailable;
    private boolean HeartBeat = false;
    private boolean JT808HeartBeat = false;
    private boolean WebSocketHeartBeat = false;
    private boolean LocationService = false;
    private boolean SpeedAlertService = false;
    private boolean BeiDouServices = false;

    private NetStateDB netStateDB = new NetStateDB(this);
    private SQLiteDatabase db;

    public NetStateService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initRecever();
        inserNetState();
        mTasks.run();
    }

    private void initRecever(){

        IntentFilter filter = new IntentFilter();
        filter.addAction("MY_HEARTbeat");
        filter.addAction("MY_Websocket_HEARTbeat");
        filter.addAction("MY_JT808_HEARTbeat");
        filter.addAction("Net_LocationService_HEARTbeat");
        filter.addAction("Net_SpeedAlertService_HEARTbeat");
        filter.addAction("Net_BeiDouService_HEARTbeat");
        registerReceiver(receiver, filter);
    }

    /**
     * 广播接收器
     */

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals("MY_HEARTbeat")) {
                HeartBeat = intent.getBooleanExtra("Heart", false);
                Log.w(TAG, "收到定位广播 ：" + HeartBeat);
            } else if (intent.getAction().equals("MY_Websocket_HEARTbeat")) {
                WebSocketHeartBeat = intent.getBooleanExtra("websocket_heart", false);
                Log.w(TAG, "收到Websocket广播 ：" + " -- " + WebSocketHeartBeat);
            } else if (intent.getAction().equals("MY_JT808_HEARTbeat")) {
                Log.w(TAG, "收到JT808 服务广播 ：" + " -- " + WebSocketHeartBeat);
                JT808HeartBeat = intent.getBooleanExtra("jt_heart", false);


            }else if (intent.getAction().equals("Net_LocationService_HEARTbeat")) {
                LocationService = intent.getBooleanExtra("LocationService", false);

            }else if (intent.getAction().equals("Net_SpeedAlertService_HEARTbeat")) {
                SpeedAlertService = intent.getBooleanExtra("SpeedAlertService", false);

            }else if (intent.getAction().equals("Net_BeiDouService_HEARTbeat")) {
                BeiDouServices = intent.getBooleanExtra("BeiDouServices", false);
            }

        }
    };

    /**
     * 检查所有状态
     */
    private void checkAllStatus() {

        if (!SystemTools.isWorked(this, "com.wgc.cmwgc.service.HttpService")) {
            isServiceRunning = false;
        } else {
            isServiceRunning = true;
        }
        if (SystemTools.isNetworkAvailable(NetStateService.this)) {
            isNetworkAvailable = true;
        } else {
            isNetworkAvailable = false;
        }
    }


    private Runnable mTasks = new Runnable() {
        public void run() {
            checkAllStatus();//检查服务是否正在运行
        }
    };

    private void inserNetState(){

        db = netStateDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("net_time" , WiStormApi.getCurrentTime());

        if (!isServiceRunning){
            values.put("net_service" , "主服务不正常");
        }else {
            values.put("net_service" , "主服务正常");
        }
        if (!isNetworkAvailable){
            values.put("net_internet" , "网络不正常");
        }else {
            values.put("net_internet" , "网络正常");
        }
        if (!HeartBeat){
            values.put("net_location" , "定位不正常");
        }else {
            values.put("net_location" , "定位正常");
        }

        if (!WebSocketHeartBeat){
            values.put("net_websockte" , "默认链路不正常");
        }else {
            values.put("net_websockte" , "默认链路正常");
        }


        if (!JT808HeartBeat){
            values.put("net_jt808" , "JT808部标不正常");
        }else {
            values.put("net_jt808" , "JT808部标正常");
        }

        if (LocationService){
            values.put("net_location_service" , "定位服务不正常");
        }else {
            values.put("net_location_service" , "定位服务正常");
        }

        if (SpeedAlertService){
            values.put("net_web_service" , "默认链路服务不正常");
        }else {
            values.put("net_web_service" , "默认链路服务正常");
        }

        if (!BeiDouServices){
            values.put("net_jt808_service" , "JT808服务不正常");
        }else {
            values.put("net_jt808_service" , "JT808服务正常");
        }

        db.insert("net_state_table" , null , values);

        if (isNetworkAvailable&&isServiceRunning&&HeartBeat&&WebSocketHeartBeat){
            db.delete("net_state_table" , null , null);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
