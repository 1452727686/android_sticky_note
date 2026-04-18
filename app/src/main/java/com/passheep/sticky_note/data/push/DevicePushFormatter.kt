package com.passheep.sticky_note.data.push

import com.passheep.sticky_note.core.model.RepeatType
import com.passheep.sticky_note.core.model.Todo
import java.time.format.DateTimeFormatter

internal fun formatTodosAsText(todos: List<Todo>): String {
    if (todos.isEmpty()) {
        return "智能便利贴\n\n当前没有待办。"
    }

    return buildString {
        appendLine("智能便利贴")
        appendLine()
        todos.forEachIndexed { index, todo ->
            append(index + 1)
            append(". ")
            append(todo.title)
            val meta = todo.metaLine()
            if (meta.isNotBlank()) {
                append("  [")
                append(meta)
                append(']')
            }
            appendLine()
            if (todo.description.isNotBlank()) {
                appendLine(todo.description.trim())
            }
            appendLine()
        }
    }.trim()
}

internal fun formatTodosAsStructured(todos: List<Todo>): Pair<String, String> {
    if (todos.isEmpty()) {
        return "智能便利贴" to "当前没有待办。"
    }

    val title = "智能便利贴 · ${todos.size} 条待办"
    val body = buildString {
        todos.forEach { todo ->
            append("• ")
            append(todo.title)
            val meta = todo.metaLine()
            if (meta.isNotBlank()) {
                append("  ")
                append(meta)
            }
            appendLine()
            if (todo.description.isNotBlank()) {
                append("  ")
                appendLine(todo.description.trim())
            }
        }
    }.trim()
    return title to body
}

private fun Todo.metaLine(): String = buildList {
    add(priorityLabel())
    dueDate?.let { date ->
        val dueText = buildString {
            append(date.format(DateTimeFormatter.ofPattern("MM-dd")))
            dueTime?.let {
                append(' ')
                append(it.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
        }
        add(dueText)
    }
    if (repeatType != RepeatType.NONE) {
        add(repeatLabel())
    }
}.joinToString(" · ")

private fun Todo.priorityLabel(): String = when (priority) {
    2 -> "紧急"
    1 -> "重要"
    else -> "普通"
}

private fun Todo.repeatLabel(): String = when (repeatType) {
    RepeatType.NONE -> ""
    RepeatType.DAILY -> "每天"
    RepeatType.WEEKLY -> "每周${listOf("日", "一", "二", "三", "四", "五", "六").getOrNull(repeatWeekday ?: -1) ?: ""}"
    RepeatType.MONTHLY -> "每月${repeatDay ?: "-"}号"
    RepeatType.YEARLY -> "每年${repeatMonth ?: "-"}月${repeatDay ?: "-"}日"
}
