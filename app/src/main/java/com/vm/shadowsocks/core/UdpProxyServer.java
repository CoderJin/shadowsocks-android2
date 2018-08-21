package com.vm.shadowsocks.core;

import android.util.Log;

import com.vm.shadowsocks.tunnel.TcpTunnel;
import com.vm.shadowsocks.tunnel.TcpBaseTunnel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class UdpProxyServer implements Runnable {
    private static final String TAG = UdpProxyServer.class.getSimpleName();
    private static final int UDP_RECV_BUFF_SIZE = 64 * 1024;

    public boolean Stopped;
    public int Port;

    Selector m_Selector;
    DatagramChannel udpServerChannel;
    Thread m_ServerThread;

    public UdpProxyServer(int port) throws IOException {
        m_Selector = Selector.open();
        udpServerChannel = DatagramChannel.open();
        udpServerChannel.configureBlocking(false);
        udpServerChannel.socket().bind(new InetSocketAddress(port));
        udpServerChannel.register(m_Selector, SelectionKey.OP_READ);
        this.Port = udpServerChannel.socket().getLocalPort();
        Log.d(TAG, String.format("AsyncUdpServer listen on %d success.\n", this.Port & 0xFFFF));
    }

    public void start() {
        m_ServerThread = new Thread(this);
        m_ServerThread.setName("UdpProxyServerThread");
        m_ServerThread.start();
    }

    public void stop() {
        this.Stopped = true;
        if (m_Selector != null) {
            try {
                m_Selector.close();
                m_Selector = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (udpServerChannel != null) {
            try {
                udpServerChannel.close();
                udpServerChannel = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                m_Selector.select();
                Iterator<SelectionKey> keyIterator = m_Selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        try {
                            if (key.isReadable()) {
//                                onCheckRemoteTunnel(key);
                                ((TcpBaseTunnel) key.attachment()).onReadable(key);
                            } else if (key.isWritable()) {
                                ((TcpBaseTunnel) key.attachment()).onWritable(key);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, e.toString());
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.stop();
            Log.d(TAG, "TcpServer thread exited.");
        }
    }

//    private void onCheckRemoteTunnel(SelectionKey key) {
//        TcpBaseTunnel localTunnel = null;
//        try {
//            DatagramChannel localChannel = (DatagramChannel) key.channel();
//            InetSocketAddress destAddress = TunnelFactory.getDestAddress(localChannel);
//            if (destAddress != null) {
//                if (!NatMapper.containUdpChannel(localChannel.socket().getPort())) {
//                    localTunnel = TunnelFactory.wrap(localChannel, m_Selector);
//                    TcpBaseTunnel remoteTunnel = TunnelFactory.createTunnelByConfig(destAddress, m_Selector, TcpTunnel.class);
//                    remoteTunnel.setBrotherTunnel(localTunnel);//关联兄弟
//                    localTunnel.setBrotherTunnel(remoteTunnel);//关联兄弟
//                    remoteTunnel.listen(destAddress);//开始连接
//                }
//            } else {
//                Log.d(TAG, String.format("Error: socket(%s:%d) target host is null.", localChannel.socket().getInetAddress().toString(), localChannel.socket().getPort()));
//                localTunnel.dispose();
//            }
//        } catch (Exception e) {
//            Log.d(TAG, "Error: remote socket create failed: " + e.toString());
//            if (localTunnel != null) {
//                localTunnel.dispose();
//            }
//        }
//    }


}