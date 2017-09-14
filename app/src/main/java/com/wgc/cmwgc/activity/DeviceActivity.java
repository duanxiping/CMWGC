package com.wgc.cmwgc.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wgc.cmwgc.R;
import com.wgc.cmwgc.Until.SystemTools;
import com.wgc.cmwgc.app.Config;

import java.math.BigDecimal;
import java.util.Iterator;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2016/11/3.
 */
public class DeviceActivity extends AppCompatActivity {

    private Context mContext;

    private final String TAG = DeviceActivity.class.getName();
    private final int ONE_SECOND = 1000;
    private final int UPDATE_UI = 100;
    private LocationManager locationManager;

    @Bind(R.id.tv_app_info)
    TextView tvAppInfo;

    @Bind(R.id.tv_dev_info)
    TextView tvDevInfo;

    @Bind(R.id.device_title)
    TextView deviceTitle;

    @Bind(R.id.tv_pg_bar)
    TextView tvPgBar;

    @Bind(R.id.tv_enable_jt808)
    Button btnEnableJt808;

    @Bind(R.id.progressBar1)
    ProgressBar pgBar;

    private boolean isServiceRunning;
    private boolean isNetworkAvailable;
    private boolean HeartBeat = false;
    private boolean WebSocketHeartBeat = false;
    private boolean JT808HeartBeat = false;
    private SharedPreferences spf;
    private SharedPreferences.Editor editor;

    private String ip = "";
    private String port = "";

    private ProgressDialog progressDialog = null;

    public static ProgressDialog wait;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        ButterKnife.bind(this);
        initView();
//        progressDialog = ProgressDialog.show(this, null, null, true);

        pgBar.setVisibility(View.VISIBLE);
        tvPgBar.setVisibility(View.VISIBLE);
        btnEnableJt808.setVisibility(View.GONE);
        ButterKnife.bind(this);
        initSP();
        initBorcast();
//        btnEnableJt808Gone();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mHandler.postDelayed(mTasks, 0);
        /* 开启一个新线程，在新线程里执行耗时的方法 */
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                spandTimeMethod();// 耗时的方法
//                handler.sendEmptyMessage(0);// 执行耗时的方法之后发送消给handler
//            }
//
//        }).start();

    }


    @Override
    protected void onRestart() {
        super.onRestart();
        btnEnableJt808Gone();
    }

    private void initView() {

        deviceTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
//                DeviceActivity.startAction(AboutDeviceActivity.this);
                Intent intent = new Intent();
                intent.setClass(DeviceActivity.this,AboutDeviceActivity.class);
                startActivity(intent);
                return false;
            }
        });
    }





    /**
     * 注册广播
     */
    private void initBorcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("MY_HEARTbeat");
        filter.addAction("MY_Websocket_HEARTbeat");
        filter.addAction("MY_JT808_HEARTbeat");
        filter.addAction("MY_JT808_Defualt_HEARTbeat");
        filter.addAction(Config.SPEED_ENCLOSURE_BEIDOUSERVICE);
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
            }

        }
    };




    /**
     * @param context
     */
    public static void startAction(Activity context) {
        Intent intent = new Intent(context, DeviceActivity.class);
        context.startActivity(intent);
    }

    /**
     * 检查所有状态
     */
    private void checkAllStatus() {

        if (!SystemTools.isWorked(this, "com.wgc.cmwgc.service.HttpService")) {
            isServiceRunning = false;
        } else {
            isServiceRunning = true;
        }
        if (SystemTools.isNetworkAvailable(DeviceActivity.this)) {
            isNetworkAvailable = true;
        } else {
            isNetworkAvailable = false;
        }

        ip = spf.getString(Config.SP_SERVICE_IP,ip);
        port = spf.getString(Config.SP_SERVICE_PORT, port);

    }


    private Runnable mTasks = new Runnable() {
        public void run() {
            checkAllStatus();//检查服务是否正在运行
            mHandler.sendEmptyMessage(UPDATE_UI);
            mHandler.postDelayed(mTasks, ONE_SECOND);
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_UI:
                    updateUi();
                    initSP();
                    break;
            }
        }
    };

    private void initSP() {
        spf = getSharedPreferences(Config.SPF_MY, MODE_PRIVATE);
        editor = spf.edit();

        ip = spf.getString(Config.SP_SERVICE_IP,ip);
        port = spf.getString(Config.SP_SERVICE_PORT,port );

        Log.e(TAG,"IP:====="+ip+"PORT:====="+port);

    }

    private void updateUi() {
        String dingwei = "";
//        String lianlu = "";
        String service = "";
        String network = "";
        String jtlianlu = "";


        if (HeartBeat && isServiceRunning && WebSocketHeartBeat){
//            progressDialog.dismiss();
//            pgBar.setVisibility(View.GONE);
//            tvPgBar.setVisibility(View.GONE);
//            tvAppInfo.setVisibility(View.VISIBLE);
            btnEnableJt808.setVisibility(View.GONE);
            if(JT808HeartBeat){
                pgBar.setVisibility(View.GONE);
                tvPgBar.setVisibility(View.GONE);
                tvAppInfo.setVisibility(View.VISIBLE);
                tvDevInfo.setVisibility(View.VISIBLE);
                btnEnableJt808.setVisibility(View.GONE);
            }else {
                if (!ip.equals("") && !port.equals("")){

                    pgBar.setVisibility(View.GONE);
                    tvPgBar.setVisibility(View.GONE);
                    tvAppInfo.setVisibility(View.VISIBLE);
                    tvDevInfo.setVisibility(View.GONE);
                    btnEnableJt808.setVisibility(View.GONE);

                }else {
                    pgBar.setVisibility(View.GONE);
                    tvPgBar.setVisibility(View.GONE);
                    tvAppInfo.setVisibility(View.VISIBLE);
                    tvDevInfo.setVisibility(View.GONE);
                    btnEnableJt808.setVisibility(View.VISIBLE);
                }

            }

        }

        if (HeartBeat) {
            dingwei = "正 常";
        } else {
            dingwei = "异 常";
        }
//        if (WebSocketHeartBeat) {
//            lianlu = "正 常";
//        } else {
//            lianlu = "异 常";
//        }
        if (!isServiceRunning) {
            service = "服务异常";
        } else if ( isServiceRunning && WebSocketHeartBeat){
            service = "正 常";

        }else if (!WebSocketHeartBeat){
            service = "数据异常";
        }else if (!isServiceRunning && !WebSocketHeartBeat){
            service = "服务数据异常";
        }
        if (isNetworkAvailable) {
            network = "正 常";

        } else {
            network = "异 常";
        }


        if (JT808HeartBeat) {
            jtlianlu = "连 接";

        } else {
            jtlianlu = "断 开";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(" 服 务 : ");
        sb.append(service);
        sb.append("\n\r网 络 : ");
        sb.append(network);
        sb.append("\n\r定 位 : ");
        sb.append(dingwei);
//        sb.append("\n数 据 : ");
//        sb.append(lianlu);
        if (ip.equals("") || port.equals("")){
                tvDevInfo.setVisibility(View.GONE);
        }else {
//            if (JT808HeartBeat){
//                tvDevInfo.setVisibility(View.VISIBLE);
                tvDevInfo.setText(" 部 标 : "+jtlianlu);
//            }else {

//                tvDevInfo.setVisibility(View.GONE);
//            }

//            btnEnableJt808Gone();

            tvDevInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent1 = new Intent(DeviceActivity.this, JT808InfoActivity.class);
                    startActivity(intent1);
                }
            });
        }

        tvAppInfo.setText(sb.toString());

    }


    private void spandTimeMethod() {
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            pgBar.setVisibility(View.GONE);
            Toast.makeText(DeviceActivity.this,"加载异常，请检查连接是否异常！",Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mTasks);
        if (receiver != null){
            unregisterReceiver(receiver);
        }

    }

    @OnClick({R.id.tv_enable_jt808})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_enable_jt808:
//                JT808SettingActivity.startAction(AboutActivity.this);
                Intent intent = new Intent(this, JT808SettingActivity.class);
                startActivity(intent);
                break;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

//        btnEnableJt808Gone();
    }

    private void btnEnableJt808Gone(){
        if (ip.equals("") || port.equals("")){
            btnEnableJt808.setVisibility(View.VISIBLE);
        }else {
            btnEnableJt808.setVisibility(View.GONE);
        }
    }
}
