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
    /** 地图左右半区最大允许价值差百分比 */
    public static final int MAP_MAX_VALUE_DIFF_PERCENT = 5;

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

    // 携带物品收回：严格按重量档位，收回速度 = 抓取瞬间绳长 / 档位耗时
    /** 轻档重量上限（weight ≤ 1.2：金块/钻石/鼹鼠） */
    public static final double WEIGHT_LIGHT_MAX = 1.2;
    /** 中档重量上限（1.2 < weight ≤ 2.5：福袋/炸弹） */
    public static final double WEIGHT_MEDIUM_MAX = 2.5;
    /** 轻档完成收回耗时（秒） */
    public static final double RETRACT_TIME_LIGHT = 1.2;
    /** 中档完成收回耗时（秒） */
    public static final double RETRACT_TIME_MEDIUM = 2.5;
    /** 重档完成收回耗时（秒）（石头/大金块） */
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

    // 物品模块跨模块公共配置 对齐人员3 FR需求
    /** 玩家初始炸药数量 */
    public static final int PLAYER_INIT_DYNAMITE_COUNT = 1;
    /** 玩家炸药库存上限 */
    public static final int PLAYER_MAX_DYNAMITE_COUNT = 3;
    /** TNT爆炸半径 单位像素（规格：150，不计分/不扣金币/不眩晕） */
    public static final int TNT_EXPLOSION_RADIUS = 150;
    /** 福袋最小随机金币 */
    public static final int MYSTERY_BAG_MIN_GOLD = 100;
    /** 福袋最大随机金币 */
    public static final int MYSTERY_BAG_MAX_GOLD = 800;
    /** 鼹鼠抓取基础金币 */
    public static final int MOLE_CAPTURE_GOLD = 10;
    /** 石头基础金币 */
    public static final int STONE_BASE_GOLD = 1;
}
