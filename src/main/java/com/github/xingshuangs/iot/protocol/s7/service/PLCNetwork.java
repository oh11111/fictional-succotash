package com.github.xingshuangs.iot.protocol.s7.service;


import com.github.xingshuangs.iot.exceptions.S7CommException;
import com.github.xingshuangs.iot.net.socket.SocketBasic;
import com.github.xingshuangs.iot.protocol.s7.enums.EErrorClass;
import com.github.xingshuangs.iot.protocol.s7.enums.EPduType;
import com.github.xingshuangs.iot.protocol.s7.enums.EReturnCode;
import com.github.xingshuangs.iot.protocol.s7.model.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * plc的网络通信
 *
 * @author xingshuang
 */
public class PLCNetwork extends SocketBasic {

    private final Object objLock = new Object();

    /**
     * 最大的PDU长度
     */
    private int maxPduLength = 0;

    public PLCNetwork() {
        super();
    }

    public PLCNetwork(String host, int port) {
        super(host, port);
    }

    //region socket连接后握手操作

    /**
     * 连接成功之后要做的动作
     */
    @Override
    protected void doAfterConnected() throws IOException {
        this.connectionRequest();
        // 减去前部的字节，留下pdu
        this.maxPduLength = this.connectDtData() - 20;
    }

    /**
     * 连接请求
     *
     * @throws IOException 异常
     */
    private void connectionRequest() throws IOException {
        S7Data req = S7Data.createConnectRequest();
        S7Data ack = this.readFromServer(req);
        if (ack.getCotp().getPduType() != EPduType.CONNECT_CONFIRM) {
            throw new S7CommException("连接请求被拒绝");
        }
    }

    /**
     * 连接setup
     *
     * @return pduLength pdu长度
     * @throws IOException 异常
     */
    private int connectDtData() throws IOException {
        S7Data req = S7Data.createConnectDtData();
        S7Data ack = this.readFromServer(req);
        if (ack.getCotp().getPduType() != EPduType.DT_DATA) {
            throw new S7CommException("连接Setup响应错误");
        }
        if (ack.getHeader() == null || ack.getHeader().byteArrayLength() != AckHeader.BYTE_LENGTH) {
            throw new S7CommException("连接Setup响应错误，缺失响应头header或响应头长度不够[12]");
        }
        AckHeader ackHeader = (AckHeader) ack.getHeader();
        if (ackHeader.getErrorClass() != EErrorClass.NO_ERROR) {
            throw new S7CommException("连接Setup响应错误，原因：" + ackHeader.getErrorClass());
        }
        int length = ((SetupComParameter) ack.getParameter()).getPduLength();
        if (length <= 0) {
            throw new S7CommException("PDU的最大长度小于0");
        }
        return length;
    }
    //endregion

    //region 底层数据通信部分

    public S7Data readFromServer(S7Data s7Data) throws IOException {
        byte[] sendData = s7Data.toByteArray();
        if (this.maxPduLength > 0 && sendData.length > this.maxPduLength) {
            throw new S7CommException("发送请求的字节数过长，已经大于最大的PDU长度");
        }
        TPKT tpkt;
        int len;
        byte[] remain;
        synchronized (this.objLock) {
            this.writeWrapper(sendData);

            byte[] data = new byte[TPKT.BYTE_LENGTH];
            len = this.readWrapper(data);
            if (len < TPKT.BYTE_LENGTH) {
                throw new S7CommException(" TPKT 无效，长度不一致");
            }
            tpkt = TPKT.fromBytes(data);
            remain = new byte[tpkt.getLength() - TPKT.BYTE_LENGTH];
            len = this.readWrapper(remain);
        }
        if (len < remain.length) {
            throw new S7CommException(" TPKT后面的数据长度，长度不一致");
        }
        return S7Data.fromBytes(tpkt, remain);
    }

    /**
     * 读取字节
     *
     * @param data 字节数组
     * @return 读取数量
     * @throws IOException 异常
     */
    private int readWrapper(final byte[] data) throws IOException {
        int offset = 0;
        while (offset < data.length) {
            int length = this.maxPduLength <= 0 ? data.length - offset : Math.min(this.maxPduLength, data.length - offset);
            this.read(data, offset, length);
            offset += length;
        }
        return offset;
    }

    /**
     * 写入字节
     *
     * @param data 字节数组
     * @throws IOException 异常
     */
    private void writeWrapper(final byte[] data) throws IOException {
        int offset = 0;
        while (offset < data.length) {
            int length = this.maxPduLength <= 0 ? data.length - offset : Math.min(this.maxPduLength, data.length - offset);
            this.write(data, offset, length);
            offset += length;
        }
    }
    //endregion

    //region S7数据读写部分

    /**
     * 读取S7协议数据
     *
     * @param requestItems 请求项列表
     * @return 数据项列表
     * @throws IOException 异常
     */
    protected List<DataItem> readS7Data(List<RequestItem> requestItems) throws IOException {
        S7Data req = S7Data.createReadDefault();
        ReadWriteParameter parameter = (ReadWriteParameter) req.getParameter();
        parameter.addItem(requestItems);
        req.selfCheck();
        S7Data ack = this.readFromServer(req);
        this.checkReqAndAck(req, ack);
        return ack.getDatum().getReturnItems().stream().map(x -> (DataItem) x).collect(Collectors.toList());
    }

    /**
     * 读取S7协议数据
     *
     * @param item 请求项
     * @return 数据项
     * @throws IOException 异常
     */
    protected DataItem readS7Data(RequestItem item) throws IOException {
        return this.readS7Data(Collections.singletonList(item)).get(0);
    }

    protected void writeS7Data(RequestItem requestItem, DataItem dataItem) throws IOException {
        this.writeS7Data(Collections.singletonList(requestItem), Collections.singletonList(dataItem));
    }

    protected void writeS7Data(List<RequestItem> requestItems, List<DataItem> dataItems) throws IOException {
        if (requestItems.size() != dataItems.size()) {
            throw new S7CommException("写操作过程中，requestItems和dataItems数据个数不一致");
        }
        S7Data req = S7Data.createWriteDefault();
        ReadWriteParameter parameter = (ReadWriteParameter) req.getParameter();
        parameter.addItem(requestItems);
        req.getDatum().addItem(dataItems);
        req.selfCheck();
        S7Data ack = this.readFromServer(req);
        this.checkReqAndAck(req, ack);
    }

    /**
     * 对请求和响应数据进行一次校验
     *
     * @param req 请求数据
     * @param ack 响应属于
     */
    private void checkReqAndAck(S7Data req, S7Data ack) {
        // 响应头正确
        AckHeader ackHeader = (AckHeader) ack.getHeader();
        if (ackHeader.getErrorClass() != EErrorClass.NO_ERROR) {
            throw new S7CommException(String.format("响应异常，原因：%s", ackHeader.getErrorClass().getDescription()));
        }
        // 发送和接收的PDU编号一致
        if (ackHeader.getPduReference() != req.getHeader().getPduReference()) {
            throw new S7CommException("pdu应用编号不一致，数据有误");
        }
        // 请求的数据个数一致
        List<ReturnItem> returnItems = ack.getDatum().getReturnItems();
        ReadWriteParameter parameter = (ReadWriteParameter) req.getParameter();
        if (returnItems.size() != parameter.getItemCount()) {
            throw new S7CommException("返回的数据个数和请求的数据个数不一致");
        }
        // 返回结果校验
        returnItems.forEach(x -> {
            if (x.getReturnCode() != EReturnCode.SUCCESS) {
                throw new S7CommException(String.format("返回结果异常，原因：%s", x.getReturnCode().getDescription()));
            }
        });
    }
    //endregion
}
