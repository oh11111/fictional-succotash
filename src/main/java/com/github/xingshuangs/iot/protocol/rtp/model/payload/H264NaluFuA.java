package com.github.xingshuangs.iot.protocol.rtp.model.payload;


import com.github.xingshuangs.iot.protocol.common.buff.ByteReadBuff;
import com.github.xingshuangs.iot.protocol.common.buff.ByteWriteBuff;
import lombok.Data;

import java.util.Arrays;

/**
 * Nalu的FuA
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | FU indicator  |   FU header   |                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+                               |
 * |                                                               |
 * |                         FU payload                            |
 * |                                                               |
 * |                               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                               :...OPTIONAL RTP padding        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * @author xingshuang
 */
@Data
public class H264NaluFuA extends H264NaluBase {

    /**
     * Fu的头
     */
    private H264NaluFuHeader fuHeader = new H264NaluFuHeader();

    /**
     * 负载
     */
    protected byte[] payload = new byte[0];

    @Override
    public int byteArrayLength() {
        return this.header.byteArrayLength() + this.fuHeader.byteArrayLength() + this.payload.length;
    }

    @Override
    public byte[] toByteArray() {
        return ByteWriteBuff.newInstance(this.byteArrayLength())
                .putBytes(this.header.toByteArray())
                .putBytes(this.fuHeader.toByteArray())
                .putBytes(this.payload)
                .getData();
    }

    /**
     * 字节数组数据解析
     *
     * @param data 字节数组数据
     * @return RtcpHeader
     */
    public static H264NaluFuA fromBytes(final byte[] data) {
        return fromBytes(data, 0);
    }

    /**
     * 字节数组数据解析
     *
     * @param data   字节数组数据
     * @param offset 偏移量
     * @return RtcpHeader
     */
    public static H264NaluFuA fromBytes(final byte[] data, final int offset) {
        if (data.length < 1) {
            throw new IndexOutOfBoundsException("解析H264NaluSingle时，字节数组长度不够");
        }
        H264NaluFuA res = new H264NaluFuA();
        int index = offset;
        res.header = H264NaluHeader.fromBytes(data, index);
        index += res.header.byteArrayLength();

        res.fuHeader = H264NaluFuHeader.fromBytes(data, index);
        index += res.fuHeader.byteArrayLength();

        ByteReadBuff buff = new ByteReadBuff(data, index);
        res.payload = buff.getBytes();
        return res;
    }
}
