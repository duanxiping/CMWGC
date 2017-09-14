package com.wgc.cmwgc.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.wgc.cmwgc.Until.SystemTools;
import com.wgc.cmwgc.Until.Utils;
import com.wgc.cmwgc.app.Config;

/**
 * Created by Administrator on 2016/12/19.
 */
public class CoreServer extends Service {

    private String TAG="CoreServer";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final String START_LOCATION = "start_my_location_service";
    private final String START_SPEED_ALERT = "start_my_speed_alert_service";
    private final String START_BEI_DOU = "start_my_bei_dou_service";
    private final String START_DEF_SERVER = "start_my_defualt_service";
    private final String START_NET_SERVER = "start_my_net_service";


    private final String HEART_BEAT = "MY_HEARTbeat";
    public static final String START_UPLPAD ="start_upload_data_incase_time_incorrect";
    public static final String ERROR_API = "api_error_happen";

    private int ONE_MINUTES = 1000 * 60;// five
    private ServiceBroadcast mServiceBroadcast;
    private Handler objHandler = new Handler();
    private  int numIsRun = 0;
    private  int numAgain = 0;
    private SharedPreferences spf;
    private SharedPreferences.Editor editor;
    private boolean isRunning;

    ///////////////////////////////////////////////////
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    startCoreServerAgin();
                    break;

                default:
                    break;
            }

        };
    };

    /**
     * 使用aidl 启动Service2
     */
    private StrongService startS2 = new StrongService.Stub() {
        @Override
        public void stopService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), Service2.class);
            getBaseContext().stopService(i);
        }

        @Override
        public void startService() throws RemoteException {
            Intent i = new Intent(getBaseContext(), Service2.class);
            getBaseContext().startService(i);
        }
    };

    /**
     * 在内存紧张的时候，系统回收内存时，会回调OnTrimMemory， 重写onTrimMemory当系统清理内存时从新启动Service2
     */
    @Override
    public void onTrimMemory(int level) {
		/*
		 * 启动service2
		 */
        startCoreServerAgin();

    }

    ////////////////////////////////////

    @Override
    public void onCreate() {
        super.onCreate();
        // 注册广播接收类
        initSP();
        mServiceBroadcast = new ServiceBroadcast();
        IntentFilter filter = new IntentFilter();
        filter.addAction(START_LOCATION);
        filter.addAction(START_SPEED_ALERT);
        filter.addAction(START_BEI_DOU);
        filter.addAction(START_DEF_SERVER);
        filter.addAction(HEART_BEAT);
        filter.addAction(ERROR_API);
        filter.addAction(START_NET_SERVER);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mServiceBroadcast, filter);
        objHandler.postDelayed(mTasks, 1000);
        Log.w(TAG, "核心服务：" + "onCreate()");

        /////////////////////////////////
//        Toast.makeText(CoreServer.this, "CoreServer 正在启动...", Toast.LENGTH_SHORT)
//                .show();
        Log.e(TAG, "CoreServer 正在启动..............." );
        startCoreServerAgin();
		/*
		 * 此线程用监听Service2的状态
		 */
        new Thread() {
            public void run() {
                while (true) {
                    boolean isRun = Utils.isServiceWork(CoreServer.this,
                            "com.lzg.strongservice.service.Service2");
                    if (!isRun) {
                        Message msg = Message.obtain();
                        msg.what = 1;
                        handler.sendMessage(msg);
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            };
        }.start();

    }


    /**
     * 判断Service2是否还在运行，如果不是则启动Service2
     */
    private void startCoreServerAgin() {
        boolean isRun = Utils.isServiceWork(CoreServer.this,
                "com.lzg.strongservice.service.Service2");
        if (isRun == false) {
            try {
                startS2.startService();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void initSP(){
        spf = getSharedPreferences(Config.SPF_MY,MODE_PRIVATE);
        editor = spf.edit();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        objHandler.removeCallbacks(mTasks);
        Intent service_again =new Intent(getApplicationContext(),CoreServer.class);
		startService(service_again);
        if (mServiceBroadcast != null){
            unregisterReceiver(mServiceBroadcast);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "核心服务：" + "onStartCommand()");
//        return START_STICKY;START_STICKY_COMPATIBILITY  START_REDELIVER_INTENT
        return START_REDELIVER_INTENT;
    }

    /**
     * @Description:发送广播启动定位服务
     * @param:
     * @return: void
     */
    private void startLocationService(){
        Intent location_service = new Intent(START_LOCATION);
        sendBroadcast(location_service);
    }


    /**
     * @Description:发送广播启动应用状态服务
     * @param:
     * @return: void
     */
    private void startNetStateService(){
        Intent net_service = new Intent(START_NET_SERVER);
        sendBroadcast(net_service);
    }


    /**
     * @Description:发送广播启动定位服务
     * @param:
     * @return: void
     */
    private void startSpeedAlertService(){
        Intent speed_alert_service = new Intent(START_SPEED_ALERT);
        sendBroadcast(speed_alert_service);
    }

    /**
     * @Description:发送广播启动北斗服务
     * @param:
     * @return: void
     */
    private void startBeiDouService(){
        Intent speed_alert_service = new Intent(START_BEI_DOU);
        sendBroadcast(speed_alert_service);
    }

    /**
     * @Description:发送广播启动默认的服务
     * @param:
     * @return: void
     */
    private void startDefualtService(){
        Intent speed_alert_service = new Intent(START_DEF_SERVER);
        sendBroadcast(speed_alert_service);
    }

    private void startUploadData(){
        Intent location_service = new Intent(START_UPLPAD);
        sendBroadcast(location_service);
    }


    boolean HeartBeat = false;
    class ServiceBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(START_LOCATION)){
                Log.e(TAG, "收到广播启动定位服务......" );
                Intent intent_service = new Intent(context,HttpService.class);
                context.startService(intent_service);
            }else if(intent.getAction().equals(START_SPEED_ALERT)){
                Intent intent_speed_enclosure = new Intent(context,SpeedEnclosureService.class);
                context.startService(intent_speed_enclosure);
            }else if(intent.getAction().equals(START_BEI_DOU)) {
                Intent intent_beidou = new Intent(context, BeiDouService.class);
                context.startService(intent_beidou);
            }else if(intent.getAction().equals(START_NET_SERVER)) {
                Intent intent_netstate = new Intent(context, NetStateService.class);
                context.startService(intent_netstate);
            }else if(intent.getAction().equals("MY_HEARTbeat")){
                HeartBeat = intent.getBooleanExtra("Heart",false);
                Log.e(TAG, "收到广播 是否在提交数据......" + HeartBeat );
            }
        }
    }


    private Runnable mTasks = new Runnable(){
        public void run(){
            Log.e(TAG, "核心服务心跳包！！！！！！！！--- "+ HeartBeat);
            if (!SystemTools.isWorked(CoreServer.this, "com.wgc.cmwgc.service.HttpService")) {
                Log.e(TAG, "startLocationService服务没运行" );
                startLocationService();

                isRunning = false;
                Intent intent = new Intent("Net_LocationService_HEARTbeat");
                intent.putExtra("LocationService",isRunning);
                sendBroadcast(intent);

            }else{
                Log.e(TAG, "startLocationService服务已经在运行" );
            }
            /*Websocket 上传数据版本*/
            if(!SystemTools.isWorked(CoreServer.this, "com.wgc.cmwgc.service.SpeedEnclosureService")){
                startSpeedAlertService();
                Log.e(TAG, "startSpeedAlertService服务没运行" );

                isRunning = false;
                Intent intent = new Intent("Net_SpeedAlertService_HEARTbeat");
                intent.putExtra("SpeedAlertService",isRunning);
                sendBroadcast(intent);
            }else{
                Log.e(TAG, "startSpeedAlertService服务已经在运行" );
            }
            /*JT808协议 上传数据版本*/
            if(!SystemTools.isWorked(CoreServer.this, "com.wgc.cmwgc.service.BeiDouService")){
                startBeiDouService();
                Log.e(TAG, "BeiDouService服务没运行" );

                isRunning = false;
                Intent intent = new Intent("Net_BeiDouService_HEARTbeat");
                intent.putExtra("BeiDouServices",isRunning);
                sendBroadcast(intent);

            }else{
                Log.e(TAG, "BeiDouService服务已经在运行" );
            }

             /*监听应用状态服务*/
            if(!SystemTools.isWorked(CoreServer.this, "com.wgc.cmwgc.service.NetStateService")){
                startNetStateService();
                Log.e(TAG, "NetStateService服务没运行" );
            }else{
                Log.e(TAG, "NetStateService服务已经在运行" );
            }
            /*JT808协议 默认服务上传数据版本*/
//            if(!SystemTools.isWorked(CoreServer.this, "com.wgc.cmwgc.service.DefaultServer")){
//                startDefualtService();
//                Log.e(TAG, "DefaultServer服务没运行" );
//            }else{
//                Log.e(TAG, "DefaultServer服务已经在运行" );
//            }
            numAgain ++;
            numIsRun ++;
            if(numAgain == 10){//10分钟再启动一次服务，（）
                startLocationService();
                Log.e(TAG, "startLocationService服务已重启了。。。。。。" );
                startBeiDouService();
                Log.e(TAG, "startBeiDouService服务已重启了。。。。。。" );
                startSpeedAlertService();
                Log.e(TAG, "startSpeedAlertService服务已重启了。。。。。。" );
//                startDefualtService();
                startNetStateService();
                Log.e(TAG, "startDefualtService服务已重启了。。。。。。" );
                numAgain = 0;
            }
            if(numIsRun == 3){
                if(!HeartBeat){
                    startUploadData();
                    HeartBeat = false;
                }
                numIsRun = 0;
            }
            objHandler.postDelayed(mTasks, ONE_MINUTES);
        }
    };

}
