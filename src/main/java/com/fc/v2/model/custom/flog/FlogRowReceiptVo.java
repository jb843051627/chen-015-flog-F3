package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 清册一行核收给回。
 * 一行进门只有三个说法——待核、已收、挂起；屏上摆的情形、行次、缘故、实收数全跟着这一份走，
 * 界面另起一套数行不通。并进前行的重复行目在这里点明并进了第几行。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogRowReceiptVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 账上本行主键（并进前行的重复行目为null，账只认前行那一行） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long rowId;

    /** 批次号 */
    private String batchNo;

    /** 进门行次（并行的重复行目带自己的进门次序，账上认 mergedIntoRowNo 那一行） */
    private Integer rowNo;

    /** 并进了同册第几行：非空即本笔为重复行目，账并前行、蓄积不落第二次 */
    private Integer mergedIntoRowNo;

    /** 申报单位底档字号 */
    private String siteNo;

    /** 小班号 */
    private String subcompNo;

    /** 行落地情形 0待核 1已收 2挂起 */
    private Integer status;

    /** 挂起前置项代号（P01/P02/P03） */
    private String itemCode;

    /** 挂起缘故（一句话） */
    private String holdReason;

    /** 申报拟采蓄积 */
    private BigDecimal declaredQty;

    /** 实收蓄积（挂起行为空） */
    private BigDecimal receivedQty;

    /** 账页 0现行页 1往期页 */
    private Integer ledgerStatus;

    /** 本笔之后整册处置 0核收中 1整册退回 2空册 3核讫（越线那一笔这里给1，本行未落地，status为空） */
    private Integer batchStatus;

    /** 给屏上的话 */
    private String message;

    public Long getRowId() {
        return rowId;
    }

    public void setRowId(Long rowId) {
        this.rowId = rowId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getMergedIntoRowNo() {
        return mergedIntoRowNo;
    }

    public void setMergedIntoRowNo(Integer mergedIntoRowNo) {
        this.mergedIntoRowNo = mergedIntoRowNo;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getHoldReason() {
        return holdReason;
    }

    public void setHoldReason(String holdReason) {
        this.holdReason = holdReason;
    }

    public BigDecimal getDeclaredQty() {
        return declaredQty;
    }

    public void setDeclaredQty(BigDecimal declaredQty) {
        this.declaredQty = declaredQty;
    }

    public BigDecimal getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(BigDecimal receivedQty) {
        this.receivedQty = receivedQty;
    }

    public Integer getLedgerStatus() {
        return ledgerStatus;
    }

    public void setLedgerStatus(Integer ledgerStatus) {
        this.ledgerStatus = ledgerStatus;
    }

    public Integer getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(Integer batchStatus) {
        this.batchStatus = batchStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
