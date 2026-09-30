package com.fc.v2.model.custom.flog;

/**
 * 清册整册处置：核收中、整册退回（越行数上限，一行不留）、空册、核讫。
 * 空册回执只准写“空册”，绝不准写“核讫”。
 *
 * @author fuce
 * @date 2026-09-29
 */
public enum FlogDeclareClose {

    /** 核收中：逐行进门未落定（常态下一行进门即定，此档只在跨册事务中途短暂可见） */
    RECEIVING(0, "核收中"),
    /** 整册退回：行数越上限，整册退、一行不留 */
    RETURNED(1, "整册退回"),
    /** 空册：一册一行也没有，回执只写空册 */
    EMPTY(2, "空册"),
    /** 核讫：本册各行已逐行落定（已收+挂起抵得上原册行数） */
    SETTLED(3, "核讫");

    private final int code;
    private final String name;

    FlogDeclareClose(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
