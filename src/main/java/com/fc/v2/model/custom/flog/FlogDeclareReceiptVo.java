package com.fc.v2.model.custom.flog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 一册清册的回执给回。
 * 本册总计几行、收下几行、挂起几行三数与库里逐行算出的同出一次计算，
 * 且挂起数加收下数抵得上原册行数；界面自行清点的一概不算。
 * 空册只写“空册”，绝不写“核讫”。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogDeclareReceiptVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 批次号 */
    private String batchNo;

    /** 整册处置 0核收中 1整册退回 2空册 3核讫 */
    private Integer status;

    /** 整册处置名目 */
    private String statusName;

    /** 本册原册总行数（逐行进门的总行数，含并前行的重复行目） */
    private Integer totalRows;

    /** 收下几行（同一回逐行计算，重复行目计入收下行但蓄积只落一次） */
    private Integer acceptedRows;

    /** 挂起几行 */
    private Integer heldRows;

    /** 回执结论原话（核讫/空册/整册退回） */
    private String receiptRemark;

    /** 落定时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date settledTime;

    /** 给屏上的话 */
    private String message;

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getAcceptedRows() {
        return acceptedRows;
    }

    public void setAcceptedRows(Integer acceptedRows) {
        this.acceptedRows = acceptedRows;
    }

    public Integer getHeldRows() {
        return heldRows;
    }

    public void setHeldRows(Integer heldRows) {
        this.heldRows = heldRows;
    }

    public String getReceiptRemark() {
        return receiptRemark;
    }

    public void setReceiptRemark(String receiptRemark) {
        this.receiptRemark = receiptRemark;
    }

    public Date getSettledTime() {
        return settledTime;
    }

    public void setSettledTime(Date settledTime) {
        this.settledTime = settledTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
