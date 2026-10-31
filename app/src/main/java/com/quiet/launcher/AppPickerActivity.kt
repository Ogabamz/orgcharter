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

/**
 * Tick apps for one of three lists:
 *  - HOME: the few apps on your home screen (YouTube and Chrome are never offered)
 *  - BEDTIME: the only apps shown during bedtime
 *  - DISTRACTING: apps that get a pause screen before opening
 */
class AppPickerActivity : Activity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val HOME = "home"
        const val BEDTIME = "bedtime"
        const val DISTRACTING = "distracting"
    }

    private lateinit var prefs: Prefs
    private lateinit var mode: String
    private lateinit var all: List<AppEntry>
    private var shown: List<AppEntry> = emptyList()
    private val selected = LinkedHashSet<String>()
    private lateinit var adapter: Adapter
    private lateinit var count: TextView
    private var firstRun = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        prefs = Prefs(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: HOME
        count = findViewById(R.id.count)
        firstRun = mode == HOME && !prefs.configured

        val everything = Apps.installed(this)
        val installed = when (mode) {
            DISTRACTING -> everything.distinctBy { it.packageName }
            else -> everything.filter { !Apps.isHidden(it.packageName) }
        }

        when (mode) {
            HOME -> {
                if (firstRun) installed.filter { Apps.looksSuggested(it) }.forEach { selected.add(it.key) }
                else {
                    val have = installed.map { it.key }.toSet()
                    prefs.homeApps.filter { it in have }.forEach { selected.add(it) }
                }
            }
            BEDTIME -> {
                val have = installed.map { it.key }.toSet()
                Apps.bedtimeApps(this, prefs, everything).filter { it in have }.forEach { selected.add(it) }
            }
            DISTRACTING -> {
                val pkgs = Apps.distracting(this, prefs)
                installed.filter { it.packageName in pkgs }.forEach { selected.add(it.key) }
            }
        }

        findViewById<TextView>(R.id.title).text = when (mode) {
            BEDTIME -> "Bedtime apps"
            DISTRACTING -> "Pause before opening"
            else -> getString(R.string.choose_apps)
        }

        // Ticked apps first so you can see the current list at a glance.
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

        findViewById<Button>(R.id.done).setOnClickListener { save() }
        updateCount()
    }

    private fun updateCount() {
        count.text = when (mode) {
            BEDTIME -> "${selected.size} shown at bedtime"
            DISTRACTING -> "${selected.size} apps will ask you to pause first"
            else -> "${selected.size} on home screen · everything else hidden"
        }
    }

    private fun ordered(old: List<String>): List<String> {
        // Keep the order you already had; add new picks at the end, alphabetically.
        val kept = old.filter { it in selected }
        val labels = all.associate { it.key to it.label.lowercase() }
        return kept + selected.filter { it !in kept }.sortedBy { labels[it] ?: it }
    }

    private fun save() {
        when (mode) {
            HOME -> {
                prefs.homeApps = ordered(prefs.homeApps)
                prefs.configured = true
                if (firstRun) Toast.makeText(this, R.string.first_run_hint, Toast.LENGTH_LONG).show()
            }
            BEDTIME -> prefs.bedtimeApps = ordered(prefs.bedtimeApps)
            DISTRACTING -> {
                val byKey = all.associate { it.key to it.packageName }
                prefs.distracting = selected.mapNotNull { byKey[it] }.toSet()
            }
        }
        finish()
    }

    @Deprecated("Back saves, so nothing is lost")
    override fun onBackPressed() = save()

    private inner class Adapter : BaseAdapter() {
        private val font = Ui.serif(this@AppPickerActivity)

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
            view.typeface = font
            return view
        }
    }
}
