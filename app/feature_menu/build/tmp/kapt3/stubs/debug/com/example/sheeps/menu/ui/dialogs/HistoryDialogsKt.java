package com.example.sheeps.menu.ui.dialogs;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.core.R;
import com.example.sheeps.data.model.ExchangeRecord;
import com.example.sheeps.data.model.PointRecord;
import java.text.SimpleDateFormat;
import java.util.*;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000,\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\u001a$\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0007\u001a$\u0010\u0007\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\b0\u00032\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00010\u0006H\u0007\u001a\u0010\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u0004H\u0003\u001a\u0010\u0010\u000b\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\bH\u0003\u001a\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0002\u00a8\u0006\u0010"}, d2 = {"PointHistoryDialog", "", "history", "", "Lcom/example/sheeps/data/model/PointRecord;", "onDismiss", "Lkotlin/Function0;", "ExchangeHistoryDialog", "Lcom/example/sheeps/data/model/ExchangeRecord;", "PointRecordItem", "record", "ExchangeRecordItem", "formatTimestamp", "", "timestamp", "", "feature_menu_debug"})
public final class HistoryDialogsKt {
    
    /**
     * 积分历史记录对话框
     */
    @androidx.compose.runtime.Composable()
    public static final void PointHistoryDialog(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.PointRecord> history, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    /**
     * 兑换历史记录对话框
     */
    @androidx.compose.runtime.Composable()
    public static final void ExchangeHistoryDialog(@org.jetbrains.annotations.NotNull()
    java.util.List<com.example.sheeps.data.model.ExchangeRecord> history, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PointRecordItem(com.example.sheeps.data.model.PointRecord record) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ExchangeRecordItem(com.example.sheeps.data.model.ExchangeRecord record) {
    }
    
    private static final java.lang.String formatTimestamp(long timestamp) {
        return null;
    }
}