package com.rk.terminal.ui.screens.terminal

import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import com.rk.resources.strings
import com.termux.view.TerminalView
import java.util.Locale

class SearchableTerminalView(context: Context) : TerminalView(context, null) {
    private val findItemId = View.generateViewId()

    override fun startActionMode(callback: ActionMode.Callback): ActionMode? =
        super.startActionMode(localize(callback))

    override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode? =
        super.startActionMode(localize(callback), type)

    private fun localize(callback: ActionMode.Callback): ActionMode.Callback =
        if (callback is LocalizedSelection) callback else LocalizedSelection(callback)

    private inner class LocalizedSelection(private val original: ActionMode.Callback) : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (!original.onCreateActionMode(mode, menu)) return false
            localizeItems(menu)
            addFindItem(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val changed = original.onPrepareActionMode(mode, menu)
            localizeItems(menu)
            addFindItem(menu)
            return changed
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == findItemId) {
                showFindDialog()
                return true
            }
            if (item.title.toString().equals(context.getString(strings.terminal_more), ignoreCase = true) ||
                item.title.toString().equals("More", ignoreCase = true)
            ) {
                val popup = PopupMenu(context, this@SearchableTerminalView)
                val actions = mutableMapOf<Int, MenuItem>()
                for (index in 0 until mode.menu.size()) {
                    val action = mode.menu.getItem(index)
                    if (action.itemId == item.itemId || !action.isVisible) continue
                    popup.menu.add(Menu.NONE, index, Menu.NONE, action.title)
                    actions[index] = action
                }
                popup.setOnMenuItemClickListener { selected ->
                    val action = actions[selected.itemId] ?: return@setOnMenuItemClickListener false
                    onActionItemClicked(mode, action)
                }
                popup.show()
                return true
            }
            return original.onActionItemClicked(mode, item)
        }

        override fun onDestroyActionMode(mode: ActionMode) = original.onDestroyActionMode(mode)

        override fun onGetContentRect(mode: ActionMode, view: android.view.View, outRect: Rect) {
            if (original is ActionMode.Callback2) {
                original.onGetContentRect(mode, view, outRect)
            } else {
                super.onGetContentRect(mode, view, outRect)
            }
        }

        private fun localizeItems(menu: Menu) {
            for (index in 0 until menu.size()) {
                val item = menu.getItem(index)
                val label = when (item.title.toString().lowercase(Locale.ROOT)) {
                    "copy" -> strings.terminal_copy
                    "paste" -> strings.shortcut_paste
                    "select all" -> strings.terminal_select_all
                    "more" -> strings.terminal_more
                    "share" -> strings.terminal_share
                    else -> continue
                }
                item.setTitle(label)
            }
        }

        private fun addFindItem(menu: Menu) {
            if (menu.findItem(findItemId) == null) {
                menu.add(Menu.NONE, findItemId, Menu.NONE, context.getString(strings.terminal_find))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
        }
    }

    fun showFindDialog() {
        val text = mEmulator?.mScreen?.getTranscriptText().orEmpty()
        val lines = text.lines()
        val padding = (16 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, 0, padding, 0)
        }
        val input = EditText(context).apply {
            hint = context.getString(strings.terminal_find)
            isSingleLine = true
        }
        val results = TextView(context)
        val scroll = ScrollView(context).apply {
            addView(results)
        }
        layout.addView(input)
        layout.addView(scroll, LinearLayout.LayoutParams(-1, (240 * resources.displayMetrics.density).toInt()))

        input.doOnTextChanged { query, _, _, _ ->
            val search = query?.toString().orEmpty()
            results.text = if (search.isBlank()) {
                ""
            } else {
                val matches = lines.withIndex().filter { it.value.contains(search, ignoreCase = true) }
                if (matches.isEmpty()) context.getString(strings.terminal_no_matches)
                else context.getString(strings.terminal_match_count, matches.size) + "\n\n" +
                    matches.take(100).joinToString("\n") { "${it.index + 1}: ${it.value.take(200)}" }
            }
        }
        AlertDialog.Builder(context)
            .setTitle(strings.terminal_find)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
