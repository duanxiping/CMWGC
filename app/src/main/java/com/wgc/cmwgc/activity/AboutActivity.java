package com.wgc.cmwgc.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.orhanobut.logger.Logger;
import com.wgc.cmwgc.R;
import com.wgc.cmwgc.Until.SystemTools;
import com.wgc.cmwgc.app.Config;
import com.wicare.wistorm.api.WDeviceApi;
import com.wicare.wistorm.http.BaseVolley;
import com.wicare.wistorm.http.OnSuccess;
import com.wicare.wistorm.versionupdate.VersionUpdate;
import com.wicare.wistorm.widget.CustomerDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Administrator on 2016/11/2.
 */
public class AboutActivity extends AppCompatActivity {

    private Context mContext;
    private SharedPreferences spf;
    private SharedPreferences.Editor editor;
    private String model = "";
    private WDeviceApi deviceApi;
    @Bind(R.id.tv_model)
    TextView tvModel;
    @Bind(R.id.tv_imei)
    TextView tvImei;
    @Bind(R.id.tv_iccid)
    TextView tvIccid;
    @Bind(R.id.tv_ver_of_Android)
    TextView tvVer;

    @Bind(R.id.tv_ver)
    TextView tvVer1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);
        initSpf();
        initView();
        initWistorm();
        getDeviceInfo();

    }

    private void initWistorm() {
        BaseVolley.init(this);
        deviceApi = new WDeviceApi(this);
    }

    private void initView() {
//        tvCheckUpdate.getPaint().setFlags(Paint. UNDERLINE_TEXT_FLAG ); //下划线
//        tvCheckUpdate.getPaint().setAntiAlias(true);//抗锯齿

        mContext = this;
        spf = getSharedPreferences(Config.SPF_MY,Activity.MODE_PRIVATE);
        model = spf.getString(Config.MODEL,"unknow");

        tvIccid.setText(Config.con_iccid);
        tvModel.setText(model);
        tvImei.setText(Config.con_serial);
        tvVer.setText("" + Build.DISPLAY);

        mContext = this;
        tvVer1.setText("VER :" + SystemTools.getVersion(this)+"."+SystemTools.getVersionCode(this));

        tvVer1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AppInfoActivity.startAction(AboutActivity.this);
                return false;
            }
        });
    }

    /**
     * @param context
     */
    public static void startAction(Activity context) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    private void initSpf(){
        spf = getSharedPreferences(Config.SPF_MY,Activity.MODE_PRIVATE);
        editor = spf.edit();
    }
    private void getDeviceInfo(){
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", Config.ACCESS_TOKEN);
        params.put("did",Config.con_serial);//459432808550306 Config.con_serial 459432808108543

        String fields = "did,binded,bindDate,uid,model";

        if(!TextUtils.isEmpty(Config.con_serial))
            deviceApi.get(params, fields, new OnSuccess() {
                @Override
                protected void onSuccess(String response) {
                    Logger.d("设备信息 ：" + response);
                    try {
                        JSONObject object = new JSONObject(response);
                        JSONObject object1 = new JSONObject(object.getString("data"));
                        if(object1.has("model")){
                            String model = object1.getString("model").toString();
                            editor.putString(Config.MODEL,model);
                            editor.commit();
                            Logger.d("设备型号 ：" + model);
                            tvModel.setText(model);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            },null);
    }


    /**
     * 安装apk
     */
    private void installApk(String saveFileName) {
        File apkfile = new File(saveFileName);
        if (!apkfile.exists()) {
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()),
                "application/vnd.android.package-archive");
        startActivity(i);
    }

    String apkFileName;
    /**
     * 马上安装对话框
     *
     * @param context
     */
    private void showInstallDialog(Context context) {
        CustomerDialog.Builder builder = new CustomerDialog.Builder(context);
        builder.setTitle(context.getResources().getString(com.wicare.wistorm.R.string.new_version_install));
        builder.setMessage(context.getResources().getString(com.wicare.wistorm.R.string.if_install));
        builder.setPositiveButton(context.getResources().getString(com.wicare.wistorm.R.string.install_now), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                installApk(apkFileName);
            }
        });
        builder.setNegativeButton(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    @OnClick({R.id.tv_check_update})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_check_update:
                VersionUpdate updata = new VersionUpdate(this);

                updata.check(Config.UPDATA_APK_URL, new VersionUpdate.UpdateListener() {
                    @Override
                    public void hasNewVersion(boolean isHad, String updateMsg, String apkUrl) {
                        if (!isHad) {
                            SystemTools.showToast(mContext, "已经是最新版本");
                        }
                    }

                    @Override
                    public void finishDownloadApk(String saveFileName) {
                        Logger.w("下载完成............." + saveFileName);
                        apkFileName = saveFileName;
                        showInstallDialog(mContext);
                    }
                });
                break;
        }
    }


}
