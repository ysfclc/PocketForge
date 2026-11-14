package com.pocketforge.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var store: WorkspaceStore
    private lateinit var page: LinearLayout
    private var activeTab = Tab.WORKSPACE
    private var nativeStatus = "Native bridge not checked"
    private val ink = Color.rgb(241, 244, 250)
    private val muted = Color.rgb(151, 163, 184)
    private val background = Color.rgb(10, 14, 23)
    private val surface = Color.rgb(19, 26, 40)
    private val surfaceRaised = Color.rgb(25, 34, 51)
    private val accent = Color.rgb(111, 147, 255)
    private val mint = Color.rgb(91, 220, 183)

    private enum class Tab { WORKSPACE, MISSIONS, FACTORY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = WorkspaceStore(this)
        window.statusBarColor = background
        window.navigationBarColor = background
        window.decorView.systemUiVisibility = 0
        nativeStatus = runCatching {
            System.loadLibrary("pocketforge_ffi")
            "Native bridge responding · ABI \${nativeAbiVersion()} · health \${nativeHealthCheck()}"
        }.getOrElse { "Native bridge unavailable · \${it.javaClass.simpleName}" }
        render()
    }

    private fun render() {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
        }
        shell.addView(buildHeader(), LinearLayout.LayoutParams(-1, dp(76)))
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(20))
        }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(20))
        }
        scroll.addView(page)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        shell.addView(buildNavigation(), LinearLayout.LayoutParams(-1, dp(76)))
        setContentView(shell)

        when (activeTab) {
            Tab.WORKSPACE -> renderWorkspace()
            Tab.MISSIONS -> renderMissions()
            Tab.FACTORY -> renderFactory()
        }
    }

    private fun buildHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(8))
        }
        val mark = TextView(this).apply {
            text = "P"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(accent, 15f)
        }
        row.addView(mark, LinearLayout.LayoutParams(dp(42), dp(42)))
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(label("POCKETFORGE", 14f, ink, true))
            addView(label("NEXUS  /  LOCAL FACTORY", 9f, muted, true))
        }
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        val status = TextView(this).apply {
            text = "●  LOCAL"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(mint)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(Color.rgb(19, 53, 51), 30f)
        }
        row.addView(status)
        return row
    }

    private fun buildNavigation(): View {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(12))
            setBackgroundColor(Color.rgb(13, 19, 30))
        }
        nav.addView(navItem("⌂", "Workspace", Tab.WORKSPACE), weighted())
        nav.addView(navItem("◈", "Missions", Tab.MISSIONS), weighted())
        nav.addView(navItem("▣", "Factory", Tab.FACTORY), weighted())
        return nav
    }

    private fun navItem(symbol: String, title: String, tab: Tab): View {
        val selected = activeTab == tab
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(4))
            background = if (selected) rounded(Color.rgb(31, 43, 68), 16f) else rounded(Color.TRANSPARENT, 16f)
            setOnClickListener { activeTab = tab; render() }
        }
        item.addView(label(symbol, 19f, if (selected) accent else muted, true).apply { gravity = Gravity.CENTER })
        item.addView(label(title, 10f, if (selected) ink else muted, selected).apply { gravity = Gravity.CENTER })
        return item
    }

    private fun renderWorkspace() {
        val projects = store.projects()
        val missions = store.missions()
        page.addView(hero())
        page.addView(space(18))
        page.addView(sectionTitle("YOUR WORKSPACE", "Projects, context and decisions stay on this device"))
        page.addView(space(10))
        val stats = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(statCard(projects.size.toString(), "PROJECTS"), weighted(8))
        stats.addView(statCard(missions.count { it.state == "Draft" }.toString(), "DRAFT MISSIONS"), weighted(8))
        stats.addView(statCard("—", "BUILDS"), weighted(8))
        page.addView(stats)
        page.addView(space(22))
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heading.addView(label("Projects", 20f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(actionText("＋ New", accent) { showCreateProject() })
        page.addView(heading)
        page.addView(space(10))
        if (projects.isEmpty()) {
            page.addView(emptyCard(
                "Your first project starts here",
                "Create a Project Passport or import an existing project folder. PocketForge will keep its context attached to the work."
            ))
        } else {
            projects.take(8).forEach { project ->
                page.addView(projectCard(project))
                page.addView(space(9))
            }
        }
        page.addView(space(12))
        page.addView(outlineAction("＋  Import project folder") { launchFolderPicker() })
        page.addView(space(24))
        page.addView(sectionTitle("RECENT MISSIONS", "A durable queue for work you want to get done"))
        page.addView(space(10))
        if (missions.isEmpty()) {
            page.addView(emptyCard("No missions yet", "Capture a task as a mission. Its brief and project link are saved locally."))
            page.addView(space(10))
            page.addView(outlineAction("＋  Create a mission") { showCreateMission() })
        } else {
            missions.take(3).forEach { mission ->
                page.addView(missionCard(mission))
                page.addView(space(8))
            }
            page.addView(actionText("View all missions  →", accent) { activeTab = Tab.MISSIONS; render() })
        }
    }

    private fun hero(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(32, 49, 83), Color.rgb(22, 36, 56), Color.rgb(18, 55, 56))
            ).apply { cornerRadius = dp(24).toFloat() }
        }
        box.addView(label("THE BUILDER'S WORKSPACE", 10f, mint, true))
        box.addView(space(12))
        box.addView(label("From idea to\ninstalled software.", 29f, ink, true))
        box.addView(space(9))
        box.addView(label("One place for project context, agent missions and the Android build loop.", 13f, Color.rgb(197, 209, 226), false))
        box.addView(space(18))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(pillButton("＋  New project", true) { showCreateProject() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(spaceWidth(9))
        actions.addView(pillButton("Import folder", false) { launchFolderPicker() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        box.addView(actions)
        return box
    }

    private fun renderMissions() {
        page.addView(sectionTitle("MISSION CONTROL", "Turn a goal into a durable, reviewable unit of work"))
        page.addView(space(14))
        page.addView(card {
            addView(label("A mission is more than a chat prompt.", 18f, ink, true))
            addView(space(8))
            addView(label("Keep the objective, acceptance notes and project link together. Agent execution and patch review will connect in a later slice.", 13f, muted))
            addView(space(14))
            addView(pillButton("＋  Create mission", true) { showCreateMission() })
        })
        page.addView(space(18))
        val missions = store.missions()
        page.addView(sectionTitle("YOUR QUEUE", "\${missions.size} saved mission\${if (missions.size == 1) "" else "s"}"))
        page.addView(space(10))
        if (missions.isEmpty()) {
            page.addView(emptyCard("The queue is clear", "Create your first mission. It will remain here when you close the app."))
        } else {
            missions.forEach {
                page.addView(missionCard(it))
                page.addView(space(9))
            }
        }
    }

    private fun renderFactory() {
        page.addView(sectionTitle("BUILD FACTORY", "A transparent path from source revision to APK"))
        page.addView(space(14))
        page.addView(card {
            addView(label("Keystone foundation", 19f, ink, true))
            addView(space(6))
            addView(label("The Android shell and Rust bridge are scaffolds. The production pipeline has not yet completed a runner-backed build.", 13f, muted))
            addView(space(14))
            addView(label(nativeStatus, 12f, if (nativeStatus.startsWith("Native bridge responding")) mint else Color.rgb(255, 190, 105), true))
        })
        page.addView(space(18))
        page.addView(sectionTitle("PIPELINE CHECKLIST", "No step is marked complete without evidence"))
        page.addView(space(10))
        listStep("01", "Project source", "Import a folder or create a Project Passport", store.projects().isNotEmpty())
        listStep("02", "Rust Android libraries", "Cross-compile arm64, armv7 and x86_64", false)
        listStep("03", "Android package", "Assemble and verify the debug APK", false)
        listStep("04", "Artifact and provenance", "Publish the APK, SBOM and build attestation", false)
        page.addView(space(18))
        page.addView(emptyCard(
            "Build execution is not connected yet",
            "GitHub Actions jobs are currently failing before a runner is assigned. The app does not pretend an APK exists. The next gate is a real CI run, then local project builds."
        ))
    }

    private fun listStep(number: String, title: String, detail: String, done: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = rounded(surface, 17f)
        }
        row.addView(label(if (done) "✓" else number, 12f, if (done) mint else accent, true), LinearLayout.LayoutParams(dp(34), -2))
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 14f, ink, true))
            addView(space(4))
            addView(label(detail, 11f, muted))
        }
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(row)
        page.addView(space(8))
    }

    private fun projectCard(project: WorkspaceProject): View = card {
        val top = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("▦", 20f, accent, true))
        top.addView(spaceWidth(10))
        top.addView(label(project.name, 16f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(label("PASSPORT  →", 9f, mint, true))
        addView(top)
        if (project.description.isNotBlank()) {
            addView(space(8))
            addView(label(project.description, 12f, muted))
        }
        addView(space(9))
        addView(label(if (project.folderUri == null) "LOCAL PASSPORT · FOLDER NOT LINKED" else "FOLDER LINKED · LOCAL ACCESS", 9f, muted, true))
        setOnClickListener { showProject(project) }
    }

    private fun missionCard(mission: WorkspaceMission): View = card {
        val top = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("◈", 18f, accent, true))
        top.addView(spaceWidth(10))
        top.addView(label(mission.title, 15f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(label(mission.state.uppercase(), 9f, if (mission.state == "Ready") mint else muted, true))
        addView(top)
        if (mission.brief.isNotBlank()) {
            addView(space(7))
            addView(label(mission.brief, 12f, muted))
        }
        val linked = store.projects().firstOrNull { it.id == mission.projectId }?.name
        if (linked != null) {
            addView(space(8))
            addView(label("PROJECT  ·  $linked", 9f, accent, true))
        }
        setOnClickListener { showMission(mission) }
    }

    private fun statCard(value: String, caption: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(13), dp(8), dp(13))
        background = rounded(surface, 16f)
        addView(label(value, 22f, ink, true))
        addView(space(3))
        addView(label(caption, 8f, muted, true))
    }

    private fun emptyCard(title: String, body: String): View = card {
        addView(label(title, 16f, ink, true))
        addView(space(7))
        addView(label(body, 12f, muted))
    }

    private fun showCreateProject() {
        val form = formColumn()
        val name = editField("Project name")
        val description = editField("What are you building? (optional)", multiline = true)
        form.addView(label("PROJECT PASSPORT", 10f, accent, true))
        form.addView(space(8))
        form.addView(name)
        form.addView(space(9))
        form.addView(description)
        AlertDialog.Builder(this)
            .setTitle("New project")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                val project = store.addProject(name.text.toString(), description.text.toString())
                Toast.makeText(this, "Passport created: \${project.name}", Toast.LENGTH_SHORT).show()
                render()
            }
            .show()
    }

    private fun launchFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        runCatching { startActivityForResult(intent, REQUEST_FOLDER) }
            .onFailure { Toast.makeText(this, "Folder picker unavailable on this device", Toast.LENGTH_LONG).show() }
    }

    @Deprecated("Handled for the Android document-tree picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_FOLDER || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        runCatching {
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, flags)
        }
        showImportedProject(uri)
    }

    private fun showImportedProject(uri: Uri) {
        val form = formColumn()
        val suggested = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Imported project"
        val name = editField("Project name").apply { setText(suggested) }
        val description = editField("Project purpose or constraints (optional)", multiline = true)
        form.addView(label("FOLDER ACCESS GRANTED", 10f, mint, true))
        form.addView(space(8))
        form.addView(name)
        form.addView(space(9))
        form.addView(description)
        AlertDialog.Builder(this)
            .setTitle("Create Project Passport")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Import") { _, _ ->
                store.addProject(name.text.toString(), description.text.toString(), uri.toString())
                Toast.makeText(this, "Project folder linked", Toast.LENGTH_SHORT).show()
                render()
            }
            .show()
    }

    private fun showCreateMission() {
        val form = formColumn()
        val title = editField("Mission title")
        val brief = editField("Objective and acceptance notes", multiline = true)
        val projects = store.projects()
        val options = listOf("No project linked") + projects.map { it.name }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, options)
        }
        form.addView(label("MISSION BRIEF", 10f, accent, true))
        form.addView(space(8))
        form.addView(title)
        form.addView(space(9))
        form.addView(brief)
        form.addView(space(9))
        form.addView(label("ATTACH TO PROJECT", 10f, muted, true))
        form.addView(space(4))
        form.addView(spinner)
        AlertDialog.Builder(this)
            .setTitle("New mission")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save mission") { _, _ ->
                val projectId = projects.getOrNull(spinner.selectedItemPosition - 1)?.id
                store.addMission(title.text.toString(), brief.text.toString(), projectId)
                Toast.makeText(this, "Mission saved on this device", Toast.LENGTH_SHORT).show()
                render()
            }
            .show()
    }

    private fun showProject(project: WorkspaceProject) {
        val folder = if (project.folderUri == null) "No folder linked yet" else "A folder permission is saved on this device."
        AlertDialog.Builder(this)
            .setTitle(project.name)
            .setMessage("PROJECT PASSPORT\\n\\n\${project.description.ifBlank { "No project brief added." }}\\n\\n$folder")
            .setPositiveButton("Close", null)
            .setNeutralButton("Create mission") { _, _ ->
                activeTab = Tab.MISSIONS
                render()
                showCreateMission()
            }
            .show()
    }

    private fun showMission(mission: WorkspaceMission) {
        val nextState = if (mission.state == "Ready") "Draft" else "Ready"
        AlertDialog.Builder(this)
            .setTitle(mission.title)
            .setMessage("\${mission.brief.ifBlank { "No acceptance notes added." }}\\n\\nCurrent state: \${mission.state}\\n\\nReady means the brief is prepared for a future agent run. It does not start execution.")
            .setNegativeButton("Close", null)
            .setPositiveButton(if (mission.state == "Ready") "Return to draft" else "Mark ready") { _, _ ->
                store.setMissionState(mission.id, nextState)
                render()
            }
            .show()
    }

    private fun sectionTitle(title: String, subtitle: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(label(title, 10f, accent, true))
        addView(space(4))
        addView(label(subtitle, 12f, muted))
    }

    private fun card(content: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(surface, 20f)
        content()
    }

    private fun formColumn() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(8), dp(22), dp(4))
    }

    private fun editField(hintText: String, multiline: Boolean = false) = EditText(this).apply {
        hint = hintText
        setTextColor(ink)
        setHintTextColor(muted)
        textSize = 14f
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = rounded(surfaceRaised, 12f)
        if (multiline) {
            minLines = 2
            maxLines = 4
            gravity = Gravity.TOP
        } else {
            singleLine = true
        }
    }

    private fun pillButton(title: String, primary: Boolean, action: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(if (primary) Color.WHITE else ink)
        background = rounded(if (primary) accent else Color.rgb(48, 62, 82), 14f)
        setOnClickListener { action() }
    }

    private fun outlineAction(title: String, action: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(ink)
        setPadding(dp(14), dp(15), dp(14), dp(15))
        background = rounded(surfaceRaised, 15f)
        setOnClickListener { action() }
    }

    private fun actionText(title: String, color: Int, action: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        setPadding(dp(8), dp(8), dp(4), dp(8))
        setOnClickListener { action() }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun spaceWidth(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun weighted(margin: Int = 0) = LinearLayout.LayoutParams(0, -2, 1f).apply {
        if (margin > 0) setMargins(0, 0, dp(margin), 0)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float) = (value * resources.displayMetrics.density)

    private external fun nativeAbiVersion(): Int
    private external fun nativeHealthCheck(): Int

    companion object {
        private const val REQUEST_FOLDER = 701
    }
}
