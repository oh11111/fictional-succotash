package com.github.xingshuangs.iot.net.server;


import com.github.xingshuangs.iot.exceptions.SocketRuntimeException;
import com.github.xingshuangs.iot.net.SocketUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.*;

/**
 * socket服务端的基础类
 *
 * @author xingshuang
 */
@Slf4j
public class ServerSocketBasic {

    /**
     * 服务器对象
     */
    private ServerSocket serverSocket;

    /**
     * 客户端MAP
     */
    private ConcurrentHashMap<String, Socket> clientMap = new ConcurrentHashMap<>();

    //region 服务端

    /**
     * 启动
     */
    public void start() {
        this.start(8088);
    }

    /**
     * 启动
     *
     * @param port 端口号
     */
    public void start(int port) {
        try {
            this.stop();
            this.serverSocket = new ServerSocket(port);
            CompletableFuture.runAsync(this::waitForClients);
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        try {
            if (this.isAlive()) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    /**
     * 是否活跃着
     *
     * @return ture：活跃着，false：死了
     */
    public boolean isAlive() {
        return this.serverSocket != null && !this.serverSocket.isClosed();
    }

    /**
     * 等待客户端连入
     */
    private void waitForClients() {
        while (this.isAlive()) {
            try {
                Socket client = this.serverSocket.accept();
                if (!this.checkClientValid(client)) {
                    SocketUtils.close(client);
                }
                CompletableFuture.runAsync(() -> this.doClientConnected(client));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    //endregion

    //region 客户端

    /**
     * 校验客户端是否允许连入
     *
     * @param client 客户端
     * @return 是否有效，true：有效，false：无效
     */
    protected boolean checkClientValid(Socket client) throws IOException {
        return true;
    }

    /**
     * 客户端连入后要做的业务
     *
     * @param client 客户端
     */
    private void doClientConnected(Socket client) {
        SocketAddress address = client.getRemoteSocketAddress();
        this.clientMap.put(address.toString(), client);
        log.debug("有客户端[{}]连入，当前客户端数量[{}]", address, this.clientMap.size());

        try {
            if (this.checkHandshake(client)) {
                while (SocketUtils.isConnected(client)) {
                    this.doClientHandle(client);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try {
                SocketUtils.close(client);
            } catch (Exception ex) {
                // NOOP
            }
        }

        this.clientMap.remove(address.toString());
        log.debug("有客户端[{}]断开，当前客户端数量[{}]", address, this.clientMap.size());
    }

    /**
     * 握手校验
     *
     * @param socket 客户端
     * @return 校验结果，true：成功，false：失败
     */
    protected boolean checkHandshake(Socket socket) {
        return true;
    }

    /**
     * 执行客户端的业务，可重写
     *
     * @param socket 客户端的socket对象
     * @throws IOException IO异常
     */
    protected void doClientHandle(Socket socket) {
        byte[] data = this.readClientData(socket);
        if (data.length == 0) {
            throw new SocketRuntimeException("客户端主动断开");
        }
        log.debug(new String(data));
    }

    /**
     * 读取客户端数据
     *
     * @param socket 客户端socket对象
     * @return 读取的字节数据
     */
    protected byte[] readClientData(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            int firstByte = in.read();
            if (firstByte == -1) {
                return new byte[0];
            }
            byte[] data = new byte[in.available() + 1];
            data[0] = (byte) firstByte;
            SocketUtils.read(socket, data, 1, data.length - 1, 1024);
            return data;
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    public void write(final Socket socket, final byte[] data) {
        try {
            SocketUtils.write(socket, data);
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }
    //endregion
}
