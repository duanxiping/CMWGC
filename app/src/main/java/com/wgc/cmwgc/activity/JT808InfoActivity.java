package com.wgc.cmwgc.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import com.wgc.cmwgc.R;
import com.wgc.cmwgc.app.Config;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * 功能： descriable
 * 作者： Administrator
 * 日期： 2017/7/7 16:38
 * 邮箱： descriable
 */
public class JT808InfoActivity extends Activity{

    @Bind(R.id.jt808_ip_port)
    TextView jt808IPPort;

    private String ip = "";
    private String port = "";

    private SharedPreferences spf;
    private SharedPreferences.Editor editor;
    private final int ONE_SECOND = 1000;
    private final int UPDATE_UI = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jt808_info);
        ButterKnife.bind(this);
        mHandler.postDelayed(mTasks, 0);
    }

    private Runnable mTasks = new Runnable() {
        public void run() {
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

        ip = spf.getString(Config.SP_SERVICE_IP, "");
        port = spf.getString(Config.SP_SERVICE_PORT, "");

    }

    private void updateUi() {
        StringBuilder sb = new StringBuilder();
        sb.append("部标IP地址 : ");
        sb.append(ip);
        sb.append("\n");
        sb.append("\n部标端口号 : ");
        sb.append(port);
        jt808IPPort.setText(sb.toString());
    }


}
