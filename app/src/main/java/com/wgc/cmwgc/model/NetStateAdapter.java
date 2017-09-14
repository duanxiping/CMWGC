package com.wgc.cmwgc.model;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wgc.cmwgc.R;

/**
 * 功能： descriable
 * 作者： Administrator
 * 日期： 2017/7/21 15:49
 * 邮箱： descriable
 */
public class NetStateAdapter extends BaseAdapter{

    private Context context;
    //创建一个查询数据库的cursors对象
    public Cursor cursors;
    @SuppressWarnings("unused")
    private LayoutInflater inflater;
    private LinearLayout layout;
    public NetStateAdapter (Context context, Cursor cursor){

        this.cursors = cursor;
        this.context=context;
        inflater = LayoutInflater.from(context);

    }

    public void addDataSource(Cursor cursors){

        this.cursors=cursors;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return cursors.getCount();
    }

    @Override
    public Object getItem(int position) {
        return cursors;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater=LayoutInflater.from(context);
        layout = (LinearLayout) inflater.inflate(R.layout.item_device_info_log,null);

        TextView mNet_service = (TextView) layout.findViewById(R.id.item_main_service);
        TextView mNet_internet = (TextView) layout.findViewById(R.id.item_net);
        TextView mNet_location = (TextView) layout.findViewById(R.id.item_location);
        TextView mNet_websockte = (TextView) layout.findViewById(R.id.item_websocket);
        TextView mNet_jt808 = (TextView) layout.findViewById(R.id.item_jt808);
        TextView mNet_location_service = (TextView) layout.findViewById(R.id.item_location_service);
        TextView mNet_web_service = (TextView) layout.findViewById(R.id.item_websocet_service);
        TextView mNet_jt808_service = (TextView) layout.findViewById(R.id.item_jt808_service);
        TextView mNet_time = (TextView) layout.findViewById(R.id.item_time);

        //通过cursors对指定的对象进行查询
        cursors.moveToPosition(position);
        String net_service = cursors.getString(cursors.getColumnIndex("net_service"));
        String net_internet = cursors.getString(cursors.getColumnIndex("net_internet"));
        String net_location = cursors.getString(cursors.getColumnIndex("net_location"));
        String net_websockte = cursors.getString(cursors.getColumnIndex("net_websockte"));
        String net_jt808 = cursors.getString(cursors.getColumnIndex("net_jt808"));
        String net_location_service = cursors.getString(cursors.getColumnIndex("net_location_service"));
        String net_web_service = cursors.getString(cursors.getColumnIndex("net_web_service"));
        String net_jt808_service = cursors.getString(cursors.getColumnIndex("net_jt808_service"));
        String net_time = cursors.getString(cursors.getColumnIndex("net_time"));

        mNet_service.setText(net_service);
        mNet_internet.setText(net_internet);
        mNet_location.setText(net_location);
        mNet_websockte.setText(net_websockte);
        mNet_jt808.setText(net_jt808);
        mNet_location_service.setText(net_location_service);
        mNet_web_service.setText(net_web_service);
        mNet_jt808_service.setText(net_jt808_service);
        mNet_time.setText(net_time);

        return layout;
    }
}
