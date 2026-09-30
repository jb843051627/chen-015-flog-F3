package com.fc.v2.model.custom.flog;

/**
 * 清册行落地三说法：待核、已收、挂起。
 * 一行进门只许落这三档之一，挂起只挂那一行，同册余下的行照常入账。
 *
 * @author fuce
 * @date 2026-09-29
 */
public enum FlogRowLanding {

    /** 待核：进门未决（正常核收一进门即定档，此档只留给早年纸录转抄的旧行） */
    PENDING(0, "待核"),
    /** 已收：前置项全过、实收不越申报，入现行账 */
    RECEIVED(1, "已收"),
    /** 挂起：前置项有没过的，按代号记一句话缘故，同册余行不受累 */
    HELD(2, "挂起");

    private final int code;
    private final String name;

    FlogRowLanding(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static FlogRowLanding of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlogRowLanding s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
