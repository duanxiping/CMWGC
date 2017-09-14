package com.wgc.cmwgc.activity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.wgc.cmwgc.R;
import com.wgc.cmwgc.db.NetStateDB;
import com.wgc.cmwgc.model.NetStateAdapter;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Administrator on 2016/11/2.
 */
public class AboutDeviceActivity extends AppCompatActivity {

    private final String TAG = AppInfoActivity.class.getName();
    @Bind(R.id.list_net_state)
    ListView lvAppState;

    private NetStateDB netStateDB = new NetStateDB(this);
    private SQLiteDatabase db;
    private NetStateAdapter netStateAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info_device);
        ButterKnife.bind(this);

        selectDB();
//        selectdb();
    }

    //创建查询数据库的方法
    public void selectDB(){
        //执行查询
        db = netStateDB.getReadableDatabase();
        Cursor cursor=db.query(NetStateDB.CREATE_TABLE_NET, null, null, null, null, null,null);
        netStateAdapter = new NetStateAdapter(this , cursor);
        lvAppState.setAdapter(netStateAdapter);
    }

    private void selectdb(){

        db = netStateDB.getReadableDatabase();
        Cursor cursors = db.query("net_state_table",new String[]{"_id,net_service,net_internet,net_location,net_websockte,net_jt808,net_location_service,net_web_service,net_jt808_service,net_time"},null,null,null,null,null,null);

        while (cursors.moveToNext()){

            int id =cursors.getInt(0);
            String a = cursors.getString(1);
            String aa = cursors.getString(2);
            String aaa = cursors.getString(3);
            String aaaa = cursors.getString(4);
            String aaaaa = cursors.getString(5);
            String aaaaaa = cursors.getString(6);
            String aaaaaaa = cursors.getString(7);
            String aaaaaaaa = cursors.getString(8);
            String aaaaaaaaa = cursors.getString(9);


        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectDB();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null){
            db.close();
        }
    }

}
