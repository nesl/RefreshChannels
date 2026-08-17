package com.example.transmitter_refreshchannel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import java.util.Arrays;
import java.util.Random;

public class FloatingSurfaceService extends Service implements Runnable {
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    private StateMachine stateMachine;
    private volatile boolean running;
    private Thread refreshThread;

    private Display display;
    private int noBitsToSend;

    SurfaceView popupView;
    WindowManager.LayoutParams p;
    private SurfaceHolder surfaceHolder;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Override
    public void onCreate() {
        super.onCreate();
        stateMachine = new StateMachine(State.STATE_IDLE);
        startForeground(NOTIFICATION_ID, createNotification());


        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);
        surfaceView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                startRefreshThread();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                stopRefreshThread();
            }
        });


        surfaceHolder = surfaceView.getHolder();


        windowManager.addView(surfaceView, layoutParams);


        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
    }

    private void startRefreshThread() {
        running = true;
        refreshThread = new Thread(this);
        refreshThread.start();
    }

    public void stopRefreshThread() {
        running = false;
        try {
            refreshThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Canvas canvas = surfaceHolder.lockCanvas();
        surfaceHolder.unlockCanvasAndPost(canvas);
        Surface surface = surfaceHolder.getSurface();


        State currentState;
        int refreshrate_to_set = 24;
        int current_refreshrate = 0;
        int sleep_duration = 0;
        boolean isRRCorrectlySet = false;
        boolean isDataEnd = false;

        int noRound = 3;
        int data_len = 10;
        int[][] data_NRound;
        int idx_round = 0;
        int idx_data = 0;

        data_NRound = new int [noRound][data_len];
        long fixedSeed = 42;
        Random random = new Random(fixedSeed);
        for (int k = 0; k < noRound; k++) {
            for (int i = 0; i < data_len; i++) {
                data_NRound[k][i] = (int) Math.round(random.nextDouble());
            }
            Log.d("TX", "data_NRound " + k +":" + Arrays.toString(data_NRound[k]));
        }

        int data[] = { 1,0,1,0,1,0,1,0,1,0 };


        while (running) {
            currentState = stateMachine.getCurrentState();
            Log.d("TX_getState", "State: " + currentState);

            sleep_duration = 100;
            switch (currentState) {
                case STATE_IDLE:
                    refreshrate_to_set = 24;
                    sleep_duration = 800;
                    break;
                case STATE_SYNC:
                    refreshrate_to_set = 30;
                    idx_data = 0;
                    isDataEnd = false;
                    data = data_NRound[idx_round % noRound];
                    Log.e("TX", "Sending SYNC: idx_round " + idx_round + " data:" + Arrays.toString(data));
                    break;
                case STATE_DATA:
                    Log.d("TX", "Sending DATA " + data[idx_data] + " of [idx_round "+ idx_round + ", idx_data " + idx_data + "]");
                    if(data[idx_data]==0){
                        refreshrate_to_set = 96;
                    }
                    else {
                        refreshrate_to_set = 120;
                    }
                    idx_data++;
                    if(idx_data == data_len){
                        isDataEnd = true;
                    }
                    break;
                case STATE_END:
                    Log.d("TX", "Sending END ");
                    refreshrate_to_set = 48;
                    isDataEnd = false;
                    break;
            }

            Log.d("TX", "Setting " + refreshrate_to_set +" Hz ");
            surface.setFrameRate(refreshrate_to_set, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT, Surface.CHANGE_FRAME_RATE_ALWAYS );

            try {
                Thread.sleep(sleep_duration ); // Sleep for N seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            // Read the variable from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            int listener_refreshrate = prefs.getInt("listener_refreshrate", 0);

            isRRCorrectlySet = (listener_refreshrate == refreshrate_to_set);
            Log.d("TX", "isRRCorrectlySet: " + isRRCorrectlySet);


            // Read the variable from SharedPreferences
            boolean listener_refreshrate_end_received = prefs.getBoolean("listener_refreshrate_end_received", false);
            Log.d("SharedPreferences", "listener_refreshrate_end_received: " + listener_refreshrate_end_received);

            if(currentState==State.STATE_END && listener_refreshrate_end_received){
                Log.e("TX", "Successfully sent: idx_round " + idx_round + " data:" + Arrays.toString(data));
                idx_round = (idx_round+1);
//                Log.d("TX", "idx_round: " + idx_round);
                if(idx_round == noRound){
                    break;
                }
            }
            stateMachine.handleEvent(isRRCorrectlySet, isDataEnd);

        }
        Log.e("TX", "STOP!!");
        stopSelf();

    }

    @Override
    public void onDestroy() {
        stopRefreshThread();
        super.onDestroy();
    }

    private static final int NOTIFICATION_ID = 1;

    private Notification createNotification() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel("ForegroundServiceChannel",
                    "Foreground Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ForegroundServiceChannel")
                .setContentTitle("RefreshChannel Floating Surface Service")
                .setContentText("RefreshChannel is running.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }



}

