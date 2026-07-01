package com.example.sheeps.game.viewmodel.helpers;

import com.example.sheeps.data.model.Tile;
import javax.inject.Inject;

/**
 * 本地关卡生成器
 * 用于在无网络情况下生成可解的关卡数据
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\u0018\u00002\u00020\u0001:\u0002\u000e\u000fB\t\b\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u0006\u0010\u0007\u001a\u00020\bJ\u0016\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\f\u001a\u00020\rH\u0002\u00a8\u0006\u0010"}, d2 = {"Lcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator;", "", "<init>", "()V", "generateSolvableLevelLocal", "", "Lcom/example/sheeps/data/model/Tile;", "levelId", "", "lcg", "Lkotlin/Function0;", "", "seed", "", "Point3D", "LocalNode", "feature_game_debug"})
public final class GameLevelGenerator {
    
    @javax.inject.Inject()
    public GameLevelGenerator() {
        super();
    }
    
    /**
     * 生成一个保证有解的本地关卡
     * 采用反向生成算法：先生成布局，然后反向填充成对的卡牌类型
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.example.sheeps.data.model.Tile> generateSolvableLevelLocal(int levelId) {
        return null;
    }
    
    /**
     * 简单的线性同余生成器，用于生成确定性的随机序列
     */
    private final kotlin.jvm.functions.Function0<java.lang.Double> lcg(long seed) {
        return null;
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\b\u0082\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0007\u0010\bJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J\'\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u001a\u0010\u0006\u001a\u00020\u0003X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\n\"\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001a"}, d2 = {"Lcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator$LocalNode;", "", "index", "", "coord", "Lcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator$Point3D;", "assignedType", "<init>", "(ILcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator$Point3D;I)V", "getIndex", "()I", "getCoord", "()Lcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator$Point3D;", "getAssignedType", "setAssignedType", "(I)V", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "toString", "", "feature_game_debug"})
    static final class LocalNode {
        private final int index = 0;
        @org.jetbrains.annotations.NotNull()
        private final com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator.Point3D coord = null;
        private int assignedType;
        
        public LocalNode(int index, @org.jetbrains.annotations.NotNull()
        com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator.Point3D coord, int assignedType) {
            super();
        }
        
        public final int getIndex() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator.Point3D getCoord() {
            return null;
        }
        
        public final int getAssignedType() {
            return 0;
        }
        
        public final void setAssignedType(int p0) {
        }
        
        public final int component1() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator.Point3D component2() {
            return null;
        }
        
        public final int component3() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator.LocalNode copy(int index, @org.jetbrains.annotations.NotNull()
        com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator.Point3D coord, int assignedType) {
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
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\b\u0082\b\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0004\b\u0007\u0010\bJ\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0006H\u00c6\u0003J\'\u0010\u0011\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0012\u001a\u00020\u00132\b\u0010\u0014\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0015\u001a\u00020\u0006H\u00d6\u0001J\t\u0010\u0016\u001a\u00020\u0017H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\nR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0018"}, d2 = {"Lcom/example/sheeps/game/viewmodel/helpers/GameLevelGenerator$Point3D;", "", "x", "", "y", "z", "", "<init>", "(FFI)V", "getX", "()F", "getY", "getZ", "()I", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "toString", "", "feature_game_debug"})
    static final class Point3D {
        private final float x = 0.0F;
        private final float y = 0.0F;
        private final int z = 0;
        
        public Point3D(float x, float y, int z) {
            super();
        }
        
        public final float getX() {
            return 0.0F;
        }
        
        public final float getY() {
            return 0.0F;
        }
        
        public final int getZ() {
            return 0;
        }
        
        public final float component1() {
            return 0.0F;
        }
        
        public final float component2() {
            return 0.0F;
        }
        
        public final int component3() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.example.sheeps.game.viewmodel.helpers.GameLevelGenerator.Point3D copy(float x, float y, int z) {
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
}