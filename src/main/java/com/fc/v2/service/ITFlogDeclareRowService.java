package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TFlogDeclareRow;
import com.fc.v2.model.custom.flog.FlogDeclareLedgerVo;
import com.fc.v2.model.custom.flog.FlogDeclareReceiptVo;
import com.fc.v2.model.custom.flog.FlogDeclareRowDto;
import com.fc.v2.model.custom.flog.FlogRowReceiptVo;

/**
 * 伐区申报清册核收行 Service接口。
 *
 * 业务铁律：一本册子从进门到落库只有一条道——就是下面那条逐行处理的方法。
 * 一行递进去，算收下、算挂起还是算挪去往期，全由它的给回定夺；
 * 屏上摆的数跟着给回走，手工补行、涂行一概不留口子。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogDeclareRowService {

    /** 按主键回查单行 */
    TFlogDeclareRow selectTFlogDeclareRowById(Long id);

    /**
     * 整册提交：逐行校验，合法的入库、非法的记入失败明细（保留原始行号），返回**成功行数**。
     * 同一批次重复提交不得重复入库；行数超过单批上限返回 -1 且整批不入库；空批次返回 0。
     */
    int submitBatch(String batchNo, List<TFlogDeclareRow> rows);

    /** 该批次的失败行明细（按行号升序） */
    List<TFlogDeclareRow> listErrors(String batchNo);

    /**
     * 唯一逐行道（另一种递法：进门报文）。
     * 行次进门那一刻钉死；同册同单位同小班重复行目并前行、蓄积落一次；
     * 前置项没过按代号挂那一行并给一句话缘故；实收不许大过申报；同号重送不覆盖、不追第二句缘故。
     */
    FlogRowReceiptVo acceptRow(FlogDeclareRowDto dto);

    /** 唯一逐行道（原实体递法照样能进）：与上面同一条道，内部没有第二套规矩 */
    FlogRowReceiptVo acceptRow(TFlogDeclareRow row);

    /** 一册过完落定：三数同一次算定回执；一行也没有只写“空册”，绝不写“核讫” */
    FlogDeclareReceiptVo closeBatch(String batchNo);

    /** 回执三数（本册总计/收下/挂起）同库里逐行同一次算出，界面自点的不算 */
    FlogDeclareReceiptVo receipt(String batchNo);

    /** 凭批次号把账拉出来：回执抬头 + 全部账上有效行（含已转往期的） */
    FlogDeclareLedgerVo ledgerByBatchNo(String batchNo);

    /** 现行页：账上有效且未转往期的行，按进门行次升序（往期行不在此露） */
    List<TFlogDeclareRow> listCurrentRows(String batchNo);

    /**
     * 往期归档：收下的行自进门满两个执行期（按年度两期）整批转往期页，现行页不再露。
     * 返回本笔转走的行数。
     */
    int archiveMatureRows();
}
