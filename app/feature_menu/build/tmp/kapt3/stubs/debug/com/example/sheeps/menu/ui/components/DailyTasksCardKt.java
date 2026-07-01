package com.example.sheeps.menu.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.core.R;
import com.example.sheeps.menu.state.MenuViewState;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000 \n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\u001a$\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a$\u0010\u0007\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\t2\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u00a8\u0006\n"}, d2 = {"DailyTasksCard", "", "state", "Lcom/example/sheeps/menu/state/MenuViewState;", "onClaimTask", "Lkotlin/Function1;", "", "TaskItem", "task", "Lcom/example/sheeps/data/model/DailyTask;", "feature_menu_debug"})
public final class DailyTasksCardKt {
    
    /**
     * 每日任务卡片组件
     * 展示用户的每日任务列表、进度以及领取奖励按钮
     *
     * @param state 界面状态数据，包含任务列表
     * @param onClaimTask 领取任务奖励的回调
     */
    @androidx.compose.runtime.Composable()
    public static final void DailyTasksCard(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.menu.state.MenuViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onClaimTask) {
    }
    
    /**
     * 单个任务条目组件
     */
    @androidx.compose.runtime.Composable()
    private static final void TaskItem(com.example.sheeps.data.model.DailyTask task, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onClaimTask) {
    }
}