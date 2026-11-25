package com.pocketforge.app

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.DocumentsContract
import android.text.InputType
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
            "Native bridge responding · ABI ${nativeAbiVersion()} · health ${nativeHealthCheck()}"
        }.getOrElse { "Native bridge unavailable · ${it.javaClass.simpleName}" }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized && activeTab == Tab.FACTORY) render()
    }

    private fun render() {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@MainActivity.background)
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
            addView(label("NEXUS  /  LOCAL + GITHUB", 9f, muted, true))
        }
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        val status = TextView(this).apply {
            text = "●  NEXUS"
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
        page.addView(outlineAction("＋  Link existing project folder") { launchFolderPicker() })
        page.addView(space(8))
        page.addView(outlineAction("⌘  Connect GitHub repository") { showConnectGitHubRepository() })
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
        actions.addView(pillButton("Link folder", false) { launchFolderPicker() }, LinearLayout.LayoutParams(0, dp(46), 1f))
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
        page.addView(sectionTitle("YOUR QUEUE", "${missions.size} saved mission${if (missions.size == 1) "" else "s"}"))
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

    private fun requestProbePermissionOrStart() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), PROCESS_PROBE_NOTIFICATION_REQUEST)
            return
        }
        val intent = Intent(this, ProcessExecutionProbeService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        Toast.makeText(this, "Process probe started", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PROCESS_PROBE_NOTIFICATION_REQUEST) {
            if (grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestProbePermissionOrStart()
            } else {
                Toast.makeText(this, "Notification permission is needed to monitor and cancel the process probe.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderFactory() {
        val projects = store.projects()
        val hasLocalFolder = projects.any { it.folderUri != null }
        val hasGitHubProject = projects.any { it.sourceKind == WorkspaceProject.SOURCE_GITHUB }
        page.addView(sectionTitle("BUILD FACTORY", "A transparent path from source revision to APK"))
        page.addView(space(14))
        page.addView(card {
            addView(label("POCKETFORGE CI · VALIDATION GREEN", 18f, mint, true))
            addView(space(7))
            addView(label(
                "Rust ABI builds, Kotlin compilation, APK signature/package checks and the hosted 10-test emulator suite pass. The current CI path uses a temporary signing key because persistent Preview secrets are not configured, so it does not publish an update-safe APK or provenance. Imported projects still cannot be built on this phone.",
                13f,
                muted
            ))
            addView(space(14))
            addView(label(nativeStatus, 12f, if (nativeStatus.startsWith("Native bridge responding")) mint else Color.rgb(255, 190, 105), true))
        })
        page.addView(space(18))
        if (packageName.endsWith(".preview")) {
            val probePrefs = getSharedPreferences(ProcessExecutionProbeService.PREFS, MODE_PRIVATE)
            var probeState = probePrefs.getString(ProcessExecutionProbeService.KEY_STATE, ProcessExecutionProbeService.STATE_IDLE) ?: ProcessExecutionProbeService.STATE_IDLE
            if (probeState == ProcessExecutionProbeService.STATE_RUNNING && !ProcessExecutionProbeService.isActive) {
                probeState = ProcessExecutionProbeService.STATE_INTERRUPTED
                probePrefs.edit().putString(ProcessExecutionProbeService.KEY_STATE, probeState)
                    .putString(ProcessExecutionProbeService.KEY_OUTPUT, "The app process restarted while the probe was marked running.")
                    .commit()
            }
            page.addView(card {
                addView(label("ANDROID ART + PROCESS FEASIBILITY · PREVIEW ONLY", 11f, accent, true))
                addView(space(7))
                addView(label("State: " + probeState, 14f, if (probeState == ProcessExecutionProbeService.STATE_SUCCEEDED) mint else ink, true))
                addView(space(5))
                addView(label(
                    "Starts a fixed Android ART child runtime from this APK, then /system/bin/id and /system/bin/sleep for 30 seconds. This is not a standalone JDK and does not run Gradle or project scripts.",
                    12f, muted
                ))
                val output = probePrefs.getString(ProcessExecutionProbeService.KEY_OUTPUT, "").orEmpty()
                if (output.isNotBlank()) {
                    addView(space(7))
                    addView(label(output.take(500), 11f, muted))
                }
                addView(space(12))
                addView(pillButton(if (probeState == ProcessExecutionProbeService.STATE_RUNNING) "Probe running…" else "Run process probe", true) {
                    if (probeState != ProcessExecutionProbeService.STATE_RUNNING) {
                        requestProbePermissionOrStart()
                    }
                })
            })
            page.addView(space(18))
    
        }
        page.addView(sectionTitle("PIPELINE CHECKLIST", "Only verified capabilities are marked complete"))
        page.addView(space(10))
        listStep(
            "01",
            "PocketForge Android package",
            "Rust ABIs, Rust tests, Kotlin, APK checks and 10 emulator tests pass; persistent-signed APK/provenance publication remains blocked",
            true
        )
        listStep(
            "02",
            "Project source",
            "Local folder linked: ${if (hasLocalFolder) "yes" else "not yet"} · GitHub repository connected: ${if (hasGitHubProject) "yes" else "not yet"}",
            hasLocalFolder || hasGitHubProject
        )
        listStep("03", "Local project build", "Read-only preflight is available; the on-device Gradle/JDK/SDK runner is still required", false)
        listStep("04", "Mission execution", "Connect an agent, run tools and verify its changes", false)
        listStep("05", "Approved release", "Sign and install a selected project's verified artifact", false)
        page.addView(space(12))
        page.addView(emptyCard(
            "What works now — and what is next",
            "You can create a Project Passport, link and inspect a local folder, connect and browse a GitHub repository, and prepare a reviewed GitHub file change. PocketForge does not yet compile imported projects locally or execute coding agents. Those are the next factory capabilities."
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
        val sourceLabel = if (project.sourceKind == WorkspaceProject.SOURCE_GITHUB) "GITHUB · ${project.githubRepository}" else if (project.folderUri == null) "LOCAL PASSPORT · FOLDER NOT LINKED" else "FOLDER LINKED · LOCAL ACCESS"
        addView(label(sourceLabel, 9f, if (project.sourceKind == WorkspaceProject.SOURCE_GITHUB) mint else muted, true))
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
                Toast.makeText(this, "Passport created: ${project.name}", Toast.LENGTH_SHORT).show()
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
        val grantedFlags = data.flags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        if ((grantedFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            Toast.makeText(this, "Android did not grant read access to that folder.", Toast.LENGTH_LONG).show()
            return
        }
        val persisted = runCatching {
            contentResolver.takePersistableUriPermission(uri, grantedFlags)
            contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        }.getOrElse {
            Toast.makeText(
                this,
                "PocketForge could not keep access to that folder: ${it.message ?: "permission unavailable"}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
        if (!persisted) {
            Toast.makeText(this, "Folder access was not saved. Choose the folder again and allow access.", Toast.LENGTH_LONG).show()
            return
        }
        showImportedProject(uri)
    }

    private fun showImportedProject(uri: Uri) {
        val form = formColumn()
        val suggested = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Imported project"
        val name = editField("Project name").apply { setText(suggested) }
        val description = editField("What is this project for? (optional)", multiline = true)
        form.addView(label("LINK EXISTING FOLDER · NO COPY", 10f, mint, true))
        form.addView(space(8))
        form.addView(name)
        form.addView(space(9))
        form.addView(description)
        form.addView(space(8))
        form.addView(label(
            "Your files stay where they are. PocketForge links this folder, identifies the project type and lets you browse its files. Nothing is copied or uploaded.",
            11f,
            muted
        ))
        AlertDialog.Builder(this)
            .setTitle("Link local project")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Link folder & inspect") { _, _ ->
                val project = store.addProject(name.text.toString(), description.text.toString(), uri.toString())
                render()
                inspectLocalProject(project)
            }
            .show()
    }

    private fun inspectLocalProject(project: WorkspaceProject) {
        val folderUri = project.folderUri ?: return
        runBackgroundTask("Inspecting ${project.name}…", {
            LocalProjectInspector.inspect(this, Uri.parse(folderUri))
        }) { result ->
            showLocalProjectOverview(project, result as LocalProjectSnapshot)
        }
    }

    private fun showLocalProjectOverview(project: WorkspaceProject, snapshot: LocalProjectSnapshot) {
        val directory = snapshot.directory
        val countLabel = "${directory.totalEntries} top-level item${if (directory.totalEntries == 1) "" else "s"}"
        val sample = directory.entries.take(10).joinToString("\n") { entry ->
            (if (entry.isDirectory) "▸ " else "· ") + entry.name
        }.ifBlank { "No visible files or folders at this level." }
        val message = buildString {
            append("FOLDER LINKED · ${if (snapshot.readOnly) "READ ONLY" else "READ / WRITE"}\n\n")
            append("${snapshot.rootName}\n")
            append("$countLabel")
            if (directory.truncated) append(" (showing first ${directory.entries.size})")
            append("\nDetected: ${snapshot.detectedProjectType}\n\n")
            append(sample)
            append("\n\nYour source files remain in the selected folder. PocketForge has inspected the folder structure; it has not copied, edited or built the project yet.")
        }
        AlertDialog.Builder(this)
            .setTitle(project.name)
            .setMessage(message)
            .setNegativeButton("Assess build") { _, _ -> assessLocalProject(project) }
            .setNeutralButton("Prepare snapshot") { _, _ -> prepareProjectSnapshot(project) }
            .setPositiveButton("Browse files") { _, _ ->
                browseLocalDirectory(project, snapshot.rootDocumentId, snapshot.rootName)
            }
            .show()
    }

    private fun prepareProjectSnapshot(project: WorkspaceProject) {
        val treeUri = project.folderUri?.let { Uri.parse(it) } ?: return
        val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
        val progress = ProgressDialog(this).apply {
            setTitle("Preparing project snapshot")
            setMessage("Copying into PocketForge private storage…")
            setCancelable(true)
            setIndeterminate(true)
            setOnCancelListener { cancelled.set(true) }
            show()
        }
        Thread {
            val result = runCatching {
                val prepared = ProjectSnapshotter(this).create(treeUri, { cancelled.get() }) { files, bytes ->
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            progress.setMessage("$files files · ${formatBytes(bytes)} copied")
                        }
                    }
                }
                val verification = ProjectSnapshotVerifier(java.io.File(filesDir, "build-workspaces")).verify(
                    directory = prepared.directory,
                    expectedSnapshotId = prepared.id,
                    expectedManifestSha256 = prepared.manifestSha256,
                    expectedFileCount = prepared.fileCount,
                    expectedTotalBytes = prepared.totalBytes,
                    expectedSourceUriSha256 = prepared.sourceUriSha256,
                    expectedSourceRootIdSha256 = prepared.sourceRootIdSha256
                )
                if (!verification.valid) {
                    prepared.directory.deleteRecursively()
                    error(verification.failure ?: "Snapshot integrity verification failed.")
                }
                prepared
            }
            runOnUiThread {
                progress.dismiss()
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { prepared ->
                    val stored = runCatching { ProjectSnapshotStore(this).add(prepared, project.id, project.name) }
                    if (stored.isFailure) {
                        prepared.directory.deleteRecursively()
                        AlertDialog.Builder(this)
                            .setTitle("Snapshot prepared but not saved")
                            .setMessage(stored.exceptionOrNull()?.message ?: "Could not save snapshot metadata.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@runOnUiThread
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Snapshot ready")
                        .setMessage(
                            "PROJECT · ${project.name}\n\nFiles: ${prepared.fileCount}\nSize: ${formatBytes(prepared.totalBytes)}\nSkipped directories: ${prepared.skippedEntries.size}\n\nSaved snapshot record for project.\nIntegrity: VERIFIED (${prepared.fileCount} files · ${formatBytes(prepared.totalBytes)}).\nPrivate workspace: ${prepared.directory.absolutePath}\nManifest: ${prepared.manifestFile.name}\nManifest SHA-256: ${prepared.manifestSha256}\n\nThis snapshot is prepared only. No build scripts have been executed."
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }.onFailure { error ->
                    val title = if (error is SnapshotCancelledException || cancelled.get()) "Snapshot cancelled" else "Snapshot preparation failed"
                    AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(error.message ?: "Unable to prepare this project.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }.start()
    }

    private fun assessLocalProject(project: WorkspaceProject) {
        val folderUri = project.folderUri?.let { Uri.parse(it) } ?: return
        runBackgroundTask("Checking build structure for ${project.name}…", {
            BuildPreflight.assess(this, folderUri)
        }) { result ->
            showBuildAssessment(project, result as BuildAssessment)
        }
    }

    private fun showBuildAssessment(project: WorkspaceProject, assessment: BuildAssessment) {
        val message = buildString {
            append("PROJECT PREFLIGHT · READ ONLY\n\n")
            append("Detected: ${assessment.projectType}\n")
            append("Build system: ${assessment.buildSystem}\n")
            append("Build root: ${assessment.buildRoot.ifBlank { "selected folder" }}\n")
            append("Gradle wrapper: ${if (assessment.hasGradleWrapperScript && assessment.hasGradleWrapperJar && assessment.gradleDistribution != null) "complete" else "incomplete"}\n")
            assessment.gradleDistribution?.let { append("Gradle distribution: $it\n") }
            if (assessment.modules.isNotEmpty()) append("Modules: ${assessment.modules.joinToString()}\n")
            append("\nFINDINGS\n")
            append(assessment.findings.joinToString("\n").ifBlank { "No build markers found." })
            if (assessment.blockers.isNotEmpty()) {
                append("\n\nPROJECT BLOCKERS\n")
                append(assessment.blockers.joinToString("\n") { "• $it" })
            }
            if (assessment.scanTruncated) append("\n\nThe scan was capped for safety; inspect the full tree before relying on this result.")
            append("\n\nIMPORTANT\nThis is a structural assessment only. PocketForge has not executed project code, downloaded dependencies, changed files or attempted a build. The on-device Gradle/JDK/Android SDK build runner is the next implementation gate.")
        }
        AlertDialog.Builder(this)
            .setTitle("Build assessment · ${project.name}")
            .setMessage(message)
            .setNegativeButton("Close", null)
            .setNeutralButton("Create mission") { _, _ -> createMissionForProject(project) }
            .setPositiveButton("Browse project") { _, _ ->
                val treeUri = project.folderUri?.let { Uri.parse(it) }
                val rootId = treeUri?.let { runCatching { DocumentsContract.getTreeDocumentId(it) }.getOrNull() }
                if (rootId != null) browseLocalDirectory(project, rootId, project.name)
            }
            .show()
    }

    private fun browseLocalDirectory(
        project: WorkspaceProject,
        documentId: String,
        pathLabel: String,
        parentDocumentId: String? = null,
        parentPathLabel: String? = null
    ) {
        val treeUri = project.folderUri?.let { Uri.parse(it) } ?: return
        runBackgroundTask("Reading $pathLabel…", {
            LocalProjectInspector.listChildren(this, treeUri, documentId)
        }) { result ->
            val directory = result as LocalProjectDirectory
            if (directory.entries.isEmpty()) {
                val message = if (directory.totalEntries == 0) "This folder is empty." else "No items could be listed."
                val builder = AlertDialog.Builder(this)
                    .setTitle(pathLabel)
                    .setMessage(message)
                    .setPositiveButton("Done", null)
                if (parentDocumentId != null) {
                    builder.setNegativeButton("Up") { _, _ ->
                        browseLocalDirectory(project, parentDocumentId, parentPathLabel ?: project.name)
                    }
                }
                builder.show()
                return@runBackgroundTask
            }
            val labels = directory.entries.map { entry ->
                if (entry.isDirectory) "▸  ${entry.name}/"
                else "·  ${entry.name}" + (entry.sizeBytes?.let { "  ·  ${formatBytes(it)}" } ?: "")
            }.toTypedArray()
            val builder = AlertDialog.Builder(this)
                .setTitle(pathLabel)
                .setItems(labels) { _, index ->
                    val entry = directory.entries[index]
                    if (entry.isDirectory) {
                        browseLocalDirectory(project, entry.documentId, "$pathLabel/${entry.name}", documentId, pathLabel)
                    } else {
                        openLocalFile(treeUri, entry)
                    }
                }
                .setPositiveButton("Done", null)
            if (parentDocumentId != null) {
                builder.setNegativeButton("Up") { _, _ ->
                    browseLocalDirectory(project, parentDocumentId, parentPathLabel ?: project.name)
                }
            } else {
                builder.setNegativeButton("Close", null)
            }
            builder.show()
        }
    }

    private fun openLocalFile(treeUri: Uri, entry: LocalProjectEntry) {
        runCatching {
            val documentUri = LocalProjectInspector.documentUri(treeUri, entry.documentId)
            val mimeType = contentResolver.getType(documentUri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(documentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }.onFailure {
            Toast.makeText(
                this,
                "No app could open ${entry.name}: ${it.message ?: "unsupported file type"}",
                Toast.LENGTH_LONG
            ).show()
        }
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
        if (project.sourceKind == WorkspaceProject.SOURCE_GITHUB) {
            val pinned = project.githubRevision?.let { Regex("^[a-fA-F0-9]{40}$").matches(it) } == true
            val projectDialog = AlertDialog.Builder(this)
                .setTitle(project.name)
                .setMessage(
                    "GITHUB REPOSITORY\n\n${project.githubRepository}\nDefault branch: ${project.githubDefaultBranch ?: "unknown"}\nPinned revision: ${project.githubRevision?.take(7) ?: "not pinned"}" +
                        "\n\n${project.description.ifBlank { "No repository description." }}" +
                        if (pinned) {
                            "\n\nBrowse the pinned source, prepare a reviewed single-file change, and open a pull request. PocketForge never merges it automatically."
                        } else {
                            "\n\nThis saved connection has no immutable commit revision. Reconnect it before browsing or creating a mission."
                        }
                )
                .setNegativeButton("Close", null)
                .setNeutralButton(if (pinned) "Browse files" else "Reconnect") { _, _ ->
                    if (pinned) browseGitHubPath(project, "")
                    else showConnectGitHubRepository(project.githubRepository)
                }
            if (pinned) {
                projectDialog.setPositiveButton("Create mission") { _, _ ->
                    createMissionForProject(project)
                }
            }
            projectDialog.show()
            return
        }
        if (project.folderUri != null) {
            inspectLocalProject(project)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(project.name)
            .setMessage("PROJECT PASSPORT\n\n${project.description.ifBlank { "No project brief added." }}\n\nNo local folder is linked yet.")
            .setPositiveButton("Close", null)
            .setNeutralButton("Create mission") { _, _ -> createMissionForProject(project) }
            .show()
    }

    private fun createMissionForProject(project: WorkspaceProject) {
        activeTab = Tab.MISSIONS
        render()
        val form = formColumn()
        val title = editField("Mission title")
        val brief = editField("Objective and acceptance notes", multiline = true)
        form.addView(title)
        form.addView(space(9))
        form.addView(brief)
        AlertDialog.Builder(this)
            .setTitle("Mission for ${project.name}")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save mission") { _, _ ->
                store.addMission(title.text.toString(), brief.text.toString(), project.id)
                render()
            }
            .show()
    }

    private fun normalizeGitHubRepositoryInput(raw: String): String? {
        val input = raw.trim()
        if (input.isBlank()) return null
        val path = when {
            input.startsWith("git@github.com:", ignoreCase = true) -> input.substringAfter(':')
            input.startsWith("https://", ignoreCase = true) || input.startsWith("http://", ignoreCase = true) -> {
                val withoutScheme = input.substringAfter("://")
                val host = withoutScheme.substringBefore('/')
                if (!host.equals("github.com", true) && !host.equals("www.github.com", true)) return null
                withoutScheme.substringAfter('/', "")
            }
            input.startsWith("www.github.com/", ignoreCase = true) -> input.substringAfter('/')
            input.startsWith("github.com/", ignoreCase = true) -> input.substringAfter('/')
            else -> input
        }.substringBefore('?').substringBefore('#').trim('/')
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null
        val owner = segments[0]
        val repository = segments[1].removeSuffix(".git")
        val fullName = "$owner/$repository"
        return fullName.takeIf { Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$").matches(it) }
    }

    private fun showConnectGitHubRepository(initialRepository: String? = null) {
        val form = formColumn()
        val repository = editField("GitHub URL or owner/repository").apply {
            if (!initialRepository.isNullOrBlank()) setText(initialRepository)
        }
        val token = editField("Fine-grained token (only for private repos)").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        form.addView(label("CONNECT A GITHUB REPOSITORY", 10f, accent, true))
        form.addView(space(8))
        form.addView(repository)
        form.addView(space(7))
        form.addView(label(
            "Public repository: paste its GitHub URL or owner/repository; no token is needed. Private repository: add a fine-grained token with Metadata read-only and Contents read-only access.",
            11f,
            muted
        ))
        form.addView(space(9))
        form.addView(token)
        form.addView(space(8))
        form.addView(actionText("Remove saved token for this repository", muted) {
            val name = normalizeGitHubRepositoryInput(repository.text.toString())
            if (name == null) {
                Toast.makeText(this, "Enter a GitHub repository URL or owner/repository first.", Toast.LENGTH_SHORT).show()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Remove GitHub token?")
                    .setMessage("The encrypted token for $name will be removed from this device.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove") { _, _ ->
                        GitHubTokenVault(this).clear(name)
                        Toast.makeText(this, "Saved token removed.", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        })
        form.addView(space(8))
        form.addView(label(
            "PocketForge verifies repository access, saves its default branch and pinned commit, then opens the connected project. Tokens are encrypted with Android Keystore. GitHub changes still require separate review and approval.",
            10f,
            muted
        ))
        AlertDialog.Builder(this)
            .setTitle("Connect GitHub")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Verify and connect") connectClick@{ _, _ ->
                val fullName = normalizeGitHubRepositoryInput(repository.text.toString())
                if (fullName == null) {
                    Toast.makeText(this, "Paste a GitHub repository URL or enter owner/repository.", Toast.LENGTH_LONG).show()
                    return@connectClick
                }
                val suppliedToken = token.text.toString().trim()
                val vault = GitHubTokenVault(this)
                val effectiveToken = suppliedToken.takeIf { it.isNotBlank() } ?: vault.read(fullName)
                runBackgroundTask("Checking GitHub access and pinning $fullName…", {
                    val remote = GitHubRepositoryClient(effectiveToken).getRepository(fullName)
                    if (suppliedToken.isNotBlank()) vault.save(remote.fullName, suppliedToken)
                    store.addGitHubProject(remote)
                }) { result ->
                    val project = result as WorkspaceProject
                    render()
                    showProject(project)
                }
            }
            .show()
    }

    private fun pinnedRevisionOrReconnect(project: WorkspaceProject): String? {
        val revision = project.githubRevision
        if (revision != null && Regex("^[a-fA-F0-9]{40}$").matches(revision)) return revision
        AlertDialog.Builder(this)
            .setTitle("Repository needs reconnection")
            .setMessage(
                "PocketForge requires an immutable commit revision before it can browse or prepare changes. Reconnect ${project.githubRepository} to pin its current default-branch commit."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reconnect") { _, _ ->
                showConnectGitHubRepository(project.githubRepository)
            }
            .show()
        return null
    }

    private fun browseGitHubPath(project: WorkspaceProject, path: String) {
        val repository = project.githubRepository ?: return
        val ref = pinnedRevisionOrReconnect(project) ?: return
        runBackgroundTask("Loading ${if (path.isBlank()) repository else path}…", {
            GitHubRepositoryClient(GitHubTokenVault(this).read(repository)).listContents(repository, path, ref)
        }) { result ->
            val entries = result as List<GitHubEntry>
            if (entries.isEmpty()) {
                AlertDialog.Builder(this).setTitle(path.ifBlank { repository })
                    .setMessage("This directory is empty.")
                    .setPositiveButton("Close", null).show()
                return@runBackgroundTask
            }
            val labels = entries.map { entry ->
                val icon = if (entry.type == "dir") "▸" else "·"
                if (entry.type == "dir") "$icon  ${entry.name}/" else "$icon  ${entry.name}  ·  ${formatBytes(entry.size)}"
            }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(path.ifBlank { repository })
                .setItems(labels) { _, index ->
                    val selected = entries[index]
                    if (selected.type == "dir") browseGitHubPath(project, selected.path)
                    else if (selected.type == "file") showGitHubFile(project, selected.path)
                    else Toast.makeText(this, "This GitHub entry type is not previewable.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(if (path.isBlank()) "Close" else "Back") { _, _ ->
                    if (path.contains('/')) browseGitHubPath(project, path.substringBeforeLast('/'))
                    else if (path.isNotBlank()) browseGitHubPath(project, "")
                }
                .show()
        }
    }

    private fun showGitHubFile(project: WorkspaceProject, path: String) {
        val repository = project.githubRepository ?: return
        val ref = pinnedRevisionOrReconnect(project) ?: return
        runBackgroundTask("Reading ${path.substringAfterLast('/')}…", {
            GitHubRepositoryClient(GitHubTokenVault(this).read(repository)).readTextFileWithSha(repository, path, ref)
        }) { result ->
            val file = result as GitHubTextFile
            val maxChars = 14_000
            val preview = if (file.content.length > maxChars) file.content.take(maxChars) + "\n\n… preview truncated …" else file.content
            AlertDialog.Builder(this)
                .setTitle(path.substringAfterLast('/'))
                .setMessage(preview.ifBlank { "(empty file)" })
                .setNegativeButton("Close", null)
                .setNeutralButton("Open on GitHub") { _, _ ->
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$repository/blob/${encodePath(ref)}/${encodePath(path)}")))
                    }
                }
                .setPositiveButton("Prepare edit") { _, _ -> showGitHubEditForm(project, path, file) }
                .show()
        }
    }

    private fun showGitHubEditForm(project: WorkspaceProject, path: String, original: GitHubTextFile) {
        val repository = project.githubRepository ?: return
        val form = formColumn()
        val branch = editField("Working branch").apply {
            setText("pocketforge/edit-${System.currentTimeMillis()}")
        }
        val message = editField("Commit message").apply {
            setText("Update ${path.substringAfterLast('/')}")
        }
        val token = editField("Fine-grained token with Contents read/write").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val replacement = editField("Full replacement file content", multiline = true).apply {
            minLines = 10
            maxLines = 16
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setHorizontallyScrolling(true)
            setText(original.content)
        }
        form.addView(label("REVIEWED FILE CHANGE", 10f, accent, true))
        form.addView(space(8))
        form.addView(label(path, 12f, muted, true))
        form.addView(space(8))
        form.addView(branch)
        form.addView(space(8))
        form.addView(message)
        form.addView(space(8))
        form.addView(replacement, LinearLayout.LayoutParams(-1, dp(300)))
        form.addView(space(8))
        form.addView(token)
        form.addView(space(6))
        form.addView(label("This token needs Contents: Read and write. Pull request creation additionally needs Pull requests: Read and write. The file is written only to a new branch based on the pinned project revision; the base file SHA is checked again before commit.", 10f, muted))
        AlertDialog.Builder(this)
            .setTitle("Prepare GitHub change")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Review change") reviewClick@{ _, _ ->
                val branchName = branch.text.toString().trim()
                val commitMessage = message.text.toString().trim()
                val newContent = replacement.text.toString()
                val suppliedToken = token.text.toString().trim()
                val credential = suppliedToken.takeIf { it.isNotBlank() } ?: GitHubTokenVault(this).read(repository)
                if (credential.isNullOrBlank()) {
                    AlertDialog.Builder(this).setTitle("GitHub write permission required")
                        .setMessage("Enter a fine-grained token with Contents: Read and write for this repository.")
                        .setPositiveButton("OK", null).show()
                    return@reviewClick
                }
                if (newContent == original.content) {
                    Toast.makeText(this, "No changes to commit.", Toast.LENGTH_SHORT).show()
                    return@reviewClick
                }
                val review = buildReviewDiff(original.content, newContent)
                AlertDialog.Builder(this)
                    .setTitle("Confirm remote commit")
                    .setMessage(review)
                    .setNegativeButton("Back", null)
                    .setPositiveButton("Create branch & commit") commitClick@{ _, _ ->
                        val baseSha = project.githubRevision
                        if (baseSha.isNullOrBlank()) {
                            Toast.makeText(this, "This repository has no pinned commit revision. Reconnect it before editing.", Toast.LENGTH_LONG).show()
                            return@commitClick
                        }
                        runBackgroundTask("Creating branch and committing reviewed file…", {
                            val client = GitHubRepositoryClient(credential)
                            client.getRepository(repository)
                            if (suppliedToken.isNotBlank()) GitHubTokenVault(this).save(repository, suppliedToken)
                            client.createBranch(repository, branchName, baseSha)
                            val commitSha = client.updateTextFile(
                                repository = repository,
                                path = path,
                                branch = branchName,
                                message = commitMessage.ifBlank { "Update $path" },
                                content = newContent,
                                expectedBlobSha = original.blobSha
                            )
                            commitSha
                        }) { result ->
                            val commitSha = result as String
                            AlertDialog.Builder(this)
                                .setTitle("Commit created")
                                .setMessage("Your reviewed change is on branch $branchName.\n\nCommit: $commitSha\n\nNothing has been merged. Creating a pull request is a separate action.")
                                .setNegativeButton("Close", null)
                                .setPositiveButton("Create pull request") { _, _ ->
                                    createGitHubPullRequest(project, branchName, commitSha, path, credential)
                                }
                                .show()
                        }
                    }
                    .show()
            }
            .show()
    }

    private fun createGitHubPullRequest(
        project: WorkspaceProject,
        branchName: String,
        commitSha: String,
        changedPath: String,
        credential: String
    ) {
        val repository = project.githubRepository ?: return
        val form = formColumn()
        val title = editField("Pull request title").apply { setText("PocketForge: update ${changedPath.substringAfterLast('/')}") }
        val body = editField("Pull request description", multiline = true).apply {
            setText("Proposed change prepared in PocketForge.\n\nRepository: $repository\nBase revision: ${project.githubRevision}\nCommit: $commitSha\nChanged file: $changedPath\n\nPlease review the complete GitHub diff before merging.")
        }
        form.addView(title)
        form.addView(space(8))
        form.addView(body)
        AlertDialog.Builder(this)
            .setTitle("Open pull request")
            .setView(form)
            .setNegativeButton("Not now", null)
            .setPositiveButton("Create pull request") { _, _ ->
                runBackgroundTask("Opening pull request…", {
                    GitHubRepositoryClient(credential).createPullRequest(
                        repository = repository,
                        title = title.text.toString().trim().ifBlank { "PocketForge change" },
                        body = body.text.toString(),
                        head = branchName,
                        base = project.githubDefaultBranch ?: "main"
                    )
                }) { result ->
                    val url = result as String
                    AlertDialog.Builder(this)
                        .setTitle("Pull request opened")
                        .setMessage(url)
                        .setPositiveButton("Open GitHub") { _, _ ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }
            }
            .show()
    }

    private fun buildReviewDiff(original: String, replacement: String): String {
        val oldLines = original.lines()
        val newLines = replacement.lines()
        var prefix = 0
        while (prefix < oldLines.size && prefix < newLines.size && oldLines[prefix] == newLines[prefix]) {
            prefix++
        }
        var suffix = 0
        while (
            suffix < oldLines.size - prefix &&
            suffix < newLines.size - prefix &&
            oldLines[oldLines.lastIndex - suffix] == newLines[newLines.lastIndex - suffix]
        ) {
            suffix++
        }
        val oldChanged = oldLines.subList(prefix, oldLines.size - suffix)
        val newChanged = newLines.subList(prefix, newLines.size - suffix)
        val diff = buildList {
            add("@@ -${prefix + 1},${oldChanged.size} +${prefix + 1},${newChanged.size} @@")
            if (prefix > 0) add(" " + oldLines[prefix - 1])
            oldChanged.forEach { add("-$it") }
            newChanged.forEach { add("+$it") }
            if (suffix > 0) add(" " + oldLines[oldLines.size - suffix])
        }.joinToString("\n")
        return if (diff.length > 12_000) diff.take(12_000) + "\n… diff preview truncated …" else diff
    }

    private fun runBackgroundTask(title: String, request: () -> Any, onSuccess: (Any) -> Unit) {
        val progress = ProgressDialog(this).apply {
            setMessage(title)
            setCancelable(false)
            show()
        }
        Thread {
            val result = runCatching(request)
            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    progress.dismiss()
                    return@runOnUiThread
                }
                progress.dismiss()
                result.onSuccess(onSuccess).onFailure {
                    AlertDialog.Builder(this)
                        .setTitle("PocketForge request failed")
                        .setMessage(it.message ?: "Unexpected network error.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }.start()
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }

    private fun encodePath(value: String): String =
        value.split('/').joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

    private fun showMission(mission: WorkspaceMission) {
        val nextState = if (mission.state == "Ready") "Draft" else "Ready"
        AlertDialog.Builder(this)
            .setTitle(mission.title)
            .setMessage("${mission.brief.ifBlank { "No acceptance notes added." }}\n\nCurrent state: ${mission.state}\n\nReady means the brief is prepared for a future agent run. It does not start execution.")
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
            setSingleLine(true)
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
        private const val PROCESS_PROBE_NOTIFICATION_REQUEST = 7315
        private const val REQUEST_FOLDER = 701
    }
}
