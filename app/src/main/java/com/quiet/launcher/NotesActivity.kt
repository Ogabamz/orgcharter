package com.quiet.launcher

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

/** Jot a thought down without opening any app. Swipe down on the home screen to get here. */
class NotesActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var input: EditText
    private lateinit var adapter: NotesAdapter
    private var notes: List<Note> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val pad = Ui.dp(this, 20)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.bg))
            fitsSystemWindows = true
            setPadding(pad, pad, pad, pad)
        }
        root.addView(Ui.text(this, "Notes", 28f, face = Ui.serifLight(this)))

        input = EditText(this).apply {
            hint = "Write it down, then get back to what matters."
            setHintTextColor(getColor(R.color.dim))
            setTextColor(getColor(R.color.fg))
            typeface = Ui.serif(this@NotesActivity)
            textSize = 18f
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        root.addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = Ui.dp(this, 12)
        })

        root.addView(Button(this).apply {
            text = "Save note"
            setOnClickListener { saveDraft() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val list = ListView(this).apply {
            divider = getDrawable(R.color.line)
            dividerHeight = 1
        }
        adapter = NotesAdapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, pos, _ -> edit(pos) }
        root.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = Ui.dp(this, 12)
        })

        setContentView(root)
        input.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onPause() {
        super.onPause()
        saveDraft(quiet = true)
    }

    private fun reload() {
        notes = prefs.notes.sortedByDescending { it.at }
        adapter.notifyDataSetChanged()
    }

    private fun saveDraft(quiet: Boolean = false) {
        val t = input.text.toString().trim()
        if (t.isEmpty()) return
        prefs.notes = prefs.notes + Note(t, System.currentTimeMillis())
        input.setText("")
        if (!quiet) reload()
    }

    private fun edit(pos: Int) {
        val note = notes[pos]
        val box = EditText(this).apply {
            setText(note.text)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val wrap = LinearLayout(this).apply {
            val p = Ui.dp(this@NotesActivity, 20)
            setPadding(p, Ui.dp(this@NotesActivity, 8), p, 0)
            addView(box, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setView(wrap)
            .setPositiveButton("Save") { _, _ ->
                val t = box.text.toString().trim()
                prefs.notes = prefs.notes.map { if (it == note) Note(t, it.at) else it }.filter { it.text.isNotEmpty() }
                reload()
            }
            .setNeutralButton("Delete") { _, _ ->
                prefs.notes = prefs.notes.filter { it != note }
                reload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private inner class NotesAdapter : BaseAdapter() {
        override fun getCount() = notes.size
        override fun getItem(position: Int) = notes[position]
        override fun getItemId(position: Int) = notes[position].at

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val ctx = this@NotesActivity
            val row = (convertView as? LinearLayout) ?: LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                val p = Ui.dp(ctx, 12)
                setPadding(0, p, 0, p)
                addView(Ui.text(ctx, "", 17f))
                addView(Ui.text(ctx, "", 12f, R.color.dim))
            }
            val n = notes[position]
            (row.getChildAt(0) as TextView).text = n.text
            (row.getChildAt(1) as TextView).text =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(n.at))
            return row
        }
    }
}
