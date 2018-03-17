/**
 * $RCSfile$
 * $Revision$
 * $Date$
 * <p>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

/**
 * SocketFactory for direct connection
 *
 * @author Atul Aggarwal
 */
class DirectSocketFactory
        extends SocketFactory {

    public DirectSocketFactory() {
    }

    static private int roundrobin = 0;

    public Socket createSocket(String host, int port)
            throws IOException, UnknownHostException {
        Socket newSocket = new Socket(Proxy.NO_PROXY);
        InetAddress resolved[] = InetAddress.getAllByName(host);
        //newSocket.connect(new InetSocketAddress(resolved[(roundrobin++) % resolved.length], port));
        //设置超时时间否则默认为3分钟没连接Socket才抛出异常
        //TODO: 标识源码修改而已！
        newSocket.connect(new InetSocketAddress(resolved[(roundrobin++) % resolved.length], port), 10 * 1000);
        return newSocket;
    }

    public Socket createSocket(String host, int port, InetAddress localHost,
                               int localPort)
            throws IOException, UnknownHostException {
        return new Socket(host, port, localHost, localPort);
    }

    public Socket createSocket(InetAddress host, int port)
            throws IOException {
        Socket newSocket = new Socket(Proxy.NO_PROXY);
        newSocket.connect(new InetSocketAddress(host, port));
        return newSocket;
    }

    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort)
            throws IOException {
        return new Socket(address, port, localAddress, localPort);
    }

}
