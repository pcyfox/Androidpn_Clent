一 服务器端的推送流程:
1、NotificationManager的方法被调用，依据参数得到一组设备标识。
2、通过设备标识在SessionManager中检索可以匹配到ClientSession。
3、定义自己的xmpp消息格式并拼接出xml消息体。
4、通过ClientSession中的deliver方法，调用Connection中的deliver方法向用户发送消息

注：要定义和组装自己的xmpp消息，将适当的信息发送给客户端并便于客户端解析，需要修改的就是第3步，实例化一个IQ对象，
放入特定格式的消息体，然后直接发送就可以了。

二  服务器端的接收流程:
1、使用mina框架中的 NioSocketAcceptor开启与客户端的连接.
2、收到用户传输的数据时，首先经过IoFilter，完成消息的接收。
3、解码操作由org.androidpn.server.xmpp.codec.XmppDecode来完成，
      解析完成后为每一个解码后的消息对象调用ProtocolDecoderOutput接口的write(Object)方法，将消息输出。
4、之后就开始消息的处理工作，以XmppIoHandler 的messageReceived方法为起点，
      后续StanzaHandler -> PacketRouter，然后依据消息体的类型选择对应的路由器，
      如处理IQ的IQRouter，Router再根据packet的namespace，选择对应的handler。
5、handler进行处理。
