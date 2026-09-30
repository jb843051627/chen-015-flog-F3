package com.fc.v2.model.custom.flog;

/**
 * 收册前置项代号：一行哪一项没过，就按该代号落挂起，缘故一句话写明。
 * 一行可以同时撞多项，账上只记头一道拦下的代号（核验按下面次序逐道过）。
 *
 * @author fuce
 * @date 2026-09-29
 */
public enum FlogPreItem {

    /** 公益林只许抚育、更新、低质低效林改造性质采伐，走了主伐（皆伐/渐伐/择伐）即挂起 */
    P01("P01", "公益林走了主伐", "公益林依法只许抚育、更新与低质低效林改造性质采伐，本行报的是主伐，挂起待改"),
    /** 权属还打着官司，权属未定先不入账 */
    P02("P02", "权属涉讼", "本行小班权属尚在诉讼，权属未定，挂起待了断后再收"),
    /** 超过省级规定的面积或蓄积量，却没附伐区调查设计材料，挂起补材料 */
    P03("P03", "超限未附调查材料", "本行申报面积或蓄积越过省级规定且未附伐区调查设计材料，挂起待补材料");

    private final String code;
    private final String title;
    private final String reason;

    FlogPreItem(String code, String title, String reason) {
        this.code = code;
        this.title = title;
        this.reason = reason;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getReason() {
        return reason;
    }

    public static FlogPreItem ofCode(String code) {
        if (code == null) {
            return null;
        }
        for (FlogPreItem p : values()) {
            if (p.code.equals(code)) {
                return p;
            }
        }
        return null;
    }
}
