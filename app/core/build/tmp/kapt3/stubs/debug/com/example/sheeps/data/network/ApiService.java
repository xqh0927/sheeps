package com.example.sheeps.data.network;

import com.example.sheeps.data.model.AppUpdateResponse;
import com.example.sheeps.data.model.DailyTask;
import com.example.sheeps.data.model.DailyPopupResponse;
import com.example.sheeps.data.model.ExchangeRecord;
import com.example.sheeps.data.model.ExchangeRequest;
import com.example.sheeps.data.model.ExchangeResponse;
import com.example.sheeps.data.model.GenericResponse;
import com.example.sheeps.data.model.LeaderboardResponse;
import com.example.sheeps.data.model.LoginRequest;
import com.example.sheeps.data.model.LoginResponse;
import com.example.sheeps.data.model.MatchJoinRequest;
import com.example.sheeps.data.model.MatchStatusResponse;
import com.example.sheeps.data.model.Notice;
import com.example.sheeps.data.model.PointRecord;
import com.example.sheeps.data.model.RefreshResponse;
import com.example.sheeps.data.model.RegisterRequest;
import com.example.sheeps.data.model.RenameRequest;
import com.example.sheeps.data.model.ScoreRequest;
import com.example.sheeps.data.model.SendCodeRequest;
import com.example.sheeps.data.model.SendCodeResponse;
import com.example.sheeps.data.model.ShopItem;
import com.example.sheeps.data.model.SignResponse;
import com.example.sheeps.data.model.SyncRequest;
import com.example.sheeps.data.model.SyncResponse;
import com.example.sheeps.data.model.TaskClaimRequest;
import com.example.sheeps.data.model.TaskClaimResponse;
import com.example.sheeps.data.model.Tile;
import com.example.sheeps.data.model.UnlockLevelRequest;
import com.example.sheeps.data.model.UnlockLevelResponse;
import com.example.sheeps.data.model.UserProfileResponse;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u00e4\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010$\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ*\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\b\b\u0001\u0010\r\u001a\u00020\u000e2\n\b\u0003\u0010\u000f\u001a\u0004\u0018\u00010\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u0010J$\u0010\u0011\u001a\u00020\u00032\n\b\u0001\u0010\u0012\u001a\u0004\u0018\u00010\u00132\b\b\u0001\u0010\u0004\u001a\u00020\u0014H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\"\u0010\u0016\u001a\u00020\u00172\b\b\u0001\u0010\r\u001a\u00020\u000e2\b\b\u0003\u0010\u0018\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u0019J\u0018\u0010\u001a\u001a\u00020\u001b2\b\b\u0001\u0010\u0004\u001a\u00020\u001cH\u00a7@\u00a2\u0006\u0002\u0010\u001dJ\u0018\u0010\u001e\u001a\u00020\u001f2\b\b\u0001\u0010\u0004\u001a\u00020 H\u00a7@\u00a2\u0006\u0002\u0010!J$\u0010\"\u001a\u00020#2\u0014\b\u0001\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0013\u0012\u0004\u0012\u00020\u00130$H\u00a7@\u00a2\u0006\u0002\u0010%J\"\u0010&\u001a\u00020\'2\b\b\u0001\u0010\u0012\u001a\u00020\u00132\b\b\u0001\u0010\u0004\u001a\u00020(H\u00a7@\u00a2\u0006\u0002\u0010)J\"\u0010*\u001a\u00020+2\b\b\u0001\u0010\u0012\u001a\u00020\u00132\b\b\u0001\u0010\u0004\u001a\u00020,H\u00a7@\u00a2\u0006\u0002\u0010-J\u0014\u0010.\u001a\b\u0012\u0004\u0012\u00020/0\u000bH\u00a7@\u00a2\u0006\u0002\u00100J\"\u00101\u001a\u0002022\b\b\u0001\u0010\u0012\u001a\u00020\u00132\b\b\u0001\u0010\u0004\u001a\u000203H\u00a7@\u00a2\u0006\u0002\u00104J\u0018\u00105\u001a\u0002062\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u00107J\u001e\u00108\u001a\b\u0012\u0004\u0012\u0002090\u000b2\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u00107J\"\u0010:\u001a\u00020;2\b\b\u0001\u0010\u0012\u001a\u00020\u00132\b\b\u0001\u0010\u0004\u001a\u00020<H\u00a7@\u00a2\u0006\u0002\u0010=J\u0014\u0010>\u001a\b\u0012\u0004\u0012\u00020?0\u000bH\u00a7@\u00a2\u0006\u0002\u00100J\u0018\u0010@\u001a\u00020A2\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u00107J\u001e\u0010B\u001a\b\u0012\u0004\u0012\u00020C0\u000b2\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u00107J\u001e\u0010D\u001a\b\u0012\u0004\u0012\u00020E0\u000b2\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u00107J6\u0010F\u001a\u00020\u00172\b\b\u0001\u0010\r\u001a\u00020\u000e2\b\b\u0001\u0010G\u001a\u00020\u00132\b\b\u0001\u0010H\u001a\u00020\u000e2\b\b\u0003\u0010\u0018\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010IJ\u0018\u0010J\u001a\u00020K2\b\b\u0001\u0010L\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010MJ\u0018\u0010N\u001a\u00020O2\b\b\u0001\u0010\u0004\u001a\u00020PH\u00a7@\u00a2\u0006\u0002\u0010QJ\u0018\u0010R\u001a\u00020O2\b\b\u0001\u0010S\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u00107J\u0018\u0010T\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020PH\u00a7@\u00a2\u0006\u0002\u0010QJ\u0018\u0010U\u001a\u00020V2\b\b\u0001\u0010\u0012\u001a\u00020\u0013H\u00a7@\u00a2\u0006\u0002\u00107\u00a8\u0006W\u00c0\u0006\u0003"}, d2 = {"Lcom/example/sheeps/data/network/ApiService;", "", "register", "Lcom/example/sheeps/data/model/GenericResponse;", "request", "Lcom/example/sheeps/data/model/RegisterRequest;", "(Lcom/example/sheeps/data/model/RegisterRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "rename", "Lcom/example/sheeps/data/model/RenameRequest;", "(Lcom/example/sheeps/data/model/RenameRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLevel", "", "Lcom/example/sheeps/data/model/Tile;", "levelId", "", "seed", "(ILjava/lang/Integer;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "submitScore", "auth", "", "Lcom/example/sheeps/data/model/ScoreRequest;", "(Ljava/lang/String;Lcom/example/sheeps/data/model/ScoreRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLeaderboard", "Lcom/example/sheeps/data/model/LeaderboardResponse;", "limit", "(IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendCode", "Lcom/example/sheeps/data/model/SendCodeResponse;", "Lcom/example/sheeps/data/model/SendCodeRequest;", "(Lcom/example/sheeps/data/model/SendCodeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "login", "Lcom/example/sheeps/data/model/LoginResponse;", "Lcom/example/sheeps/data/model/LoginRequest;", "(Lcom/example/sheeps/data/model/LoginRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refreshToken", "Lcom/example/sheeps/data/model/RefreshResponse;", "", "(Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "syncData", "Lcom/example/sheeps/data/model/SyncResponse;", "Lcom/example/sheeps/data/model/SyncRequest;", "(Ljava/lang/String;Lcom/example/sheeps/data/model/SyncRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "unlockLevel", "Lcom/example/sheeps/data/model/UnlockLevelResponse;", "Lcom/example/sheeps/data/model/UnlockLevelRequest;", "(Ljava/lang/String;Lcom/example/sheeps/data/model/UnlockLevelRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getShopItems", "Lcom/example/sheeps/data/model/ShopItem;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "exchangeItem", "Lcom/example/sheeps/data/model/ExchangeResponse;", "Lcom/example/sheeps/data/model/ExchangeRequest;", "(Ljava/lang/String;Lcom/example/sheeps/data/model/ExchangeRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "signIn", "Lcom/example/sheeps/data/model/SignResponse;", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getDailyTasks", "Lcom/example/sheeps/data/model/DailyTask;", "claimTaskReward", "Lcom/example/sheeps/data/model/TaskClaimResponse;", "Lcom/example/sheeps/data/model/TaskClaimRequest;", "(Ljava/lang/String;Lcom/example/sheeps/data/model/TaskClaimRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getNotices", "Lcom/example/sheeps/data/model/Notice;", "getUserProfile", "Lcom/example/sheeps/data/model/UserProfileResponse;", "getPointsHistory", "Lcom/example/sheeps/data/model/PointRecord;", "getExchangeHistory", "Lcom/example/sheeps/data/model/ExchangeRecord;", "getLeaderboardPaged", "type", "page", "(ILjava/lang/String;IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkUpdate", "Lcom/example/sheeps/data/model/AppUpdateResponse;", "versionCode", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "joinMatch", "Lcom/example/sheeps/data/model/MatchStatusResponse;", "Lcom/example/sheeps/data/model/MatchJoinRequest;", "(Lcom/example/sheeps/data/model/MatchJoinRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getMatchStatus", "playerId", "leaveMatch", "getDailyPopup", "Lcom/example/sheeps/data/model/DailyPopupResponse;", "core_debug"})
public abstract interface ApiService {
    
    @retrofit2.http.POST(value = "/api/register")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object register(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.RegisterRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.GenericResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/user/rename")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object rename(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.RenameRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.GenericResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/level")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLevel(@retrofit2.http.Query(value = "id")
    int levelId, @retrofit2.http.Query(value = "seed")
    @org.jetbrains.annotations.Nullable()
    java.lang.Integer seed, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.sheeps.data.model.Tile>> $completion);
    
    @retrofit2.http.POST(value = "/api/score/submit")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object submitScore(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.Nullable()
    java.lang.String auth, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.ScoreRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.GenericResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/leaderboard")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLeaderboard(@retrofit2.http.Query(value = "level_id")
    int levelId, @retrofit2.http.Query(value = "limit")
    int limit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.LeaderboardResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/auth/send-code")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object sendCode(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.SendCodeRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.SendCodeResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/auth/login")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object login(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.LoginRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.LoginResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/auth/refresh")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refreshToken(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.RefreshResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/user/sync")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncData(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.SyncRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.SyncResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/level/unlock")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object unlockLevel(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.UnlockLevelRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.UnlockLevelResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/shop/items")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getShopItems(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.sheeps.data.model.ShopItem>> $completion);
    
    @retrofit2.http.POST(value = "/api/shop/exchange")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object exchangeItem(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.ExchangeRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.ExchangeResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/sign/today")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object signIn(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.SignResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/task/daily")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDailyTasks(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.sheeps.data.model.DailyTask>> $completion);
    
    @retrofit2.http.POST(value = "/api/task/claim")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object claimTaskReward(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.TaskClaimRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.TaskClaimResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/notice/list")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getNotices(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.sheeps.data.model.Notice>> $completion);
    
    @retrofit2.http.GET(value = "/api/user/profile")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getUserProfile(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.UserProfileResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/user/points-history")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPointsHistory(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.sheeps.data.model.PointRecord>> $completion);
    
    @retrofit2.http.GET(value = "/api/user/exchange-history")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getExchangeHistory(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.example.sheeps.data.model.ExchangeRecord>> $completion);
    
    @retrofit2.http.GET(value = "/api/leaderboard")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLeaderboardPaged(@retrofit2.http.Query(value = "level_id")
    int levelId, @retrofit2.http.Query(value = "type")
    @org.jetbrains.annotations.NotNull()
    java.lang.String type, @retrofit2.http.Query(value = "page")
    int page, @retrofit2.http.Query(value = "limit")
    int limit, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.LeaderboardResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/app/check-update")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object checkUpdate(@retrofit2.http.Query(value = "version_code")
    int versionCode, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.AppUpdateResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/match/join")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object joinMatch(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.MatchJoinRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.MatchStatusResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/match/status")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMatchStatus(@retrofit2.http.Query(value = "playerId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String playerId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.MatchStatusResponse> $completion);
    
    @retrofit2.http.POST(value = "/api/match/leave")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object leaveMatch(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.example.sheeps.data.model.MatchJoinRequest request, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.GenericResponse> $completion);
    
    @retrofit2.http.GET(value = "/api/leaderboard/daily-popup")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDailyPopup(@retrofit2.http.Header(value = "Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String auth, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.sheeps.data.model.DailyPopupResponse> $completion);
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}