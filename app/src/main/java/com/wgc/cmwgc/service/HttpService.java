package com.wgc.cmwgc.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.orhanobut.logger.Logger;
import com.wgc.cmwgc.Until.SystemTools;
import com.wgc.cmwgc.activity.LeadMainActivity;
import com.wgc.cmwgc.app.Config;
import com.wgc.cmwgc.db.DBManager;
import com.wgc.cmwgc.db.DeviceDataEntity;
import com.wgc.cmwgc.interfac.NetEvent;
import com.wgc.cmwgc.receiver.BootUpReceiver;
import com.wgc.cmwgc.receiver.NetBroadcastReceiver;
import com.wicare.wistorm.WiStormApi;
import com.wicare.wistorm.api.WDeviceApi;
import com.wicare.wistorm.api.WGpsDataApi;
import com.wicare.wistorm.http.BaseVolley;
import com.wicare.wistorm.http.OnFailure;
import com.wicare.wistorm.http.OnSuccess;
import com.wicare.wistorm.versionupdate.VersionUpdate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 三十秒定位 数据处理
 * 2016-09-12
 */
public class HttpService extends Service implements NetEvent {

	private static final String TAG = "HttpService";
	private int ONE_SECOND = 1000  ;// 1s
    private Boolean isFirst = true;
    private double latt  = 0 ;
    private double lonn  = 0 ;
    private int speedGps = 0;
	private int speedGps1 = 0;
	private int speedLimit = 0;
	private int speedLimitB = 0;
    private int gpsType  = 2;
	private int alertd  = 1;
	private int alertb  = 1;
    private int singnal  = 0;
	private double mileage = 0;
	private float bearing = 0;
	private String status = "[]";
	private int acc = 1;

    private WDeviceApi deviceApi;
	private WGpsDataApi gpsDataApi;
	private Handler objHandler = new Handler();
	private TelephonyManager Tel;
	private MyPhoneStateListener myListener;
	private LocationManager locationManager = null;
	private LocationListner gpsListner = null;
	private SharedPreferences spf;
	private SharedPreferences.Editor editor;

	private boolean isNetwork = false;
	private boolean isRegister = false;
	private boolean isHadOfflineData = false;
	private boolean isRunning = false;
	private DBManager dbManager;

	/**
	 * 网络状态
	 */
	private int netMobile;
	/**
	 * 监控网络的广播
	 */
	private NetBroadcastReceiver netBroadcastReceiver;

	@Override
	public void onCreate() {
		super.onCreate();
		initSpf();
		initWistorm();
		initDevice();
		initBorcast();
		Log.d(TAG, "onCreate:  服务。。。。" + Config.con_serial);
		checkIsCreate();

	}

	private void initSpf(){
		dbManager = DBManager.getInstance(this);//获取数据库实例
		spf = getSharedPreferences(Config.SPF_MY, Activity.MODE_PRIVATE);
//		spf.registerOnSharedPreferenceChangeListener("");
		editor = spf.edit();
		mileage = Double.valueOf(spf.getString(Config.TOTAL_MILEAGE,"0"));
		if (mileage==-1){
			mileage = 0.0;
		}
		isRegister = spf.getBoolean(Config.IS_REGISTER,false);
		latt = Double.valueOf(spf.getString(Config.LAST_LAT,"0.0"));
		lonn = Double.valueOf(spf.getString(Config.LAST_LON,"0.0"));
		Logger.d("取出最后保存的 经度： " + lonn + " 纬度 ：" + latt  +  " 里程 ：" + mileage);
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Tel.listen(myListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		Logger.w("==== 服务 === " + "onStartCommand");
		return START_STICKY;
	}

	/**
	 * @author Wu
	 *         <p>
	 *         位置变化监听、
	 */
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	long diff;
	private float speed;
	private double dis = 0;
	private Date d1;
	private Date d2;
	private class LocationListner implements LocationListener {

		public void onLocationChanged(Location location) {
				if(isFirst){
				Log.e(TAG,"是否第一次 ：" +isFirst);
				isFirst = false;
				latt = location.getLatitude();
				lonn = location.getLongitude();
				Config.gps_time = WiStormApi.getCurrentTime();
				bearing = location .getBearing();
				if (location.getProvider().equals("gps")) {
					gpsType = 2;
				}else{
					gpsType = 1;
				}

			}else{
				/*计算两点距离*/
//					double dis =0;
					try {
						if(location.getProvider().equals("gps")) {
							d1 = df.parse(Config.gps_time);
							d2 = df.parse(WiStormApi.getCurrentTime());
							Log.i(TAG, "第一个时间=====" + d1.toString());
							Log.i(TAG, "第二个时间=====" + d2.toString());
							diff = d2.getTime() - d1.getTime();//这样得到的差值是微秒级别
							Log.i(TAG, "时间差是=====" + diff);
							if (0 < diff && diff < 5 * 1000 ) {
								dis = SystemTools.GetDistance(latt, lonn, location.getLatitude(), location.getLongitude());

							} else {

								dis = 0;
							}
						}


					} catch (ParseException e) {
						e.printStackTrace();
					}
//					speed = location.getSpeed();
//					float accuracy = location.getAccuracy();//精确度，以密为单位
//					double altitude = location.getAltitude();//获取海拔高度
					Log.e(TAG, "行驶里程=====" + dis);
//					Log.e(TAG, "行驶速度=====" + speed);
//					Log.e(TAG, "行驶精确度=====" + accuracy);
//					Log.e(TAG, "海拔高度=====" + altitude);

				latt = location.getLatitude();
				lonn = location.getLongitude();
				Config.gps_time = WiStormApi.getCurrentTime();
				bearing = location .getBearing();

				if (location.getProvider().equals("gps")) {
					gpsType = 2;
				}else{
					gpsType = 1;
				}

//				distanceCaculate(dis);
					distanceCaculate();
				Log.e(TAG,  " 角度 ："   + location .getBearing() +  " 定位成功----:"  + " 里程 ：" + dis + " 速度 ：" + lastSpeed  +"方向："+bearing+" type : " + gpsType  +  "   lon：" + location.getLongitude() + "   lat：" + location.getLatitude());
				if(!isNetwork){//没有网络的时候定位到的数据保存起来 等到有网络就上传
					if (time_uptate_data==30){
						DeviceDataEntity dataEntity = new DeviceDataEntity();
						dataEntity.setCreatedAt(WiStormApi.getCurrentTime());
						dataEntity.setDirect(bearing);
						dataEntity.setFuel(-1);
						dataEntity.setGpsFlag(gpsType);
						dataEntity.setGpsTime(Config.gps_time);
						dataEntity.setLat(latt);
						dataEntity.setLon(lonn);
						dataEntity.setMileage(mileage);
						dataEntity.setRcvTime(WiStormApi.getCurrentTime());
						dataEntity.setSignal(singnal);
						dataEntity.setSpeed(lastSpeed);
						dataEntity.setStatus(status);
						dbManager.insertDeviceData(dataEntity);
						time_uptate_data = 0;
						isHadOfflineData = true;
						Log.d(TAG,  " 保存定位数据 .......... ");
					}
				}
			}
			sendCheckEvent();
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			Logger.w("定位状态onStatusChanged ：" + status + " == " + provider);
		}
		public void onProviderEnabled(String provider) {
			Logger.w("定位状态 onProviderEnabled：" +" == " + provider);
			checkIsCreate();
			go();
			//acc点火之后会执行
		}
		public void onProviderDisabled(String provider) {
			Logger.w("定位状态onProviderDisabled ：" + " == " + provider);
//			objHandler.removeCallbacks(mTasks);//休眠的时候会执行
		}
	};

	/**
	 * 广播接收器
	 */
	private int sleepOrShake = 0;
	private final BroadcastReceiver receiver = new BroadcastReceiver(){
		@Override
		public void onReceive(final Context context, final Intent intent) {

			if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
				ConnectivityManager connectivityManager = (ConnectivityManager)
						context.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
				if (networkInfo != null && networkInfo.isAvailable()) {
					Log.e(TAG, "有网络服务  : ");
					//在线更新版本号
					updataONlineDevice();
					isNetwork = true;
					checkIsCreate();
					if (isHadOfflineData) {
						uploadOfflineData();
						Log.e(TAG, "离线数据上传成功！");
					}
				} else {
					isNetwork = false;
					Log.e(TAG, "没有网络连接");
				}
			} else if (intent.getAction().equals(CoreServer.START_UPLPAD)) {//主服务1
				checkIsCreate();
			}
			else if(intent.getAction().equals(CoreServerAgin.START_UPLPAD)) {//主服务2
				checkIsCreate();
			}

			//联英达的ACC 接收广播
			if (intent.getAction().equals("com.android.rmt.ACTION_ACC_ON")) {
//				distanceCaculate();
				acc = 1;
				Log.e(TAG,"Acc 的值是============"+acc);
				checkIsCreate();
				Toast.makeText(context, "监听到ACC连接广播", Toast.LENGTH_SHORT).show();
			}
			if (intent.getAction().equals("com.android.rmt.ACTION_ACC_OFF")) {
				acc = 0;
				Log.e(TAG,"Acc 的值是============"+acc);
				lastSpeed = 0;
				sleepOrShake = intent.getIntExtra("sleep_or_shake", 0);
				if (sleepOrShake == 0) {
					Toast.makeText(context, "休眠模式", Toast.LENGTH_SHORT).show();
				} else if (sleepOrShake == 1) {

					Toast.makeText(context, "防震防盗模式", Toast.LENGTH_SHORT).show();
				}
				Toast.makeText(context, "监听到ACC断开广播", Toast.LENGTH_SHORT).show();
			}

			//车连连Acc接收广播
			String action = intent.getAction();
			Log.e(TAG,"车连连Acc接收广播 ============"+action);
			if (action.equals(Config.CAR_SIGNAL)) {
				Toast.makeText(context, "监听到ACC连接广播", Toast.LENGTH_SHORT).show();
				String message = intent.getStringExtra(Config.CAR_MODE);
				if (message.equals(Config.CAR_POWERON_WORKING)) {
					//打火处理
					acc = 1;
					Log.e(TAG,"Acc 的值是============"+acc);
					checkIsCreate();
				} else if (message.equals(Config.CAR_POWERDOWN_SUSPEND)) {
					//熄火处理
					acc = 0;
					Log.e(TAG,"Acc 的值是============"+acc);
					lastSpeed = 0;
				}

			}
		}
	};


	/**
	 * 里程计算
	 * @param d
	 */
	int lastSpeed = 0;
	double lastD = 0d;
	int speedCount;
	private String speedLimits;
	private double timeGps;
	private String speedAddGps;
	private double dd;
	private void distanceCaculate( ){

		speedGps =(int) Math.round(dis*3600);//公里/小时

		diff = d2.getTime() - d1.getTime();//这样得到的差值是微秒级别
		Log.e(TAG,"diff ====="+diff);
		timeGps = (double)diff/3600;
		Log.e(TAG,"速度speedGps ====="+speedGps);
		Log.e(TAG,"时间timeGps ====="+timeGps);
		if (timeGps != 0){
			dd = 80*timeGps;
			Log.e(TAG,"加速度speedAddGps====="+dd);
		}else {

			speedGps =0;
		}

		if (dd >30 ){

			Log.e(TAG,"速度异常，异常速度为====="+speedGps);
			speedGps =0;

		}

		if (speedGps > 180){
			Log.e(TAG,"速度超过180，定位不正常====="+speedGps);
			speedGps = 0;

		}else if (diff < 1000 && speedGps > 80){
			Log.e(TAG,"速度异常，异常速度为====="+speedGps);
			speedGps =0;

		}else if (diff < 5000 && speedGps > 100){
			Log.e(TAG,"速度异常，异常速度为====="+speedGps);
			speedGps =0;

		}
		Log.e(TAG, "现在的速度为"+speedGps+"");


		if(acc==0){
			status = "[]";
		}else if(acc==1){
			status = "[8196]";
		}
		if(Math.abs(dis-lastD)>0.05){
			mileage = mileage + (Math.abs(dis-lastD)/2) ;//里程累计
		}else{
			mileage = mileage + dis ;//里程累计
			lastD = dis;
		}

//		Intent intent = new Intent("MY_JT808_SPEED_DEFAULT_LIMIT");
//		intent.getIntExtra("jt_speed_limit",speedLimitGps);
//		Log.e(TAG,  "收到的限速度是 ------- " + speedLimitGps);
//		//北斗部标超速报警设置
//
//		Intent intentb = new Intent("MY_JT808_SPEED_BEIDOU_LIMIT");
//		intentb.getIntExtra("jt_speed_beidou_limit", speedLimitB);

		/*汽车里程*/
		editor.putString(Config.TOTAL_MILEAGE,String.valueOf(mileage));
		editor.putString(Config.SPEED,String.valueOf(speedGps));
		editor.putString(Config.BEARING,String.valueOf(bearing));
		editor.putString(Config.LAST_LON,String.valueOf(lonn));
		editor.putString(Config.LAST_LAT,String.valueOf(latt));
//		editor.putString(Config.SPEED_LIMIT_D,String.valueOf(speedLimit));
//		editor.putString(Config.SPEED_LIMIT_B,String.valueOf(speedLimitB));
		editor.commit();
		if(Math.abs(speedGps-lastSpeed)>120){
			if(Math.abs(speedGps-lastSpeed)>200){
				speedGps = lastSpeed;
			}else{
				speedGps = (lastSpeed +speedGps)/2 ;
			}
		}
		lastSpeed = speedGps;


//		//默认链路超报警速设置

//		 speedLimits = spf.getString(Config.SPEED_LIMIT_D, "0");
//		 speedLimitGps = Integer.valueOf(speedLimits);

//		speedCount++;
//		if (speedCount < 9) {
//			if (speedGps < speedLimit) {
////			if (lastSpeed < 2) {
//				alertd = 0;
//				Log.e(TAG,"时间："+speedCount+  "默认链路没有超速，正常速度是 ------- " + speedGps);
//				speedCount = 0;
//			}
//		} else {
//			alertd = 2;
//			Log.e(TAG, "时间："+speedCount+ "默认链路超出的速度是 ------- " + lastSpeed);
//			speedCount = 0;
//		}

		if (speedCount < 9) {
			if (speedGps < speedLimitB) {
//			if (lastSpeed < 2) {
				alertb = 0;
				speedCount = 0;
				Log.e(TAG,"时间："+speedCount+  "部标链路没有超速，正常速度是 ------- " + lastSpeed);

			}
		} else {
			alertb = 2;
			Log.e(TAG, "时间："+speedCount+ "部标链路超出的速度是 ------- " + speedGps);
			speedCount = 0;
		}


	}



	/**
	 * Wistorm
	 */
	private void initWistorm(){
		BaseVolley.init(this);
		deviceApi = new WDeviceApi(this);
		gpsDataApi = new WGpsDataApi(this);
	}

	/**
	 * 注册广播
	 */
	private void initBorcast(){
		IntentFilter filter = new IntentFilter();
	    filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		filter.addAction("com.android.rmt.ACTION_ACC_ON");//接上ACC广播
		filter.addAction("com.android.rmt.ACTION_ACC_OFF");//断开ACC广播
//		filter.addAction("my_bro_is_enable_jt");
//		filter.addAction("my_bro_is_enable_jtd");
//		filter.addAction("my_bro_is_speed");
		filter.addAction("MY_JT808_SPEED_DEFAULT_LIMIT");
		filter.addAction(Config.CAR_SIGNAL);
		filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
//		filter.addAction("MY_JT808_SPEED_BEIDOU_LIMIT");
		filter.addAction(CoreServer.START_UPLPAD);
	    registerReceiver(receiver, filter);

	}

	/**
	 * 初始化设备
	 */
	private void initDevice(){
		/* Update the listener, and start it */
        myListener = new MyPhoneStateListener();
        Tel = ( TelephonyManager )getSystemService(Context.TELEPHONY_SERVICE);
        Tel.listen(myListener ,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
      	Config.con_serial = Tel.getDeviceId();
		Config.con_iccid = Tel.getSimSerialNumber();
      	Log.e(TAG, "IMEA :" + Config.con_serial);
	}

	/**
	 * 检查设备是否注册过
	 */
	private void checkIsCreate(){
//		Logger.d("==== 是否注册=== " + isRegister);
		if(isRegister){
			go();
		}else{
			isCreate();
		}
	}

	/**
	 * 判断设备是否注册 没有注册自动注册设备
	 */
	private void isCreate(){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", Config.ACCESS_TOKEN);
		params.put("did", "4594328085393400");//459432808539341 Config.con_serial
		String fields = "did,activeGpsData";
		deviceApi.get(params, fields, new OnSuccess() {

			@Override
			protected void onSuccess(String response) {
				// TODO Auto-generated method stub
				Log.d(TAG, "服务获取设备返回信息 ：  " + response);
				try {
					JSONObject jsonObject = new JSONObject(response);
					if(jsonObject.has("data")){
						if(!jsonObject.isNull("data")){
							JSONObject object1 = new JSONObject(jsonObject.getString("data"));
							getMileage(object1);
						}
					}
					if("0".equals(jsonObject.getString("status_code"))){
						if(jsonObject.isNull("data")){
							Log.e(TAG, "设备没有注册请进行注册");
							createDevice();
						}else{
							Log.e(TAG, "该设备已经注册，开始定位");
							editor.putBoolean(Config.IS_REGISTER,true);
							editor.commit();
							isRegister = true;
							go();
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}, new OnFailure() {

			@Override
			protected void onFailure(VolleyError error) {
				// TODO Auto-generated method stub
			}
		});
	}

	/**
	 * 开始定位
	 */
	private void go(){
		objHandler.removeCallbacks(mTasks);
		objHandler.postDelayed(mTasks, 1000);
	}

	private void getMileage(JSONObject jsonObject){
		if(jsonObject.has("activeGpsData")){
			try{
				JSONObject object = new JSONObject(jsonObject.getString("activeGpsData").toString());
				if (object.has("mileage")){
					Logger.d(TAG,"服务-》里程 ：" + object.getString("mileage"));
					editor.putString(Config.TOTAL_MILEAGE,object.getString("mileage"));
					editor.commit();
					mileage = Double.valueOf(object.getString("mileage"));
					if (mileage==-1){
						mileage = 0.0;
					}
				}else {
					editor.putInt(Config.TOTAL_MILEAGE,0);
					editor.commit();
				}
			}catch (JSONException e){
			}
		}
	}

	private void createDevice(){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", Config.ACCESS_TOKEN);
		params.put("did", Config.con_serial);
		params.put("uid", "0");
		deviceApi.create(params, new OnSuccess() {

			@Override
			protected void onSuccess(String response) {
				// TODO Auto-generated method stub
				Log.d("TEST_WISTORM", response);
				try {
					JSONObject jsonObject = new JSONObject(response);
					if ("0".equals(jsonObject.getString("status_code"))) {
						editor.putBoolean(Config.IS_REGISTER, true);
						editor.commit();
						isRegister = true;
						go();
					}else if("15".equals(jsonObject.getString("status_code"))){
						editor.putBoolean(Config.IS_REGISTER, true);
						editor.commit();
						isRegister = true;
						go();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}, new OnFailure() {
			@Override
			protected void onFailure(VolleyError error) {
			}
		});
	}

	/**
	 * 是否检查更新
	 */
	boolean isUpdate = true;
 	private void isUpdate() {
		time_uptate_apk = 0;
		if (SystemTools.isSdCardExist()) {
			VersionUpdate updata = new VersionUpdate(this);
			updata.checkInBackService(Config.UPDATA_APK_URL, new VersionUpdate.UpdateListener() {
				@Override
				public void hasNewVersion(boolean isHad, String updateMsg, String apkUrl) {
					if(isHad){
						if (isUpdate){
							isUpdate = false;
							startIntent();
						}
					}
				}
				@Override
				public void finishDownloadApk(String saveFileName) {
				}
			});
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		objHandler.removeCallbacks(mTasks);
		if (receiver != null){
			unregisterReceiver(receiver);
		}

		if (netBroadcastReceiver != null){

			unregisterReceiver(netBroadcastReceiver);
		}
		removeLocationListener();
		Tel.listen(myListener, PhoneStateListener.LISTEN_NONE);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * 启动界面
	 */
	private void startIntent(){
		Intent mIntent = new Intent();
		mIntent.setClass(getApplicationContext(), LeadMainActivity.class);
		mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(mIntent);
	}

	/**
	 * 监听手机信号
     * @author wu
     */
    private class MyPhoneStateListener extends PhoneStateListener {
    	@Override
    	public void onSignalStrengthsChanged(SignalStrength signalStrength) {
    		super.onSignalStrengthsChanged(signalStrength);
    		singnal =  signalStrength.getGsmSignalStrength();
    	}
    };

	/**
	 * @return json activeGpsData
	 */
	private Object getDeviceParams(){
		JSONObject jObject=new JSONObject();
		try {
			jObject.put("version", "v"+ SystemTools.getVersion(this));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jObject;
	}

	int time_uptate_apk  = 0;
	int time_uptate_data = 0;
	/**
	 * 定时提交数据  ；每隔一秒执行
	 */
	private Runnable mTasks = new Runnable(){
		public void run(){
			isRunning = true;
			time_uptate_apk ++;
			time_uptate_data ++;
				startLocation();
			if(time_uptate_apk%5==0){
				Log.d(TAG,"-------- " + time_uptate_apk);
//				Intent intent = new Intent("MY_HEARTbeat");
//				intent.putExtra("Heart",isRunning);
//				sendBroadcast(intent);
			}
			if(time_uptate_apk == 7200){
				time_uptate_apk = 0;
				if(isNetwork){
					isUpdate();//一个小时检查更新一次
				}
			}
			if(time_uptate_data == 30){
				if(latt!=0 || lonn!=0){
					if(isNetwork){
						uploadLocation();
						time_uptate_data = 0;
					}
				}
			}else if(time_uptate_data>30){
				time_uptate_data = 0;//				定位没信号并且没有网络的时候
			}
			objHandler.postDelayed(mTasks, ONE_SECOND);
		}
	};


	/**
	 * 发送数据检查超速和围栏报警
	 */
	private void sendCheckEvent(){
		Intent intent = new Intent(Config.SPEED_ENCLOSURE_DEFAULTSERVER);
		intent.putExtra("lat",String.valueOf(latt));
		intent.putExtra("lon",String.valueOf(lonn));
		intent.putExtra("speed",lastSpeed);
		intent.putExtra("alertd",alertd);
		intent.putExtra("network",isNetwork);
		intent.putExtra("gpsTime",WiStormApi.getCurrentTime());
		intent.putExtra("direct", (int)bearing);
		intent.putExtra("mileage",String.valueOf(mileage));
		intent.putExtra("gpsFlag",gpsType);
		intent.putExtra("status",status);
		sendBroadcast(intent);

		Intent intentb = new Intent(Config.SPEED_ENCLOSURE_BEIDOUSERVICE);
		intentb.putExtra("lat",String.valueOf(latt));
		intentb.putExtra("lon",String.valueOf(lonn));
		intentb.putExtra("speed",lastSpeed);
		intentb.putExtra("alertb",alertb);
		intentb.putExtra("network",isNetwork);
		intentb.putExtra("gpsTime",WiStormApi.getCurrentTime());
		intentb.putExtra("direct", (int)bearing);
		intentb.putExtra("mileage",String.valueOf(mileage));
		intentb.putExtra("gpsFlag",gpsType);
		intentb.putExtra("status",status);
		intent.putExtra("acc",acc);
		Log.e(TAG,"Acc 发送的广播值是============"+acc);
		sendBroadcast(intentb);
	}

	/**
	 *
	 * getCurrentTime 获取当前时间
	 *
	 * @return
	 */
	@SuppressLint("SimpleDateFormat") public static String getCurrentTime() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd-hh-mm-ss");
		String str = sdf.format(date);
		return str;
	}


	private void startLocation(){

		if(locationManager == null){
			locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		}
		if(gpsListner == null){
			gpsListner = new LocationListner();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED) {
			}
		}else{
			locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,1000 * 30, 0, gpsListner);
			locationManager.addGpsStatusListener(listener);//侦听GPS状态
		}

		try {
			ready(this);
			changeGPSState(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		NetBroadcast();
		isNetConnect();
	}

	private void removeLocationListener(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(getApplication().checkSelfPermission(Manifest.permission.CALL_PHONE)==PackageManager.PERMISSION_GRANTED) {
			}
		}else{
			if (gpsListner != null) {
				locationManager.removeUpdates(gpsListner);
			}
			locationManager = null;
		}
	}

	//设置Toast对象
	private Toast mToast = null;
	private void showTextToast(String msg) {
		//判断队列中是否包含已经显示的Toast
		if (mToast == null) {
			mToast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		}else{
			mToast.setText(msg);
		}
		mToast.show();
	}

	/**---------监听网络的变化情况---------------------------------------------------------------------------------------------*/
	private void NetBroadcast(){
		//注册广播
		if (netBroadcastReceiver == null) {
			netBroadcastReceiver = new NetBroadcastReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			registerReceiver(netBroadcastReceiver, filter);
			/**
			 * 设置监听
			 */
			netBroadcastReceiver.setNetEvent(this);
		}

	}

	@Override
	public void onNetChange(int netMobile) {
		this.netMobile = netMobile;
		isNetConnect();
	}

	private void isNetConnect() {
		switch (netMobile) {
			case 1://wifi
//				Toast.makeText(this,"当前网络类型:wifi！", 0).show();
				Log.e(TAG,"当前网络类型:wifi！");
				break;
			case 0://移动数据
//				Toast.makeText(this,"当前网络类型:移动数据", 0).show();
				Log.e(TAG,"当前网络类型:移动数据");
				break;
			case -1://没有网络
//				Toast.makeText(this,"当前网络异常，请检查连接！", 0).show();
				Log.e(TAG,"当前网络异常，请检查连接！");
				showTextToast("当前网络异常，请检查连接！");
				break;
		}

	}



	/**---------监听GPS的变化情况---------------------------------------------------------------------------------------------*/
	private int useOfSatellites;
	private GpsStatus.Listener listener = new GpsStatus.Listener() {
		@Override
		public void onGpsStatusChanged(int i) {
			GpsStatus gpsStatus= locationManager.getGpsStatus(null);
			switch (i){
				case GpsStatus.GPS_EVENT_SATELLITE_STATUS:

					Iterable<GpsSatellite> allSatellites = gpsStatus.getSatellites();
					Iterator<GpsSatellite> iterator = allSatellites.iterator();
					int satellites = 0;
					int useInfix = 0;
					int maxSatellites=gpsStatus.getMaxSatellites();
					while(iterator.hasNext() && satellites<maxSatellites){
						satellites++;
//                        iterator.next();
						GpsSatellite satellite = iterator.next();
						if (satellite.usedInFix())
							useInfix++;
					}
					useOfSatellites = useInfix;
					if (useOfSatellites >= 4){

						isRunning = true;
						Intent intent = new Intent("MY_HEARTbeat");
						intent.putExtra("Heart",isRunning);
						sendBroadcast(intent);
						Log.e(TAG,"当前卫星信号强");
					}else {
						Log.e(TAG,"当前卫星信号弱");
						showTextToast("当前GPS卫星信号弱");
					}
					break;
			}
		}
	};

	/**
	 * 获取ＧＰＳ当前状态
	 * @param context
	 * @return
	 */
	private boolean getGPSState(Context context){
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		boolean on = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		return on;
	}

	/**
	 * 注册监听广播
	 * @param context
	 * @throws Exception
	 */
	public void ready(Context context)throws Exception{
		IntentFilter filter = new IntentFilter();
		filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
		context.registerReceiver(new GpsStatusReceiver(), filter);
	}

	boolean currentGPSState = false;

	/**
	 * 监听GPS 状态变化广播
	 */
	private class GpsStatusReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)){
				currentGPSState = getGPSState(context);
			}
		}
	}

	/**
	 * 改变GPS状态
	 * @param context
	 * @throws Exception
	 */
	private int gpsCounntent;
	public void changeGPSState(Context context)throws Exception {
		boolean before = getGPSState(context);
		ContentResolver resolver = context.getContentResolver();
//		gpsCounntent++;
		if (before){
//			Toast.makeText(this,"GPS开启",Toast.LENGTH_SHORT).show();
			Settings.Secure.putInt(resolver,Settings.Secure.LOCATION_MODE,Settings.Secure.LOCATION_MODE_OFF);
//			gpsCounntent = 0;
			}else {
//			if (gpsCounntent == 30){
//			showTextToast("GPS处于关闭状态，请打开GPS！");
				//重启APP
//				final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//				startActivity(intent);
//				gpsCounntent=0;
//			}
			Settings.Secure.putInt(resolver,Settings.Secure.LOCATION_MODE,Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
		}
		currentGPSState = getGPSState(context);
	}

	/**---------提交离线定位数据---------------------------------------------------------------------------------------------*/

	int currentUploadIndex = 0;
	List<DeviceDataEntity> entityList;
	private void  uploadOfflineData(){
		entityList = dbManager.queryDeviceDataList();
		Log.d(TAG, "离线数据库数据 ：" + entityList.size());
		if(entityList!=null){
			if (entityList.size()>0 && currentUploadIndex <entityList.size()){
				Log.e(TAG, "离线数据 ：" + entityList.size() + "当前提交 : " + currentUploadIndex);
				uploadOfflineLocation(entityList.get(currentUploadIndex));
			}
		}
	}

	/**
	 * @param entity
	 */
	private void uploadOfflineLocation(final DeviceDataEntity entity){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", Config.ACCESS_TOKEN);
		params.put("did", Config.con_serial);
		params.put("lat", String.valueOf(entity.getLat()));
		params.put("lon", String.valueOf(entity.getLon()));
		params.put("gpsFlag", String.valueOf(entity.getGpsFlag()));
//		params.put("alert", String.valueOf(entity.getAlert()));
		params.put("speed", String.valueOf(entity.getSpeed()));
		params.put("direct", String.valueOf(entity.getDirect()));
		params.put("signal", String.valueOf(entity.getSignal()));
		params.put("createdAt", entity.getCreatedAt());
		params.put("gpsTime", entity.getGpsTime());
		params.put("rcvTime", entity.getRcvTime());//�
		params.put("mileage", String.valueOf(entity.getMileage()));//�
		params.put("fuel", "-1");//
		params.put("status",entity.getStatus());
		Log.d(TAG, Config.con_serial +  " 定位方式： " + entity.getGpsFlag() + "  信号强度: " + entity.getSignal()+ "  定位时间: " + entity.getGpsTime());
		gpsDataApi.gpsCreate(params, new OnSuccess() {

			@Override
			protected void onSuccess(String response) {
				// TODO Auto-generated method stub
				Log.e(TAG, response);
				try {
					JSONObject jsonObject = new JSONObject(response);
					if("0".equals(jsonObject.getString("status_code"))){
						updataOfflineDevice(entity);//
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}, new OnFailure() {

			@Override
			protected void onFailure(VolleyError error) {}
		});
	}


	/**
	 * 更新设备信息
	 */
	private void updataOfflineDevice(DeviceDataEntity entity){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", Config.ACCESS_TOKEN);
		params.put("_did", Config.con_serial);
//		params.put("activeGpsData", getOfflineActiveGpsData(entity).toString());
		params.put("params.version", "v"+SystemTools.getVersion(this));
//		params.put("params",getDeviceParams().toString());
		deviceApi.updata(params, new OnSuccess() {

			@Override
			protected void onSuccess(String response) {
				// TODO Auto-generated method stub
				Log.w(TAG, "更新设备离线数据返回信息 : " + response);
				currentUploadIndex++;
				if (currentUploadIndex >entityList.size()-1){
					isHadOfflineData = false;
					currentUploadIndex = 0;
					dbManager.deleteDeviceDataAll();
					Log.w(TAG, "离线数据提交完成..............................");
				}else{
					Log.e(TAG, "离线数据 ：" + entityList.size() + "当前提交 : " + currentUploadIndex);
					uploadOfflineLocation(entityList.get(currentUploadIndex));
				}
			}
		}, new OnFailure() {

			@Override
			protected void onFailure(VolleyError error) {
				// TODO Auto-generated method stub
				Log.e(TAG, "更新设备返回信息: " + error.toString());
			}
		});
	}


	/**
	 * 在线更新设备信息
	 */
	private void updataONlineDevice(){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", Config.ACCESS_TOKEN);
		params.put("_did", Config.con_serial);
		params.put("params.version", "v"+ SystemTools.getVersion(this));
//		params.put("activeGpsData", getOfflineActiveGpsData(entity).toString());
		Log.e(TAG, "在线更新版本号信息 : " + SystemTools.getVersion(this));

		deviceApi.updata(params, new OnSuccess() {

			@Override
			protected void onSuccess(String response) {
				// TODO Auto-generated method stub
				Log.e(TAG, "更新设备离线数据返回信息 : " + response);
			}
		}, new OnFailure() {

			@Override
			protected void onFailure(VolleyError error) {
				// TODO Auto-generated method stub
				Log.e(TAG, "更新设备返回信息: " + error.toString());
			}
		});
	}


	/**
	 * @return json activeGpsData
	 */
	private Object getOfflineActiveGpsData(DeviceDataEntity entity){
		JSONObject jObject=new JSONObject();
		try {
			jObject.put("lon", entity.getLon());
			jObject.put("lat", entity.getLat());
			jObject.put("gpsTime", entity.getGpsTime());
			jObject.put("did", Config.con_serial);
			jObject.put("gpsFlag", entity.getGpsFlag());
			jObject.put("speed", entity.getSpeed());
			jObject.put("direct", entity.getDirect());
			jObject.put("signal", entity.getSignal());
			jObject.put("rcvTime", entity.getRcvTime());//�
			jObject.put("mileage", entity.getMileage());//�
			jObject.put("fuel", "-1");//
			jObject.put("status", entity.getStatus());//
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jObject;
	}

    /**
	 * 更新位置信息
	 */
	String revTime = "";
	private void uploadLocation(){
		revTime = WiStormApi.getCurrentTime();
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", Config.ACCESS_TOKEN);
		params.put("did", Config.con_serial);
		params.put("lat", String.valueOf(latt));
		params.put("lon", String.valueOf(lonn));
		params.put("gpsFlag", String.valueOf(gpsType));
		params.put("speed", String.valueOf(lastSpeed));
		params.put("direct", String.valueOf(bearing));
		params.put("signal", String.valueOf(singnal));
		params.put("createdAt", revTime);
		params.put("gpsTime", Config.gps_time);
		params.put("rcvTime", revTime);//�
		params.put("mileage", String.valueOf(mileage));//�
		params.put("fuel", "-1");//
		params.put("status",status);
		Log.d(TAG, Config.con_serial +  " 定位时间： " + Config.gps_time + "  : " + WiStormApi.getCurrentTime());
		gpsDataApi.gpsCreate(params, new OnSuccess() {

			@Override
			protected void onSuccess(String response) {
				// TODO Auto-generated method stub
				Log.e(TAG, response);
				try {
					JSONObject jsonObject = new JSONObject(response);
					if("0".equals(jsonObject.getString("status_code"))){
						updataDevice();//
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}, new OnFailure() {

			@Override
			protected void onFailure(VolleyError error) {}
		});
	}

	/**
	 * 更新设备信息
	 */
	private void updataDevice(){
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", Config.ACCESS_TOKEN);
//		params.put("uid", Config.USER_ID);//不能更新UID 不然别人绑定会出问题
		params.put("_did", Config.con_serial);
		params.put("activeGpsData", getActiveGpsData().toString());
		params.put("params.version", "v"+SystemTools.getVersion(this));
		deviceApi.updata(params, new OnSuccess() {

			@Override
			protected void onSuccess(String response) {
				// TODO Auto-generated method stub
				Log.i(TAG, "更新设备返回信息 : " + response);
			}
		}, new OnFailure() {

			@Override
			protected void onFailure(VolleyError error) {
				// TODO Auto-generated method stub
				Log.e(TAG, "更新设备返回信息: " + error.toString());
			}
		});
	}

	/**
	 * @return json activeGpsData
	 */
	private Object getActiveGpsData(){
    	JSONObject jObject=new JSONObject();
        try {
        	jObject.put("lon", lonn);
        	jObject.put("lat", latt);
        	jObject.put("gpsTime", WiStormApi.getCurrentTime());
        	jObject.put("did", Config.con_serial);
        	jObject.put("gpsFlag", gpsType);
        	jObject.put("speed", lastSpeed);
        	jObject.put("direct", bearing);
        	jObject.put("signal", singnal);
			jObject.put("rcvTime", revTime);//�
			jObject.put("mileage", mileage);//�
			jObject.put("fuel", "-1");//
			jObject.put("status", status);//
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return jObject;
    }
}
