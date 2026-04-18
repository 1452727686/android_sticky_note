package com.passheep.sticky_note.data.push

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.passheep.sticky_note.core.model.Todo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoImageRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun renderTodosToImage(todos: List<Todo>): File {
        val width = 800
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 24f
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 20f
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        var y = 56f
        canvas.drawText("智能便利贴", 36f, y, titlePaint)
        y += 36f
        canvas.drawLine(36f, y, width - 36f, y, dividerPaint)
        y += 34f

        if (todos.isEmpty()) {
            canvas.drawText("当前没有待办。", 36f, y, textPaint)
        } else {
            todos.take(10).forEachIndexed { index, todo ->
                if (y > height - 48f) return@forEachIndexed
                canvas.drawText("${index + 1}. ${todo.title}", 36f, y, textPaint)
                y += 28f
                val meta = buildString {
                    append(todo.priorityLabel())
                    todo.dueDate?.let { date ->
                        append("  ")
                        append(date.format(DateTimeFormatter.ofPattern("MM-dd")))
                        todo.dueTime?.let {
                            append(' ')
                            append(it.format(DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }
                if (meta.isNotBlank()) {
                    canvas.drawText(meta, 58f, y, metaPaint)
                    y += 24f
                }
                if (todo.description.isNotBlank()) {
                    val snippet = todo.description.trim().replace('\n', ' ').take(38)
                    canvas.drawText(snippet, 58f, y, metaPaint)
                    y += 24f
                }
                y += 12f
            }
        }

        val file = File(context.cacheDir, "todo_push_snapshot.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file
    }

    private fun Todo.priorityLabel(): String = when (priority) {
        2 -> "紧急"
        1 -> "重要"
        else -> "普通"
    }
}
