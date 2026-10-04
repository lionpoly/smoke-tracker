package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AiAdviceResult
import com.example.ui.AiAdviceSource
import com.example.ui.AiAdviceState
import com.example.ui.AiCoachCard
import com.example.ui.AiCoachPersona
import com.example.ui.AiCustomConfig
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AiCoachLayoutTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun narrowCardWithLongModelName() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Box(Modifier.width(280.dp).testTag("narrow-container")) {
                    AiCoachCard(
                        aiAdviceState = AiAdviceState.Success(AiAdviceResult(
                            advice = "试着延后下一支。\n- 喝水并短暂散步，观察想抽烟的时刻。",
                            source = AiAdviceSource.OPENAI_COMPATIBLE,
                            modelName = "a-very-long-model-name-for-small-screen-preview",
                            persona = AiCoachPersona.ANALYTICAL
                        )),
                        aiCustomConfig = AiCustomConfig(),
                        lang = AppLanguage.ZH,
                        onRefresh = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
        val container = composeTestRule.onNodeWithTag("narrow-container").fetchSemanticsNode().boundsInRoot
        listOf(
            composeTestRule.onNodeWithText("a-very-long-model-name", substring = true),
            composeTestRule.onNodeWithText("理性数据型", substring = true),
            composeTestRule.onNodeWithContentDescription("刷新 AI 建议")
        ).forEach { node ->
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("Advice controls should stay inside the card", bounds.left >= container.left && bounds.right <= container.right)
        }
    }
}
