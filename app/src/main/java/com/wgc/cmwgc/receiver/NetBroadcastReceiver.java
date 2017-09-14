package com.wgc.cmwgc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.wgc.cmwgc.Until.NetUtil;
import com.wgc.cmwgc.interfac.NetEvent;

/**
 * 功能： descriable
 * 作者： Administrator
 * 日期： 2017/7/4 15:53
 * 邮箱： descriable
 */
public class NetBroadcastReceiver extends BroadcastReceiver{

    private NetEvent netEvent;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            //检查网络状态的类型
            int netWrokState = NetUtil.getNetWorkState(context);
            if (netEvent != null)
                // 接口回传网络状态的类型
                netEvent.onNetChange(netWrokState);
        }
    }

    public void setNetEvent(NetEvent netEvent) {
        this.netEvent = netEvent;
    }
}
