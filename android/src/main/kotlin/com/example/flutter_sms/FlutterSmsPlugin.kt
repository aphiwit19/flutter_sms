package com.example.flutter_sms

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class FlutterSmsPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_sms")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "sendSMS" -> {
                val message = call.argument<String>("message")
                val recipients = call.argument<List<String>>("recipients")
                val sendDirect = call.argument<Boolean>("sendDirect") ?: false

                if (message == null || recipients == null) {
                    result.error("INVALID_ARGUMENTS", "Message and recipients cannot be null", null)
                    return
                }

                if (sendDirect) {
                    sendDirectSMS(recipients, message, result)
                } else {
                    sendSMS(recipients, message, result)
                }
            }
            "canSendSMS" -> {
                result.success(true)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun sendDirectSMS(recipients: List<String>, message: String, result: Result) {
        try {
            val smsManager = activity?.getSystemService(android.telephony.SmsManager::class.java)
            if (smsManager == null) {
                result.error("SMS_NOT_AVAILABLE", "SMS service not available", null)
                return
            }

            for (recipient in recipients) {
                smsManager.sendTextMessage(recipient, null, message, null, null)
            }
            result.success("SMS sent successfully")
        } catch (e: Exception) {
            result.error("SMS_ERROR", "Failed to send SMS: ${e.message}", null)
        }
    }

    private fun sendSMS(recipients: List<String>, message: String, result: Result) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${recipients.joinToString(";")}")
                putExtra("sms_body", message)
            }
            activity?.startActivity(intent)
            result.success("SMS app opened")
        } catch (e: Exception) {
            result.error("SMS_ERROR", "Failed to open SMS app: ${e.message}", null)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }
}