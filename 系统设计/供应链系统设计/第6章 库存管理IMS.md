# 第 6 章 库存管理 IMS（Inventory Management System）

> 本章定位：库存的"账本系统"——管多级库存、管批次、管在途、管安全库存。**WMS 管"实体"，IMS 管"账本"**。
>
> **学习建议**：库存是供应链的"血液"，本章的核心是**可用量计算**、**批次管理**、**多级库存协同**三大主题。

---

## 6.1 IMS 在供应链中的位置

IMS 是连接 WMS、OMS、采购、财务的"数据中心"，负责库存的**账面一致性**。

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize:'14px'}}}%%
flowchart LR
    subgraph 实物流
        WMS1[WMS 入库]
        WMS2[WMS 出库]
        WMS3[WMS 调拨]
        WMS4[WMS 盘点]
    end

    subgraph 数据流
        OMS[OMS 预占]
        PUR[Procurement 在途]
        SCY[SCM 调拨在途]
        SCP[SCP 计划]
    end

    IMS[IMS 库存数据中心]

    WMS1 -->|入库流水| IMS
    WMS2 -->|出库流水| IMS
    WMS3 -->|调拨流水| IMS
    WMS4 -->|盘点差异| IMS
    OMS -->|预占/扣减| IMS
    PUR -->|在途预报| IMS
    SCY -->|在途库存| IMS
    SCP -->|查询/分析| IMS

    IMS -->|可用量| OMS
    IMS -->|预测建议| SCP
    IMS -->|库存账| FIN

    style IMS fill:#a855f7,color:#fff
```

### 6.1.1 IMS 与 WMS 的边界

| 维度 | WMS | IMS |
|------|-----|-----|
| **视角** | 物理实体 | 账面账本 |
| **粒度** | 库位、批次 | SKU、批次、汇总 |
| **操作** | 收货、上架、拣货 | 调拨、锁定、预占 |
| **准确性** | 实时（秒级） | 准实时（分钟级） |
| **系统位置** | 现场执行 | 数据中心 |

### 6.1.2 库存类型总览

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
mindmap
  root((库存<br/>类型))
    按位置
      在库库存
      在途库存
      在制库存
      委外库存
      寄售库存
    按状态
      可用
      锁定
      冻结
      质检
      待处理
    按所有权
      自有
      寄售
      客户寄存
      供应商寄存
    按用途
      安全库存
      战略库存
      缓冲库存
      呆滞库存
```

---

## 6.2 库存数据模型

### 6.2.1 库存核心表

```sql
-- ============ 库存汇总表（按 SKU+仓 聚合）============
CREATE TABLE t_inventory_summary (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_code        VARCHAR(64) NOT NULL,
    warehouse_code  VARCHAR(32) NOT NULL,
    owner_code      VARCHAR(32) DEFAULT 'OWN' COMMENT '货主',
    batch_strategy  VARCHAR(16) DEFAULT 'FIFO' COMMENT '批次策略',
    
    on_hand_qty     INT DEFAULT 0 COMMENT '在库数量',
    available_qty   INT DEFAULT 0 COMMENT '可用数量',
    locked_qty      INT DEFAULT 0 COMMENT '锁定数量',
    on_hold_qty     INT DEFAULT 0 COMMENT '冻结数量',
    in_transit_qty  INT DEFAULT 0 COMMENT '在途数量',
    inspection_qty  INT DEFAULT 0 COMMENT '质检数量',
    
    avg_cost        DECIMAL(18,4) COMMENT '加权平均成本',
    total_value     DECIMAL(18,2) COMMENT '库存总价值',
    
    last_in_date    DATE COMMENT '最后入库日',
    last_out_date   DATE COMMENT '最后出库日',
    last_count_date DATE COMMENT '最后盘点日',
    
    version         BIGINT DEFAULT 0 COMMENT '乐观锁',
    updated_at      DATETIME ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_inv (sku_code, warehouse_code, owner_code)
) ENGINE=InnoDB COMMENT='库存汇总表';

-- ============ 库存明细表（按 SKU+仓+库位+批次）============
CREATE TABLE t_inventory_detail (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_code        VARCHAR(64) NOT NULL,
    warehouse_code  VARCHAR(32) NOT NULL,
    location_code   VARCHAR(64) NOT NULL,
    batch_no        VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
    serial_no       VARCHAR(64) COMMENT '序列号',
    owner_code      VARCHAR(32) DEFAULT 'OWN',
    
    quantity        INT DEFAULT 0 COMMENT '数量',
    available_qty   INT DEFAULT 0 COMMENT '可用',
    locked_qty      INT DEFAULT 0 COMMENT '锁定',
    
    production_date DATE,
    expire_date     DATE,
    inbound_date    DATE,
    shelf_life_days INT,
    
    quality_status  VARCHAR(16) DEFAULT 'QUA' COMMENT 'QUA/PRE/UNQ/HOLD',
    inbound_order   VARCHAR(32) COMMENT '入库单',
    
    UNIQUE KEY uk_detail (sku_code, warehouse_code, location_code, batch_no, serial_no),
    INDEX idx_batch (batch_no),
    INDEX idx_expire (expire_date)
) ENGINE=InnoDB COMMENT='库存明细表';

-- ============ 在途库存表 ============
CREATE TABLE t_inventory_in_transit (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_code        VARCHAR(64) NOT NULL,
    from_warehouse  VARCHAR(32) COMMENT '调出仓',
    to_warehouse    VARCHAR(32) COMMENT '调入仓',
    supplier_code   VARCHAR(32) COMMENT '供应商',
    po_no           VARCHAR(32) COMMENT '采购单',
    transfer_no     VARCHAR(32) COMMENT '调拨单',
    
    quantity        INT NOT NULL,
    shipped_qty     INT DEFAULT 0,
    received_qty    INT DEFAULT 0,
    
    expected_date   DATE COMMENT '预计到达',
    shipped_date    DATE,
    received_date   DATE,
    
    status          VARCHAR(16) COMMENT 'PENDING/SHIPPED/PARTIAL/RECEIVED',
    
    INDEX idx_sku (sku_code),
    INDEX idx_destination (to_warehouse, status)
) ENGINE=InnoDB COMMENT='在途库存表';
```

### 6.2.2 库存状态机

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
stateDiagram-v2
    [*] --> IN_TRANSIT: ASN推送
    IN_TRANSIT --> INSPECTION: 收货
    INSPECTION --> AVAILABLE: 质检通过
    INSPECTION --> ON_HOLD: 质检不通过
    INSPECTION --> QUARANTINE: 待复检

    AVAILABLE --> LOCKED: 订单锁定
    LOCKED --> PICKING: 拣货中
    PICKING --> SHIPPED: 发货出库
    PICKING --> AVAILABLE: 拣货取消

    AVAILABLE --> ON_HOLD: 主动冻结
    ON_HOLD --> AVAILABLE: 解冻
    AVAILABLE --> TRANSFERRING: 调拨中
    TRANSFERRING --> IN_TRANSIT: 调拨出库
    IN_TRANSIT --> AVAILABLE: 调拨入库

    SHIPPED --> [*]
    ON_HOLD --> SCRAPPED: 报废
    QUARANTINE --> SCRAPPED: 报废
    SCRAPPED --> [*]
```

---

## 6.3 可用量计算（ATP）

### 6.3.1 可用量公式

**ATP（Available to Promise）** 是库存最核心的概念：

```
ATP = 物理在库 - 已锁定 - 安全库存
    = on_hand - locked - safety_stock
```

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart TB
    A[总账面库存<br/>on_hand = 1000] --> B[扣减锁定<br/>locked = 200]
    B --> C[扣减冻结<br/>on_hold = 50]
    C --> D[扣减质检<br/>inspection = 30]
    D --> E[扣减安全库存<br/>safety = 100]
    E --> F[ATP 可用量<br/>= 1000 - 200 - 50 - 30 - 100<br/>= 620]

    style A fill:#3b82f6,color:#fff
    style F fill:#10b981,color:#fff
```

### 6.3.2 多维 ATP

```java
/**
 * 多维 ATP 计算
 */
@Service
public class AtpService {

    /**
     * 全局 ATP（按 SKU 汇总所有仓）
     */
    public AtpResult globalAtp(String skuCode) {
        List<InventorySummary> summaries = summaryRepository.findBySku(skuCode);
        AtpResult result = new AtpResult();

        int totalOnHand = 0;
        int totalLocked = 0;
        int totalOnHold = 0;
        int totalInspection = 0;
        int totalSafety = 0;
        int totalInTransit = 0;

        for (InventorySummary s : summaries) {
            totalOnHand += s.getOnHandQty();
            totalLocked += s.getLockedQty();
            totalOnHold += s.getOnHoldQty();
            totalInspection += s.getInspectionQty();
            totalSafety += getSafetyStock(skuCode, s.getWarehouseCode());
            totalInTransit += s.getInTransitQty();
        }

        result.setOnHand(totalOnHand);
        result.setLocked(totalLocked);
        result.setOnHold(totalOnHold);
        result.setInspection(totalInspection);
        result.setSafetyStock(totalSafety);
        result.setInTransit(totalInTransit);
        result.setAtp(totalOnHand - totalLocked - totalOnHold
            - totalInspection - totalSafety);
        result.setEAtp(result.getAtp() + totalInTransit);  // 扩展可用量

        return result;
    }

    /**
     * 单仓 ATP
     */
    public int warehouseAtp(String skuCode, String warehouseCode) {
        InventorySummary s = summaryRepository
            .findBySkuAndWarehouse(skuCode, warehouseCode);
        if (s == null) return 0;

        int safety = getSafetyStock(skuCode, warehouseCode);
        return s.getOnHandQty() - s.getLockedQty()
            - s.getOnHoldQty() - s.getInspectionQty() - safety;
    }

    /**
     * CTP（Capable to Promise，考虑生产/采购）
     */
    public AtpResult ctp(String skuCode, Date requiredDate) {
        AtpResult result = globalAtp(skuCode);

        // 在 requiredDate 之前能到货的补货
        List<InboundPlan> inbounds = inboundService
            .findArrivingBy(requiredDate, skuCode);
        int inboundQty = inbounds.stream()
            .mapToInt(InboundPlan::getQuantity).sum();

        // 减去 requiredDate 之前的需求
        List<OutboundPlan> outbounds = outboundService
            .findRequiredBy(requiredDate, skuCode);
        int outboundQty = outbounds.stream()
            .mapToInt(OutboundPlan::getQuantity).sum();

        result.setAtp(result.getAtp() + inboundQty - outboundQty);
        return result;
    }
}
```

### 6.3.3 库存共享（防超卖）

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
sequenceDiagram
    autonumber
    participant EC as 电商前台
    participant OMS as OMS
    participant IMS as IMS
    participant R as Redis
    participant DB as MySQL

    EC->>OMS: 1. 下单
    OMS->>IMS: 2. 查询可用量
    IMS->>R: 3. GET ATP
    R-->>IMS: 4. ATP = 620
    IMS-->>OMS: 5. 可用
    OMS->>R: 6. Lua 预减 1
    R-->>OMS: 7. 预减成功 619
    OMS->>EC: 8. 下单成功
    OMS->>DB: 9. 持久化订单
    DB-->>OMS: 10. 订单已建
    OMS->>IMS: 11. 确认预占
    IMS->>DB: 12. UPDATE 锁定 +1
```

### 6.3.4 预占的原子性

```java
/**
 * Redis Lua 原子预占
 */
public class RedisInventoryDeductor {

    private static final String DEDUCT_SCRIPT =
        "local available = tonumber(redis.call('GET', KEYS[1]))\n" +
        "if available == nil then return -1 end\n" +
        "if available < tonumber(ARGV[1]) then return 0 end\n" +
        "redis.call('DECRBY', KEYS[1], ARGV[1])\n" +
        "redis.call('INCRBY', KEYS[2], ARGV[1])\n" +
        "return 1\n";

    /**
     * 预占（Lua 保证原子性）
     */
    public boolean tryDeduct(String sku, String warehouse, int quantity) {
        String availableKey = "inv:atp:" + sku + ":" + warehouse;
        String lockedKey = "inv:locked:" + sku + ":" + warehouse;

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(DEDUCT_SCRIPT, Long.class),
            List.of(availableKey, lockedKey),
            String.valueOf(quantity)
        );
        return result != null && result == 1;
    }

    /**
     * 释放预占
     */
    public void release(String sku, String warehouse, int quantity) {
        String availableKey = "inv:atp:" + sku + ":" + warehouse;
        String lockedKey = "inv:locked:" + sku + ":" + warehouse;

        String releaseScript =
            "redis.call('INCRBY', KEYS[1], ARGV[1])\n" +
            "redis.call('DECRBY', KEYS[2], ARGV[1])\n";
        redisTemplate.execute(
            new DefaultRedisScript<>(releaseScript),
            List.of(availableKey, lockedKey),
            String.valueOf(quantity)
        );
    }
}
```

---

## 6.4 批次与序列号

### 6.4.1 批次管理模型

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize:'14px'}}}%%
erDiagram
    BATCH ||--o{ INVENTORY : "包含"
    BATCH ||--|| SUPPLIER : "来源"
    BATCH ||--o{ PURCHASE_ORDER : "入库"
    BATCH ||--o{ SALES_ORDER : "出库"

    BATCH {
        string batch_no PK
        string sku_code
        string supplier_code
        date production_date
        date expire_date
        int shelf_life_days
        string cert_no "质检证书"
        string status "QUA/UNQ/HOLD"
    }
    INVENTORY {
        string batch_no FK
        string location_code
        int quantity
        date inbound_date
    }
```

### 6.4.2 批次策略

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
mindmap
  root((批次<br/>策略))
    FIFO
      先进先出
      按入库日期
      通用
    FEFO
      先到期先出
      按过期日期
      食药妆
    LIFO
      后进先出
      按入库日期
      压货
    FFA
      先分配先出
      批次分配
      服装
```

```java
/**
 * 批次拣货建议
 */
@Service
public class BatchPickService {

    /**
     * FEFO 拣货建议
     */
    public List<PickBatch> suggestPickFeFo(String sku, int quantity) {
        // 1. 查询可用批次
        List<InventoryDetail> batches = inventoryDetailRepository
            .findBySkuAndStatus(sku, AVAILABLE);

        // 2. 按过期日期升序
        batches.sort(Comparator.comparing(
            InventoryDetail::getExpireDate,
            Comparator.nullsLast(Comparator.naturalOrder())
        ));

        // 3. 累计达到目标数量
        List<PickBatch> suggestions = new ArrayList<>();
        int remaining = quantity;
        for (InventoryDetail batch : batches) {
            if (remaining <= 0) break;
            int pickQty = Math.min(batch.getAvailableQty(), remaining);

            // 临期预警
            if (isNearExpire(batch)) {
                pickQty = batch.getAvailableQty();  // 临期批次全出
            }

            PickBatch suggestion = new PickBatch();
            suggestion.setBatchNo(batch.getBatchNo());
            suggestion.setLocationCode(batch.getLocationCode());
            suggestion.setPickQty(pickQty);
            suggestion.setExpireDate(batch.getExpireDate());
            suggestions.add(suggestion);

            remaining -= pickQty;
        }

        return suggestions;
    }

    /**
     * 临期判断（保质期 < 30%）
     */
    private boolean isNearExpire(InventoryDetail batch) {
        LocalDate today = LocalDate.now();
        LocalDate expire = batch.getExpireDate().toLocalDate();
        long totalDays = ChronoUnit.DAYS.between(batch.getProductionDate().toLocalDate(), expire);
        long remainDays = ChronoUnit.DAYS.between(today, expire);
        return (double) remainDays / totalDays < 0.3;
    }
}
```

### 6.4.3 序列号管理

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart TB
    A[生产序列号] --> B[入库绑定]
    B --> C[在库]
    C --> D[出库绑定订单]
    D --> E[激活/保修]
    E --> F{使用状态}
    F -->|正常| G[继续使用]
    F -->|维修| H[送修]
    F -->|报废| I[报废]

    H --> J[修复]
    J --> C

    style A fill:#22d3ee,color:#000
    style C fill:#10b981,color:#fff
    style I fill:#ef4444,color:#fff
```

### 6.4.4 双向追溯

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart LR
    A[消费者<br/>投诉产品] --> B[销售订单]
    B --> C[出库单]
    C --> D[批次号]
    D --> E[入库单]
    E --> F[采购单]
    F --> G[供应商]
    G --> H[原料]

    A -.查询路径.-> B
    B -.查询路径.-> C
    C -.查询路径.-> D
    D -.查询路径.-> E
    E -.查询路径.-> F
    F -.查询路径.-> G

    style A fill:#ef4444,color:#fff
    style H fill:#22d3ee,color:#000
```

```java
/**
 * 双向追溯服务
 */
@Service
public class TraceabilityService {

    /**
     * 正向追溯（从原料到消费者）
     */
    public TraceNode forwardTrace(String batchNo) {
        TraceNode root = new TraceNode();
        root.setBatchNo(batchNo);

        // 上游：原料
        List<RawMaterial> materials = materialService.findByBatch(batchNo);
        root.setMaterials(materials);

        // 当前：库存
        List<InventoryDetail> inventories = inventoryRepo.findByBatchNo(batchNo);
        root.setInventories(inventories);

        // 下游：订单/消费者
        List<OrderItem> orders = orderService.findByBatchNo(batchNo);
        root.setOrders(orders);

        return root;
    }

    /**
     * 反向追溯（从消费者到原料）
     */
    public List<TraceNode> backwardTrace(String orderId) {
        // 1. 订单 → 出库单
        OutboundOrder outbound = outboundService.findByOrder(orderId);
        // 2. 出库单 → 批次
        List<String> batches = outbound.getItems().stream()
            .map(Item::getBatchNo)
            .distinct()
            .collect(toList());
        // 3. 批次 → 采购单
        // 4. 采购单 → 供应商
        // 5. 供应商 → 原料
        return buildTraceChain(batches);
    }
}
```

---

## 6.5 多级库存

### 6.5.1 多级库存模型

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart TB
    subgraph DC[中央仓 / DC]
        DC1[中央库存]
    end
    subgraph RDC[区域仓 / RDC]
        RDC1[区域1库存]
        RDC2[区域2库存]
    end
    subgraph FC[前置仓 / FC]
        FC1[前置仓1]
        FC2[前置仓2]
    end

    DC1 --> RDC1
    DC1 --> RDC2
    RDC1 --> FC1
    RDC2 --> FC2

    style DC1 fill:#3b82f6,color:#fff
    style RDC1 fill:#6366f1,color:#fff
    style FC1 fill:#a855f7,color:#fff
```

### 6.5.2 多级调拨决策

```java
/**
 * 多级调拨决策
 */
@Service
public class MultiEchelonTransferService {

    /**
     * 调拨决策
     * 触发条件：
     * 1. 某前置仓缺货
     * 2. 区域仓有货
     * 3. 调拨成本 < 紧急采购成本
     */
    public TransferPlan decide(String sku, String fcWarehouse) {
        InventorySummary fcInv = summaryRepo.findBySkuAndWarehouse(sku, fcWarehouse);
        InventorySummary rdcInv = summaryRepo.findByRdcForFc(sku, fcWarehouse);
        InventorySummary dcInv = summaryRepo.findByDcForRdc(sku, fcWarehouse);

        TransferPlan plan = new TransferPlan();

        // 场景 1：前置仓缺货 → 从 RDC 调
        if (fcInv.getAtp() < SAFETY_STOCK) {
            if (rdcInv.getAtp() > SAFETY_STOCK * 2) {
                int transferQty = Math.min(rdcInv.getAtp() - SAFETY_STOCK, REPLENISH_QTY);
                plan.setFromWarehouse(rdcInv.getWarehouseCode());
                plan.setToWarehouse(fcWarehouse);
                plan.setQuantity(transferQty);
                plan.setReason("RDC2FC");
                return plan;
            }

            // 场景 2：RDC 也缺 → 从 DC 调
            if (dcInv.getAtp() > SAFETY_STOCK * 2) {
                int transferQty = Math.min(dcInv.getAtp() - SAFETY_STOCK * 2, REPLENISH_QTY);
                plan.setFromWarehouse(dcInv.getWarehouseCode());
                plan.setToWarehouse(rdcInv.getWarehouseCode());
                plan.setQuantity(transferQty);
                plan.setReason("DC2RDC");
                return plan;
            }

            // 场景 3：DC 也缺 → 紧急采购
            plan.setReason("URGENT_PO");
            return plan;
        }

        return null;  // 不需要调拨
    }
}
```

### 6.5.3 调拨 vs 补货

| 维度 | 调拨 | 补货 |
|------|------|------|
| **来源** | 仓间 | 供应商/工厂 |
| **速度** | 快（小时-天） | 慢（天-周） |
| **成本** | 内部转运费 | 采购价 + 运费 |
| **数量** | 小批量 | 中大批量 |
| **触发** | 缺货响应 | 周期性 |

---

## 6.6 安全库存

### 6.6.1 安全库存公式

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
mindmap
  root((安全库存<br/>模型))
    基础
      SS = Z × σ × √L
      Z 服务水平
      σ 需求波动
      L 补货周期
    改进
      考虑R
      考虑R平方
      考虑相关需求
    动态
      滚动计算
      机器学习
      分段服务
```

### 6.6.2 标准安全库存公式

```
SS = Z × σ_d × √L
```

其中：
- **SS**：安全库存
- **Z**：服务水平对应的 Z 值（95% → 1.65, 99% → 2.33）
- **σ_d**：日需求标准差
- **L**：补货前置期

```java
/**
 * 安全库存计算
 */
@Service
public class SafetyStockService {

    /**
     * 基础安全库存
     */
    public int calculate(String sku, String warehouse, double serviceLevel) {
        // 历史需求统计（过去 90 天）
        DemandStats stats = demandRepo.getStats(sku, warehouse, 90);
        double avgDailyDemand = stats.getAvgDailyDemand();
        double stdDevDaily = stats.getStdDevDaily();

        // 当前补货前置期
        int leadTimeDays = supplierService.getLeadTime(sku);

        // Z 值
        double z = normalDistribution.inverseCumulativeProbability(serviceLevel);

        // SS = Z × σ × √L
        double ss = z * stdDevDaily * Math.sqrt(leadTimeDays);

        return (int) Math.ceil(ss);
    }

    /**
     * 考虑前置期波动的安全库存
     * SS = Z × √(L × σ_d² + D² × σ_L²)
     */
    public int calculateWithLeadTimeVariance(String sku, String warehouse,
                                             double serviceLevel) {
        DemandStats stats = demandRepo.getStats(sku, warehouse, 90);
        double avgLeadTime = stats.getAvgLeadTime();
        double stdDevLeadTime = stats.getStdDevLeadTime();
        double avgDailyDemand = stats.getAvgDailyDemand();
        double stdDevDaily = stats.getStdDevDaily();

        double z = normalDistribution.inverseCumulativeProbability(serviceLevel);

        // SS = Z × √(L × σ_d² + D² × σ_L²)
        double variance = avgLeadTime * Math.pow(stdDevDaily, 2)
            + Math.pow(avgDailyDemand, 2) * Math.pow(stdDevLeadTime, 2);
        double ss = z * Math.sqrt(variance);

        return (int) Math.ceil(ss);
    }
}
```

### 6.6.3 动态安全库存

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart LR
    A[历史需求] --> B[特征工程]
    B --> C[机器学习模型<br/>LSTM/Prophet]
    C --> D[预测需求]
    D --> E[动态安全库存]
    E --> F[动态再订货点]

    H[外部因子<br/>促销/天气/事件] --> C

    style C fill:#a855f7,color:#fff
```

---

## 6.7 库存优化

### 6.7.1 ABC 分类

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart LR
    A[全部 SKU] --> B[按年消耗金额排序]
    B --> C[A 类<br/>前 20% 占 80% 金额]
    B --> D[B 类<br/>中 30% 占 15%]
    B --> E[C 类<br/>后 50% 占 5%]

    C --> F[高服务 99%<br/>小批量高频]
    D --> G[中服务 95%<br/>中批量]
    E --> H[低服务 90%<br/>大批量低频]

    style C fill:#ef4444,color:#fff
    style D fill:#fbbf24,color:#000
    style E fill:#10b981,color:#fff
```

```java
/**
 * ABC 分类
 */
@Service
public class AbcClassificationService {

    public List<AbcClass> classify(String warehouse) {
        // 1. 计算每个 SKU 的年消耗金额
        List<SkuValue> skuValues = inventoryRepo.findAllSkus(warehouse).stream()
            .map(sku -> {
                DemandStats stats = demandRepo.getAnnualStats(sku.getCode());
                double annualValue = stats.getAnnualQty() * sku.getUnitPrice();
                return new SkuValue(sku.getCode(), annualValue);
            })
            .sorted(Comparator.comparing(SkuValue::getValue).reversed())
            .collect(toList());

        // 2. 累计占比
        double totalValue = skuValues.stream()
            .mapToDouble(SkuValue::getValue).sum();
        double cumulative = 0;
        List<AbcClass> classes = new ArrayList<>();

        for (SkuValue sv : skuValues) {
            cumulative += sv.getValue();
            double percent = cumulative / totalValue;

            String classification;
            if (percent <= 0.80) classification = "A";
            else if (percent <= 0.95) classification = "B";
            else classification = "C";

            AbcClass cls = new AbcClass();
            cls.setSkuCode(sv.getSkuCode());
            cls.setClassification(classification);
            classes.add(cls);
        }

        return classes;
    }
}
```

### 6.7.2 呆滞库存管理

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart TB
    A[库龄分析] --> B[呆滞识别]
    B --> C[分类处理]
    C -->|90天无动销| D[预警]
    C -->|180天无动销| E[限制补货]
    C -->|365天无动销| F[促销清理]
    C -->|720天无动销| G[报废/捐赠]

    D --> H[继续观察]
    E --> I[运营介入]
    F --> J[营销活动]
    G --> K[财务核销]

    style F fill:#fbbf24,color:#000
    style G fill:#ef4444,color:#fff
```

### 6.7.3 库存周转率

```java
/**
 * 库存健康度分析
 */
@Service
public class InventoryHealthService {

    public InventoryHealth analyze(String warehouse) {
        InventoryHealth health = new InventoryHealth();

        // 1. 库存周转率
        double annualCogs = outboundRepo.getAnnualCogs(warehouse);
        double avgInventory = inventoryRepo.getAvgInventory(warehouse);
        double turnover = annualCogs / avgInventory;
        health.setTurnover(turnover);

        // 2. 售罄率
        double salesRatio = salesRepo.getAnnualSalesQty(warehouse)
            / inventoryRepo.getAvgInventoryQty(warehouse);
        health.setSalesRatio(salesRatio);

        // 3. 缺货率
        double stockoutCount = orderRepo.getStockoutCount(warehouse);
        double totalOrders = orderRepo.getTotalOrders(warehouse);
        health.setStockoutRate(stockoutCount / totalOrders);

        // 4. 呆滞率
        double slowMovingValue = inventoryRepo.getSlowMovingValue(warehouse, 90);
        health.setSlowMovingRate(slowMovingValue / avgInventory);

        // 5. 健康度评分
        double score = 0;
        if (turnover >= 8) score += 30;  // 行业基准 8 次
        if (stockoutRate < 0.05) score += 30;  // 缺货率 < 5%
        if (slowMovingRate < 0.10) score += 20;  // 呆滞率 < 10%
        if (salesRatio >= 6) score += 20;  // 售罄率
        health.setHealthScore(score);

        return health;
    }
}
```

---

## 6.8 在途库存

### 6.8.1 在途库存分类

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
mindmap
  root((在途<br/>库存))
    采购在途
      供应商 → 仓
      ASN 已推送
      未到货
    调拨在途
      仓 → 仓
      已发未到
    销退在途
      客户 → 仓
      退货在途
    移库在途
      库位 → 库位
      仓内转移
```

### 6.8.2 在途库存管理

```java
/**
 * 在途库存管理
 */
@Service
public class InTransitService {

    /**
     * 推送 ASN
     */
    public void pushAsn(AsnRequest request) {
        // 1. 在途库存表增加记录
        InventoryInTransit inTransit = new InventoryInTransit();
        inTransit.setSkuCode(request.getSkuCode());
        inTransit.setToWarehouse(request.getToWarehouse());
        inTransit.setSupplierCode(request.getSupplierCode());
        inTransit.setPoNo(request.getPoNo());
        inTransit.setQuantity(request.getQuantity());
        inTransit.setExpectedDate(request.getExpectedDate());
        inTransit.setStatus("PENDING");
        inTransitRepo.save(inTransit);

        // 2. 扩展可用量
        inventoryService.addEAtp(request.getSkuCode(),
            request.getToWarehouse(), request.getQuantity());
    }

    /**
     * 实际到货
     */
    public void confirmArrival(String poNo, int actualQty) {
        InventoryInTransit inTransit = inTransitRepo.findByPoNo(poNo);

        // 1. 状态更新
        inTransit.setReceivedQty(actualQty);
        if (actualQty >= inTransit.getQuantity()) {
            inTransit.setStatus("RECEIVED");
        } else {
            inTransit.setStatus("PARTIAL");
        }
        inTransitRepo.save(inTransit);

        // 2. 减少扩展可用量
        inventoryService.subEAtp(inTransit.getSkuCode(),
            inTransit.getToWarehouse(), actualQty);

        // 3. 增加在库
        inventoryService.addOnHand(inTransit.getSkuCode(),
            inTransit.getToWarehouse(), actualQty);
    }
}
```

---

## 6.9 库存可视化

### 6.9.1 库存看板

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize':'14px'}}}%%
flowchart TB
    subgraph 库存总览
        A1[总库存价值]
        A2[总 SKU 数]
        A3[周转率]
        A4[缺货率]
    end

    subgraph 分仓分析
        B1[各仓库存]
        B2[各仓周转]
        B3[各仓占比]
        B4[各仓异常]
    end

    subgraph ABC 分析
        C1[A 类 TOP 100]
        C2[B 类中坚]
        C3[C 类长尾]
    end

    subgraph 呆滞预警
        D1[90 天未动]
        D2[180 天未动]
        D3[365 天未动]
    end

    style A1 fill:#3b82f6,color:#fff
    style D1 fill:#fbbf24,color:#000
    style D3 fill:#ef4444,color:#fff
```

### 6.9.2 案例：Zara 的库存管理

**Zara 的极速库存周转**：

- 设计到门店：2 周
- 单 SKU 首批 5000 件
- 周转次数：12+ 次/年（行业平均 4 次）
- 售罄率：85%
- 折扣率：< 10%

**核心做法**：

1. **小批量**：首批小，根据销售追加
2. **快速响应**：销售数据 24 小时回流
3. **款式多样**：年 12000+ 新款
4. **全球调拨**：热销地区紧急调货

### 6.9.3 案例：京东的库存 ABC-XYZ 分类

**双维度分类**：

```mermaid
%%{init: {'theme':'dark','themeVariables':{'primaryColor':'#1f2937','primaryTextColor':'#f9fafb','primaryBorderColor':'#22d3ee','lineColor':'#22d3ee','fontSize:'14px'}}}%%
quadrantChart
    title 库存 ABC-XYZ 矩阵
    x-axis "低波动" --> "高波动"
    y-axis "高价值" --> "低价值"
    quadrant-1 "高价值高波动<br/>密切监控"
    quadrant-2 "高价值低波动<br/>战略库存"
    quadrant-3 "低价值高波动<br/>快速周转"
    quadrant-4 "低价值低波动<br/>常规补货"
```

| 分类 | A（高价值） | B（中价值） | C（低价值） |
|------|-------------|-------------|-------------|
| **X（低波动）** | 战略库存 | 安全库存 | 常规补货 |
| **Y（中波动）** | 密切监控 | 安全库存 | 大批量 |
| **Z（高波动）** | 动态 SS | 周期盘点 | JIT |

---

## 本章小结

IMS 是供应链的"账本系统"，核心是**可用量（ATP）计算**、**批次管理**、**多级库存协同**。ATP 公式：可用 = 在库 - 锁定 - 冻结 - 质检 - 安全库存 + 在途。Redis Lua 脚本保证预占原子性，30 分钟 TTL 自动释放，消息队列异步回滚。

**批次管理**支持 FIFO/FEFO/LIFO/FFA 等多种策略，FEFO 是食药妆行业标准。**双向追溯**通过 batch_no 关联上游供应商、原料和下游订单、客户，满足监管要求。

**多级库存**（DC → RDC → FC）需要智能调拨决策：前置仓缺货时优先从 RDC 调，RDC 缺货时从 DC 调，DC 缺货时紧急采购。**安全库存**基础公式 SS = Z × σ × √L，动态安全库存结合机器学习考虑促销、天气、事件等外部因子。

**库存优化**通过 ABC 分类（价值维度）+ XYZ 分类（波动维度）的双维度矩阵，差异化策略管理。Zara 的极速周转、京东的 ABC-XYZ 矩阵是行业典型案例。

下一章将进入**自动化与机器人仓储**，介绍 AGV/AMR、立体库、数字孪生等前沿技术。

---

## 面试高频问题

**1. ATP 的计算公式是什么？多维 ATP 如何计算？**

参考答案要点：ATP = 物理在库 - 已锁定 - 冻结 - 质检 - 安全库存。考虑在途：E-ATP = ATP + 在途入库。考虑生产/采购：CTP = E-ATP + 在 requiredDate 前能到货 - 同期需求。多维 ATP 包括全局（所有仓）、单仓、渠道、批次等不同维度。

**2. FEFO 和 FIFO 的区别？**

参考答案要点：FIFO（First In First Out）按入库日期升序，适合无保质期商品；FEFO（First Expired First Out）按过期日期升序，适合有保质期商品（食品、药品、化妆品）。FEFO 优先出临期批次，避免过期损失。

**3. 多级库存如何决策调拨路径？**

参考答案要点：① 前置仓缺货 → 优先从 RDC 调拨（速度快）；② RDC 也缺 → 从 DC 调拨；③ DC 也缺 → 紧急采购。决策因子：① 调拨成本 vs 紧急采购成本；② 调拨前置期 vs 紧急采购前置期；③ 上下游安全库存水位。

**4. 库存预占的原子性如何保证？**

参考答案要点：Redis Lua 脚本保证 GET + DECRBY 原子性。同时操作可用量（atp）和锁定量（locked）两个 Key，要么都成功要么都失败。MySQL 层面用乐观锁（version 字段）保证。预占有 30 分钟 TTL，过期自动释放。

**5. 安全库存的公式和动态调整？**

参考答案要点：基础公式 SS = Z × σ_d × √L（Z 服务水平，σ_d 需求标准差，L 补货前置期）。考虑前置期波动：SS = Z × √(L × σ_d² + D² × σ_L²)。动态安全库存结合机器学习（LSTM/Prophet）预测需求，叠加促销、天气、事件等外部因子。

**6. ABC 分类的判断标准？**

参考答案要点：按年消耗金额排序，累计占比 0-80% 为 A 类（前 20% SKU），80-95% 为 B 类（中 30% SKU），95-100% 为 C 类（后 50% SKU）。A 类高服务水平（99%）、小批量高频；C 类低服务水平（90%）、大批量低频。

**7. 双向追溯如何实现？**

参考答案要点：所有流转单据（入库/出库/调拨/退货）都记录 batch_no。追溯查询：① 正向（从原料到消费者）通过 batch_no 关联采购单、入库单、库存、出库单、订单；② 反向（从消费者到原料）通过订单号反向查找出库单 → 批次 → 采购单 → 供应商 → 原料。

**8. 在途库存如何管理？**

参考答案要点：在途库存表记录采购、调拨、退货在途记录。状态机：PENDING → SHIPPED → PARTIAL → RECEIVED。考虑在途库存时，扩展可用量 E-ATP = ATP + 在途。实际到货时，在途减少、在库增加，并触发扩展可用量同步。

**9. 库存周转率如何计算？行业基准？**

参考答案要点：库存周转率 = 年销货成本 / 平均库存金额。行业基准：零售 6-8 次，电商 8-12 次，快时尚 12+ 次，制造业 4-6 次。Zara 12+、沃尔玛 8+、京东 10+。提高周转率方法：① 优化 SKU 结构；② 加快动销；③ 清理呆滞；④ 精准补货。

**10. 库存健康度的关键指标？**

参考答案要点：① 库存周转率（核心）；② 售罄率（销售/库存）；③ 缺货率（缺货订单/总订单）；④ 呆滞率（90+ 天未动销金额/总库存）；⑤ 库龄分布；⑥ 库存准确率（99.9% 目标）。综合评分 0-100，< 60 警告、60-80 良好、> 80 优秀。
