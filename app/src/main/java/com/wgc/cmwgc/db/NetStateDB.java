package com.wgc.cmwgc.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 功能： descriable
 * 作者： Administrator
 * 日期： 2017/5/27 14:30
 * 邮箱： descriable
 */
public class NetStateDB extends SQLiteOpenHelper {

    public static final String CREATE_TABLE_NET = "net_state_table";
    public static final String NET_ID = "_id";
    public static final String NET_SERVICE= "net_service";
    public static final String NET_INTERNET= "net_internet";
    public static final String NET_LOCATION= "net_location";
    public static final String NET_WEBSOCKTE= "net_websockte";
    public static final String NET_JT808= "net_jt808";
    public static final String NET_LOCATION_SERVICE= "net_location_service";
    public static final String NET_WEB_SERVICE= "net_web_service";
    public static final String NET_JT808_SERVICE= "net_jt808_service";
    public static final String NET_TIME= "net_time";

    //北斗ip和端口
    private static final String CREATE_TABLE_JT808 = "table_jt808";
    private static final String JT808_ID = "jt808_id";
    private static final String JT808_IP= "jt808_ip";
    private static final String JT808_PORT= "jt808_port";

    public NetStateDB(Context context) {
        super(context, "wcl", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE "
                +CREATE_TABLE_NET+" ("
                +NET_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
                +NET_SERVICE+" TEXT ,"
                +NET_INTERNET+" TEXT ,"
                +NET_LOCATION+" TEXT ,"
                +NET_WEBSOCKTE+"TEXT ,"
                +NET_JT808+"TEXT ,"
                +NET_LOCATION_SERVICE+"TEXT ,"
                +NET_WEB_SERVICE+"TEXT ,"
                +NET_JT808_SERVICE+"TEXT ,"
                +NET_TIME+" TEXT)");
//        sqLiteDatabase.execSQL("CREATE TABLE "+CREATE_TABLE_JT808+" ("+JT808_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+JT808_IP+" TEXT, "+JT808_PORT+" TEXT )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
