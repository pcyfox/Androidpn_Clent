/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.androidpn.server.xmpp.push;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javassist.compiler.ast.NewExpr;

import org.androidpn.server.model.Notification;
import org.androidpn.server.model.User;
import org.androidpn.server.service.NotificationService;
import org.androidpn.server.service.ServiceLocator;
import org.androidpn.server.service.UserNotFoundException;
import org.androidpn.server.service.UserService;
import org.androidpn.server.xmpp.session.ClientSession;
import org.androidpn.server.xmpp.session.SessionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmpp.packet.IQ;

/**
 * (含有向client发送消息的接口) This class is to manage sending the notifcations to the
 * users.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class NotificationManager {

	private static final String NOTIFICATION_NAMESPACE = "androidpn:iq:notification";

	private final Log log = LogFactory.getLog(getClass());

	private SessionManager sessionManager;

	private NotificationService notificationService;

	private UserService userService;

	private static final int SEND_TO_USER = 0;
	private static final int SEND_TO_ALL = 1;

	/**
	 * Constructor.
	 */
	public NotificationManager() {
		sessionManager = SessionManager.getInstance();
		notificationService = ServiceLocator.getNotificationService();
		userService = ServiceLocator.getUserService();
	}

	/**
	 * Broadcasts a newly created notification message to all connected users.
	 * 
	 * @param apiKey
	 *            the API key
	 * @param title
	 *            the title
	 * @param message
	 *            the message details
	 * @param uri
	 *            the uri
	 */
	public void sendBroadcast(String apiKey, String title, String message,
			String uri) {
		log.debug("sendBroadcast()...title----->" + title);
		List<User> users = userService.getUsers();
		for (User user : users) {
			Random random = new Random();
			String id = Integer.toHexString(random.nextInt());
			// 构建自定义IO实例
			IQ notificationIQ = createNotificationIQ(id, apiKey, title,
					message, uri);
			ClientSession session = sessionManager.getSession(user
					.getUsername());
			if (session != null && session.getPresence().isAvailable()) {
				notificationIQ.setTo(session.getAddress());
				session.deliver(notificationIQ);
			}
			// 将发送的每一条信息都保存至数据库，当接收到用户确认消息后就从数据库删除，否则当用户连接后会继续发送该通知
			saveNotification(apiKey, user.getUsername(), title, message, uri,
					id);
		}
	}

	/**
	 * Sends a newly created notification message to the specific user.
	 * 
	 * @param apiKey
	 *            the API key
	 * @param title
	 *            the title
	 * @param message
	 *            the message details
	 * @param uri
	 *            the uri
	 */
	public void sendNotifcationToUser(String apiKey, String username,
			String title, String message, String uri, boolean shouldsave) {

		log.debug("sendNotifcationToUser——>" + "apiKey:" + apiKey + "username:"
				+ username + "message" + message + "uri" + uri + "title"
				+ title);
		try {
			// 在数据库中查找用户
			User user = userService.getUserByUsername(username);

			if (user != null) {
				Random random = new Random();
				String id = Integer.toHexString(random.nextInt());
				IQ notificationIQ = createNotificationIQ(id, apiKey, title,
						message, uri);
				ClientSession session = sessionManager.getSession(username);
				if (session != null) {
					if (session.getPresence().isAvailable()) {
						notificationIQ.setTo(session.getAddress());
						session.deliver(notificationIQ);
					}
				}

				if (shouldsave) {
					// 保存消息
					saveNotification(apiKey, username, title, message, uri, id);
				}

			}

		} catch (UserNotFoundException e) {

			e.printStackTrace();
		}

	}

	/**
	 * 按真实名字推送消息
	 * 
	 * @param apiKey
	 *            the API key
	 * @param title
	 *            the title
	 * @param message
	 *            the message details
	 * @param uri
	 *            the uri
	 */
	public void sendNotifcationToUserByRealName(String realName, String apiKey,
			String title, String message, String uri, boolean shouldsave) {
		log.debug("sendNotifcationToUserByRealName()...title----->" + title);
		List<User> users = null;
		try {
			// 在数据库中查找用户
			users = userService.getUserByRealName(realName);
			log.debug("sendNotifcationToUserByRealName findUser------->"
					+ users);
		} catch (UserNotFoundException e) {
			e.printStackTrace();
		}

		for (User user : users) {
			if (user != null) {
				Random random = new Random();
				String id = Integer.toHexString(random.nextInt());
				IQ notificationIQ = createNotificationIQ(id, apiKey, title,
						message, uri);
				ClientSession session = sessionManager.getSession(user
						.getUsername());
				if (session != null) {
					if (session.getPresence().isAvailable()) {
						notificationIQ.setTo(session.getAddress());
						session.deliver(notificationIQ);
					}
				}

				if (shouldsave) {
					// 保存消息
					saveNotification(apiKey, realName, title, message, uri, id);
				}

			}

		}

	}

	// 通过别名发送通知
	public void sendNotificatoionByalias(String alias, String apiKey,
			String title, String message, String uri, boolean shouldsave) {
		String username = sessionManager.getUsernameByAlias(alias);
		log.debug("sendNotificatoionByalias-->username"+username);
		if (username != null) {
			sendNotifcationToUser(apiKey, username, title, message, uri,
					shouldsave);
		}
	}

	public void sendNotificationByTag(String tag, String apiKey, String title,
			String message, String uri, boolean shouldsave) {
		Set<String> usernameSet = sessionManager.getUsernamesBytag(tag);
		if (usernameSet != null && usernameSet.size() > 0) {
			for (String username : usernameSet) {
				sendNotifcationToUser(apiKey, username, title, message, uri,
						shouldsave);
			}
		}
	}

	// 保存离线消息
	public void saveNotification(String apiKey, String username, String title,
			String message, String uri, String uuid) {

		Notification notification = new Notification();
		notification.setApiKey(apiKey);
		notification.setUri(uri);
		notification.setMessage(message);
		notification.setUsername(username);
		notification.setTitle(title);
		notification.setUuid(uuid);
		notificationService.saveNotification(notification);
	}

	/**
	 * Creates a new notification IQ and returns it.
	 */
	private IQ createNotificationIQ(String id, String apiKey, String title,
			String message, String uri) {
		// String id = String.valueOf(System.currentTimeMillis());

		if (null == id || null == apiKey || null == title || null == message
				|| null == uri) {
			return null;
		}

		Element notification = DocumentHelper.createElement(QName.get(
				"notification", NOTIFICATION_NAMESPACE));
		notification.addElement("id").setText(id);
		notification.addElement("apiKey").setText(apiKey);
		notification.addElement("title").setText(title);
		notification.addElement("message").setText(message);
		notification.addElement("uri").setText(uri);

		IQ iq = new IQ();
		iq.setType(IQ.Type.set);
		iq.setChildElement(notification);

		return iq;
	}

	private IQ createNotificationIQ(Notification notification) {
		if (notification == null) {
			return null;
		}

		Element element = DocumentHelper.createElement(QName.get(
				"notification", NOTIFICATION_NAMESPACE));
		element.addElement("id").setText("" + notification.getId());
		element.addElement("apiKey").setText(notification.getApiKey());
		element.addElement("title").setText(notification.getTitle());
		element.addElement("message").setText(notification.getMessage());
		element.addElement("uri").setText(notification.getUri());

		IQ iq = new IQ();
		iq.setType(IQ.Type.set);
		iq.setChildElement(element);

		return iq;
	}

	/**
	 * @param delay
	 *            重新发送延迟时间
	 * @param userName
	 *            用户名
	 * @param frequency
	 *            发送次数
	 * 
	 */
	public void retrySendToUser(int intervalTime, final String userName,
			final int frequency) {
		int time = 3 * 1000;
		time = intervalTime >= time ? intervalTime : time;
		ReSendnotificationIQ(time, frequency, userName, SEND_TO_USER);

	}

	/**
	 * @param delay
	 *            重新发送延迟时间
	 * @param frequency
	 *            发送次数
	 * 
	 */
	public void retrySendToAll(int delay, final int frequency) {
		int time = 10 * 1000;
		time = delay >= time ? delay : time;
		ReSendnotificationIQ(time, frequency, null, SEND_TO_ALL);

	}

	private void ReSendnotificationIQ(final int delay, final int frequency,
			final String userName, final int sendFlag) {
		if ((null == userName || userName.length() == 0)
				&& sendFlag == SEND_TO_USER) {
			return;
		}

		new Thread(new Runnable() {

			public void run() {
				int f = frequency;
				while (f > 0) {

					try {
						// 等待一段时间
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					final List<Notification> list;

					final List<Notification> notificationList = new ArrayList<Notification>();

					switch (sendFlag) {
					case SEND_TO_USER:
						list = notificationService
								.findNotificationsByUsername(userName);
						if (list == null || list.size() == 0) {
							break;
						}
						notificationList.addAll(list);// 去数据库查询数据
						break;
					case SEND_TO_ALL:
						list = notificationService.findAllNotifications();
						if (list == null || list.size() == 0) {
							break;
						}
						notificationList.addAll(list);
						break;
					}

					if (null != notificationList
							&& notificationList.size() != 0) {
						break;
					}

					synchronized (notificationService) {

						for (Notification notification : notificationList) {
							IQ notificationIQ = createNotificationIQ(notification);

							if (notificationIQ == null) {
							}
							ClientSession session = sessionManager
									.getSession(notification.getUsername());
							if (session != null) {
								if (session.getPresence().isAvailable()) {
									notificationIQ.setTo(session.getAddress());
									session.deliver(notificationIQ);
									log.debug("重发至：" + session.getAddress()
											+ "--------------" + f);
								}
							}

						}

						f--;
					}
				}

			}
		}).start();

	}

}
