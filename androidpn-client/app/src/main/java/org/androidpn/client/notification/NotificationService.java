/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.androidpn.client.notification;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.androidpn.client.ConnectivityReceiver;
import org.androidpn.client.XmppManager;
import org.androidpn.client.listener.PhoneStateChangeListener;
import org.androidpn.client.utils.LogUtil;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.os.PowerManager;

import org.androidpn.client.Constants;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Service that continues to run in background and respond to the push
 * notification events from the server. This should be registered as service in
 * AndroidManifest.xml.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class NotificationService extends Service {

    private static final String LOGTAG = LogUtil.makeLogTag(NotificationService.class);

    public static final String SERVICE_NAME = "org.androidpn.client.notification.NotificationService";

    private static NotificationService notificationService;

    private TelephonyManager telephonyManager;

    private BroadcastReceiver notificationReceiver;

    private BroadcastReceiver connectivityReceiver;

    private PhoneStateListener phoneStateListener;
    private PowerManager.WakeLock mWakeLock;

    private ExecutorService executorService;

    private TaskSubmitter taskSubmitter;

    private TaskTracker taskTracker;

    private XmppManager xmppManager;

    private SharedPreferences sharedPrefs;

    private String deviceId;

    public NotificationService() {
        notificationReceiver = new NotificationReceiver();
        connectivityReceiver = new ConnectivityReceiver(this);
        phoneStateListener = new PhoneStateChangeListener(this);
        // 开启线程池
        executorService = Executors.newSingleThreadExecutor();
        taskSubmitter = new TaskSubmitter();
        taskTracker = new TaskTracker();
    }

    public static NotificationService getNotification() {
        return notificationService;
    }

    @Override
    public void onCreate() {
        Log.d(LOGTAG, "onCreate()...");
        notificationService = this;
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        sharedPrefs = getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        // Get deviceId
        deviceId = telephonyManager.getDeviceId();
        // Log.d(LOGTAG, "deviceId=" + deviceId);
        Editor editor = sharedPrefs.edit();
        editor.putString(Constants.DEVICE_ID, deviceId);
        editor.commit();

        // If running on an emulator
        //如果是模拟器上运行，手动创建deviceId。
        if (deviceId == null || deviceId.trim().length() == 0 || deviceId.matches("0+")) {
            if (sharedPrefs.contains("EMULATOR_DEVICE_ID")) {
                deviceId = sharedPrefs.getString(Constants.EMULATOR_DEVICE_ID, "");
            } else {
                deviceId = (new StringBuilder("EMU")).append((new Random(System.currentTimeMillis())).nextLong())
                        .toString();
                editor.putString(Constants.EMULATOR_DEVICE_ID, deviceId);
                editor.commit();
            }
        }
        Log.d(LOGTAG, "deviceId=" + deviceId);
        xmppManager = new XmppManager(this);

    }

    @Override
    public void onStart(Intent intent, int startId) {
        acquireWakeLock();
        // 启动线程驱动添加监听与连接至服务器
        taskSubmitter.submit(new Runnable() {
            public void run() {
                NotificationService.this.start();
            }
        });
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(LOGTAG, "onDestroy()...");
        notificationService = null;
        stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public static Intent getIntent() {
        return new Intent(SERVICE_NAME);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public TaskSubmitter getTaskSubmitter() {
        return taskSubmitter;
    }

    public TaskTracker getTaskTracker() {
        return taskTracker;
    }

    public XmppManager getXmppManager() {
        return xmppManager;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPrefs;
    }

    public String getDeviceId() {
        return deviceId;
    }

    /**
     * 申请设备电源锁 (端锁屏状态，无法发送心跳包解决方案，6.0及其以后的系统能行？？不晓得！！)
     */
    private final void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getPackageName());
        }
        if (mWakeLock != null) {
            mWakeLock.acquire();
            Log.d(LOGTAG, "mWakeLock.acquire()");
        }
    }

    /**
     * 释放设备电源锁
     */
    private final void releaseWakeLock() {
        Log.d(LOGTAG, "releaseWakeLock");
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    /**
     * 连接XmppServer
     */
    public void connect() {
        Log.d(LOGTAG, "connect()...");
        taskSubmitter.submit(new Runnable() {
            public void run() {
                NotificationService.this.getXmppManager().connect();
            }
        });
    }

    public void disconnect() {
        Log.d(LOGTAG, "disconnect()...");
        taskSubmitter.submit(new Runnable() {
            public void run() {
                NotificationService.this.getXmppManager().disconnect();
            }
        });
    }

    private void registerNotificationReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_SHOW_NOTIFICATION);
        filter.addAction(Constants.ACTION_NOTIFICATION_CLICKED);
        filter.addAction(Constants.ACTION_NOTIFICATION_CLEARED);
        registerReceiver(notificationReceiver, filter);
    }

    private void unregisterNotificationReceiver() {
        unregisterReceiver(notificationReceiver);
    }

    private void registerConnectivityReceiver() {
        Log.d(LOGTAG, "registerConnectivityReceiver()...");
        IntentFilter filter = new IntentFilter();
        filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);
    }

    private void unregisterConnectivityReceiver() {
        Log.d(LOGTAG, "unregisterConnectivityReceiver()...");
        unregisterReceiver(connectivityReceiver);
    }

    private void registerPhoneStateListener() {
        // 监听数据连接状态
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

    }

    private void unregisterPhoneStateListener() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private void start() {
        Log.d(LOGTAG, "start()...");
        // 监听广播
        registerNotificationReceiver();
        // 监听电话状态
        registerPhoneStateListener();
        // 监听网络连接状态
        registerConnectivityReceiver();
        // 连接至服务器
        xmppManager.connect();
    }

    private void stop() {
        Log.d(LOGTAG, "stop()...");
        releaseWakeLock();
        unregisterNotificationReceiver();
        unregisterPhoneStateListener();
        unregisterConnectivityReceiver();
        xmppManager.disconnect();
        executorService.shutdown();
    }

    /**
     * Class for summiting a new runnable task.
     */
    public class TaskSubmitter {
        @SuppressWarnings("rawtypes")
        public Future submit(Runnable task) {
            Future result = null;
            if (!getExecutorService().isTerminated()
                    && !getExecutorService().isShutdown() && task != null) {
                result = getExecutorService().submit(task);
            }
            return result;
        }

    }

    /**
     * (支持并发的任务计数) Class for monitoring the running task count.
     */
    public class TaskTracker {

        private final AtomicInteger count;

        public TaskTracker() {
            count = new AtomicInteger(0);
        }

        public void increase() {
            count.incrementAndGet();
            Log.d(LOGTAG, "Incremented task count to " + count);
        }

        public void decrease() {
            count.decrementAndGet();
            Log.d(LOGTAG, "Decremented task count to " + count);
        }

    }

}
