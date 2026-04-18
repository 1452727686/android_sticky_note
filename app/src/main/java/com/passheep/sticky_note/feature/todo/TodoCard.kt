@file:OptIn(ExperimentalLayoutApi::class)

package com.passheep.sticky_note.feature.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.passheep.sticky_note.core.model.ConflictState
import com.passheep.sticky_note.core.model.RemoteDeletedResolution
import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.SyncState
import com.passheep.sticky_note.core.model.Todo
import java.time.format.DateTimeFormatter

@Composable
fun TodoCard(
    todo: Todo,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onResolveRemoteDeleted: (RemoteDeletedResolution) -> Unit,
) {
    val titleColor by animateColorAsState(
        targetValue = if (todo.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(220),
        label = "todoTitleColor",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                CompletionOrb(
                    completed = todo.completed,
                    onClick = onToggle,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedStrikeText(
                        text = todo.title,
                        completed = todo.completed,
                        color = titleColor,
                    )
                    if (todo.description.isNotBlank()) {
                        Text(
                            text = todo.description,
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.pressScale()) {
                    Icon(Icons.Rounded.Edit, contentDescription = "编辑")
                }
            }

            FlowRow(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(todo.priority.label()) },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = todo.priority.color().copy(alpha = 0.14f),
                        disabledLabelColor = todo.priority.color(),
                    ),
                )
                todo.dueDate?.let {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                it.format(DateTimeFormatter.ofPattern("MM.dd")) + todo.dueTimeSuffix(),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            disabledLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
                if (todo.repeatType != RepeatType.NONE) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(todo.repeatType.label(todo.repeatWeekday, todo.repeatMonth, todo.repeatDay)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.EventRepeat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                            disabledLabelColor = MaterialTheme.colorScheme.secondary,
                        ),
                    )
                }
                when {
                    todo.syncState == SyncState.PENDING -> MetaChip("待同步", MaterialTheme.colorScheme.primary)
                    todo.syncState == SyncState.FAILED -> MetaChip("同步失败", Color(0xFFEE6B5A))
                    todo.conflictState == ConflictState.REMOTE_DELETED -> MetaChip("云端已删除", Color(0xFFF2A94A))
                }
            }

            AnimatedVisibility(
                visible = todo.syncState == SyncState.FAILED && !todo.lastSyncError.isNullOrBlank(),
            ) {
                Text(
                    text = "同步失败：${todo.lastSyncError.orEmpty()}",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEE6B5A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AnimatedVisibility(visible = todo.conflictState == ConflictState.REMOTE_DELETED) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    Text(
                        text = "云端这条待办已经被删除了。你可以只删本地，也可以恢复成新的云端待办。",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = { onResolveRemoteDeleted(RemoteDeletedResolution.DELETE_LOCAL) },
                            modifier = Modifier.pressScale(),
                        ) {
                            Text("删除本地")
                        }
                        Button(
                            onClick = { onResolveRemoteDeleted(RemoteDeletedResolution.RESTORE_REMOTE) },
                            modifier = Modifier.pressScale(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("恢复云端")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.pressScale(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun CompletionOrb(completed: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.92f,
        animationSpec = tween(180),
        label = "completionScale",
    )
    val interactionSource = rememberPressInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    Surface(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = if (pressed) 0.98f else scale
                scaleY = if (pressed) 0.98f else scale
            },
        shape = CircleShape,
        color = if (completed) {
            Color(0xFF46C18F).copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (completed) "标记未完成" else "标记完成",
                tint = if (completed) Color(0xFF46C18F) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AnimatedStrikeText(text: String, completed: Boolean, color: Color) {
    var textSize by remember { mutableStateOf(IntSize.Zero) }
    val progress by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(220),
        label = "strikeProgress",
    )
    Box(modifier = Modifier.onSizeChanged { textSize = it }) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        if (textSize != IntSize.Zero) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val y = size.height * 0.56f
                drawLine(
                    color = Color(0xFFB6B6B6),
                    start = Offset(0f, y),
                    end = Offset(size.width * progress, y),
                    strokeWidth = 2.6.dp.toPx(),
                )
            }
        }
    }
}

@Composable
private fun MetaChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

private fun Int.label(): String = when (this) {
    2 -> "紧急"
    1 -> "重要"
    else -> "普通"
}

private fun Int.color(): Color = when (this) {
    2 -> Color(0xFFEE6B5A)
    1 -> Color(0xFFF2A94A)
    else -> Color(0xFF6AA7FF)
}

private fun RepeatType.label(weekday: Int?, month: Int?, day: Int?): String = when (this) {
    RepeatType.NONE -> "不重复"
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周${listOf("日", "一", "二", "三", "四", "五", "六").getOrNull(weekday ?: -1) ?: ""}"
    RepeatType.MONTHLY -> "每月 ${day ?: "-"} 号"
    RepeatType.YEARLY -> "每年 ${month ?: "-"} 月 ${day ?: "-"} 日"
}

private fun Todo.dueTimeSuffix(): String =
    dueTime?.let { "  ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}" }.orEmpty()
