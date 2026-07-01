package com.example.sheeps.menu.state;

import com.example.sheeps.data.model.*;

/**
 * 菜单主界面状态模型。
 * 包含了商城、个人中心、对战大厅及全局 App 状态所需的数据。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000`\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\bE\b\u0086\b\u0018\u00002\u00020\u0001B\u00b7\u0002\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0006\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\b\b\u0002\u0010\n\u001a\u00020\t\u0012\b\b\u0002\u0010\u000b\u001a\u00020\u0003\u0012\b\b\u0002\u0010\f\u001a\u00020\t\u0012\b\b\u0002\u0010\r\u001a\u00020\t\u0012\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f\u0012\u000e\b\u0002\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\u000f\u0012\u000e\b\u0002\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u000f\u0012\u000e\b\u0002\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u000f\u0012\u000e\b\u0002\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\u000f\u0012\u000e\b\u0002\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000f\u0012\u0014\b\u0002\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\t0\u001c\u0012\b\b\u0002\u0010\u001d\u001a\u00020\u001e\u0012\b\b\u0002\u0010\u001f\u001a\u00020\u0006\u0012\n\b\u0002\u0010 \u001a\u0004\u0018\u00010!\u0012\b\b\u0002\u0010\"\u001a\u00020\u0006\u0012\b\b\u0002\u0010#\u001a\u00020\u0006\u0012\n\b\u0002\u0010$\u001a\u0004\u0018\u00010\u0006\u0012\n\b\u0002\u0010%\u001a\u0004\u0018\u00010\u0006\u0012\b\b\u0002\u0010&\u001a\u00020\t\u0012\b\b\u0002\u0010\'\u001a\u00020\t\u00a2\u0006\u0004\b(\u0010)J\t\u0010H\u001a\u00020\u0003H\u00c6\u0003J\t\u0010I\u001a\u00020\u0003H\u00c6\u0003J\t\u0010J\u001a\u00020\u0006H\u00c6\u0003J\t\u0010K\u001a\u00020\u0006H\u00c6\u0003J\t\u0010L\u001a\u00020\tH\u00c6\u0003J\t\u0010M\u001a\u00020\tH\u00c6\u0003J\t\u0010N\u001a\u00020\u0003H\u00c6\u0003J\t\u0010O\u001a\u00020\tH\u00c6\u0003J\t\u0010P\u001a\u00020\tH\u00c6\u0003J\u000f\u0010Q\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fH\u00c6\u0003J\u000f\u0010R\u001a\b\u0012\u0004\u0012\u00020\u00120\u000fH\u00c6\u0003J\u000f\u0010S\u001a\b\u0012\u0004\u0012\u00020\u00140\u000fH\u00c6\u0003J\u000f\u0010T\u001a\b\u0012\u0004\u0012\u00020\u00160\u000fH\u00c6\u0003J\u000f\u0010U\u001a\b\u0012\u0004\u0012\u00020\u00180\u000fH\u00c6\u0003J\u000f\u0010V\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000fH\u00c6\u0003J\u0015\u0010W\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\t0\u001cH\u00c6\u0003J\t\u0010X\u001a\u00020\u001eH\u00c6\u0003J\t\u0010Y\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010Z\u001a\u0004\u0018\u00010!H\u00c6\u0003J\t\u0010[\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\\\u001a\u00020\u0006H\u00c6\u0003J\u000b\u0010]\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\u000b\u0010^\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\t\u0010_\u001a\u00020\tH\u00c6\u0003J\t\u0010`\u001a\u00020\tH\u00c6\u0003J\u00b9\u0002\u0010a\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\t2\b\b\u0002\u0010\u000b\u001a\u00020\u00032\b\b\u0002\u0010\f\u001a\u00020\t2\b\b\u0002\u0010\r\u001a\u00020\t2\u000e\b\u0002\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f2\u000e\b\u0002\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\u000f2\u000e\b\u0002\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u000f2\u000e\b\u0002\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u000f2\u000e\b\u0002\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\u000f2\u000e\b\u0002\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000f2\u0014\b\u0002\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\t0\u001c2\b\b\u0002\u0010\u001d\u001a\u00020\u001e2\b\b\u0002\u0010\u001f\u001a\u00020\u00062\n\b\u0002\u0010 \u001a\u0004\u0018\u00010!2\b\b\u0002\u0010\"\u001a\u00020\u00062\b\b\u0002\u0010#\u001a\u00020\u00062\n\b\u0002\u0010$\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010%\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010&\u001a\u00020\t2\b\b\u0002\u0010\'\u001a\u00020\tH\u00c6\u0001J\u0013\u0010b\u001a\u00020\u00032\b\u0010c\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010d\u001a\u00020\tH\u00d6\u0001J\t\u0010e\u001a\u00020\u0006H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0002\u0010*R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010*R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010,R\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010,R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010/R\u0011\u0010\n\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010/R\u0011\u0010\u000b\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010*R\u0011\u0010\f\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b2\u0010/R\u0011\u0010\r\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010/R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u00105R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u00105R\u0017\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b7\u00105R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b8\u00105R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u00105R\u0017\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001a0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u00105R\u001d\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\t0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b;\u0010<R\u0011\u0010\u001d\u001a\u00020\u001e\u00a2\u0006\b\n\u0000\u001a\u0004\b=\u0010>R\u0011\u0010\u001f\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b?\u0010,R\u0013\u0010 \u001a\u0004\u0018\u00010!\u00a2\u0006\b\n\u0000\u001a\u0004\b@\u0010AR\u0011\u0010\"\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\bB\u0010,R\u0011\u0010#\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\bC\u0010,R\u0013\u0010$\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\bD\u0010,R\u0013\u0010%\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\bE\u0010,R\u0011\u0010&\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\bF\u0010/R\u0011\u0010\'\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\bG\u0010/\u00a8\u0006f"}, d2 = {"Lcom/example/sheeps/menu/state/MenuViewState;", "", "isLoading", "", "isLoggedIn", "username", "", "phone", "points", "", "unlockedLevel", "todaySigned", "signStreak", "highestLevelCleared", "backpackItems", "", "Lcom/example/sheeps/data/model/UserItem;", "shopItems", "Lcom/example/sheeps/data/model/ShopItem;", "notices", "Lcom/example/sheeps/data/model/Notice;", "dailyTasks", "Lcom/example/sheeps/data/model/DailyTask;", "pointsHistory", "Lcom/example/sheeps/data/model/PointRecord;", "exchangeHistory", "Lcom/example/sheeps/data/model/ExchangeRecord;", "selectedCarryItems", "", "networkStatus", "Lcom/example/sheeps/core/utils/NetworkStatus;", "language", "appUpdateInfo", "Lcom/example/sheeps/data/model/AppUpdateResponse;", "currentSkin", "matchStatus", "matchedGameId", "matchedOpponentId", "duelLevel", "gameSeed", "<init>", "(ZZLjava/lang/String;Ljava/lang/String;IIZIILjava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/Map;Lcom/example/sheeps/core/utils/NetworkStatus;Ljava/lang/String;Lcom/example/sheeps/data/model/AppUpdateResponse;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", "()Z", "getUsername", "()Ljava/lang/String;", "getPhone", "getPoints", "()I", "getUnlockedLevel", "getTodaySigned", "getSignStreak", "getHighestLevelCleared", "getBackpackItems", "()Ljava/util/List;", "getShopItems", "getNotices", "getDailyTasks", "getPointsHistory", "getExchangeHistory", "getSelectedCarryItems", "()Ljava/util/Map;", "getNetworkStatus", "()Lcom/example/sheeps/core/utils/NetworkStatus;", "getLanguage", "getAppUpdateInfo", "()Lcom/example/sheeps/data/model/AppUpdateResponse;", "getCurrentSkin", "getMatchStatus", "getMatchedGameId", "getMatchedOpponentId", "getDuelLevel", "getGameSeed", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component20", "component21", "component22", "component23", "component24", "component25", "copy", "equals", "other", "hashCode", "toString", "feature_menu_release"})
public final class MenuViewState {
    
    /**
     * 是否正在执行全局加载（如刷新数据）
     */
    private final boolean isLoading = false;
    
    /**
     * 当前用户是否已登录
     */
    private final boolean isLoggedIn = false;
    
    /**
     * 用户昵称
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String username = null;
    
    /**
     * 用户绑定的手机号（UID）
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String phone = null;
    
    /**
     * 当前账户可用积分余额
     */
    private final int points = 0;
    
    /**
     * 用户已解锁的最高关卡 ID
     */
    private final int unlockedLevel = 0;
    
    /**
     * 今日是否已完成签到
     */
    private final boolean todaySigned = false;
    
    /**
     * 连续签到天数
     */
    private final int signStreak = 0;
    
    /**
     * 闯关历史上清除过的最高关卡数
     */
    private final int highestLevelCleared = 0;
    
    /**
     * 用户背包内的道具列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.UserItem> backpackItems = null;
    
    /**
     * 商店可兑换的商品列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.ShopItem> shopItems = null;
    
    /**
     * 系统公告通知列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.Notice> notices = null;
    
    /**
     * 每日任务进度列表
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.DailyTask> dailyTasks = null;
    
    /**
     * 积分变动记录
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.PointRecord> pointsHistory = null;
    
    /**
     * 道具兑换记录
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.example.sheeps.data.model.ExchangeRecord> exchangeHistory = null;
    
    /**
     * 当前准备进入关卡时选择携带的道具映射 (ItemType -> Count)
     */
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.Integer> selectedCarryItems = null;
    
    /**
     * 当前网络连接监控状态
     */
    @org.jetbrains.annotations.NotNull()
    private final com.example.sheeps.core.utils.NetworkStatus networkStatus = null;
    
    /**
     * 当前应用生效的语言代码（如 "zh", "en"）
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String language = null;
    
    /**
     * 应用版本更新信息
     */
    @org.jetbrains.annotations.Nullable()
    private final com.example.sheeps.data.model.AppUpdateResponse appUpdateInfo = null;
    
    /**
     * 当前生效的卡牌皮肤 ID
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String currentSkin = null;
    
    /**
     * 匹配状态：none (空闲), searching (匹配中), matched (已匹配), error (异常)
     */
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String matchStatus = null;
    
    /**
     * 匹配成功后的房间 ID
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String matchedGameId = null;
    
    /**
     * 匹配到的对手玩家 ID
     */
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String matchedOpponentId = null;
    
    /**
     * 对决所使用的关卡 ID
     */
    private final int duelLevel = 0;
    
    /**
     * 对决地图生成的随机种子
     */
    private final int gameSeed = 0;
    
    public MenuViewState(boolean isLoading, boolean isLoggedIn, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String phone, int points, int unlockedLevel, boolean todaySigned, int signStreak, int highestLevelCleared, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.UserItem> backpackItems, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.ShopItem> shopItems, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Notice> notices, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.DailyTask> dailyTasks, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.PointRecord> pointsHistory, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.ExchangeRecord> exchangeHistory, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.Integer> selectedCarryItems, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.utils.NetworkStatus networkStatus, @org.jetbrains.annotations.NotNull()
    java.lang.String language, @org.jetbrains.annotations.Nullable()
    com.example.sheeps.data.model.AppUpdateResponse appUpdateInfo, @org.jetbrains.annotations.NotNull()
    java.lang.String currentSkin, @org.jetbrains.annotations.NotNull()
    java.lang.String matchStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String matchedGameId, @org.jetbrains.annotations.Nullable()
    java.lang.String matchedOpponentId, int duelLevel, int gameSeed) {
        super();
    }
    
    /**
     * 是否正在执行全局加载（如刷新数据）
     */
    public final boolean isLoading() {
        return false;
    }
    
    /**
     * 当前用户是否已登录
     */
    public final boolean isLoggedIn() {
        return false;
    }
    
    /**
     * 用户昵称
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getUsername() {
        return null;
    }
    
    /**
     * 用户绑定的手机号（UID）
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPhone() {
        return null;
    }
    
    /**
     * 当前账户可用积分余额
     */
    public final int getPoints() {
        return 0;
    }
    
    /**
     * 用户已解锁的最高关卡 ID
     */
    public final int getUnlockedLevel() {
        return 0;
    }
    
    /**
     * 今日是否已完成签到
     */
    public final boolean getTodaySigned() {
        return false;
    }
    
    /**
     * 连续签到天数
     */
    public final int getSignStreak() {
        return 0;
    }
    
    /**
     * 闯关历史上清除过的最高关卡数
     */
    public final int getHighestLevelCleared() {
        return 0;
    }
    
    /**
     * 用户背包内的道具列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.UserItem> getBackpackItems() {
        return null;
    }
    
    /**
     * 商店可兑换的商品列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.ShopItem> getShopItems() {
        return null;
    }
    
    /**
     * 系统公告通知列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Notice> getNotices() {
        return null;
    }
    
    /**
     * 每日任务进度列表
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.DailyTask> getDailyTasks() {
        return null;
    }
    
    /**
     * 积分变动记录
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.PointRecord> getPointsHistory() {
        return null;
    }
    
    /**
     * 道具兑换记录
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.ExchangeRecord> getExchangeHistory() {
        return null;
    }
    
    /**
     * 当前准备进入关卡时选择携带的道具映射 (ItemType -> Count)
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.Integer> getSelectedCarryItems() {
        return null;
    }
    
    /**
     * 当前网络连接监控状态
     */
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.utils.NetworkStatus getNetworkStatus() {
        return null;
    }
    
    /**
     * 当前应用生效的语言代码（如 "zh", "en"）
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLanguage() {
        return null;
    }
    
    /**
     * 应用版本更新信息
     */
    @org.jetbrains.annotations.Nullable()
    public final com.example.sheeps.data.model.AppUpdateResponse getAppUpdateInfo() {
        return null;
    }
    
    /**
     * 当前生效的卡牌皮肤 ID
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getCurrentSkin() {
        return null;
    }
    
    /**
     * 匹配状态：none (空闲), searching (匹配中), matched (已匹配), error (异常)
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMatchStatus() {
        return null;
    }
    
    /**
     * 匹配成功后的房间 ID
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getMatchedGameId() {
        return null;
    }
    
    /**
     * 匹配到的对手玩家 ID
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getMatchedOpponentId() {
        return null;
    }
    
    /**
     * 对决所使用的关卡 ID
     */
    public final int getDuelLevel() {
        return 0;
    }
    
    /**
     * 对决地图生成的随机种子
     */
    public final int getGameSeed() {
        return 0;
    }
    
    public MenuViewState() {
        super();
    }
    
    public final boolean component1() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.UserItem> component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.ShopItem> component11() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Notice> component12() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.DailyTask> component13() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.PointRecord> component14() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.ExchangeRecord> component15() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.Integer> component16() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.core.utils.NetworkStatus component17() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component18() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.example.sheeps.data.model.AppUpdateResponse component19() {
        return null;
    }
    
    public final boolean component2() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component20() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component21() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component22() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component23() {
        return null;
    }
    
    public final int component24() {
        return 0;
    }
    
    public final int component25() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    public final int component5() {
        return 0;
    }
    
    public final int component6() {
        return 0;
    }
    
    public final boolean component7() {
        return false;
    }
    
    public final int component8() {
        return 0;
    }
    
    public final int component9() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.sheeps.menu.state.MenuViewState copy(boolean isLoading, boolean isLoggedIn, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String phone, int points, int unlockedLevel, boolean todaySigned, int signStreak, int highestLevelCleared, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.UserItem> backpackItems, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.ShopItem> shopItems, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.Notice> notices, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.DailyTask> dailyTasks, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.PointRecord> pointsHistory, @org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.ExchangeRecord> exchangeHistory, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.Integer> selectedCarryItems, @org.jetbrains.annotations.NotNull()
    com.example.sheeps.core.utils.NetworkStatus networkStatus, @org.jetbrains.annotations.NotNull()
    java.lang.String language, @org.jetbrains.annotations.Nullable()
    com.example.sheeps.data.model.AppUpdateResponse appUpdateInfo, @org.jetbrains.annotations.NotNull()
    java.lang.String currentSkin, @org.jetbrains.annotations.NotNull()
    java.lang.String matchStatus, @org.jetbrains.annotations.Nullable()
    java.lang.String matchedGameId, @org.jetbrains.annotations.Nullable()
    java.lang.String matchedOpponentId, int duelLevel, int gameSeed) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}