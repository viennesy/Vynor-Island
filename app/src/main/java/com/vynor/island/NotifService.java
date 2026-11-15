package com.vynor.island;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotifService extends NotificationListenerService {
    static class Blocked { Icon icon; PendingIntent pi; CharSequence t, x; long when; }

    static final List<Blocked> blocked = Collections.synchronizedList(new ArrayList<Blocked>());
    static int nid = 1000;

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getPackageName().equals(getPackageName())) return;
        Notification n = sbn.getNotification();
        if ((n.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        if (Notification.CATEGORY_TRANSPORT.equals(n.category)) return;
        Bundle e = n.extras;
        CharSequence t = e.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence x = e.getCharSequence(Notification.EXTRA_TEXT);
        if (t == null && x == null) return;

        if (!getSharedPreferences("island", MODE_PRIVATE).getBoolean("pl_notif", true)) return;
        IslandService s = IslandService.instance;
        if (s != null) s.flash(t == null ? "" : t.toString(), x == null ? "" : x.toString(),
                getSharedPreferences("island", MODE_PRIVATE).getInt("notifSec", 4) * 1000L);

        if (getSharedPreferences("island", MODE_PRIVATE).getBoolean("block", false)) {
            Blocked b = new Blocked();
            b.icon = n.getSmallIcon();
            b.pi = n.contentIntent;
            b.t = t == null ? "" : t;
            b.x = x == null ? "" : x;
            b.when = n.when;
            blocked.add(b);
            if (blocked.size() > 50) blocked.remove(0);
            cancelNotification(sbn.getKey());
        }
    }

    /** Re-posts every blocked notification as a new notification from this app. */
    static void restoreAll(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("restored",
                c.getString(R.string.restored_channel), NotificationManager.IMPORTANCE_DEFAULT));
        synchronized (blocked) {
            for (Blocked b : blocked) {
                try { nm.notify(nid++, build(c, b, true)); }
                catch (Exception ex) { nm.notify(nid++, build(c, b, false)); }
            }
            blocked.clear();
        }
    }

    static Notification build(Context c, Blocked b, boolean origIcon) {
        Notification.Builder nb = new Notification.Builder(c, "restored")
                .setContentTitle(b.t).setContentText(b.x).setWhen(b.when).setAutoCancel(true);
        if (origIcon && b.icon != null) nb.setSmallIcon(b.icon);
        else nb.setSmallIcon(android.R.drawable.ic_dialog_info);
        if (b.pi != null) nb.setContentIntent(b.pi);
        return nb.build();
    }
}
