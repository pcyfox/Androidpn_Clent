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
package org.androidpn.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Future;

import org.androidpn.client.iq.NotificationIQ;
import org.androidpn.client.iq.NotificationIQProvider;
import org.androidpn.client.listener.NotificationPacketListener;
import org.androidpn.client.listener.PersistentConnectionListener;
import org.androidpn.client.notification.NotificationService;
import org.androidpn.client.utils.LogUtil;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.ProviderManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.util.Log;

/**
 * This class is to manage the XMPP connection between client and server.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class XmppManager {
    private static final String TAG = "XmppManager";
    /**
     * 心跳时间间隔(单位：毫秒)
     */
    private static final int BEAT_TIME = 3 * 60 * 1000;
    /**
     * 注册时等待服务器响应的时间
     */
    private static final int REGISTER_WAITING = 12 * 1000;

    private static final String LOGTAG = LogUtil.makeLogTag(XmppManager.class);

    private static final String XMPP_RESOURCE_NAME = "AndroidpnClient";

    private Context context;

    private NotificationService.TaskSubmitter taskSubmitter;

    private NotificationService.TaskTracker taskTracker;

    private SharedPreferences sharedPrefs;

    private String xmppHost;

    private int xmppPort;

    private XMPPConnection connection;

    private String username;

    private String password;

    private ConnectionListener connectionListener;

    private PacketListener notificationPacketListener;

    private Handler handler;
    //TODO 使用阻塞队列效果更佳
    private List<Runnable> taskList;

    private boolean running = false;

    private Future<?> futureTask;

    private Thread reconnection;

    public XmppManager(NotificationService notificationService) {
        context = notificationService;
        taskSubmitter = notificationService.getTaskSubmitter();
        taskTracker = notificationService.getTaskTracker();
        sharedPrefs = notificationService.getSharedPreferences();

        xmppHost = sharedPrefs.getString(Constants.XMPP_HOST, "localhost");
        xmppPort = sharedPrefs.getInt(Constants.XMPP_PORT, 5222);
        username = sharedPrefs.getString(Constants.XMPP_USERNAME, "");
        password = sharedPrefs.getString(Constants.XMPP_PASSWORD, "");

        connectionListener = new PersistentConnectionListener(this);
        notificationPacketListener = new NotificationPacketListener(this);

        handler = new Handler();
        //线程安全的列表
        taskList = Collections.synchronizedList(new ArrayList<Runnable>());
        reconnection = new ReconnectionThread(this);
    }

    public Context getContext() {
        return context;
    }

    /**
     * 每次执行连接都会重新添加：连接->注册->登录三个任务（线程）;
     */
    public void connect() {
        Log.d(LOGTAG, "connect()...");
        submitLoginTask();
    }

    /**
     * 关闭与Xmpp服务的连接
     */
    public void disconnect() {
        Log.d(LOGTAG, "disconnect()...");
        terminatePersistentConnection();
    }

    private void terminatePersistentConnection() {
        Log.d(LOGTAG, "terminatePersistentConnection()...");
        Runnable runnable = new Runnable() {
            final XmppManager xmppManager = XmppManager.this;

            public void run() {
                if (xmppManager.isConnected()) {
                    Log.d(LOGTAG, "terminatePersistentConnection()... run()");
                    xmppManager.getConnection().removePacketListener(xmppManager.getNotificationPacketListener());
                    xmppManager.getConnection().disconnect();
                }
                runNextTask();
            }
        };
        addTask(runnable);
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public PacketListener getNotificationPacketListener() {
        return notificationPacketListener;
    }

    /**
     * 开启断线重连线程
     */
    public void startReconnectionThread() {
        synchronized (reconnection) {

            if (reconnection == null || !reconnection.isAlive()) {
                reconnection = new ReconnectionThread(this);
                reconnection.setName("Xmpp Reconnection Thread");
                reconnection.start();
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public void registerAccount() {
        removeAccount();
        submitLoginTask();
        runNextTask();
    }

    public List<Runnable> getTaskList() {
        return taskList;
    }

    public Future<?> getFutureTask() {
        return futureTask;
    }


    private String newRandomUUID() {
        String uuidRaw = UUID.randomUUID().toString();
        return uuidRaw.replaceAll("-", "");
    }

    private boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    public boolean isAuthenticated() {
        return connection != null && connection.isConnected() && connection.isAuthenticated();
    }

    private boolean isRegistered() {
        return sharedPrefs.contains(Constants.XMPP_USERNAME) && sharedPrefs.contains(Constants.XMPP_PASSWORD);
    }

    private void submitConnectTask() {
        Log.d(LOGTAG, "submitConnectTask()...");
        addTask(new ConnectTask());
    }

    private void submitRegisterTask() {
        Log.d(LOGTAG, "submitRegisterTask()...");
        submitConnectTask();
        addTask(new RegisterTask());
    }

    private void submitLoginTask() {
        Log.d(LOGTAG, "submitLoginTask()...");
        submitRegisterTask();
        addTask(new LoginTask());
    }

    //添加任务至taskList，并计数加一，当第一次执行该方法时会运行taskList中的第一个任务
    private void addTask(Runnable runnable) {
        Log.d(LOGTAG, "addTask(runnable)...");
        taskTracker.increase();
        synchronized (taskList) {
            if (taskList.isEmpty() && !running) {
                running = true;
                futureTask = taskSubmitter.submit(runnable);
                if (futureTask == null) {
                    taskTracker.decrease();
                }
            } else {
                taskList.add(runnable);
            }
        }
        Log.d(LOGTAG, "addTask(runnable)... done");
    }

    //先移除taskList中的第一个任务，然后再提交taskList中的第一个任务（即相当于前的下一个任务）。
    private void runNextTask() {
        synchronized (taskList) {
            running = false;
            futureTask = null;
            if (!taskList.isEmpty()) {
                Runnable runnable = taskList.get(0);
                Log.d(TAG, "runTask() called" + "taskList.size():" + taskList.size());
                taskList.remove(0);
                running = true;
                futureTask = taskSubmitter.submit(runnable);
                if (futureTask == null) {
                    taskTracker.decrease();
                }

            }
        }
        taskTracker.decrease();
        Log.d(LOGTAG, "runTask()...done");
    }

    private void removeAccount() {
        Editor editor = sharedPrefs.edit();
        editor.remove(Constants.XMPP_USERNAME);
        editor.remove(Constants.XMPP_PASSWORD);
        editor.commit();
    }

    //回滚任务
    private void dropTask(int dropCount) {
        synchronized (taskList) {
            if (taskList.size() >= dropCount) {
                for (int i = 0; i < dropCount; i++) {
                    Log.d(TAG, "dropTask() called with: dropCount = [" + dropCount + "]" + "taskList.size():" + taskList.size());
                    taskList.remove(0);
                    taskTracker.increase();
                }
            }
        }
    }

    /**
     * (创建连接任务) A runnable task to connect the server.
     */
    private class ConnectTask implements Runnable {

        public void run() {
            Log.i(LOGTAG, "ConnectTask.run()...................");

            if (!isConnected()) {
                // Create the configuration for this new connection
                ConnectionConfiguration connConfig = new ConnectionConfiguration(xmppHost, xmppPort);
                // TODO 暂时不需要SASL认证
                connConfig.setSecurityMode(SecurityMode.disabled);
                // connConfig.setSecurityMode(SecurityMode.required);
                connConfig.setSASLAuthenticationEnabled(false);
                connConfig.setCompressionEnabled(false);
                //创建连接
                XMPPConnection connection = new XMPPConnection(connConfig);
                setConnection(connection);

                try {
                    // 连接至服务器(通过socket)
                    connection.connect();
                    Log.e(LOGTAG, "服务器连接成功！");
                    // add packet provider
                    ProviderManager.getInstance().addIQProvider("notification", "androidpn:iq:notification",
                            new NotificationIQProvider());
                    runNextTask();
                } catch (XMPPException e) {
                    Log.e(LOGTAG, "服务器连接失败！", e);
                    // 清除任务
                    dropTask(2);
                    runNextTask();
                    //开启重连
                    startReconnectionThread();
                }

            } else {
                Log.d(LOGTAG, "XMPP connected already");
                runNextTask();
            }
        }
    }

    /**
     * {注册用户} A runnable task to register a new user onto the server.
     */
    private class RegisterTask implements Runnable {

        final XmppManager xmppManager;
        boolean isRegisterSuccessful;// 标志注册是否成功
        boolean hasDropTask;// 标志任务是否被丢弃

        private RegisterTask() {
            xmppManager = XmppManager.this;
        }

        public void run() {
            Log.i(LOGTAG, "RegisterTask.run()...................");

            if (!xmppManager.isRegistered()) {
                isRegisterSuccessful = false;
                hasDropTask = false;
                // 还可注册邮件地址用户名等

                final String newUsername = newRandomUUID();
                final String newPassword = newRandomUUID();

                PacketListener packetListener = new PacketListener() {

                    public void processPacket(Packet packet) {
                        synchronized (xmppManager) {
                            Log.d(".PacketListener", "processPacket().....");
                            Log.d(".PacketListener", "packet=" + packet.toXML());

                            if (packet instanceof IQ) {
                                IQ response = (IQ) packet;
                                if (response.getType() == IQ.Type.ERROR) {
                                    if (!response.getError().toString().contains("409")) {
                                        Log.e(LOGTAG, "Unknown error while registering XMPP account! "
                                                + response.getError().getCondition());
                                    }
                                } else if (response.getType() == IQ.Type.RESULT) {
                                    setUsername(newUsername);
                                    setPassword(newPassword);
                                    Log.d(LOGTAG, "username=" + newUsername);
                                    Log.d(LOGTAG, "password=" + newPassword);

                                    Editor editor = sharedPrefs.edit();
                                    editor.putString(Constants.XMPP_USERNAME, newUsername);
                                    editor.putString(Constants.XMPP_PASSWORD, newPassword);
                                    editor.commit();
                                    isRegisterSuccessful = true;
                                    Log.i(LOGTAG, "Account registered successfully");

                                    if (!hasDropTask) {
                                        runNextTask();
                                    }
                                }
                            }
                        }
                    }
                };


                //创建用于添加注册信息IQ
                Registration registration = new Registration();
                registration.setType(IQ.Type.SET);
                //创建过滤器
                PacketFilter packetFilter = new AndFilter(new PacketIDFilter(registration.getPacketID()),
                        new PacketTypeFilter(IQ.class));
                //给连接添加监听器、过滤器
                connection.addPacketListener(packetListener, packetFilter);

                final String[] names = {"陆小凤", "李寻欢", "郭靖", "张无忌", "赵敏", "韦小宝"};
                // 这个名字一旦注册就不会发生改变的
                int  random=(int) (Math.random()*(names.length-0))+0;
                final String name = names[random];
                registration.addAttribute("name", name);
                registration.addAttribute("username", newUsername);
                registration.addAttribute("password", newPassword);
                connection.sendPacket(registration);
                Log.d(TAG, "RegisterTask-sendPacket: "+registration);
                // 等待给服务器处理
                try {
                    Thread.sleep(REGISTER_WAITING);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 等待一段时间后查看是否注册成功，不成功再做处理
                synchronized (xmppManager) {
                    if (!isRegisterSuccessful) {
                        dropTask(1);
                        hasDropTask = true;
                        runNextTask();
                        startReconnectionThread();
                    }
                }

            } else {
                Log.i(LOGTAG, "Account registered already");
                runNextTask();
            }
        }
    }

    /**
     * （用户登录） A runnable task to log into the server.
     */
    private class LoginTask implements Runnable {

        final XmppManager xmppManager;

        private LoginTask() {
            this.xmppManager = XmppManager.this;
        }

        public void run() {
            Log.i(LOGTAG, "LoginTask.run().......................");

            if (!xmppManager.isAuthenticated()) {

                try {
                    xmppManager.getConnection().login(xmppManager.getUsername(), xmppManager.getPassword(),
                            XMPP_RESOURCE_NAME);
                    Log.d(LOGTAG, "登录成功！");

                    // 更改在线状
                    Presence presence = new Presence(Presence.Type.available);
                    presence.setMode(Presence.Mode.available);
                    getConnection().sendPacket(presence);
                    Log.d(TAG, "LoginTask-sendPacket: "+presence);
                    // c添加自定义的 connection listener
                    if (xmppManager.getConnectionListener() != null) {
                        xmppManager.getConnection().addConnectionListener(xmppManager.getConnectionListener());
                    }

                    // packet filter
                    PacketFilter packetFilter = new PacketTypeFilter(NotificationIQ.class);
                    // packet listener
                    PacketListener packetListener = getNotificationPacketListener();
                    connection.addPacketListener(packetListener, packetFilter);
                    // 开启心跳
                    connection.startHeartBeat(BEAT_TIME);
                    // 更新xmppManager
                    synchronized (xmppManager) {
                        xmppManager.notifyAll();
                    }

                } catch (XMPPException e) {
                    Log.e(LOGTAG, "LoginTask.run()... xmpp error");
                    Log.e(LOGTAG, "Failed to login to xmpp server. Caused by: " + e.getMessage());
                    String INVALID_CREDENTIALS_ERROR_CODE = "401";
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains(INVALID_CREDENTIALS_ERROR_CODE)) {
                        xmppManager.registerAccount();
                        return;
                    }
                    //开启重连
                    startReconnectionThread();

                } catch (Exception e) {
                    Log.e(LOGTAG, "LoginTask.run()... other error");
                    Log.e(LOGTAG, "Failed to login to xmpp server. Caused by: " + e.getMessage());
                    //开启重连
                    startReconnectionThread();
                } finally {
                    runNextTask();
                }

            } else {
                Log.i(LOGTAG, "Logged in already");
                runNextTask();
            }

        }
    }

}
