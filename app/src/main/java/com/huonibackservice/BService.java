package com.huonibackservice;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.annotation.Nullable;
import android.util.Log;

import com.honeywell.barcode.ActiveCamera;
import com.honeywell.barcode.HSMDecodeResult;
import com.honeywell.barcode.HSMDecoder;
import com.honeywell.barcode.Symbology;
import com.honeywell.camera.CameraManager;
import com.huonibackservice.service.BxService;
import com.sc100.HuoniManage;
import com.sc100.Huoniinterface.HuoniScan;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class BService extends Service implements HuoniScan.DisplayBarcodeDataListener, HuoniScan.HuoniscanListener {
    private HuoniScan huoniScan;
    private HSMDecoder hsmDecoder;
    private CameraManager cameraManager;
    private Camera camera1;
    public Camera.Parameters parameters1;
    private Intent intents = new Intent();
    private Intent intent = new Intent();
    private final String HOME_STATE = "com.geenk.action.HOMEKEY_SWITCH_STATE";//设置home按键是否可用
    private final String STATUSBAR_STATE = "com.geenk.action.STATUSBAR_SWITCH_STATE";//设置下拉菜单可用
    private final String SET_DATETIME = "com.geenk.action.SET_DATETIME";//设置系统时间
    private final String BACK_SHOW = "com.huoniBack.show";//后置预览广播

    @Override
    public void onCreate() {
        super.onCreate();
        initLibrary();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BACK_SHOW);
        intentFilter.addAction(HOME_STATE);
        intentFilter.addAction(STATUSBAR_STATE);
        intentFilter.addAction(SET_DATETIME);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getAction();
            switch (state) {
                case BACK_SHOW:
                    if (intent.getBooleanExtra("backState", true)) {
                        if (isWorked(BService.this)) {
                            return;
                        } else {
                            intents.setClass(BService.this, BxService.class);
                            intents.putExtra("width", intent.getStringExtra("width"));
                            intents.putExtra("height", intent.getStringExtra("height"));
                            intents.putExtra("marginRight", intent.getStringExtra("marginRight"));
                            intents.putExtra("marginBottom", intent.getStringExtra("marginBottom"));
                            boolean[] codeType = intent.getBooleanArrayExtra("codeType");
                            for (int i = 0; i < codeType.length; i++) {
                                if (codeType[0]) {
                                    hsmDecoder.enableSymbology(Symbology.CODE128);
                                } else {
                                    hsmDecoder.disableSymbology(Symbology.CODE128);
                                }
                                if (codeType[1]) {
                                    hsmDecoder.enableSymbology(Symbology.CODE39);
                                } else {
                                    hsmDecoder.disableSymbology(Symbology.CODE39);
                                }
                                if (codeType[2]) {
                                    hsmDecoder.enableSymbology(Symbology.QR);
                                } else {
                                    hsmDecoder.disableSymbology(Symbology.QR);
                                }
                            }
                            startService(intents);
                        }
                    } else {
                        if (isWorked(BService.this)) {
                            stopService(intents);
                        } else {
                            return;
                        }
                    }
                    break;
                case HOME_STATE:
                    SystemProperties.set(SystemSet.homeSet, String.valueOf(intent.getBooleanExtra("enable", true)));
                    break;
                case STATUSBAR_STATE://下拉菜单
                    SystemProperties.set(SystemSet.menuDwon, String.valueOf(intent.getBooleanExtra("enable", true)));
                    break;
                case SET_DATETIME:
                    SystemClock.setCurrentTimeMillis(intent.getLongExtra("datetime", 0));
                    break;
            }
        }
    };


    @SuppressWarnings("unchecked")
    private void setCameraParams() {
        Camera.Parameters parameters = cameraManager.getCamera().getParameters();
        try {
            //获取支持的参数
            Method parametersSetEdgeMode = Camera.Parameters.class
                    .getMethod("setEdgeMode", String.class);
            Method parametersSetBrightnessMode = Camera.Parameters.class
                    .getMethod("setBrightnessMode", String.class);
            Method parametersSetContrastMode = Camera.Parameters.class
                    .getMethod("setContrastMode", String.class);

            //锐度 亮度 对比度
            parametersSetEdgeMode.invoke(parameters, "high");
            parametersSetBrightnessMode.invoke(parameters, "high");
            parametersSetContrastMode.invoke(parameters, "high");
            Method parametersGetEdgeMode = Camera.Parameters.class
                    .getMethod("getEdgeMode");
            Method parametersGetBrightnessMode = Camera.Parameters.class
                    .getMethod("getBrightnessMode");
            Method parametersGetContrastMode = Camera.Parameters.class
                    .getMethod("getContrastMode");

            //锐度亮度对比度 是否设置成功
            String ruidu = (String) parametersGetEdgeMode.invoke(parameters);
            String liangdu = (String) parametersGetBrightnessMode.invoke(parameters);
            String duibidu = (String) parametersGetContrastMode.invoke(parameters);

            Log.d("cameraSetting", "mlist is" + ruidu + "-----" + liangdu + "-----" + duibidu);


        } catch (Exception e) {
            Log.d("cameraSetting", "error is::" + Log.getStackTraceString(e));
        }
    }


    private void initLibrary() {
        huoniScan = HuoniManage.getKuaishouIntance();
        huoniScan.intScanDecode(BService.this);
        hsmDecoder = huoniScan.getHuoniHsmDecoder();
        hsmDecoder.setActiveCamera(ActiveCamera.REAR_FACING);
        hsmDecoder.enableSound(false);
        hsmDecoder.enableSymbology(Symbology.CODE39);
        hsmDecoder.enableSymbology(Symbology.CODE128);
        huoniScan.setdisplayBarcodeData(this);
        huoniScan.setHuoniScanLibraryState(this);
        cameraManager = huoniScan.getHuoniCameraManager(this);
        cameraManager.reopenCamera();
        camera1 = cameraManager.getCamera();
        parameters1 = camera1.getParameters();
        parameters1.setExposureCompensation(-3);
        parameters1.setAutoWhiteBalanceLock(true);
        parameters1.setColorEffect(Camera.Parameters.EFFECT_MONO);
        parameters1.setPreviewSize(1920, 1080);
        setCameraParams();
        camera1.setParameters(parameters1);
        intent.setAction("com.huoniBack.barcode");
    }

    @Override
    public void displayBarcodeData(String s, long l, HSMDecodeResult[] hsmDecodeResults) {
        Log.i("stw", "displayBarcodeData: 解码 ");
//        StringBuilder result = new StringBuilder();
        String[] codeBytes = new String[hsmDecodeResults.length];
        for (int i = 0; i < hsmDecodeResults.length; i++) {
            codeBytes[i] = hsmDecodeResults[i].getBarcodeData();
//            String bar = hsmDecodeResults[i].getBarcodeData();
//            result.append("解码" + i + ":" + bar + "\n");
        }
        intent.putExtra("huoniBack", codeBytes);
        //发送广播
        sendBroadcast(intent);
        Log.i("stw", "displayBarcodeData: 发广播 " + hsmDecodeResults[0].getDecodeTime());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
//        Toast.makeText(this, "服务被关闭", Toast.LENGTH_SHORT).show();
        if (isWorked(this)) {
            stopService(intents);
        }
        if (huoniScan != null) {
            huoniScan.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * 判断某个服务是否正在运行的方法
     * 是包名+服务的类名（例如：net.loonggg.testbackstage.TestService）
     *
     * @return true代表正在运行，false代表服务没有正在运行
     */
    public static boolean isWorked(Context context) {
        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString().equals("com.huonibackservice.service.BxService")) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void huoniLibraryState(String s) {

    }
}
