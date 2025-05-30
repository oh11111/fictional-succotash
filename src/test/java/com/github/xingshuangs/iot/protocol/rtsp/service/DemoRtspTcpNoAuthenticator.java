package com.github.xingshuangs.iot.protocol.rtsp.service;


import com.github.xingshuangs.iot.protocol.rtp.model.frame.H264VideoFrame;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author xingshuang
 */
public class DemoRtspTcpNoAuthenticator {

    public static void main(String[] args) {
        // rtsp的地址
        URI uri = URI.create("rtsp://127.0.0.1:8554/11");
        // 构建RTSP客户端对象
        RtspClient client = new RtspClient(uri);
        // 设置RTSP交互过程信息打印，若不需要则可以不设置
        client.onCommCallback(System.out::println);
        // 设置接收的视频数据帧事件
        client.onFrameHandle(x -> {
            H264VideoFrame f = (H264VideoFrame) x;
            System.out.println(f.getFrameType() + ", " + f.getNaluType() + ", " + f.getTimestamp() + ", " + f.getFrameSegment().length);
        });
        // 采用异步的形式关闭，由于是测试示例，写在启动前面
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            client.stop();
        });
        // 启动，返回异步执行对象
        CompletableFuture<Void> future = client.start();
        // 循环等待结束
        while (!future.isDone()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
