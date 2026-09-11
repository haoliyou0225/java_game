package Main.config;

/**
 * 游戏全局配置常量（严格对齐 UML 与分工文档）
 */
public final class GameConfig {

    private GameConfig() {}

    // 窗口与渲染基础配置
    /** 窗口宽度 */
    public static final int WINDOW_WIDTH = 800;
    /** 窗口高度 */
    public static final int WINDOW_HEIGHT = 600;
    /** 全局渲染帧率 */
    public static final int FRAME_RATE = 60;
    /** 单帧时间间隔 单位毫秒 */
    public static final long FRAME_DELAY_MS = 1000 / FRAME_RATE;

    // 对局全局配置
    /** 游戏总时长（秒） */
    public static final int GAME_TOTAL_SEC = 90;
    /** 地图左右半区最大允许价值差百分比（FR-01：≤5%） */
    public static final int MAP_MAX_VALUE_DIFF_PERCENT = 5;
    /** FR-01 每局物品数量下限 */
    public static final int SCENE_ITEM_MIN_COUNT = 25;
    /** FR-01 每局物品数量上限 */
    public static final int SCENE_ITEM_MAX_COUNT = 30;
    /** FR-01 左右半区福袋数量最大差值（≤1） */
    public static final int MAP_BAG_MAX_DIFF = 1;
    /** FR-01 公平性校验最大重试次数（耗尽则取价值差最小的兜底方案） */
    public static final int MAP_FAIRNESS_MAX_RETRY = 300;

    // 钩子锚点（渲染与碰撞共用，双钩左右分置）
    // 规格：P1 锚点 (320,80)、P2 锚点 (960,80)，初始摆动方向相反
    /** P1 钩子锚点 X 坐标 */
    public static final double HOOK_ANCHOR_X_P1 = 320;
    /** P2 钩子锚点 X 坐标 */
    public static final double HOOK_ANCHOR_X_P2 = 960;
    /** 钩子锚点 Y 坐标（地面条内） */
    public static final double HOOK_ANCHOR_Y = 80;

    // 矿洞活动边界（钩爪触达边界自动触发空钩收回）
    /** 矿洞左边界 X */
    public static final double MINE_MIN_X = 50;
    /** 矿洞右边界 X */
    public static final double MINE_MAX_X = 1230;
    /** 矿洞下边界 Y */
    public static final double MINE_MAX_Y = 660;

    // 钩子物理模块专属配置
    /** 钩子钟摆摆动角速度（弧度/秒，固定 1.5） */
    public static final double HOOK_SWING_SPEED = 1.5;
    /** 钟摆幅度限制（相对垂直向下 ±1.25 弧度） */
    public static final double HOOK_SWING_MAX_OFFSET = 1.25;
    /** 绳索最大延伸长度 单位像素（≥1150 覆盖全矿洞对角） */
    public static final double ROPE_MAX_EXTEND_LENGTH = 1150;
    /** 钩爪直线抛出速度（像素/秒，固定 500） */
    public static final double HOOK_THROW_SPEED = 500;
    /** 空钩收回速度（像素/秒，固定 800，显著快于带物品） */
    public static final double HOOK_EMPTY_RETRACT_SPEED = 800;

    // 携带物品收回：FR-04 重量三档（轻1.2s/中2.5s/重5.0s）为默认规格；
    // FR-10 物品数值表对每个品类指定精确收回耗时（1.0/1.2/1.5/2.0/2.5/3.0/4.0/5.0），
    // 由 Item.getRetractDuration() 逐物品提供，收回速度 = 抓取瞬间绳长 / 该耗时。
    /** 轻档重量上限（weight ≤ 1.2） */
    public static final double WEIGHT_LIGHT_MAX = 1.2;
    /** 中档重量上限（1.2 < weight ≤ 2.5） */
    public static final double WEIGHT_MEDIUM_MAX = 2.5;
    /** 轻档完成收回耗时（秒） */
    public static final double RETRACT_TIME_LIGHT = 1.2;
    /** 中档完成收回耗时（秒） */
    public static final double RETRACT_TIME_MEDIUM = 2.5;
    /** 重档完成收回耗时（秒） */
    public static final double RETRACT_TIME_HEAVY = 5.0;

    /** 双钩抢夺/碰撞眩晕持续时长（秒） */
    public static final double HOOK_STUN_DURATION_SEC = 2.0;
    /** 双钩抢夺判定时间窗口（毫秒） */
    public static final long HOOK_STEAL_WINDOW_MS = 50;
    /** 鼹鼠偏转角度下限（度） */
    public static final double MOLE_DEFLECT_MIN_DEG = 15;
    /** 鼹鼠偏转角度上限（度） */
    public static final double MOLE_DEFLECT_MAX_DEG = 30;

    /** 冰冻效果持续时长 单位秒 */
    public static final int HOOK_FREEZE_DURATION_SEC = 3;
    /** 强力药水收回速度倍率 */
    public static final double HOOK_SPEED_BOOST_MULTIPLIER = 2.0;
    /** 强力药水效果持续时长 单位秒 */
    public static final int HOOK_SPEED_BOOST_DURATION_SEC = 10;

    // 物品模块跨模块公共配置 对齐人员3 FR需求（FR-10 数值表）
    /** 玩家初始炸药数量（FR-15：开局每人 1 个） */
    public static final int PLAYER_INIT_DYNAMITE_COUNT = 1;
    /** 玩家炸药库存上限（FR-15） */
    public static final int PLAYER_MAX_DYNAMITE_COUNT = 3;
    /** 短时道具（强力药水/冰冻箱）单种库存上限（FR-15：短时道具上限 5） */
    public static final int PLAYER_MAX_SHORT_ITEM_COUNT = 5;
    /** 持续道具（幸运草/钻石升级/石头书）单种库存上限（FR-18：按键使用，单激活位） */
    public static final int PLAYER_MAX_PERSIST_ITEM_COUNT = 5;
    /** TNT爆炸半径 单位像素（FR-12：150，不计分/不扣金币/不眩晕） */
    public static final int TNT_EXPLOSION_RADIUS = 150;
    /** 福袋必给金币下限（FR-01/FR-14：生成时预计算 100~800） */
    public static final int MYSTERY_BAG_MIN_GOLD = 100;
    /** 福袋必给金币上限 */
    public static final int MYSTERY_BAG_MAX_GOLD = 800;
    /** 鼹鼠抓取基础金币（FR-10：10） */
    public static final int MOLE_CAPTURE_GOLD = 10;
    /** 石头基础金币（FR-10：11） */
    public static final int STONE_BASE_GOLD = 11;
    /** 石头收藏书激活时石头价值倍率（FR-17：石头×3） */
    public static final int STONE_BOOK_MULTIPLIER = 3;

    // ===== 福袋额外奖励（FR-14：35% 金币 / 35% 炸药 / 20% 短时道具 / 10% 持续道具） =====
    /** 炸药满 3、短时道具满 5、持续道具已激活时，再次获得自动转为金币（FR-15/FR-18） */
    public static final int ITEM_DUP_AUTO_GOLD = 50;
    /** 幸运草效果：本局物品基础收益倍率（+50%，FR-17/FR-18） */
    public static final double LUCKY_CLOVER_BONUS_RATE = 1.5;
    /** 钻石升级药水效果：钻石价值倍率（FR-17：钻石×2） */
    public static final int DIAMOND_BOOST_MULTIPLIER = 2;
    /** 强力药水效果：自身钩爪收回速度倍率（FR-16：翻倍） */
    public static final double POWER_POTION_SPEED_MULTIPLIER = 2.0;
    /** 强力药水效果持续时长（秒，FR-16：10 秒） */
    public static final int POWER_POTION_DURATION_SEC = 10;
    /** 福袋加权抽奖：金币权重（FR-14：35%） */
    public static final int BAG_WEIGHT_GOLD = 35;
    /** 福袋加权抽奖：炸药权重（FR-14：35%） */
    public static final int BAG_WEIGHT_DYNAMITE = 35;
    /** 福袋加权抽奖：短时道具权重（FR-14：20%，强力药水/冰冻箱等概率） */
    public static final int BAG_WEIGHT_SHORT_ITEM = 20;
    /** 福袋加权抽奖：持续道具权重（FR-14：10%，幸运草/钻石药/石头书等概率） */
    public static final int BAG_WEIGHT_PERSIST = 10;

    // ===== FR-11 钻石猪 =====
    /** 钻石猪携带钻石颗数下限（FR-01：生成时预计算 2~5 颗） */
    public static final int PIG_DIAMOND_MIN = 2;
    /** 钻石猪携带钻石颗数上限 */
    public static final int PIG_DIAMOND_MAX = 5;
    /** 单颗钻石价值 */
    public static final int PIG_DIAMOND_VALUE = 600;
    /** 钻石猪捕获后固定附加金币（FR-10：颗数×600+10） */
    public static final int PIG_BASE_GOLD = 10;
    /** 钻石猪目标捕获率（FR-11：约 40%） */
    public static final double PIG_CAPTURE_RATE = 0.4;
    /** 钻石猪基础移动速度（像素/秒） */
    public static final double PIG_MOVE_SPEED = 55;
    /** 钻石猪短暂加速倍率（FR-11：+50%） */
    public static final double PIG_DASH_MULTIPLIER = 1.5;
    /** 钻石猪每次加速持续时长（秒，FR-11：0.5 秒） */
    public static final double PIG_DASH_DURATION_SEC = 0.5;
    /** 钻石猪加速触发间隔下限（秒，FR-11：每 2~4 秒） */
    public static final double PIG_DASH_INTERVAL_MIN_SEC = 2.0;
    /** 钻石猪加速触发间隔上限（秒） */
    public static final double PIG_DASH_INTERVAL_MAX_SEC = 4.0;
    /** 钻石猪逃脱钩爪后，再次碰撞判定冷却（秒，避免同一次重叠反复掷骰） */
    public static final double PIG_ESCAPE_COOLDOWN_SEC = 0.5;

    /**
     * 福袋额外奖励枚举（FR-14 七类）：
     * 金币 / 炸药 / 强力药水（短时）/ 冰冻箱（短时）/ 幸运草（持续）/ 钻石升级（持续）/ 石头书（持续）。
     * 同时用作结算飘字的图标类型。
     */
    public enum MysteryReward {
        MYSTERY_GOLD("金币"),          // 额外 100~800 随机金币
        DYNAMITE("炸药"),              // 炸药 +1，满 3 转 50 金币
        POWER_POTION("强力药水"),      // 短时道具：自身收回×2 持续 10 秒，库存上限 5
        FREEZE_BOX("冰冻箱"),          // 短时道具：冻结对方钩爪 3 秒，库存上限 5
        LUCKY_CLOVER("幸运草"),        // 持续道具：本局物品收益 +50%，单激活位
        DIAMOND_BOOST("钻石升级"),     // 持续道具：钻石×2，单激活位
        STONE_BOOK("石头书");          // 持续道具：石头×3，单激活位

        private final String cnName;
        MysteryReward(String cnName) { this.cnName = cnName; }
        public String cnName() { return cnName; }
    }
}
