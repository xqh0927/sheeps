package com.example.sheeps.core.startup;

import android.content.Context;
import androidx.startup.Initializer;
import com.tencent.mmkv.MMKV;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0003\u0010\u0004J\u0010\u0010\u0005\u001a\u00020\u00022\u0006\u0010\u0006\u001a\u00020\u0007H\u0016J\u001a\u0010\b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\b\u0001\u0012\u0006\u0012\u0002\b\u00030\u00010\n0\tH\u0016\u00a8\u0006\u000b"}, d2 = {"Lcom/example/sheeps/core/startup/MmkvInitializer;", "Landroidx/startup/Initializer;", "", "<init>", "()V", "create", "context", "Landroid/content/Context;", "dependencies", "", "Ljava/lang/Class;", "core_release"})
public final class MmkvInitializer implements androidx.startup.Initializer<kotlin.Unit> {
    
    public MmkvInitializer() {
        super();
    }
    
    @java.lang.Override()
    public void create(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.util.List<java.lang.Class<? extends androidx.startup.Initializer<?>>> dependencies() {
        return null;
    }
}