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

import org.androidpn.client.Constants;
import org.androidpn.client.utils.LogUtil;
import org.androidpn.demoapp.NotificationDetailsActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is to notify the user of messages with NotificationManager.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class Notifier {

	private static final String LOGTAG = LogUtil.makeLogTag(Notifier.class);
	private Random random;
	private Context context;
	private SharedPreferences sharedPrefs;
	private NotificationManager notificationManager;

	public Notifier(Context context) {
		random = new Random();
		this.context = context;
		this.sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void notify(String notificationId, String apiKey, String title, String message, String uri) {
		Log.d(LOGTAG, "notify()...");
		Log.d(LOGTAG, "notificationId=" + notificationId);
		Log.d(LOGTAG, "notificationApiKey=" + apiKey);
		Log.d(LOGTAG, "notificationTitle=" + title);
		Log.d(LOGTAG, "notificationMessage=" + message);
		Log.d(LOGTAG, "notificationUri=" + uri);

		if (isNotificationEnabled()) {
			// Show the toast
			if (isNotificationToastEnabled()) {
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}

			Intent intent = new Intent(context, NotificationDetailsActivity.class);
			intent.putExtra(Constants.NOTIFICATION_ID, notificationId);
			intent.putExtra(Constants.NOTIFICATION_API_KEY, apiKey);
			intent.putExtra(Constants.NOTIFICATION_TITLE, title);
			intent.putExtra(Constants.NOTIFICATION_MESSAGE, message);
			intent.putExtra(Constants.NOTIFICATION_URI, uri);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			PendingIntent contentIntent = PendingIntent.getActivity(context, random.nextInt(), intent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
					// 设置小图标
					.setSmallIcon(getNotificationIcon())
					// 设置通知标题
					.setContentTitle(title)
					// 设置通知内容
					.setContentText(message).setTicker(message)

					.setWhen(System.currentTimeMillis()).setContentIntent(contentIntent);

			builder.setDefaults(Notification.DEFAULT_LIGHTS);
			if (isNotificationSoundEnabled()) {
				builder.setDefaults(Notification.DEFAULT_SOUND);
			}
			if (isNotificationVibrateEnabled()) {
				builder.setDefaults(Notification.DEFAULT_VIBRATE);
			}

			notificationManager.notify(random.nextInt(), builder.build());

		} else {
			Log.w(LOGTAG, "Notificaitons disabled.");
		}
	}

	private int getNotificationIcon() {
		return sharedPrefs.getInt(Constants.NOTIFICATION_ICON, 0);
	}

	private boolean isNotificationEnabled() {
		return sharedPrefs.getBoolean(Constants.SETTINGS_NOTIFICATION_ENABLED, true);
	}

	private boolean isNotificationSoundEnabled() {
		return sharedPrefs.getBoolean(Constants.SETTINGS_SOUND_ENABLED, true);
	}

	private boolean isNotificationVibrateEnabled() {
		return sharedPrefs.getBoolean(Constants.SETTINGS_VIBRATE_ENABLED, true);
	}

	private boolean isNotificationToastEnabled() {
		return sharedPrefs.getBoolean(Constants.SETTINGS_TOAST_ENABLED, false);
	}

}
