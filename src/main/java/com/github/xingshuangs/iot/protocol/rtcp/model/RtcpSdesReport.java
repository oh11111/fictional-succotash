package com.github.xingshuangs.iot.protocol.rtcp.model;


import com.github.xingshuangs.iot.protocol.common.IObjectByteArray;
import com.github.xingshuangs.iot.protocol.common.buff.ByteWriteBuff;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源描述报告
 *
 * @author xingshuang
 */
@Data
public class RtcpSdesReport implements IObjectByteArray {

    private RtcpHeader header;

    private List<RtcpSdesChunk> sdesChunks = new ArrayList<>();

    @Override
    public int byteArrayLength() {
        int length = 0;
        length += this.header != null ? this.header.byteArrayLength() : 0;
        for (RtcpSdesChunk chunk : this.sdesChunks) {
            length += chunk.byteArrayLength();
        }
        return length;
    }

    @Override
    public byte[] toByteArray() {
        ByteWriteBuff buff = ByteWriteBuff.newInstance(this.byteArrayLength());
        if (this.header != null) {
            buff.putBytes(this.header.toByteArray());
        }
        for (RtcpSdesChunk chunk : this.sdesChunks) {
            buff.putBytes(chunk.toByteArray());
        }
        return buff.getData();
    }

    /**
     * 字节数组数据解析
     *
     * @param data 字节数组数据
     * @return RtcpHeader
     */
    public static RtcpSdesReport fromBytes(final byte[] data) {
        return fromBytes(data, 0);
    }

    /**
     * 字节数组数据解析
     *
     * @param data   字节数组数据
     * @param offset 偏移量
     * @return RtcpHeader
     */
    public static RtcpSdesReport fromBytes(final byte[] data, final int offset) {
        if (data.length < 4) {
            throw new IndexOutOfBoundsException("解析RtcpReceiverReport时，字节数组长度不够");
        }
        int off = offset;
        RtcpSdesReport res = new RtcpSdesReport();
        res.header = RtcpHeader.fromBytes(data, off);
        off += res.header.byteArrayLength();
        for (int i = 0; i < res.header.getReceptionCount(); i++) {
            RtcpSdesChunk chunk = RtcpSdesChunk.fromBytes(data, off);
            res.sdesChunks.add(chunk);
            off += chunk.byteArrayLength();
        }
        return res;
    }
}
