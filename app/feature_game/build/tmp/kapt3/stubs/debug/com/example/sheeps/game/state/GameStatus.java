package com.example.sheeps.game.state;

import com.example.sheeps.data.model.RankingEntry;
import com.example.sheeps.data.model.Tile;

/**
 * 游戏进行状态枚举。
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lcom/example/sheeps/game/state/GameStatus;", "", "<init>", "(Ljava/lang/String;I)V", "INIT", "MENU", "PLAYING", "WON", "LOST", "feature_game_debug"})
public enum GameStatus {
    /*public static final*/ INIT /* = new INIT() */,
    /*public static final*/ MENU /* = new MENU() */,
    /*public static final*/ PLAYING /* = new PLAYING() */,
    /*public static final*/ WON /* = new WON() */,
    /*public static final*/ LOST /* = new LOST() */;
    
    GameStatus() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.example.sheeps.game.state.GameStatus> getEntries() {
        return null;
    }
}