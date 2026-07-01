package com.example.sheeps.game.ui.components;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import com.example.sheeps.game.state.DuelViewState;

@kotlin.Metadata(mv = {2, 2, 0}, k = 2, xi = 48, d1 = {"\u0000:\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a$\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a@\u0010\u0007\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00010\u00102\b\b\u0002\u0010\u0011\u001a\u00020\u0012H\u0003\u00a8\u0006\u0013"}, d2 = {"DuelSpellPanel", "", "state", "Lcom/example/sheeps/game/state/DuelViewState;", "onCastSpell", "Lkotlin/Function1;", "", "SpellButton", "info", "Lcom/example/sheeps/game/ui/components/SpellInfo;", "currentEnergy", "", "isUsed", "", "isSilenced", "onClick", "Lkotlin/Function0;", "modifier", "Landroidx/compose/ui/Modifier;", "feature_game_release"})
public final class DuelSpellPanelKt {
    
    /**
     * 对决法术/大招面板
     * 展示可用的恶搞技能，消耗能量释放
     */
    @androidx.compose.runtime.Composable()
    public static final void DuelSpellPanel(@org.jetbrains.annotations.NotNull()
    com.example.sheeps.game.state.DuelViewState state, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onCastSpell) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SpellButton(com.example.sheeps.game.ui.components.SpellInfo info, int currentEnergy, boolean isUsed, boolean isSilenced, kotlin.jvm.functions.Function0<kotlin.Unit> onClick, androidx.compose.ui.Modifier modifier) {
    }
}