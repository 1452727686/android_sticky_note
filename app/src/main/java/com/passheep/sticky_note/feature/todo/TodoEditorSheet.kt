package com.passheep.sticky_note.feature.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.passheep.sticky_note.core.model.RepeatType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodoEditorSheet(
    state: TodoEditorState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onStateChange: (TodoEditorState) -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = if (state.isEditing) "编辑待办" else "新建待办",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "保存后会立刻写入本地，后台再异步同步。",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = { onStateChange(state.copy(title = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                label = { Text("标题") },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { onStateChange(state.copy(description = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                label = { Text("描述") },
                minLines = 3,
                maxLines = 5,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = state.dueDateText,
                    onValueChange = { onStateChange(state.copy(dueDateText = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("日期") },
                    placeholder = { Text("2026-04-16") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.dueTimeText,
                    onValueChange = { onStateChange(state.copy(dueTimeText = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("时间") },
                    placeholder = { Text("09:30") },
                    singleLine = true,
                )
            }

            Text(
                text = "优先级",
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (0..2).forEach { priority ->
                    FilterChip(
                        selected = state.priority == priority,
                        onClick = { onStateChange(state.copy(priority = priority)) },
                        label = { Text(priority.priorityLabel()) },
                        modifier = Modifier.pressScale(),
                    )
                }
            }

            Text(
                text = "重复规则",
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RepeatType.entries.forEach { repeatType ->
                    FilterChip(
                        selected = state.repeatType == repeatType,
                        onClick = {
                            onStateChange(
                                state.copy(
                                    repeatType = repeatType,
                                    repeatWeekday = if (repeatType == RepeatType.WEEKLY) state.repeatWeekday else null,
                                    repeatMonthText = if (repeatType == RepeatType.YEARLY) state.repeatMonthText else "",
                                    repeatDayText = if (repeatType == RepeatType.MONTHLY || repeatType == RepeatType.YEARLY) state.repeatDayText else "",
                                ),
                            )
                        },
                        label = { Text(repeatType.editorLabel()) },
                        modifier = Modifier.pressScale(),
                    )
                }
            }

            AnimatedVisibility(visible = state.repeatType == RepeatType.WEEKLY) {
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val weekdayLabels = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                    weekdayLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = state.repeatWeekday == index,
                            onClick = { onStateChange(state.copy(repeatWeekday = index)) },
                            label = { Text(label) },
                            modifier = Modifier.pressScale(),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.repeatType == RepeatType.MONTHLY || state.repeatType == RepeatType.YEARLY,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.repeatType == RepeatType.YEARLY) {
                        OutlinedTextField(
                            value = state.repeatMonthText,
                            onValueChange = { onStateChange(state.copy(repeatMonthText = it.filter(Char::isDigit))) },
                            modifier = Modifier.weight(1f),
                            label = { Text("月份") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = state.repeatDayText,
                        onValueChange = { onStateChange(state.copy(repeatDayText = it.filter(Char::isDigit))) },
                        modifier = Modifier.weight(1f),
                        label = { Text("日期") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(54.dp)
                    .pressScale(),
            ) {
                Text(if (isSaving) "保存中..." else "保存")
            }
        }
    }
}

private fun Int.priorityLabel(): String = when (this) {
    2 -> "紧急"
    1 -> "重要"
    else -> "普通"
}

private fun RepeatType.editorLabel(): String = when (this) {
    RepeatType.NONE -> "不重复"
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周"
    RepeatType.MONTHLY -> "每月"
    RepeatType.YEARLY -> "每年"
}
