package com.fc.v2.model.custom.flog;

import com.fc.v2.model.auto.TFlogDeclareRow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 凭批次号把一册账拉出来的给回：回执抬头 + 账上有效行（并前行的重复行目不另列）。
 * 现行页不露往期行，这里含往期；行次都是进门钉死的原次，挂起行直写出自第几行。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class FlogDeclareLedgerVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 册抬头与三数回执 */
    private FlogDeclareReceiptVo receipt;

    /** 账上有效行（merged_row_no 为空的），按进门行次升序，含已转往期的行 */
    private List<TFlogDeclareRow> rows = new ArrayList<>();

    public FlogDeclareLedgerVo() {
    }

    public FlogDeclareLedgerVo(FlogDeclareReceiptVo receipt, List<TFlogDeclareRow> rows) {
        this.receipt = receipt;
        this.rows = rows;
    }

    public FlogDeclareReceiptVo getReceipt() {
        return receipt;
    }

    public void setReceipt(FlogDeclareReceiptVo receipt) {
        this.receipt = receipt;
    }

    public List<TFlogDeclareRow> getRows() {
        return rows;
    }

    public void setRows(List<TFlogDeclareRow> rows) {
        this.rows = rows;
    }
}
