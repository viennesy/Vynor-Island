package com.vynor.island;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public class IslandService extends Service {
    static IslandService instance;

    SharedPreferences sp;
    WindowManager wm;
    WindowManager.LayoutParams lp;
    GradientDrawable bg;
    LinearLayout root, controls;
    TextView tTitle, tSub;
    final Handler h = new Handler(Looper.getMainLooper());
    String tmpTitle, tmpSub;
    long tmpUntil, timerEnd, stopStart;
    boolean expanded, receiverOn, force;
    int curW, idleW = 110, expW = 260, hgt = 34, posY = 6;
    MediaController mc;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override public IBinder onBind(Intent i) { return null; }

    final SharedPreferences.OnSharedPreferenceChangeListener pl = (s, k) -> h.post(this::applyPrefs);

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        sp = getSharedPreferences("island", MODE_PRIVATE);
        startFg();
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildView();
        sp.registerOnSharedPreferenceChangeListener(pl);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rcv, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(rcv, f);
        receiverOn = true;
        h.post(tick);
    }

    void startFg() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("island",
                getString(R.string.island_channel), NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "island")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.island_active)).build();
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
    }

    void buildView() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(14), dp(6), dp(14), dp(6));
        bg = new GradientDrawable();
        bg.setColor(Color.BLACK);
        root.setBackground(bg);

        tTitle = new TextView(this);
        tTitle.setTextColor(Color.WHITE);
        tTitle.setTextSize(14);
        tTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tTitle.setSingleLine(true);
        tTitle.setEllipsize(TextUtils.TruncateAt.END);
        tSub = new TextView(this);
        tSub.setTextColor(Color.LTGRAY);
        tSub.setTextSize(12);
        tSub.setSingleLine(true);
        tSub.setEllipsize(TextUtils.TruncateAt.END);

        controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        String[] sym = {"⏮", "⏯", "⏭"};
        for (int k = 0; k < 3; k++) {
            final int key = k;
            TextView b = new TextView(this);
            b.setText(sym[k]);
            b.setTextSize(24);
            b.setTextColor(Color.WHITE);
            b.setPadding(dp(18), dp(4), dp(18), dp(4));
            b.setOnClickListener(v -> media(key));
            controls.addView(b);
        }
        root.addView(tTitle);
        root.addView(tSub);
        root.addView(controls);
        tTitle.setVisibility(View.GONE);
        tSub.setVisibility(View.GONE);
        controls.setVisibility(View.GONE);

        // Tap: open/close music controls. Long press: stop timer/stopwatch.
        root.setOnClickListener(v -> { if (mc != null) { expanded = !expanded; render(); } });
        root.setOnLongClickListener(v -> { timerEnd = 0; stopStart = 0; render(); return true; });

        curW = dp(110);
        lp = new WindowManager.LayoutParams(curW, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if (Build.VERSION.SDK_INT >= 28)
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        wm.addView(root, lp);
        applyPrefs();
    }

    /** Reads size settings and applies them to the live island. */
    void applyPrefs() {
        idleW = sp.getInt("idleW", 110);
        expW = sp.getInt("expW", 260);
        hgt = sp.getInt("height", 34);
        posY = sp.getInt("posY", 6);
        if (root == null) return;
        root.setMinimumHeight(dp(hgt));
        bg.setCornerRadius(Math.min(dp(22), dp(hgt) / 2f));
        lp.y = dp(posY);
        force = true;
        render();
    }

    void media(int k) {
        if (mc == null) return;
        MediaController.TransportControls t = mc.getTransportControls();
        if (k == 0) t.skipToPrevious();
        else if (k == 2) t.skipToNext();
        else {
            PlaybackState s = mc.getPlaybackState();
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) t.pause(); else t.play();
        }
    }

    void findMedia() {
        mc = null;
        try {
            MediaSessionManager m = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            List<MediaController> l = m.getActiveSessions(new ComponentName(this, NotifService.class));
            for (MediaController c : l) {
                PlaybackState s = c.getPlaybackState();
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING) { mc = c; break; }
            }
            if (mc == null && !l.isEmpty()) mc = l.get(0);
        } catch (SecurityException ignored) { }
    }

    void render() {
        if (root == null) return;
        long now = System.currentTimeMillis();
        String t = null, s = null;
        boolean ctrl = false;
        findMedia();
        if (mc == null) expanded = false;

        if (now < tmpUntil) { t = tmpTitle; s = tmpSub; }
        else if (timerEnd > 0) {
            long left = timerEnd - now;
            if (left <= 0) {
                timerEnd = 0;
                vibrate();
                tmpTitle = "⏰ " + getString(R.string.timer_done); tmpSub = ""; tmpUntil = now + 6000;
                t = tmpTitle; s = "";
            } else { t = "⏳ " + getString(R.string.timer); s = fmt(left); }
        } else if (stopStart > 0) { t = "⏱ " + getString(R.string.stopwatch); s = fmt(now - stopStart); }
        else if (mc != null) {
            MediaMetadata md = mc.getMetadata();
            PlaybackState ps = mc.getPlaybackState();
            boolean play = ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
            if (md != null && (play || expanded)) {
                String title = md.getString(MediaMetadata.METADATA_KEY_TITLE);
                t = "🎵 " + (title == null ? getString(R.string.media) : title);
                s = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
                ctrl = expanded;
            }
        }

        boolean show = t != null;
        tTitle.setVisibility(show ? View.VISIBLE : View.GONE);
        tSub.setVisibility(show && s != null && !s.isEmpty() ? View.VISIBLE : View.GONE);
        controls.setVisibility(ctrl ? View.VISIBLE : View.GONE);
        if (show) { tTitle.setText(t); tSub.setText(s); }
        animateTo(dp(show ? (ctrl ? expW + 40 : expW) : idleW));
    }

    void animateTo(int w) {
        if (force) {
            force = false;
            lp.width = w;
            curW = w;
            try { wm.updateViewLayout(root, lp); } catch (Exception ignored) { }
            return;
        }
        if (w == curW) return;
        ValueAnimator a = ValueAnimator.ofInt(curW, w);
        a.setDuration(250);
        a.addUpdateListener(v -> {
            lp.width = (int) v.getAnimatedValue();
            try { wm.updateViewLayout(root, lp); } catch (Exception ignored) { }
        });
        a.start();
        curW = w;
    }

    String fmt(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s >= 3600) return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    void vibrate() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) v.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    void flash(String t, String s, long ms) {
        h.post(() -> {
            tmpTitle = t; tmpSub = s;
            tmpUntil = System.currentTimeMillis() + ms;
            render();
        });
    }

    int battery() {
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return 0;
        int l = b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int sc = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return l * 100 / sc;
    }

    final BroadcastReceiver rcv = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_POWER_CONNECTED.equals(i.getAction()))
                flash("⚡ " + getString(R.string.charging), battery() + "%", 3500);
            else flash("🔋 " + getString(R.string.unplugged), battery() + "%", 2500);
        }
    };

    final Runnable tick = new Runnable() {
        @Override public void run() { render(); h.postDelayed(this, 1000); }
    };

    @Override public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && i.getAction() != null) {
            long now = System.currentTimeMillis();
            switch (i.getAction()) {
                case "TIMER": timerEnd = now + i.getIntExtra("min", 1) * 60000L; stopStart = 0; break;
                case "STOPWATCH": stopStart = now; timerEnd = 0; break;
                case "STOP": timerEnd = 0; stopStart = 0; break;
            }
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        instance = null;
        h.removeCallbacksAndMessages(null);
        if (sp != null) sp.unregisterOnSharedPreferenceChangeListener(pl);
        if (root != null) { try { wm.removeView(root); } catch (Exception ignored) { } }
        if (receiverOn) { try { unregisterReceiver(rcv); } catch (Exception ignored) { } }
        super.onDestroy();
    }
}
