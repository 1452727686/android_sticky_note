package com.passheep.sticky_note.widget

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.passheep.sticky_note.core.model.Todo
import com.passheep.sticky_note.core.settings.ThemeMode
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

private val WidgetBeijingZoneId: ZoneId = ZoneId.of("Asia/Shanghai")

class StickyNoteGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val fetchedTodos = entryPoint.cloudTodoStore().snapshotTodos(limit = 50)
            .map { it.toStoredWidgetTodoItem() }
        val settings = entryPoint.settingsRepository().settings.first()
        val lastSyncAt = settings.lastSyncAtMillis
            ?: if (fetchedTodos.isNotEmpty()) System.currentTimeMillis() else null
        val todoPreview = fetchedTodos.take(5)
            .joinToString(separator = ",") { "${it.remoteId}:${it.completed}" }
        Log.e(
            TAG,
            "provideGlance fetched=${fetchedTodos.size} preview=[$todoPreview] lastSync=$lastSyncAt dark=${settings.themeMode} alpha=${settings.widgetTransparency} deviceId=${settings.selectedDeviceId}",
        )
        val snapshot = WidgetSnapshot(
            todos = fetchedTodos,
            isDark = resolveDarkMode(context, settings.themeMode),
            hasRequiredSyncConfig = settings.platformEnabled && settings.apiKey.isNotBlank(),
            backgroundAlpha = 1f - settings.widgetTransparency.coerceIn(0f, 0.92f),
            lastSyncLabel = lastSyncAt.toWidgetLastSyncLabel(),
            renderToken = System.currentTimeMillis(),
        )

        provideContent {
            StickyNoteWidgetContent(snapshot = snapshot)
        }
    }

    private fun resolveDarkMode(context: Context, themeMode: ThemeMode): Boolean = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> {
            val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMode == Configuration.UI_MODE_NIGHT_YES
        }
    }
}

@androidx.compose.runtime.Composable
private fun StickyNoteWidgetContent(snapshot: WidgetSnapshot) {
    val chromeAlpha = (0.35f + snapshot.backgroundAlpha * 0.65f).coerceIn(0.35f, 1f)
    val colors = if (snapshot.isDark) {
        WidgetColors(
            background = ComposeColor(0xFF0B0F17).copy(alpha = snapshot.backgroundAlpha),
            actionBackground = ComposeColor(0xFF121A28).copy(alpha = chromeAlpha),
            divider = ComposeColor(0xFF2A3344).copy(alpha = chromeAlpha),
            accent = ComposeColor(0xFF7CC6FF),
            textPrimary = ComposeColor(0xFFF7F9FC),
            textSecondary = ComposeColor(0xFF9AA4B2),
            pending = ComposeColor(0xFFB45309),
            completed = ComposeColor(0xFF047857),
        )
    } else {
        WidgetColors(
            background = ComposeColor(0xFFFFFFFF).copy(alpha = snapshot.backgroundAlpha),
            actionBackground = ComposeColor(0xFFF8FAFC).copy(alpha = chromeAlpha),
            divider = ComposeColor(0xFFE5E7EB).copy(alpha = chromeAlpha),
            accent = ComposeColor(0xFF1D7CF2),
            textPrimary = ComposeColor(0xFF111827),
            textSecondary = ComposeColor(0xFF6B7280),
            pending = ComposeColor(0xFFB45309),
            completed = ComposeColor(0xFF047857),
        )
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(fixedColor(colors.background))
            .cornerRadius(28.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        WidgetHeader(
            lastSyncLabel = snapshot.lastSyncLabel,
            colors = colors,
        )
        if (snapshot.todos.isEmpty()) {
            WidgetEmptyState(
                hasRequiredSyncConfig = snapshot.hasRequiredSyncConfig,
                colors = colors,
            )
        } else {
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize(),
            ) {
                items(snapshot.todos, itemId = { todo -> todo.glanceItemId(snapshot.renderToken) }) { todo ->
                    WidgetTodoRow(todo = todo, colors = colors)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetHeader(
    lastSyncLabel: String,
    colors: WidgetColors,
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 0.dp),
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                WidgetHeaderIcon(
                    text = "+",
                    onClick = actionRunCallback<OpenQuickCreateAction>(),
                    colors = colors,
                    filled = true,
                )
            }
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "上次更新 $lastSyncLabel",
                        modifier = GlanceModifier
                            .padding(start = 3.dp, top = 2.dp)
                            .clickable(actionRunCallback<OpenHomeAction>()),
                        maxLines = 1,
                        style = TextStyle(
                            color = fixedColor(colors.textSecondary),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(2.dp))
                    WidgetHeaderIcon(
                        text = "↻",
                        onClick = actionRunCallback<RefreshWidgetAction>(),
                        colors = colors,
                    )
                }
            }
        }
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(fixedColor(colors.divider)),
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
    }
}

@androidx.compose.runtime.Composable
private fun WidgetHeaderIcon(
    text: String,
    onClick: androidx.glance.action.Action,
    colors: WidgetColors,
    filled: Boolean = false,
) {
    Box(
        modifier = GlanceModifier
            .size(28.dp)
            .padding(bottom = 2.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = fixedColor(if (filled) colors.accent else colors.textSecondary),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun WidgetEmptyState(
    hasRequiredSyncConfig: Boolean,
    colors: WidgetColors,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 6.dp, end = 4.dp),
    ) {
        Text(
            text = if (hasRequiredSyncConfig) "暂无待办" else "请先配置平台",
            style = TextStyle(
                color = fixedColor(colors.textPrimary),
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = if (hasRequiredSyncConfig) {
                "点击刷新后会重新拉取云端待办"
            } else {
                "先在设置中心填写平台地址和 API 密钥"
            },
            style = TextStyle(color = fixedColor(colors.textSecondary)),
            maxLines = 2,
        )
    }
}

@androidx.compose.runtime.Composable
private fun WidgetTodoRow(
    todo: StoredWidgetTodoItem,
    colors: WidgetColors,
) {
    val titleColor = if (todo.completed) colors.completed else colors.pending
    val toggleAction = todo.remoteId?.let { remoteId ->
        actionRunCallback<ToggleTodoAction>(
            actionParametersOf(WidgetActionKeys.RemoteTodoId to remoteId.toString()),
        )
    }
    val openAction = actionRunCallback<OpenTodoAction>(
        actionParametersOf(WidgetActionKeys.TodoId to todo.localId),
    )

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(24.dp)
                .let { base ->
                    if (toggleAction != null) base.clickable(toggleAction) else base
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (todo.completed) "●" else "○",
                style = TextStyle(
                    color = fixedColor(titleColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(openAction),
        ) {
            Text(
                text = todo.title,
                style = TextStyle(
                    color = fixedColor(titleColor),
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (todo.completed) TextDecoration.LineThrough else TextDecoration.None,
                ),
                maxLines = 1,
            )
        }
    }
}

data class StoredWidgetTodoItem(
    val localId: String,
    val remoteId: Long? = null,
    val title: String,
    val completed: Boolean,
)

private data class WidgetSnapshot(
    val todos: List<StoredWidgetTodoItem>,
    val isDark: Boolean,
    val hasRequiredSyncConfig: Boolean,
    val backgroundAlpha: Float,
    val lastSyncLabel: String,
    val renderToken: Long,
)

private data class WidgetColors(
    val background: ComposeColor,
    val actionBackground: ComposeColor,
    val divider: ComposeColor,
    val accent: ComposeColor,
    val textPrimary: ComposeColor,
    val textSecondary: ComposeColor,
    val pending: ComposeColor,
    val completed: ComposeColor,
)

private fun Long?.toWidgetLastSyncLabel(): String {
    if (this == null) return "暂未同步"
    return Instant.ofEpochMilli(this)
        .atZone(WidgetBeijingZoneId)
        .toLocalDateTime()
        .format(WidgetLastSyncFormatter)
}

private val WidgetLastSyncFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun fixedColor(color: ComposeColor) = ColorProvider(day = color, night = color)

private const val TAG = "StickyNoteWidgetUi"

private fun Todo.toStoredWidgetTodoItem(): StoredWidgetTodoItem = StoredWidgetTodoItem(
    localId = localId,
    remoteId = remoteId,
    title = title,
    completed = completed,
)

private fun StoredWidgetTodoItem.glanceItemId(renderToken: Long): Long {
    val stableBase = (remoteId ?: localId.hashCode().toLong()) and Long.MAX_VALUE
    val completedBit = if (completed) 1L else 0L
    return (stableBase shl 1) xor completedBit xor (renderToken and 0x3FL)
}
