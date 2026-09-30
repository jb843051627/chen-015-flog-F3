package com.fc.v2.model.custom.flog;

import java.math.BigDecimal;

/**
 * 清册一行进门报文：一行递进来，不带行次（行次由进门那一刻的顺序钉死），
 * 不带落地情形（收下、挂起由业务层给回定夺），更没有手工改账的格子。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogDeclareRowDto {

    /** 伐区申报清册批次号（一册一号） */
    private String batchNo;

    /** 申报编制单位底档字号（同一家以此字号为准认） */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 林种类别 1公益林 2商品林 */
    private Integer forestKind;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    private Integer cutMode;

    /** 本行申报拟采蓄积(立方米) */
    private BigDecimal qty;

    /** 本行申报拟采面积(公顷) */
    private BigDecimal fineAmt;

    /** 权属是否涉讼 0否 1是 */
    private Integer ownershipDispute;

    /** 附没附伐区调查设计材料 0没附 1附了 */
    private Integer surveyAttached;

    /** 实收蓄积(立方米)：空着按申报全收；只许不大于本行申报，多的不许塞本行也不许挪邻行 */
    private BigDecimal receivedQty;

    /** 备注 */
    private String remark;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public Integer getForestKind() {
        return forestKind;
    }

    public void setForestKind(Integer forestKind) {
        this.forestKind = forestKind;
    }

    public Integer getCutMode() {
        return cutMode;
    }

    public void setCutMode(Integer cutMode) {
        this.cutMode = cutMode;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public Integer getOwnershipDispute() {
        return ownershipDispute;
    }

    public void setOwnershipDispute(Integer ownershipDispute) {
        this.ownershipDispute = ownershipDispute;
    }

    public Integer getSurveyAttached() {
        return surveyAttached;
    }

    public void setSurveyAttached(Integer surveyAttached) {
        this.surveyAttached = surveyAttached;
    }

    public BigDecimal getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(BigDecimal receivedQty) {
        this.receivedQty = receivedQty;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
