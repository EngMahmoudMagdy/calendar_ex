package com.magdy.calendarex

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel


public class AlarmReceiver : BroadcastReceiver() {
    var NOTIFICATION_ID = "notification-id"
    override fun onReceive(context: Context?, intent: Intent?) {
        val manager =
            context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mainIntent = Intent(context, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pendingIntent =
            PendingIntent.getActivity(context, 444, mainIntent, PendingIntent.FLAG_ONE_SHOT)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(
                NOTIFICATION_ID,
                "NOTIFICATION_CHANNEL_NAME",
                importance
            )
            assert(manager != null)
            manager.createNotificationChannel(notificationChannel)
        }
        val noti = Notification.Builder(context)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_logo_white)
            .setContentTitle(
                context.getString(
                    R.string.app_name
                )
            )
            .setContentText("ok")
            .setAutoCancel(true)
            .setChannelId(NOTIFICATION_ID   )

        manager.notify(
            444,
            noti.build()
        )

//        Log.e("hello", "hello world")
//        Toast.makeText(context!!, "Hello world", Toast.LENGTH_LONG).show()
    }
}