package com.pocketforge.app

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
        page.addView(outlineAction("＋  Import local project folder") { launchFolderPicker() })
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
        if (project.sourceKind == WorkspaceProject.SOURCE_GITHUB) {
            AlertDialog.Builder(this)
                .setTitle(project.name)
                .setMessage("GITHUB REPOSITORY\n\n${project.githubRepository}\nDefault branch: ${project.githubDefaultBranch ?: "unknown"}\n\n${project.description.ifBlank { "No repository description." }}\n\nRepository browsing is read-only in this first connector slice.")
                .setNegativeButton("Close", null)
                .setNeutralButton("Browse files") { _, _ -> browseGitHubPath(project, "") }
                .setPositiveButton("Create mission") { _, _ -> createMissionForProject(project) }
                .show()
            return
        }
        val folder = if (project.folderUri == null) "No folder linked yet" else "A folder permission is saved on this device."
        AlertDialog.Builder(this)
            .setTitle(project.name)
            .setMessage("PROJECT PASSPORT\n\n${project.description.ifBlank { "No project brief added." }}\n\n$folder")
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

    private fun showConnectGitHubRepository() {
        val form = formColumn()
        val repository = editField("owner/repository")
        val token = editField("Fine-grained token (optional for public repos)").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        form.addView(label("CONNECT A REPOSITORY", 10f, accent, true))
        form.addView(space(8))
        form.addView(repository)
        form.addView(space(9))
        form.addView(token)
        form.addView(space(8))
        form.addView(label("Use a fine-grained GitHub token restricted to this repository with Contents: Read-only and Metadata: Read-only. Tokens are encrypted with Android Keystore and never saved in the project record.", 11f, muted))
        AlertDialog.Builder(this)
            .setTitle("Connect GitHub")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Verify and connect") { _, _ ->
                val fullName = repository.text.toString().trim()
                val suppliedToken = token.text.toString().trim()
                val vault = GitHubTokenVault(this)
                val effectiveToken = suppliedToken.takeIf { it.isNotBlank() } ?: vault.read(fullName)
                runGitHubRequest("Verifying repository…", {
                    val remote = GitHubRepositoryClient(effectiveToken).getRepository(fullName)
                    if (suppliedToken.isNotBlank()) vault.save(remote.fullName, suppliedToken)
                    store.addGitHubProject(remote)
                }) { result ->
                    val project = result as WorkspaceProject
                    Toast.makeText(this, "Connected ${project.githubRepository}", Toast.LENGTH_LONG).show()
                    render()
                }
            }
            .show()
    }

    private fun browseGitHubPath(project: WorkspaceProject, path: String) {
        val repository = project.githubRepository ?: return
        val ref = project.githubRevision ?: project.githubDefaultBranch ?: "main"
        runGitHubRequest("Loading ${if (path.isBlank()) repository else path}…", {
            GitHubRepositoryClient(GitHubTokenVault(this).read(repository)).listContents(repository, path, ref)
        }) { result ->
            val entries = result as List<GitHubEntry>
            if (entries.isEmpty()) {
                AlertDialog.Builder(this).setTitle(path.ifBlank { repository })
                    .setMessage("This directory is empty.")
                    .setPositiveButton("Close", null).show()
                return@runGitHubRequest
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
        val ref = project.githubRevision ?: project.githubDefaultBranch ?: "main"
        runGitHubRequest("Reading ${path.substringAfterLast('/')}…", {
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
            .setPositiveButton("Review change") { _, _ ->
                val branchName = branch.text.toString().trim()
                val commitMessage = message.text.toString().trim()
                val newContent = replacement.text.toString()
                val suppliedToken = token.text.toString().trim()
                val credential = suppliedToken.takeIf { it.isNotBlank() } ?: GitHubTokenVault(this).read(repository)
                if (credential.isNullOrBlank()) {
                    AlertDialog.Builder(this).setTitle("GitHub write permission required")
                        .setMessage("Enter a fine-grained token with Contents: Read and write for this repository.")
                        .setPositiveButton("OK", null).show()
                    return@setPositiveButton
                }
                if (newContent == original.content) {
                    Toast.makeText(this, "No changes to commit.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val review = buildReviewDiff(original.content, newContent)
                AlertDialog.Builder(this)
                    .setTitle("Confirm remote commit")
                    .setMessage(review)
                    .setNegativeButton("Back", null)
                    .setPositiveButton("Create branch & commit") { _, _ ->
                        val baseSha = project.githubRevision
                        if (baseSha.isNullOrBlank()) {
                            Toast.makeText(this, "This repository has no pinned commit revision. Reconnect it before editing.", Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        runGitHubRequest("Creating branch and committing reviewed file…", {
                            val client = GitHubRepositoryClient(credential)
                            client.createBranch(repository, branchName, baseSha)
                            val commitSha = client.updateTextFile(
                                repository = repository,
                                path = path,
                                branch = branchName,
                                message = commitMessage.ifBlank { "Update $path" },
                                content = newContent,
                                expectedBlobSha = original.blobSha
                            )
                            if (suppliedToken.isNotBlank()) GitHubTokenVault(this).save(repository, suppliedToken)
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
                runGitHubRequest("Opening pull request…", {
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

    private fun runGitHubRequest(title: String, request: () -> Any, onSuccess: (Any) -> Unit) {
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
                        .setTitle("GitHub request failed")
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
