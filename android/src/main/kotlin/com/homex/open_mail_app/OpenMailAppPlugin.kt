package com.homex.open_mail_app

import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class OpenMailAppPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.flutterEngine.dartExecutor, "open_mail_app")
        channel.setMethodCallHandler(this);
        init(flutterPluginBinding.applicationContext)
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "open_mail_app")
            val plugin = OpenMailAppPlugin()
            channel.setMethodCallHandler(plugin)
            plugin.init(registrar.context())
        }
    }

    fun init(context: Context) {
        applicationContext = context
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "openMailApp") {
            val opened = emailAppIntent()
            if (opened) {
                result.success(null)
            } else {
                result.error("noMailApps", "No mail apps installed", "")
            }
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun emailAppIntent(): Boolean {
        val emailIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
        val packageManager = applicationContext.packageManager

        val activitiesHandlingEmails = packageManager.queryIntentActivities(emailIntent, 0)
        if (activitiesHandlingEmails.isNotEmpty()) {
            // use the first email package to create the chooserIntent
            val firstEmailPackageName = activitiesHandlingEmails.first().activityInfo.packageName
            val firstEmailInboxIntent = packageManager.getLaunchIntentForPackage(firstEmailPackageName)
            val emailAppChooserIntent = Intent.createChooser(firstEmailInboxIntent, "")

            // created UI for other email packages and add them to the chooser
            val emailInboxIntents = mutableListOf<LabeledIntent>()
            for (i in 1 until activitiesHandlingEmails.size) {
                val activityHandlingEmail = activitiesHandlingEmails[i]
                val packageName = activityHandlingEmail.activityInfo.packageName
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                emailInboxIntents.add(
                        LabeledIntent(
                                intent,
                                packageName,
                                activityHandlingEmail.loadLabel(packageManager),
                                activityHandlingEmail.icon
                        )
                )
            }
            val extraEmailInboxIntents = emailInboxIntents.toTypedArray()
            val finalIntent = emailAppChooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraEmailInboxIntents)
            applicationContext.startActivity(finalIntent);
            return true;
        } else {
            return false;
        }
    }
}
