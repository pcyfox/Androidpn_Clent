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
package org.androidpn.server.console.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.androidpn.server.util.Config;
import org.androidpn.server.xmpp.push.NotificationManager;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
 * A controller class to process the notification related requests.
 *
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class NotificationController extends MultiActionController {

	private NotificationManager notificationManager;

	public NotificationController() {
		notificationManager = new NotificationManager();
	}

	public ModelAndView list(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView mav = new ModelAndView();
		// mav.addObject("list", null);
		mav.setViewName("notification/form");
		return mav;
	}

	public ModelAndView send(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String broadcast = ServletRequestUtils.getStringParameter(request, "broadcast", "0");
		String username = ServletRequestUtils.getStringParameter(request, "username","null");
		String realName = ServletRequestUtils.getStringParameter(request, "realName","null");
		String alias = ServletRequestUtils.getStringParameter(request, "alias","null");
		String title = ServletRequestUtils.getStringParameter(request, "title","null");
		String message = ServletRequestUtils.getStringParameter(request, "message","null");
		String uri = ServletRequestUtils.getStringParameter(request, "uri","null");
		String apiKey = Config.getString("apiKey", "null");
		

		if (broadcast.equalsIgnoreCase("0")) {
			// 所有用户推送
			notificationManager.sendBroadcast(apiKey, title, message, uri);
			//开启重发机制
			notificationManager.retrySendToAll(5*1000,3);
		} else if (broadcast.equalsIgnoreCase("1")) {
			// 项指定(注册名)的单个用户推送（先查询数据库中用户是否存在，再检查session连接是否有效）
			notificationManager.sendNotifcationToUser(apiKey, username.trim(), title, message, uri, true);
		} else if (broadcast.equalsIgnoreCase("2")) {
			// 向指定别名用户推送（不需要查询数据库）
			//别名并没有持久化处理,且相同别名会发生覆盖，连接用户过多会导致对应的map过大，不知道会不会产生暗病。
			notificationManager.sendNotificatoionByalias(alias, apiKey, title, message, uri, false);
			
		}else if (broadcast.equalsIgnoreCase("3")) {
			logger.debug("realName=-->" + realName);
			// 向指定名称用户推送
			notificationManager.sendNotifcationToUserByRealName(realName.trim(), apiKey, title, message, uri, false);
			
		}

		ModelAndView mav = new ModelAndView();
		mav.setViewName("redirect:notification.do");
		return mav;
	}

}
