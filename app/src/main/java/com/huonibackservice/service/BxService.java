package com.huonibackservice.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

import com.honeywell.barcode.HSMDecodeComponent;
import com.honeywell.camera.CameraManager;
import com.huonibackservice.R;

import java.util.Timer;
import java.util.TimerTask;


public class BxService extends Service {

    //定义浮动窗口布局
    private static LinearLayout mFloatLayout;
    private static LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
    private static WindowManager mWindowManager;

    private static View mFloatView;
    private static final String TAG = "BxService";
    public static HSMDecodeComponent decCom;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int width = Integer.parseInt(intent.getStringExtra("width"));
        int height = Integer.parseInt(intent.getStringExtra("height"));
        int marginRight = Integer.parseInt(intent.getStringExtra("marginRight"));
        int marginBottom = Integer.parseInt(intent.getStringExtra("marginBottom"));
        createFloatView(width, height, marginRight, marginBottom);
        Log.i(TAG, "onBind: " + width + height);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Timer timer = null;

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Camera camera1 = CameraManager.getInstance(BxService.this).getCamera();
                Camera.Parameters parameters1 = camera1.getParameters();
                parameters1.setExposureCompensation(-1);
                parameters1.setAutoWhiteBalanceLock(true);
                parameters1.setColorEffect(Camera.Parameters.EFFECT_MONO);
                parameters1.setPreviewSize(1920, 1080);
                parameters1.setAutoExposureLock(true);
                camera1.setParameters(parameters1);
            }
        }, 4000);
    }

    public void createFloatView(int width, int height, int marginRight, int marginBottom) {

        startTimer();
        wmParams = new LayoutParams();
        //获取WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
//                Display display = mWindowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        //设置window type
        wmParams.type = LayoutParams.TYPE_PHONE;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
//        wmParams.flags =
////          LayoutParams.FLAG_NOT_TOUCH_MODAL |
//                LayoutParams.FLAG_NOT_FOCUSABLE
////          LayoutParams.FLAG_NOT_TOUCHABLE
//        ;
//        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
//      wmParams.flags=LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT |Gravity.BOTTOM;
//        wmParams.gravity = 332 | 420;
//        wmParams.horizontalMargin = 4;
        //设置悬浮窗口长宽数据
        wmParams.width = width;
        wmParams.height = height;
        wmParams.y = marginBottom;
        wmParams.x = marginRight;
        //设置悬浮窗口长宽数据
//        wmParams.width = LayoutParams.WRAP_CONTENT;
//        wmParams.height = LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.mydecodeactivity, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        //浮动窗口按钮
        decCom = mFloatLayout.findViewById(R.id.hsm_decodeComponent);
//        decCom.setVisibility(View.GONE);
//        mFloatView = mFloatLayout.findViewById(R.id.float_id);
//        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
//                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
//                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        //设置监听浮动窗口的触摸移动
//        mFloatLayout.setOnTouchListener(new OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
//                wmParams.x = (int) event.getRawX() - mFloatView.getMeasuredWidth() / 2;
//                //25为状态栏的高度
//                wmParams.y = (int) event.getRawY() - mFloatView.getMeasuredHeight() / 2 - 25;
//                //刷新
//                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
//                return false;
//            }
//        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatLayout != null) {
            mWindowManager.removeView(mFloatLayout);
            decCom.dispose();
        }
        if (timer != null) {
            timer.cancel();
        }
    }

}
