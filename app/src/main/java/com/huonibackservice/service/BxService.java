package com.huonibackservice.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.honeywell.barcode.BarcodeBounds;
import com.honeywell.barcode.HSMDecodeComponent;
import com.honeywell.camera.CameraManager;
import com.huonibackservice.DrawView;
import com.huonibackservice.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class BxService extends Service {
    //定义浮动窗口布局
    private static LinearLayout mFloatLayout;
    private static LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
    private static WindowManager mWindowManager;
    private static final String TAG = "BxService";
    public static HSMDecodeComponent decCom;
    private Timer timer = null;
    private int width;
    private int height;
    private int marginRight;
    private int marginBottom;

    public HSMDecodeComponent getLayout() {
        return decCom;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //获取WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        EventBus.getDefault().register(this);
        width = Integer.parseInt(intent.getStringExtra("width"));
        height = Integer.parseInt(intent.getStringExtra("height"));
        marginRight = Integer.parseInt(intent.getStringExtra("marginRight"));
        marginBottom = Integer.parseInt(intent.getStringExtra("marginBottom"));
        createFloatView(width, height, marginRight, marginBottom);
        mWindowManager.addView(mFloatLayout, wmParams);
        Log.i(TAG, "onBind: " + width + height);
        if (mFloatLayout != null) {
            mWindowManager.removeView(mFloatLayout);
            mWindowManager = null;
        }
        handler.postDelayed(runnable, 300);
        return super.onStartCommand(intent, flags, startId);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            mWindowManager.getDefaultDisplay().getMetrics(dm);
            createFloatView(width, height, marginRight, marginBottom);
            mWindowManager.addView(mFloatLayout, wmParams);

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void createFloatView(int width, int height, int marginRight, int marginBottom) {
        wmParams = new LayoutParams();
        //设置window type
        wmParams.type = LayoutParams.TYPE_PHONE;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        //设置悬浮窗口长宽数据  及 左、底部距离
        wmParams.width = width;
        wmParams.height = height;
        wmParams.y = marginBottom;
        wmParams.x = marginRight;
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.mydecodeactivity, null);
        //浮动窗口按钮
        decCom = mFloatLayout.findViewById(R.id.hsm_decodeComponent);

    }

    private DrawView drawView;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showkuang(List<BarcodeBounds> barcodeBoundsList) {
        Log.i("testttt", "showkuang: 收到 画框");
        if (drawView != null) {
            decCom.removeView(drawView);
//            handler.postAtTime(runnableUi, 1000);
        }
        drawView = new DrawView(this, barcodeBoundsList);
        decCom.addView(drawView);

    }

    Handler handler = new Handler();
    // 构建Runnable对象，在runnable中更新界面
    Runnable runnableUi = new Runnable() {
        @Override
        public void run() {
            //更新界面
            if (drawView != null) {
                decCom.removeView(drawView);
            } else {
                drawView = null;
            }
        }

    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatLayout != null) {
            if (mWindowManager != null) {
                mWindowManager.removeView(mFloatLayout);
            }
            decCom.dispose();
        }
        if (timer != null) {
            timer.cancel();
        }
        EventBus.getDefault().unregister(this);
    }

}
