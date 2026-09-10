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
    /** 特殊物品（福袋/炸弹）两侧数量最大允许差值 */
    public static final int MAP_SPECIAL_ITEM_MAX_DIFF = 1;
    /** 地图公平性校验最大重试次数（避免死循环） */
    public static final int MAP_FAIRNESS_MAX_RETRY = 50;

    // 钩子锚点（渲染与碰撞共用，双钩左右分置）
    // 已对齐 UI 大画布（1280x720）：与 Config.HOOK1_START_X / HOOK2_START_X / HOOK_START_Y 保持一致
    /** P1 钩子锚点 X 坐标（与 Config.HOOK1_START_X 对齐） */
    public static final double HOOK_ANCHOR_X_P1 = 320;
    /** P2 钩子锚点 X 坐标（与 Config.HOOK2_START_X 对齐） */
    public static final double HOOK_ANCHOR_X_P2 = 960;
    /** 钩子锚点 Y 坐标（顶部地面下缘，与 Config.HOOK_START_Y 对齐） */
    public static final double HOOK_ANCHOR_Y = 150;

    // 钩子物理模块专属配置 完全对齐人员2 FR需求
    /** 钩子钟摆摆动速度（弧度/秒） */
    public static final double HOOK_SWING_SPEED = 1.5;
    /** 绳索最大延伸长度 单位像素（锚点Y=150 → 画布底720，留余量 580 让钩能抓到矿洞底部） */
    public static final double ROPE_MAX_EXTEND_LENGTH = 580;
    /** 空钩收回速度 像素/秒 */
    public static final double HOOK_EMPTY_RETRACT_SPEED = 600;
    /** 带物品基础收回速度 像素/秒 */
    public static final double HOOK_WITH_ITEM_BASE_RETRACT_SPEED = 200;
    /** 冰冻效果持续时长 单位秒 */
    public static final int HOOK_FREEZE_DURATION_SEC = 3;
    /** 双钩抢同一物品停滞惩罚时长 单位秒 */
    public static final int HOOK_STUN_DURATION_SEC = 2;
    /** 强力药水收回速度倍率 */
    public static final double HOOK_SPEED_BOOST_MULTIPLIER = 2.0;
    /** 强力药水效果持续时长 单位秒 */
    public static final int HOOK_SPEED_BOOST_DURATION_SEC = 10;

    // 关卡场景物品数量配置（FR-13）
    /** 场景物品最小数量 */
    public static final int SCENE_ITEM_MIN_COUNT = 25;
    /** 场景物品最大数量 */
    public static final int SCENE_ITEM_MAX_COUNT = 30;

    // 物品模块跨模块公共配置 对齐人员3 FR需求
    /** 玩家初始炸药数量 */
    public static final int PLAYER_INIT_DYNAMITE_COUNT = 1;
    /** 玩家炸药库存上限 */
    public static final int PLAYER_MAX_DYNAMITE_COUNT = 3;
    /** TNT爆炸半径 单位像素 */
    public static final int TNT_EXPLOSION_RADIUS = 300;
    /** 福袋最小随机金币 */
    public static final int MYSTERY_BAG_MIN_GOLD = 100;
    /** 福袋最大随机金币 */
    public static final int MYSTERY_BAG_MAX_GOLD = 800;
    /** 鼹鼠抓取基础金币 */
    public static final int MOLE_CAPTURE_GOLD = 10;
    /** 石头基础金币 */
    public static final int STONE_BASE_GOLD = 1;
}
