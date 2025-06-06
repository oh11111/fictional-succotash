package com.github.xingshuangs.iot.protocol.modbus.model;


import lombok.Data;

/**
 * 请求读输入寄存器
 *
 * @author xingshuang
 */
@Data
public class MbReadInputRegisterRequest extends MbPdu {

    /**
     * 起始地址 说是从0x0000 至 0xFFFF，但对应实际却只是0001-9999，对应0x0000-0x270F <br>
     * 字节大小：2个字节
     */
    private int address;

    /**
     * 输入寄存器数量，该功能码读取线圈的 1 至 2000 连续状态，1 至 2000（0x7D0）
     * 字节大小：2个字节
     */
    private int quantity;

}
