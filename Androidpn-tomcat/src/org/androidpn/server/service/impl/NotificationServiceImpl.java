package org.androidpn.server.service.impl;

import java.util.List;

import org.androidpn.server.dao.NotificationDao;
import org.androidpn.server.model.Notification;
import org.androidpn.server.service.NotificationService;

public class NotificationServiceImpl implements NotificationService {

	private NotificationDao notificationDao;

	public synchronized void saveNotification(Notification notification) {
		notificationDao.saveNotification(notification);
	}

	public synchronized List<Notification> findNotificationsByUsername(
			String username) {
		return notificationDao.findNotificationsByUsername(username);
	}

	public synchronized void deleteNotification(Notification notification) {
		notificationDao.deleteNotification(notification);
	}

	public synchronized NotificationDao getNotificationDao() {
		return notificationDao;
	}

	public void setNotificationDao(NotificationDao notificationDao) {
		this.notificationDao = notificationDao;
	}

	public void deleteNotification(String uuid) {
		notificationDao.deleteNotificationByUuid(uuid);
	}

	public List<Notification> findAllNotifications() {
		return notificationDao.findAllNotifications();
	}

}
