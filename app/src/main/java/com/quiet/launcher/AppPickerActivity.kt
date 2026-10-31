package com.quiet.launcher

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

/** Tick the few apps that are allowed on the home screen. Everything else stays hidden. */
class AppPickerActivity : Activity() {

    companion object {
        const val EXTRA_FIRST_RUN = "first_run"
    }

    private lateinit var prefs: Prefs
    private lateinit var all: List<AppEntry>
    private var shown: List<AppEntry> = emptyList()
    private val selected = LinkedHashSet<String>()
    private lateinit var adapter: Adapter
    private lateinit var count: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        prefs = Prefs(this)
        count = findViewById(R.id.count)

        val firstRun = !prefs.configured
        val installed = Apps.installed(this)
        if (firstRun) {
            installed.filter { Apps.looksSuggested(it) }.forEach { selected.add(it.key) }
        } else {
            val have = installed.map { it.key }.toSet()
            prefs.homeApps.filter { it in have }.forEach { selected.add(it) }
        }
        // Ticked apps first so you can see what's on your home screen at a glance.
        all = installed.sortedWith(compareBy<AppEntry>({ it.key !in selected }, { it.label.lowercase() }))
        shown = all

        adapter = Adapter()
        val list = findViewById<ListView>(R.id.list)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val key = shown[position].key
            if (!selected.remove(key)) selected.add(key)
            adapter.notifyDataSetChanged()
            updateCount()
        }

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                shown = if (q.isEmpty()) all else all.filter { it.label.lowercase().contains(q) }
                adapter.notifyDataSetChanged()
            }
        })

        findViewById<Button>(R.id.done).setOnClickListener { save(firstRun) }
        updateCount()
    }

    private fun updateCount() {
        count.text = "${selected.size} on home screen · everything else hidden"
    }

    private fun save(firstRun: Boolean) {
        // Keep the order you already had; add new picks at the end, alphabetically.
        val old = prefs.homeApps.filter { it in selected }
        val labels = all.associate { it.key to it.label.lowercase() }
        val added = selected.filter { it !in old }.sortedBy { labels[it] ?: it }
        prefs.homeApps = old + added
        prefs.configured = true
        if (firstRun) Toast.makeText(this, R.string.first_run_hint, Toast.LENGTH_LONG).show()
        finish()
    }

    @Deprecated("Back saves, so nothing is lost")
    override fun onBackPressed() {
        save(!prefs.configured)
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = (convertView ?: layoutInflater.inflate(
                android.R.layout.simple_list_item_multiple_choice, parent, false
            )) as CheckedTextView
            val app = shown[position]
            view.text = app.label
            view.isChecked = app.key in selected
            view.setTextColor(getColor(R.color.fg))
            return view
        }
    }
}
