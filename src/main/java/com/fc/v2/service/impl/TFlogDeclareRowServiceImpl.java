package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.exception.biz.FlogBizException;
import com.fc.v2.mapper.auto.TFlogDeclareBatchMapper;
import com.fc.v2.mapper.auto.TFlogDeclareRowMapper;
import com.fc.v2.model.auto.TFlogDeclareBatch;
import com.fc.v2.model.auto.TFlogDeclareRow;
import com.fc.v2.model.custom.flog.FlogDeclareClose;
import com.fc.v2.model.custom.flog.FlogDeclareLedgerVo;
import com.fc.v2.model.custom.flog.FlogDeclareReceiptVo;
import com.fc.v2.model.custom.flog.FlogDeclareRowDto;
import com.fc.v2.model.custom.flog.FlogPreItem;
import com.fc.v2.model.custom.flog.FlogRowLanding;
import com.fc.v2.model.custom.flog.FlogRowReceiptVo;
import com.fc.v2.service.ITFlogDeclareRowService;
import com.fc.v2.util.StringUtils;

/**
 * 伐区申报清册核收行 Service业务层处理。
 *
 * 一册一册地过，每册再一行一行地落：{@link #acceptRow} 是进门到落库唯一的一条道，
 * 行次进门钉死、同册同单位同小班并前行、前置项按代号只挂那一行、实收不许大过申报、
 * 同号重送不覆盖不追第二句缘故、越上限整册退回一行不留、空册只写空册。
 * 回执三数与库里逐行同一次算定，界面另起一套数行不通，也不留手工补涂的口子。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogDeclareRowServiceImpl implements ITFlogDeclareRowService {

    /** 单册行数上限：越线整册退回、一行不留 */
    private static final int MAX_ROWS = 500;

    /** 林种：公益林（现行法只许抚育、更新与低质低效林改造性质采伐） */
    private static final int FOREST_PUBLIC = 1;

    /** 权属涉讼：是 */
    private static final int DISPUTE_YES = 1;

    /** 没附伐区调查设计材料 */
    private static final int SURVEY_NONE = 0;

    /** 省级规定：单行拟采蓄积上限(立方米)。样例取值，按省厅文件调。 */
    private static final BigDecimal PROV_MAX_QTY = new BigDecimal("300");
    /** 省级规定：单行拟采面积上限(公顷)。样例取值，按省厅文件调。 */
    private static final BigDecimal PROV_MAX_AREA = new BigDecimal("20");

    /** 主伐方式：1皆伐 2渐伐 3择伐（公益林不许走这三样） */
    private static final int CUT_CLEAR = 1;
    private static final int CUT_GRADUAL = 2;
    private static final int CUT_SELECTIVE = 3;

    /** 账页：现行页 */
    private static final int LEDGER_CURRENT = 0;
    /** 账页：往期页 */
    private static final int LEDGER_PAST = 1;

    /** 收下的行满几个执行期转往期（执行期按年度，满两年） */
    private static final int ARCHIVE_PERIODS = 2;

    @javax.annotation.Resource
    private TFlogDeclareRowMapper flogDeclareRowMapper;

    @javax.annotation.Resource
    private TFlogDeclareBatchMapper flogDeclareBatchMapper;

    @Override
    public TFlogDeclareRow selectTFlogDeclareRowById(Long id) {
        return this.flogDeclareRowMapper.selectById(id);
    }

    // ------------------------------------------------------------------
    // 旧整册提交口：入参照旧、签名一字不动，内部一行一行走唯一逐行道，没有第二套规矩
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitBatch(String batchNo, List<TFlogDeclareRow> rows) {
        if (rows == null || rows.isEmpty()) {
            // 一册一行也没有：不立批次、不写核讫
            return 0;
        }
        if (rows.size() > MAX_ROWS) {
            // 越线整册退回、一行不留：在批次账上记下同一笔退回处置（已落定的册不动），回执口径与逐行道一致
            TFlogDeclareBatch existed = lockBatch(batchNo);
            if (existed == null) {
                existed = lockOrCreateBatch(batchNo);
            }
            if (existed.getStatus() == null || existed.getStatus() == FlogDeclareClose.RECEIVING.getCode()) {
                this.flogDeclareRowMapper.delete(new QueryWrapper<TFlogDeclareRow>().eq("batch_no", batchNo));
                existed.setTotalRows(0);
                existed.setAcceptedRows(0);
                existed.setHeldRows(0);
                existed.setStatus(FlogDeclareClose.RETURNED.getCode());
                existed.setReceiptRemark("全册 " + rows.size() + " 行越过单册上限 " + MAX_ROWS + " 行，整册退回，一行不留");
                existed.setSettledTime(new Date());
                this.flogDeclareBatchMapper.updateById(existed);
            }
            return -1;
        }
        for (TFlogDeclareRow r : rows) {
            if (r != null) {
                r.setBatchNo(batchNo);
                acceptRow(r);
            }
        }
        FlogDeclareReceiptVo receipt = closeBatch(batchNo);
        return receipt.getAcceptedRows() == null ? 0 : receipt.getAcceptedRows();
    }

    @Override
    public List<TFlogDeclareRow> listErrors(String batchNo) {
        // 旧“失败明细”即今之挂起行：账上有效（非并账行）、按进门行次升序
        return listLedgerRows(batchNo, FlogRowLanding.HELD.getCode());
    }

    // ------------------------------------------------------------------
    // 唯一逐行道
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogRowReceiptVo acceptRow(FlogDeclareRowDto dto) {
        if (dto == null) {
            throw new FlogBizException("送上来的行是空的，没东西可收");
        }
        TFlogDeclareRow row = new TFlogDeclareRow();
        row.setBatchNo(dto.getBatchNo());
        row.setSiteNo(dto.getSiteNo());
        row.setSubcompNo(dto.getSubcompNo());
        row.setForestKind(dto.getForestKind());
        row.setCutMode(dto.getCutMode());
        row.setQty(dto.getQty());
        row.setFineAmt(dto.getFineAmt());
        row.setOwnershipDispute(dto.getOwnershipDispute());
        row.setSurveyAttached(dto.getSurveyAttached());
        row.setReceivedQty(dto.getReceivedQty());
        row.setRemark(dto.getRemark());
        return acceptRow(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogRowReceiptVo acceptRow(TFlogDeclareRow input) {
        if (input == null) {
            throw new FlogBizException("送上来的行是空的，没东西可收");
        }
        String batchNo = input.getBatchNo();
        if (StringUtils.isEmpty(batchNo)) {
            throw new FlogBizException("没给清册批次号，这行进不了门");
        }

        // 锁册：同一册的行在批次行锁上串行，行次、并账、重送才落得清
        TFlogDeclareBatch batch = lockOrCreateBatch(batchNo);
        if (batch.getStatus() != null && batch.getStatus() == FlogDeclareClose.RETURNED.getCode()) {
            throw new FlogBizException("批次号 " + batchNo + " 已越线整册退回，一行不留；须另立批次号重新造册报送");
        }
        if (batch.getStatus() != null && batch.getStatus() == FlogDeclareClose.EMPTY.getCode()) {
            throw new FlogBizException("批次号 " + batchNo + " 已按空册回执在案，空册不接纳行");
        }
        if (batch.getStatus() != null && batch.getStatus() == FlogDeclareClose.SETTLED.getCode()) {
            // 同一批次号再送第二遍：账上仍是头一遍那批行——找到同号行原样给回，不覆盖、不追第二句缘故
            TFlogDeclareRow prior = findLedgerLine(batchNo, input.getSiteNo(), input.getSubcompNo());
            if (prior != null) {
                FlogRowReceiptVo vo = buildRowVo(prior, null,
                        "批次号 " + batchNo + " 头一遍已落定，本行照头一遍那批行给回，不另收");
                vo.setBatchStatus(FlogDeclareClose.SETTLED.getCode());
                return vo;
            }
            throw new FlogBizException("批次号 " + batchNo + " 已核讫在案，重送的新行进不来；蓄积真要改，走差异清单那一路");
        }

        validateShape(input);

        // 行次由进门那一刻的顺序定下：册内已进门几行（含并前行的重复行目），下一序接着排
        int delivered = countRows(batchNo, null);
        int rowNo = delivered + 1;

        // 越线：到此判定整册退回，已进门的行一并清掉，一行不留（同事务提交，不抛异常以免退回处置被回滚）
        if (rowNo > MAX_ROWS) {
            this.flogDeclareRowMapper.delete(new QueryWrapper<TFlogDeclareRow>().eq("batch_no", batchNo));
            String remark = "全册 " + rowNo + " 行越过单册上限 " + MAX_ROWS + " 行，整册退回，一行不留";
            batch.setTotalRows(0);
            batch.setAcceptedRows(0);
            batch.setHeldRows(0);
            batch.setStatus(FlogDeclareClose.RETURNED.getCode());
            batch.setReceiptRemark(remark);
            batch.setSettledTime(new Date());
            this.flogDeclareBatchMapper.updateById(batch);
            FlogRowReceiptVo rejectVo = new FlogRowReceiptVo();
            rejectVo.setBatchNo(batchNo);
            rejectVo.setRowNo(rowNo);
            rejectVo.setLedgerStatus(LEDGER_CURRENT);
            rejectVo.setBatchStatus(FlogDeclareClose.RETURNED.getCode());
            rejectVo.setMessage(remark);
            return rejectVo;
        }

        // 同一家名下同一个小班在一册里占了两个行目：账上并成一行，蓄积只落一次，行次认前头那一个
        TFlogDeclareRow first = findLedgerLine(batchNo, input.getSiteNo(), input.getSubcompNo());
        if (first != null) {
            TFlogDeclareRow dup = newRowCommon(input, rowNo);
            dup.setMergedRowNo(first.getRowNo());
            // 并账行不另判前置项、不第二次落蓄积：落地情形随头一行，挂起也不追第二句缘故
            dup.setStatus(first.getStatus());
            dup.setReceivedQty(null);
            dup.setItemCode(null);
            dup.setHoldReason(null);
            this.flogDeclareRowMapper.insert(dup);
            bumpBatchTotals(batch, 1, first.getStatus() != null
                    && first.getStatus() == FlogRowLanding.HELD.getCode() ? 1 : 0);
            String msg = "本行为同册第 " + first.getRowNo() + " 行（"
                    + input.getSiteNo() + "·" + input.getSubcompNo() + "）的重复行目，并作一行，蓄积只落头一行那一次";
            FlogRowReceiptVo dupVo = buildRowVo(first, dup.getRowNo(), msg);
            // 重复行目账上不另立，主键只认前行那一行
            dupVo.setRowId(null);
            return dupVo;
        }

        // 头一道没过的前置项拦下这一行：只挂这一行，同册余下的行照常入账
        FlogPreItem hit = firstFailedItem(input);
        TFlogDeclareRow row = newRowCommon(input, rowNo);
        String msg;
        if (hit != null) {
            row.setStatus(FlogRowLanding.HELD.getCode());
            row.setItemCode(hit.getCode());
            row.setHoldReason(hit.getReason());
            row.setReceivedQty(null);
            msg = "第 " + rowNo + " 行挂起（" + hit.getCode() + " " + hit.getTitle() + "）：" + hit.getReason()
                    + "；同册其余行照常入账";
        } else {
            BigDecimal received = input.getReceivedQty() == null ? input.getQty() : input.getReceivedQty();
            // 收回来的数不能大过本行申报：多的既不许硬塞本行，也不准挪去应付邻行
            if (received.compareTo(input.getQty()) > 0) {
                throw new FlogBizException("第 " + rowNo + " 行（" + input.getSiteNo() + "·"
                        + input.getSubcompNo() + "）申报 " + input.getQty() + " 立方米、实收 " + received
                        + " 立方米，多出的 " + received.subtract(input.getQty())
                        + " 既不许塞进本行，也不准挪邻行，本笔驳回");
            }
            row.setStatus(FlogRowLanding.RECEIVED.getCode());
            row.setReceivedQty(received);
            msg = "第 " + rowNo + " 行（" + input.getSiteNo() + "·" + input.getSubcompNo()
                    + "）已收，申报 " + input.getQty() + "、实收 " + received + " 立方米";
        }
        this.flogDeclareRowMapper.insert(row);
        bumpBatchTotals(batch, 1, hit == null ? 0 : 1);
        return buildRowVo(row, null, msg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlogDeclareReceiptVo closeBatch(String batchNo) {
        if (StringUtils.isEmpty(batchNo)) {
            throw new FlogBizException("没给清册批次号，落不了册");
        }
        TFlogDeclareBatch batch = lockBatch(batchNo);
        if (batch == null) {
            // 一行也没进门：回执只写空册，落一笔空册批次账在案，绝不写核讫
            batch = new TFlogDeclareBatch();
            batch.setBatchNo(batchNo);
            settleEmpty(batch);
            try {
                this.flogDeclareBatchMapper.insert(batch);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发两笔同时给空册落账：唯一号挡住一个，取先落那笔同一份回执
                return buildReceiptVo(requireLockBatch(batchNo), null);
            }
            return buildReceiptVo(batch, "批次号 " + batchNo + " 一册一行也没有，回执只写空册，不写核讫");
        }
        // 已落定的册再来收口：给回仍是头一遍同一次算出的那份，不重算第二套数
        if (batch.getStatus() != null && batch.getStatus() != FlogDeclareClose.RECEIVING.getCode()) {
            return buildReceiptVo(batch, null);
        }
        int total = countRows(batchNo, null);
        if (total == 0) {
            settleEmpty(batch);
            this.flogDeclareBatchMapper.updateById(batch);
            return buildReceiptVo(batch, "批次号 " + batchNo + " 一册一行也没有，回执只写空册，不写核讫");
        }
        if (total > MAX_ROWS) {
            this.flogDeclareRowMapper.delete(new QueryWrapper<TFlogDeclareRow>().eq("batch_no", batchNo));
            batch.setTotalRows(0);
            batch.setAcceptedRows(0);
            batch.setHeldRows(0);
            batch.setStatus(FlogDeclareClose.RETURNED.getCode());
            batch.setReceiptRemark("全册 " + total + " 行越过单册上限 " + MAX_ROWS + " 行，整册退回，一行不留");
            batch.setSettledTime(new Date());
            this.flogDeclareBatchMapper.updateById(batch);
            return buildReceiptVo(batch, batch.getReceiptRemark());
        }
        // 三数同一次逐行算定：收下+挂起必须抵得上原册行数，并账重复行目随头一行计入同档
        int held = countRows(batchNo, FlogRowLanding.HELD.getCode());
        int accepted = countRows(batchNo, FlogRowLanding.RECEIVED.getCode());
        if (held + accepted != total) {
            // 账对不平宁可挡住，不许端出两把尺子的回执
            throw new FlogBizException("批次号 " + batchNo + " 行数对不平（总 " + total + "、收 " + accepted
                    + "、挂 " + held + "），回执开不出");
        }
        batch.setTotalRows(total);
        batch.setAcceptedRows(accepted);
        batch.setHeldRows(held);
        batch.setStatus(FlogDeclareClose.SETTLED.getCode());
        batch.setReceiptRemark("核讫：本册总计 " + total + " 行，收下 " + accepted + " 行，挂起 " + held + " 行");
        batch.setSettledTime(new Date());
        this.flogDeclareBatchMapper.updateById(batch);
        return buildReceiptVo(batch, batch.getReceiptRemark());
    }

    @Override
    public FlogDeclareReceiptVo receipt(String batchNo) {
        if (StringUtils.isEmpty(batchNo)) {
            throw new FlogBizException("没给清册批次号，翻不出回执");
        }
        TFlogDeclareBatch batch = this.flogDeclareBatchMapper.selectOne(new QueryWrapper<TFlogDeclareBatch>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        if (batch == null) {
            // 连册都没立过，按空册口径给回，照样不许写核讫
            TFlogDeclareBatch empty = new TFlogDeclareBatch();
            empty.setBatchNo(batchNo);
            settleEmpty(empty);
            return buildReceiptVo(empty, "批次号 " + batchNo + " 无册在案，回执只写空册，不写核讫");
        }
        return buildReceiptVo(batch, null);
    }

    @Override
    public FlogDeclareLedgerVo ledgerByBatchNo(String batchNo) {
        if (StringUtils.isEmpty(batchNo)) {
            throw new FlogBizException("没给清册批次号，拉不出账");
        }
        TFlogDeclareBatch batch = this.flogDeclareBatchMapper.selectOne(new QueryWrapper<TFlogDeclareBatch>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .last("limit 1"));
        FlogDeclareReceiptVo head = batch == null ? receipt(batchNo) : buildReceiptVo(batch, null);
        // 凭批次号把账拉出来：账上有效行（并账行目不另列），现行、往期都在此
        List<TFlogDeclareRow> rows = this.flogDeclareRowMapper.selectList(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo)
                .isNull("merged_row_no")
                .eq("del_flag", 0)
                .orderByAsc("row_no"));
        return new FlogDeclareLedgerVo(head, rows);
    }

    @Override
    public List<TFlogDeclareRow> listCurrentRows(String batchNo) {
        // 现行页：只露未转往期的账上有效行（收下行满两个执行期即不在此露）
        return this.flogDeclareRowMapper.selectList(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo)
                .isNull("merged_row_no")
                .eq("ledger_status", LEDGER_CURRENT)
                .eq("del_flag", 0)
                .orderByAsc("row_no"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archiveMatureRows() {
        // 执行期按年度：自进门那年起满两个执行期（如今年-进门年>=2）的收下账行，整笔转往期页
        int yearNow = yearOf(new Date());
        List<TFlogDeclareRow> rows = this.flogDeclareRowMapper.selectList(new QueryWrapper<TFlogDeclareRow>()
                .eq("status", FlogRowLanding.RECEIVED.getCode())
                .eq("ledger_status", LEDGER_CURRENT)
                .eq("del_flag", 0));
        int moved = 0;
        Date now = new Date();
        for (TFlogDeclareRow r : rows) {
            Date t = r.getCreateTime() == null ? now : r.getCreateTime();
            if (yearNow - yearOf(t) >= ARCHIVE_PERIODS) {
                r.setLedgerStatus(LEDGER_PAST);
                r.setArchivedTime(now);
                this.flogDeclareRowMapper.updateById(r);
                moved++;
            }
        }
        return moved;
    }

    // ------------------------------------------------------------------
    // 内中规矩
    // ------------------------------------------------------------------

    /** 进门报文的基本形状：这几格不点名前置项，缺了/不像话就在门口驳回，不进册 */
    private void validateShape(TFlogDeclareRow r) {
        List<String> missing = new ArrayList<>();
        if (StringUtils.isEmpty(r.getSiteNo())) {
            missing.add("申报编制单位底档字号");
        }
        if (StringUtils.isEmpty(r.getSubcompNo())) {
            missing.add("小班号");
        }
        if (r.getForestKind() == null || (r.getForestKind() != 1 && r.getForestKind() != 2)) {
            missing.add("林种类别（1公益林 2商品林）");
        }
        if (r.getCutMode() == null || r.getCutMode() < 1 || r.getCutMode() > 6) {
            missing.add("采伐方式（1皆伐 2渐伐 3择伐 4抚育 5更新 6低改）");
        }
        if (!missing.isEmpty()) {
            throw new FlogBizException("这行进不了门，缺了/不像话的格子：" + String.join("、", missing));
        }
        if (r.getQty() == null || r.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new FlogBizException("第 " + r.getSiteNo() + "·" + r.getSubcompNo()
                    + " 行申报拟采蓄积为零或为负，这行进不了门，须填正数");
        }
        if (r.getFineAmt() != null && r.getFineAmt().compareTo(BigDecimal.ZERO) < 0) {
            throw new FlogBizException("第 " + r.getSiteNo() + "·" + r.getSubcompNo()
                    + " 行申报拟采面积为负，这行进不了门");
        }
        BigDecimal received = r.getReceivedQty();
        if (received != null && received.compareTo(BigDecimal.ZERO) < 0) {
            throw new FlogBizException("第 " + r.getSiteNo() + "·" + r.getSubcompNo()
                    + " 行实收蓄积为负，这行进不了门");
        }
    }

    /**
     * 前置项按固定次序逐道过，头一道没过的即拦下本行（一项一行一句缘故）：
     * P01 公益林走主伐；P02 权属涉讼；P03 超省级面积/蓄积规定却没附调查设计材料。
     */
    private FlogPreItem firstFailedItem(TFlogDeclareRow r) {
        if (r.getForestKind() != null && r.getForestKind() == FOREST_PUBLIC
                && (r.getCutMode() == CUT_CLEAR || r.getCutMode() == CUT_GRADUAL
                        || r.getCutMode() == CUT_SELECTIVE)) {
            return FlogPreItem.P01;
        }
        if (r.getOwnershipDispute() != null && r.getOwnershipDispute() == DISPUTE_YES) {
            return FlogPreItem.P02;
        }
        boolean overQty = r.getQty() != null && r.getQty().compareTo(PROV_MAX_QTY) > 0;
        boolean overArea = r.getFineAmt() != null && r.getFineAmt().compareTo(PROV_MAX_AREA) > 0;
        boolean noSurvey = r.getSurveyAttached() == null || r.getSurveyAttached() == SURVEY_NONE;
        if ((overQty || overArea) && noSurvey) {
            return FlogPreItem.P03;
        }
        return null;
    }

    private TFlogDeclareRow newRowCommon(TFlogDeclareRow input, int rowNo) {
        TFlogDeclareRow r = new TFlogDeclareRow();
        r.setBatchNo(input.getBatchNo());
        r.setRowNo(rowNo);
        r.setSiteNo(input.getSiteNo().trim());
        r.setSubcompNo(input.getSubcompNo().trim());
        r.setForestKind(input.getForestKind());
        r.setCutMode(input.getCutMode());
        r.setQty(input.getQty());
        r.setFineAmt(input.getFineAmt());
        r.setOwnershipDispute(input.getOwnershipDispute());
        r.setSurveyAttached(input.getSurveyAttached());
        r.setLedgerStatus(LEDGER_CURRENT);
        r.setDelFlag(0);
        r.setRemark(input.getRemark());
        return r;
    }

    /** 账上认的有效行（非并账行）：同册同单位同小班认头一行那一对 */
    private TFlogDeclareRow findLedgerLine(String batchNo, String siteNo, String subcompNo) {
        if (StringUtils.isEmpty(siteNo) || StringUtils.isEmpty(subcompNo)) {
            return null;
        }
        return this.flogDeclareRowMapper.selectOne(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo)
                .eq("site_no", siteNo.trim())
                .eq("subcomp_no", subcompNo.trim())
                .isNull("merged_row_no")
                .eq("del_flag", 0)
                .last("limit 1"));
    }

    private int countRows(String batchNo, Integer status) {
        QueryWrapper<TFlogDeclareRow> w = new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0);
        if (status != null) {
            w.eq("status", status);
        }
        Integer c = this.flogDeclareRowMapper.selectCount(w);
        return c == null ? 0 : c;
    }

    /** 册上三数随每进门一笔在锁内累加（与收口时逐行复算同一口径） */
    private void bumpBatchTotals(TFlogDeclareBatch batch, int totalDelta, int heldDelta) {
        batch.setTotalRows(value(batch.getTotalRows()) + totalDelta);
        batch.setHeldRows(value(batch.getHeldRows()) + heldDelta);
        batch.setAcceptedRows(value(batch.getAcceptedRows()) + (totalDelta - heldDelta));
        this.flogDeclareBatchMapper.updateById(batch);
    }

    private TFlogDeclareBatch lockOrCreateBatch(String batchNo) {
        TFlogDeclareBatch batch = lockBatch(batchNo);
        if (batch != null) {
            return batch;
        }
        batch = new TFlogDeclareBatch();
        batch.setBatchNo(batchNo);
        batch.setTotalRows(0);
        batch.setAcceptedRows(0);
        batch.setHeldRows(0);
        batch.setStatus(FlogDeclareClose.RECEIVING.getCode());
        batch.setDelFlag(0);
        try {
            this.flogDeclareBatchMapper.insert(batch);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发两笔同册首行：唯一号挡住一个，输给先建那笔后重取加锁
            batch = requireLockBatch(batchNo);
        }
        return batch;
    }

    private TFlogDeclareBatch lockBatch(String batchNo) {
        return this.flogDeclareBatchMapper.selectOne(new QueryWrapper<TFlogDeclareBatch>()
                .eq("batch_no", batchNo)
                .eq("del_flag", 0)
                .last("limit 1 for update"));
    }

    private TFlogDeclareBatch requireLockBatch(String batchNo) {
        TFlogDeclareBatch b = lockBatch(batchNo);
        if (b == null) {
            throw new FlogBizException("批次号 " + batchNo + " 批次账抢建失败，请重试这一行");
        }
        return b;
    }

    private List<TFlogDeclareRow> listLedgerRows(String batchNo, int status) {
        return this.flogDeclareRowMapper.selectList(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo)
                .eq("status", status)
                .isNull("merged_row_no")
                .eq("del_flag", 0)
                .orderByAsc("row_no"));
    }

    private void settleEmpty(TFlogDeclareBatch batch) {
        batch.setTotalRows(0);
        batch.setAcceptedRows(0);
        batch.setHeldRows(0);
        batch.setStatus(FlogDeclareClose.EMPTY.getCode());
        batch.setReceiptRemark("空册：一册一行也没有，不写核讫");
        batch.setSettledTime(new Date());
        batch.setDelFlag(0);
    }

    private FlogRowReceiptVo buildRowVo(TFlogDeclareRow row, Integer echoRowNo, String msg) {
        FlogRowReceiptVo vo = new FlogRowReceiptVo();
        vo.setRowId(row.getId());
        vo.setBatchNo(row.getBatchNo());
        vo.setRowNo(echoRowNo == null ? row.getRowNo() : echoRowNo);
        vo.setMergedIntoRowNo(echoRowNo == null ? row.getMergedRowNo() : row.getRowNo());
        vo.setSiteNo(row.getSiteNo());
        vo.setSubcompNo(row.getSubcompNo());
        vo.setStatus(row.getStatus());
        vo.setItemCode(row.getItemCode());
        vo.setHoldReason(row.getHoldReason());
        vo.setDeclaredQty(row.getQty());
        vo.setReceivedQty(row.getReceivedQty());
        vo.setLedgerStatus(row.getLedgerStatus());
        vo.setBatchStatus(FlogDeclareClose.RECEIVING.getCode());
        vo.setMessage(msg);
        return vo;
    }

    /**
     * 回执三数只取自批次账——而批次账三数由逐行进门累加、收口时再同一次逐行复算钉死，
     * 两处出口（收口与查回执）端的是同一份数。
     */
    private FlogDeclareReceiptVo buildReceiptVo(TFlogDeclareBatch batch, String msg) {
        FlogDeclareReceiptVo vo = new FlogDeclareReceiptVo();
        vo.setBatchNo(batch.getBatchNo());
        vo.setStatus(batch.getStatus());
        FlogDeclareClose close = FlogDeclareClose.RECEIVING;
        if (batch.getStatus() != null) {
            for (FlogDeclareClose c : FlogDeclareClose.values()) {
                if (c.getCode() == batch.getStatus()) {
                    close = c;
                    break;
                }
            }
        }
        vo.setStatusName(close.getName());
        vo.setTotalRows(value(batch.getTotalRows()));
        vo.setAcceptedRows(value(batch.getAcceptedRows()));
        vo.setHeldRows(value(batch.getHeldRows()));
        vo.setReceiptRemark(batch.getReceiptRemark());
        vo.setSettledTime(batch.getSettledTime());
        vo.setMessage(msg == null ? batch.getReceiptRemark() : msg);
        return vo;
    }

    private int value(Integer v) {
        return v == null ? 0 : v;
    }

    private int yearOf(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }
}
