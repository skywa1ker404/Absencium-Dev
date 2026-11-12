package skywa1ker404.absencium

import skywa1ker404.absencium.data.ai.ChatAppTools
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import android.app.Activity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import skywa1ker404.absencium.display.RefreshRatePreference
import skywa1ker404.absencium.ui.theme.AbsenciumTheme
import skywa1ker404.absencium.ui.theme.isDarkTheme
import skywa1ker404.absencium.data.backup.BackupManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import skywa1ker404.absencium.data.db.AbsenciumDatabase
import skywa1ker404.absencium.logging.DebugLogger
import kotlin.concurrent.thread

class AbsenciumApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // The logger comes first so everything after it, including crashes, is captured.
        DebugLogger.init(this)
        DebugLogger.lifecycle("Application created")
        // A restore chosen in Settings is applied here, before anything opens the database.
        BackupManager.applyStagedRestore(this)
        container = AppContainer(this)
        installChatHost()

        // The reminder alarm follows the settings and the timetable, exams and holidays; a change sets it again after a pause.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default).launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            val changes = kotlinx.coroutines.flow.combine(container.settingsRepository.settings, container.calendarRepository.data, container.database.eventDao().observeAll()) { a, b, c -> Triple(a, b, c) }
                .debounce(2000)
            changes.collect { runCatching { skywa1ker404.absencium.notify.ReminderScheduler.reschedule(this@AbsenciumApp) } }
        }

        // Opening the database and touching storage are file operations, so keep them
        // off the main thread. Results and any failures go to debug.log.
        thread(name = "startup-checks") {
            AbsenciumDatabase.verifyOpen(this)
            runStartupMaintenance()
            DebugLogger.i("Storage", "Notes storage: ${container.notesStorage.describeState()}")
        }
    }

    /** Things that can wait for the database to be open: settings from a restore, the Trash clean-up. */
    private fun runStartupMaintenance() {
        try {
            container.backupManager.takeStagedSettings()?.let {
                runBlocking { container.settingsRepository.importJson(it) }
            }
            runBlocking { container.trashRepository.purgeExpired() }
        } catch (e: Exception) {
            DebugLogger.e("Startup", "Maintenance at startup failed", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        DebugLogger.w("Memory", "onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        DebugLogger.w("Memory", "onTrimMemory level=$level")
        me.rerere.rikkahub.AiApp.trim(level)
    }

    /** The embedded Absencium AI chat wears this app's theme engine and follows its refresh-rate cap. */
    private fun installChatHost() {
        me.rerere.rikkahub.AiHost.theme = { content ->
            val settings by container.settingsState.collectAsStateWithLifecycle()
            settings?.let { loaded ->
                AbsenciumTheme(settings = loaded) {
                    // Liquid Glass: the same wallpaper layer the app's own screens sit on, so the chat's panes are glass over it.
                    // The chat keeps its own glass colours (see HostedChatTheme), so its popups stay solid.
                    skywa1ker404.absencium.ui.components.GlassStage(Modifier.fillMaxSize(), glassSurfaces = false) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            me.rerere.rikkahub.LocalHostGlass provides skywa1ker404.absencium.ui.theme.LocalGlassEnabled.current,
                        ) { content(isDarkTheme(loaded)) }
                    }
                }
            }
        }
        // The chat's user (name, avatar, {{user}}) is the student's Absencium profile.
        me.rerere.rikkahub.AiHost.userName = container.settingsState
            .map { it?.displayName.orEmpty() }
            .distinctUntilChanged()
        me.rerere.rikkahub.AiHost.userAvatar = { name, modifier ->
            val colors = androidx.compose.material3.MaterialTheme.colorScheme
            val initials = skywa1ker404.absencium.ui.main.initialsOf(name)
            androidx.compose.foundation.layout.Box(
                modifier = modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                if (initials == null) {
                    androidx.compose.material3.Icon(
                        skywa1ker404.absencium.ui.icons.AppIcons.Person,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    androidx.compose.material3.Text(
                        text = initials,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = colors.onPrimaryContainer,
                    )
                }
            }
        }
        // The chat's gear opens the app's own AI settings page; Imagine shows in the chat drawer only when "Generate images" is on.
        me.rerere.rikkahub.AiHost.openAppSettings = { context ->
          // Inside the app's own window the chat just closes and the settings page opens; only the stand-alone chat starts the app.
          if (!skywa1ker404.absencium.ui.AbsenciumAi.showAppSettings()) {
            val intent = android.content.Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_AI_SETTINGS, true)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (context !is Activity) intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
          }
        }
        me.rerere.rikkahub.AiHost.imagineEnabled = container.settingsState
            .map { it?.generateImagesEnabled == true }
            .distinctUntilChanged()
        val chatTools = skywa1ker404.absencium.data.ai.ChatAppTools(container)
        // The AI actions folder under Statistics lists the hidden hub chats; these say which of them made something that can be opened.
        me.rerere.rikkahub.AiHost.actionTargets = container.aiActionLog.chatsWithTargets
        me.rerere.rikkahub.AiHost.vaultLinks = { chatTools.vaultLinks() }
        me.rerere.rikkahub.AiHost.openActionTarget = { context, chatId ->
            container.aiActionLog.targetOf(chatId)?.let { e -> e.targetType?.let { skywa1ker404.absencium.ui.AbsenciumAi.openTarget(context, it, e.targetId) } }
        }
        me.rerere.rikkahub.AiHost.forgetAction = { chatId -> container.aiActionLog.removeChat(chatId) }
        me.rerere.rikkahub.AiHost.describe = { name, args -> skywa1ker404.absencium.data.ai.ChatAppTools.describe(name, args) }
        me.rerere.rikkahub.AiHost.planSteps = { name, args -> skywa1ker404.absencium.data.ai.ChatAppTools.planSteps(name, args) }
        me.rerere.rikkahub.AiHost.skipPlanSteps = { name, args, skipped -> skywa1ker404.absencium.data.ai.ChatAppTools.skipPlanSteps(name, args, skipped) }
        me.rerere.rikkahub.AiHost.suggestions = { runCatching { ChatAppTools(container).suggestions() }.getOrDefault(emptyList()) }
        me.rerere.rikkahub.AiHost.tools = { policy, chatId -> ChatAppTools(container, chatId).tools(policy) }
        // An action chat has nobody to approve: changes run by themselves, except deleting, which only a normal chat may ask for.
        me.rerere.rikkahub.AiHost.actionToolPolicy = {
            val names = chatTools.catalog().map { it.name }
            val off = names.filter { it.startsWith("delete_") || it.startsWith("bulk_delete") || it == "empty_trash" }.toSet()
            me.rerere.rikkahub.AiHost.ToolPolicy(off = off, auto = names.toSet() - off)
        }
        me.rerere.rikkahub.AiHost.toolCatalog = { chatTools.catalog() }
        val rates = HashMap<Activity, RefreshRatePreference>()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (activity.javaClass.name.startsWith("me.rerere.rikkahub.")) {
                    rates.getOrPut(activity) { RefreshRatePreference(activity) }.start()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                rates.remove(activity)?.stop()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
