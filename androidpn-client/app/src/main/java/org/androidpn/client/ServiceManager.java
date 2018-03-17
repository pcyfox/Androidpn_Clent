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

import java.util.List;
import java.util.Properties;

import org.androidpn.client.iq.AliasIQ;
import org.androidpn.client.notification.NotificationService;
import org.androidpn.client.utils.LogUtil;
import org.androidpn.demoapp.NotificationSettingsActivity;
import org.jivesoftware.smack.packet.IQ;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

/**
 * This class is to manage the notificatin service and to load the
 * configuration.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public final class ServiceManager {

	private static final String LOGTAG = LogUtil.makeLogTag(ServiceManager.class);

	private Context context;

	private SharedPreferences sharedPrefs;

	private Properties props;

	private String version = "0.2.1";

	private String apiKey;

	private String xmppHost;

	private String xmppPort;

	private String callbackActivityPackageName;

	private String callbackActivityClassName;

	public ServiceManager(Context context) {
		this.context = context;

		if (context instanceof Activity) {
			Log.i(LOGTAG, "Callback Activity...");
			Activity callbackActivity = (Activity) context;
			callbackActivityPackageName = callbackActivity.getPackageName();
			callbackActivityClassName = callbackActivity.getClass().getName();
		}

		props = loadProperties();
		apiKey = props.getProperty("apiKey", "");
		xmppHost = props.getProperty("xmppHost", "127.0.0.1");
		xmppPort = props.getProperty("xmppPort", "5222");
		Log.i(LOGTAG, "apiKey=" + apiKey);
		Log.i(LOGTAG, "xmppHost=" + xmppHost);
		Log.i(LOGTAG, "xmppPort=" + xmppPort);
		// 将androidpn.properties中的信息及其他数据添加至SharedPreferences
		sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
		Editor editor = sharedPrefs.edit();
		editor.putString(Constants.API_KEY, apiKey);
		editor.putString(Constants.VERSION, version);
		editor.putString(Constants.XMPP_HOST, xmppHost);
		editor.putInt(Constants.XMPP_PORT, Integer.parseInt(xmppPort));
		editor.putString(Constants.CALLBACK_ACTIVITY_PACKAGE_NAME, callbackActivityPackageName);
		editor.putString(Constants.CALLBACK_ACTIVITY_CLASS_NAME, callbackActivityClassName);
		editor.commit();

	}

	/**
	 * 启动 NotificationService，从这里进入启动整个服务的大门
	 */
	public void startService() {
		Intent intent = NotificationService.getIntent();
		context.startService(intent);
	}

	public void stopService() {
		Intent intent = NotificationService.getIntent();
		context.stopService(intent);
	}

	/**
	 * 读取raw包下androidpn.properties文件
	 * 
	 * @return 与服务器连接配置信息
	 * 
	 **/
	private Properties loadProperties() {

		Properties props = new Properties();
		try {
			int id = context.getResources().getIdentifier("androidpn", "raw", context.getPackageName());
			props.load(context.getResources().openRawResource(id));
		} catch (Exception e) {
			Log.e(LOGTAG, "Could not find the properties file.", e);
		}
		return props;
	}

	/** 给通知设置默认图标 */
	public void setNotificationIcon(int iconId) {
		Editor editor = sharedPrefs.edit();
		editor.putInt(Constants.NOTIFICATION_ICON, iconId);
		editor.commit();
	}

	/* 打开通知提示设置页面 **/
	public static void viewNotificationSettings(Context context) {
		Intent intent = new Intent().setClass(context, NotificationSettingsActivity.class);
		context.startActivity(intent);
	}

	/**
	 * 设置用户别名，服务端可以根据这个临时别名推送消息。
	 */
	public void setAlias(final String alias) {
		final String username = sharedPrefs.getString(Constants.XMPP_USERNAME, "");
		if (TextUtils.isEmpty(alias) || TextUtils.isEmpty(username)) {
			return;
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					//等待登录操作
					Thread.sleep(500);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}

				NotificationService notificationService = NotificationService.getNotification();
				XmppManager xmppManager = notificationService.getXmppManager();
				
				if (xmppManager != null) {
					if (!xmppManager.isAuthenticated()) {
						synchronized (xmppManager) {
							try {
								Log.d(LOGTAG, "setAlias:wait for authenticate");
								xmppManager.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

					AliasIQ setAliasIQ = AliasIQ.Singleton.INSTANCE.get();
					setAliasIQ.setType(IQ.Type.SET);
					setAliasIQ.setUsername(username);
					setAliasIQ.setAlias(alias);
					Log.d(LOGTAG, "setAlias:username" + username + "alias" + alias);
					xmppManager.getConnection().sendPacket(setAliasIQ);
				}
			}
		}).start();
	}

	/**
	 * 判断某个service是否启动
	 * 
	 * @Description:
	 * @param mContext
	 * @param className
	 * @return
	 * @return 如果存在返回service否则返回null
	 */
	public static ActivityManager.RunningServiceInfo isServiceRunning(Context mContext, String className) {

		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(30);

		if (!(serviceList.size() > 0)) {
			return null;
		}
		ActivityManager.RunningServiceInfo runningService = null;
		for (int i = 0; i < serviceList.size(); i++) {
			if (serviceList.get(i).service.getClassName().equals(className) == true) {
				runningService = serviceList.get(i);
				break;
			}
		}
		return runningService;
	}
}
