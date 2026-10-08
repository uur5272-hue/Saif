package com.example.motoai.action

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import com.example.motoai.data.model.MotoAction

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(val reason: String) : ActionResult()
    data class NeedsConfirmation(val message: String, val onConfirm: () -> ActionResult) : ActionResult()
}

class ActionDispatcher(private val context: Context) {

    // Common app packages mapping
    private val knownAppPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "settings" to "com.android.settings",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "play store" to "com.android.vending",
        "google play" to "com.android.vending",
        "free fire" to "com.dts.freefireth",
        "free fire max" to "com.dts.freefiremax",
        "spotify" to "com.spotify.music",
        "whatsapp" to "com.whatsapp",
        "gmail" to "com.google.android.gm"
    )

    fun execute(action: MotoAction, confirmed: Boolean = false): ActionResult {
        return when (action.intent) {
            "OPEN_APP" -> openApp(action.target ?: "")
            "WEB_SEARCH" -> searchWeb(action.target ?: "")
            "OPEN_URL" -> openUrl(action.target ?: "")
            "OPEN_SETTINGS" -> openSettings(action.target ?: "main")
            "MAKE_CALL" -> {
                if (action.requiresConfirmation && !confirmed) {
                    ActionResult.NeedsConfirmation(
                        action.confirmationMessage ?: "MOTO wants to call ${action.target}. Continue?"
                    ) {
                        makeCall(action.target ?: "")
                    }
                } else {
                    makeCall(action.target ?: "")
                }
            }
            "SEND_SMS" -> {
                if (action.requiresConfirmation && !confirmed) {
                    ActionResult.NeedsConfirmation(
                        "MOTO wants to send SMS to ${action.target}. Continue?"
                    ) {
                        sendSms(action.target ?: "", action.param ?: "")
                    }
                } else {
                    sendSms(action.target ?: "", action.param ?: "")
                }
            }
            "CREATE_REMINDER" -> createReminder(action.target ?: "Reminder")
            "START_NAVIGATION" -> startNavigation(action.target ?: "")
            "PLAY_STORE_SEARCH" -> openPlayStore(action.target ?: "")
            "GENERATE_IMAGE", "GENERATE_VIDEO", "CONVERSE" ->
                ActionResult.Success(action.displayText)
            else -> ActionResult.Success(action.displayText.ifEmpty { "Command recognized." })
        }
    }

    fun openApp(appName: String): ActionResult {
        val trimmed = appName.trim().lowercase()
        val pm: PackageManager = context.packageManager

        // Check known app packages first
        val targetPkg = knownAppPackages[trimmed]

        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult.Success("Opened $appName")
            } else {
                // Not installed, open Play Store page as required
                return openPlayStore(targetPkg)
            }
        }

        // Search installed apps by label
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val matched = resolveInfos.firstOrNull {
                it.loadLabel(pm).toString().lowercase().contains(trimmed)
            }

            if (matched != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ActionResult.Success("Opened ${matched.loadLabel(pm)}")
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        // If not installed on the device, open official Google Play Store page
        return openPlayStore(appName)
    }

    fun searchWeb(query: String): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Searching web for '$query'")
        } catch (e: Exception) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            ActionResult.Success("Opened web search for '$query'")
        }
    }

    fun openUrl(url: String): ActionResult {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened $cleanUrl")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to open URL: ${e.message}")
        }
    }

    fun openSettings(type: String): ActionResult {
        val action = when (type.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "app", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened Settings")
        } catch (e: Exception) {
            ActionResult.Failure("Unable to open settings: ${e.message}")
        }
    }

    fun makeCall(contactOrNumber: String): ActionResult {
        return try {
            // Use ACTION_DIAL so Android dialer is preloaded safely without dropping directly if permission denied
            val isNumber = contactOrNumber.matches(Regex("^[+0-9\\-\\s]+$"))
            val uri = if (isNumber) {
                Uri.parse("tel:${contactOrNumber.replace(" ", "")}")
            } else {
                Uri.parse("tel:") // Dial pad ready
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Dialer opened for $contactOrNumber")
        } catch (e: Exception) {
            ActionResult.Failure("Cannot launch phone call: ${e.message}")
        }
    }

    fun sendSms(contactOrNumber: String, body: String): ActionResult {
        return try {
            val uri = Uri.parse("smsto:${contactOrNumber.replace(" ", "")}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened messaging for $contactOrNumber")
        } catch (e: Exception) {
            ActionResult.Failure("Cannot open messaging: ${e.message}")
        }
    }

    fun createReminder(title: String): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, title)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Setting reminder/alarm: $title")
        } catch (e: Exception) {
            ActionResult.Failure("Alarm/Reminder app not available: ${e.message}")
        }
    }

    fun startNavigation(destination: String): ActionResult {
        return try {
            val gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webMap = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination))
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webMap)
            }
            ActionResult.Success("Starting navigation to $destination")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to start navigation: ${e.message}")
        }
    }

    fun openPlayStore(queryOrPackage: String): ActionResult {
        return try {
            val isPkg = queryOrPackage.contains(".")
            val uri = if (isPkg) {
                Uri.parse("market://details?id=$queryOrPackage")
            } else {
                Uri.parse("market://search?q=" + Uri.encode(queryOrPackage))
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened Play Store for $queryOrPackage")
        } catch (e: Exception) {
            // Web fallback
            val webUri = if (queryOrPackage.contains(".")) {
                Uri.parse("https://play.google.com/store/apps/details?id=$queryOrPackage")
            } else {
                Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(queryOrPackage))
            }
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ActionResult.Success("Opened Play Store web page")
        }
    }
}
