package skywa1ker404.absencium.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import me.rerere.rikkahub.RouteActivity

/** The one door into Absencium AI, the embedded chat. Every AI button in the app goes through here. */
object AbsenciumAi {
    /** Opens the chat, or with [settings] straight on its settings (providers, models, assistants). */
    fun open(context: Context, settings: Boolean = false, destination: String? = null) {
        val intent = Intent(context, RouteActivity::class.java)
        if (settings) intent.putExtra(RouteActivity.EXTRA_OPEN_SETTINGS, true)
        if (destination != null) intent.putExtra(RouteActivity.EXTRA_SETTINGS_DESTINATION, destination)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // A springy grow-in instead of the default cut, so opening the chat never flashes.
        val options = android.app.ActivityOptions.makeCustomAnimation(
            context,
            if (destination != null) me.rerere.rikkahub.R.anim.ai_page_enter else skywa1ker404.absencium.R.anim.ai_open_enter,
            if (destination != null) me.rerere.rikkahub.R.anim.ai_page_exit else skywa1ker404.absencium.R.anim.ai_open_exit,
        )
        context.startActivity(intent, options.toBundle())
    }
}
