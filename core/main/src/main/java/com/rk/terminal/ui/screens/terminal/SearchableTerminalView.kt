package com.rk.terminal.ui.screens.terminal

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import com.blankj.utilcode.util.ClipboardUtils
import com.rk.resources.strings
import com.termux.view.TerminalView

class SearchableTerminalView(context: Context) : FrameLayout(context) {
    val terminal = TerminalView(context, null).also {
        addView(it, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
    private val findItemId = 0x10000000
    private val selectAllItemId = 0x10000001
    private val shareItemId = 0x10000002

    override fun startActionModeForChild(originalView: View, callback: ActionMode.Callback): ActionMode? =
        super.startActionModeForChild(originalView, localize(callback))

    override fun startActionModeForChild(originalView: View, callback: ActionMode.Callback, type: Int): ActionMode? =
        super.startActionModeForChild(originalView, localize(callback), type)

    private fun localize(callback: ActionMode.Callback): ActionMode.Callback =
        if (callback is LocalizedSelection) callback else LocalizedSelection(callback)

    private inner class LocalizedSelection(private val original: ActionMode.Callback) : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (!original.onCreateActionMode(mode, menu)) return false
            localizeItems(menu)
            addSelectionItems(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val changed = original.onPrepareActionMode(mode, menu)
            localizeItems(menu)
            addSelectionItems(menu)
            return changed
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == findItemId) {
                showFindDialog()
                return true
            }
            if (item.itemId == selectAllItemId) {
                terminal.stopTextSelectionMode()
                showSelectAllDialog()
                return true
            }
            if (item.itemId == shareItemId) {
                val selected = terminal.getSelectedText().orEmpty()
                terminal.stopTextSelectionMode()
                shareText(selected)
                return true
            }
            if (item.itemId == 3) {
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
                val label = when (item.itemId) {
                    1 -> strings.terminal_copy
                    2 -> strings.shortcut_paste
                    3 -> strings.terminal_more
                    else -> continue
                }
                item.setTitle(label)
            }
        }

        private fun addSelectionItems(menu: Menu) {
            if (menu.findItem(findItemId) == null) {
                menu.add(Menu.NONE, findItemId, Menu.NONE, context.getString(strings.terminal_find))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
            if (menu.findItem(selectAllItemId) == null) {
                menu.add(Menu.NONE, selectAllItemId, Menu.NONE, context.getString(strings.terminal_select_all))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
            if (menu.findItem(shareItemId) == null) {
                menu.add(Menu.NONE, shareItemId, Menu.NONE, context.getString(strings.terminal_share))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            }
        }
    }

    private fun showSelectAllDialog() {
        val text = terminal.mEmulator?.getScreen()?.getTranscriptText().orEmpty()
        if (text.isEmpty()) return
        val selection = EditText(context).apply {
            setText(text)
            setTextIsSelectable(true)
            showSoftInputOnFocus = false
            minLines = 5
            maxLines = 12
            setSelection(0, text.length)
        }
        AlertDialog.Builder(context)
            .setTitle(strings.terminal_select_all)
            .setView(selection)
            .setPositiveButton(strings.terminal_copy) { _, _ ->
                ClipboardUtils.copyText("Terminal", selectedText(selection))
            }
            .setNeutralButton(strings.terminal_share) { _, _ -> shareText(selectedText(selection)) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun selectedText(selection: EditText): String {
        val start = selection.selectionStart.coerceAtLeast(0)
        val end = selection.selectionEnd.coerceAtLeast(0)
        return selection.text.substring(minOf(start, end), maxOf(start, end))
    }

    private fun shareText(text: String) {
        if (text.isEmpty()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(strings.terminal_share)))
    }

    fun showFindDialog() {
        val text = terminal.mEmulator?.getScreen()?.getTranscriptText().orEmpty()
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
