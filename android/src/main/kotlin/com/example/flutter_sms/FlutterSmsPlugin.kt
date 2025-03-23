package com.example.flutter_sms

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
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
    private val TAG = "FlutterSmsPlugin"

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_sms")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "sendSMS" -> {
                val message = call.argument<String>("message")
                val recipientsArg = call.argument<Any>("recipients") // เปลี่ยนเป็น Any เพื่อป้องกัน ClassCastException
                val sendDirect = call.argument<Boolean>("sendDirect") ?: false

                // ตรวจสอบและแปลง recipients
                val recipients: List<String> = when (recipientsArg) {
                    is List<*> -> recipientsArg.filterIsInstance<String>()
                    is String -> listOf(recipientsArg) // ถ้าเป็น String ให้แปลงเป็น List<String>
                    else -> emptyList()
                }

                Log.d(TAG, "sendSMS called with sendDirect: $sendDirect, recipients: $recipients")

                if (message == null || recipients.isEmpty()) {
                    Log.e(TAG, "Invalid arguments: message=$message, recipients=$recipients")
                    result.error("INVALID_ARGUMENTS", "Message and recipients cannot be null or empty", null)
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
            if (activity == null) {
                Log.e(TAG, "Activity is null, cannot send SMS directly")
                result.error("ACTIVITY_NULL", "Activity is null", null)
                sendSMS(recipients, message, result) // Fallback to sendSMS
                return
            }

            val smsManager = activity?.getSystemService(android.telephony.SmsManager::class.java)
            if (smsManager == null) {
                Log.e(TAG, "SMS service not available")
                result.error("SMS_NOT_AVAILABLE", "SMS service not available", null)
                sendSMS(recipients, message, result) // Fallback to sendSMS
                return
            }

            for (recipient in recipients) {
                Log.d(TAG, "Sending SMS to $recipient")
                smsManager.sendTextMessage(recipient, null, message, null, null)
            }
            Log.d(TAG, "SMS sent successfully")
            result.success("SMS sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
            result.error("SMS_ERROR", "Failed to send SMS: ${e.message}", null)
            sendSMS(recipients, message, result) // Fallback to sendSMS
        }
    }

    private fun sendSMS(recipients: List<String>, message: String, result: Result) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${recipients.joinToString(";")}")
                putExtra("sms_body", message)
            }
            activity?.startActivity(intent)
            Log.d(TAG, "SMS app opened")
            result.success("SMS app opened")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open SMS app: ${e.message}")
            result.error("SMS_ERROR", "Failed to open SMS app: ${e.message}", null)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        Log.d(TAG, "Activity attached")
    }

    override fun onDetachedFromActivity() {
        activity = null
        Log.d(TAG, "Activity detached")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        Log.d(TAG, "Activity reattached")
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
        Log.d(TAG, "Activity detached for config changes")
    }
}