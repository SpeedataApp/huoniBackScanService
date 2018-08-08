package com.huonibackservice;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.honeywell.barcode.BarcodeBounds;
import com.honeywell.barcode.HSMDecodeResult;
import com.honeywell.barcode.HSMDecoder;
import com.honeywell.barcode.Symbology;
import com.honeywell.camera.CameraManager;
import com.honeywell.plugins.decode.DecodeResultListener;
import com.huonibackservice.service.BxService;
import com.speedata.hwlib.ActivationManager;
import com.speedata.hwlib.net.DialogShowMsg;
import com.speedata.hwlib.net.Global;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class BService extends Service implements DecodeResultListener {
    private HSMDecoder hsmDecoder;
    private Intent intents = new Intent();
    private Intent intent = new Intent();
    private final String HOME_STATE = "com.geenk.action.HOMEKEY_SWITCH_STATE";//设置home按键是否可用
    private final String STATUSBAR_STATE = "com.geenk.action.STATUSBAR_SWITCH_STATE";//设置下拉菜单可用
    private final String SET_DATETIME = "com.geenk.action.SET_DATETIME";//设置系统时间
    private final String BACK_SHOW = "com.huoniBack.show";//后置预览广播
    private DrawView drawView;
    private int width = 360;
    private int height = 640;

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        initLibrary();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BACK_SHOW);
        intentFilter.addAction(HOME_STATE);
        intentFilter.addAction(STATUSBAR_STATE);
        intentFilter.addAction(SET_DATETIME);
        registerReceiver(broadcastReceiver, intentFilter);
//        if (!EventBus.getDefault().isRegistered(this))
//        {
//            EventBus.getDefault().register(this);
//        }
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
                            width = Integer.parseInt(intent.getStringExtra("width"));
                            width = Integer.parseInt(intent.getStringExtra("height"));
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


    private void initLibrary() {
        //初始化霍尼扫描解码
        hsmDecoder = HSMDecoder.getInstance(this);
        new ActivationManager(this).activate();
        Camera camera1 = CameraManager.getInstance(this).getCamera();
        Camera.Parameters parameters1 = camera1.getParameters();
        parameters1.setExposureCompensation(-3);
        parameters1.setAutoWhiteBalanceLock(true);
        parameters1.setColorEffect(Camera.Parameters.EFFECT_MONO);
        parameters1.setPreviewSize(1920, 1080);
        camera1.setParameters(parameters1);
        hsmDecoder.enableSound(false);
        hsmDecoder.addResultListener(this);
        hsmDecoder.enableSymbology(Symbology.CODE39);
        hsmDecoder.enableSymbology(Symbology.CODE128);
        intent.setAction("com.huoniBack.barcode");
    }

//    @Override
//    public void displayBarcodeData(String s, long l, HSMDecodeResult[] hsmDecodeResults) {
//        Log.i("stw", "displayBarcodeData: 解码 ");
////        StringBuilder result = new StringBuilder();
//        String[] codeBytes = new String[hsmDecodeResults.length];
//        List<BarcodeBounds> barcodeBoundsList = new ArrayList<>();
//        for (int i = 0; i < hsmDecodeResults.length; i++) {
//            codeBytes[i] = hsmDecodeResults[i].getBarcodeData();
//            barcodeBoundsList.add(hsmDecodeResults[i].getBarcodeBounds());
////            String bar = hsmDecodeResults[i].getBarcodeData();
////            result.append("解码" + i + ":" + bar + "\n");
//
//        }
//        Log.i("testttt", "displayBarcodeData: 发送 ");
//        EventBus.getDefault().post(barcodeBoundsList);
////        View view = getApplication().getBaseContext().inflater(R.layout.布局文件名,null);
////        LayoutInflater lf = (LayoutInflater)getBaseContext().getSystemServic(Context.LAYOUT_INFLATER_SERVICE);
////        View view = lf.inflate(R.layout.布局文件名,null);
//
//        intent.putExtra("huoniBack", codeBytes);
//        //发送广播
//        sendBroadcast(intent);
//        Log.i("stw", "displayBarcodeData: 发广播 " + hsmDecodeResults[0].getDecodeTime());
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
//        Toast.makeText(this, "服务被关闭", Toast.LENGTH_SHORT).show();
        if (isWorked(this)) {
            stopService(intents);
        }
        if (hsmDecoder != null) {
            hsmDecoder.removeResultListener(this);
        }
        HSMDecoder.disposeInstance();
        EventBus.getDefault().unregister(this);
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

    /**
     * 激活解码返回
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showDialogMsg(DialogShowMsg event) {
        String msg = event.getMsg();
        // 需要初始或使能条码类型
        hsmDecoder.enableAimer(false);
        hsmDecoder.setOverlayText("");
        hsmDecoder.setOverlayTextColor(Color.RED);
        switch (event.getTag()) {
            case Global.REQUEST_PREPARE://请求服务准备
                Logcat.d("REQUEST_PREPARE:" + msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                break;
            case Global.REQUEST_ERROR://请求服务失败y
                Logcat.d("error " + msg);
                if (msg != null) {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                break;
            case Global.REQUEST_SUCCESS://请求服务成功
                Logcat.d("REQUEST_SUCCESS:" + msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                break;
            case Global.REGISTER_SUCCESS://激活成功 需要初始或使能条码类型
                hsmDecoder.enableSymbology(Symbology.CODE128);
                hsmDecoder.enableSymbology(Symbology.CODE39);
                Logcat.d("REGISTER_SUCCESS");
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                break;
            case Global.REGISTER_FAILED://激活失败  也需要初始或使能条码类型
                Logcat.d("REGISTER_FAILED:" + msg);
                Toast.makeText(this, "激活失败", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onHSMDecodeResult(HSMDecodeResult[] hsmDecodeResults) {
        //************************
        Log.i("stw", "displayBarcodeData: 解码 ");
//        StringBuilder result = new StringBuilder();
        String[] codeBytes = new String[hsmDecodeResults.length];
        List<BarcodeBounds> barcodeBoundsList = new ArrayList<>();
        for (int i = 0; i < hsmDecodeResults.length; i++) {
            codeBytes[i] = hsmDecodeResults[i].getBarcodeData();
            barcodeBoundsList.add(hsmDecodeResults[i].getBarcodeBounds());
//            String bar = hsmDecodeResults[i].getBarcodeData();
//            result.append("解码" + i + ":" + bar + "\n");

        }
        Log.i("testttt", "displayBarcodeData: 发送 ");
        EventBus.getDefault().post(barcodeBoundsList);
        intent.putExtra("huoniBack", codeBytes);
        //发送广播
        sendBroadcast(intent);
        Log.i("stw", "displayBarcodeData: 发广播 " + hsmDecodeResults[0].getDecodeTime());
        //***********************

    }

}
