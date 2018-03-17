package org.androidpn.server.xmpp.handler;

import org.androidpn.server.xmpp.UnauthorizedException;
import org.androidpn.server.xmpp.session.ClientSession;
import org.androidpn.server.xmpp.session.Session;
import org.androidpn.server.xmpp.session.SessionManager;
import org.dom4j.Element;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

/**
 * 用户设置用户别名
 */
public class IQsetAliasHandler extends IQHandler {
	private static final String TAG = "IQsetAliasHandler";
	private static final String NAMESPACE = "androidpn:iq:setalias";

	private SessionManager sessionManager;

	public IQsetAliasHandler() {
		sessionManager = sessionManager.getInstance();
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		IQ reply = null;
	
		log.debug(TAG + packet);
		ClientSession session = sessionManager.getSession(packet.getFrom());
		if (session == null) {
			log.error("Session not found for key " + packet.getFrom());
			reply = IQ.createResultIQ(packet);
			reply.setChildElement(packet.getChildElement().createCopy());
			reply.setError(PacketError.Condition.internal_server_error);
			return reply;
		}
		if (session.getStatus() == Session.STATUS_AUTHENTICATED) {
			if (IQ.Type.set.equals(packet.getType())) {
				Element element = packet.getChildElement();
				String username = element.elementText("username");
				String alias = element.elementText("alias");
				//将客户端IQ中的别名及用户名映射保存至SessionManager中
				if (username != null && !username.equals("") && alias != null
						&& !alias.equals(""))
					sessionManager.setUserAlias(username, alias);
			}
		}
		return null;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

}
