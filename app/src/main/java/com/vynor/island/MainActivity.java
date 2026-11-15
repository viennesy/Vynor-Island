package com.vynor.island;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.function.IntConsumer;

/** Vynor Island app: Home / Customize / Plugins / Settings with a liquid-glass look. */
public class MainActivity extends Activity {
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final int ACCENT = 0xFF4C72F0, GREEN = 0xFF2ED8A0, AMBER = 0xFFF5A524, PURPLE = 0xFF9C7BFF, CYAN = 0xFF22C7E8, PINK = 0xFFE85D9A;
    static final String[] PROFILES = {"Subtle", "Balanced", "Strong"};
    static final String[][] PLUGINS = {
            {"notif", "Notifications", "Pop-up alerts from other apps"},
            {"media", "Music", "Now playing and playback controls"},
            {"timer", "Timer", "Countdown shown live in the island"},
            {"stopwatch", "Stopwatch", "Count-up shown live in the island"},
            {"charge", "Charging", "Battery level when you plug in or unplug"}};
    static final int[] P_ICON = {R.drawable.ic_bell, R.drawable.ic_music, R.drawable.ic_timer, R.drawable.ic_timer, R.drawable.ic_bolt};
    static final int[] P_TINT = {ACCENT, PINK, AMBER, CYAN, GREEN};
    static final String[] ISLAND_KEYS = {"idleW", "height", "radius", "opacity", "textSize", "islandColor", "posY", "posX",
            "speed", "bounce", "notifSec", "liquidMotion", "w_notif", "w_media", "w_timer", "w_stopwatch", "w_charge"};

    SharedPreferences sp;
    FrameLayout root, content;
    final LinearLayout[] navItems = new LinearLayout[4];
    final ImageView[] navIcons = new ImageView[4];
    final TextView[] navLabels = new TextView[4];
    boolean dark, glass;
    int profile, cText, cSub, cLine, tab, sub, pv;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("island", MODE_PRIVATE);
        int mode = sp.getInt("themeMode", 0);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        dark = mode == 2 || (mode == 0 && night);
        glass = sp.getBoolean("glass", true);
        profile = Math.max(0, Math.min(2, sp.getInt("profile", 1)));
        cText = dark ? 0xFFFFFFFF : 0xFF12162B;
        cSub = dark ? 0xFF8E96B8 : 0xFF5E6788;
        cLine = dark ? 0x1FFFFFFF : 0x14000000;
        int top = dark ? 0xFF080A14 : 0xFFEEF1FB, bot = dark ? 0xFF0C1226 : 0xFFE3E8F6;

        root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bot}));
        content = new FrameLayout(this);
        root.addView(content, new FrameLayout.LayoutParams(MATCH, MATCH));
        FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(MATCH, dp(72), Gravity.BOTTOM);
        nl.setMargins(dp(16), 0, dp(16), dp(12));
        root.addView(buildNav(), nl);
        setContentView(root);
        getWindow().setStatusBarColor(top);
        getWindow().setNavigationBarColor(bot);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0
                : (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
        tab = sp.getInt("tab", 0);
        sub = sp.getInt("sub", 0);
        sp.edit().remove("tab").remove("sub").apply();
        open();
    }

    @Override protected void onResume() { super.onResume(); if (content != null && sub == 0 && tab == 0) open(); }

    @Override public void onBackPressed() {
        if (sub != 0) { sub = 0; open(); } else super.onBackPressed();
    }

    void rebuild() { sp.edit().putInt("tab", tab).putInt("sub", sub).apply(); recreate(); }
    void go(int t) { tab = t; sub = 0; open(); }
    void goSub(int s) { sub = s; tab = s == 4 ? 3 : 1; open(); }

    void open() {
        content.removeAllViews();
        View v;
        if (sub == 1) v = subSize();
        else if (sub == 2) v = subAnim();
        else if (sub == 3) v = subEvents();
        else if (sub == 4) v = subGlass();
        else v = tab == 0 ? home() : tab == 1 ? customize() : tab == 2 ? plugins() : settings();
        content.addView(v);
        for (int i = 0; i < 4; i++) {
            boolean sel = i == tab;
            navIcons[i].setColorFilter(sel ? ACCENT : cSub);
            navLabels[i].setTextColor(sel ? ACCENT : cSub);
            navItems[i].setBackground(sel ? solid(dark ? 0x26FFFFFF : 0x14000000, dp(28)) : null);
        }
    }

    // ================= screens =================

    View home() {
        LinearLayout c = col("Vynor Island", "Liquid Glass Edition", false);
        boolean running = IslandService.instance != null;
        boolean ov = Settings.canDrawOverlays(this), na = notifAccess();
        int sc = running ? GREEN : AMBER;

        LinearLayout k = new LinearLayout(this);
        k.setOrientation(LinearLayout.VERTICAL);
        k.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, dark
                ? new int[]{running ? 0xFF0F2F36 : 0xFF33260F, 0xFF111733}
                : new int[]{running ? 0xFFE0F6EE : 0xFFFFF1DC, 0xFFFFFFFF});
        g.setCornerRadius(dp(32));
        g.setStroke(dp(2), a(sc, 0x66));
        k.setBackground(g);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout ring = new FrameLayout(this);
        GradientDrawable rg = new GradientDrawable();
        rg.setShape(GradientDrawable.OVAL);
        rg.setStroke(dp(2), a(sc, 0x55));
        ring.setBackground(rg);
        FrameLayout inner = new FrameLayout(this);
        GradientDrawable ig = new GradientDrawable();
        ig.setShape(GradientDrawable.OVAL);
        ig.setColor(sc);
        inner.setBackground(ig);
        inner.addView(icon(running ? R.drawable.ic_check : R.drawable.ic_bolt, 0xFF08201A, 26),
                new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        ring.addView(inner, new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(dp(70), dp(70));
        rl.rightMargin = dp(18);
        top.addView(ring, rl);
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        TextView es = text("Engine Status", 14, cSub, true);
        es.setLetterSpacing(0.1f);
        tx.addView(es);
        tx.addView(text(running ? "Service Connected" : "Service Stopped", 24, sc, true));
        top.addView(tx, new LinearLayout.LayoutParams(0, WRAP, 1f));
        View dot = new View(this);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(sc);
        dot.setBackground(dg);
        top.addView(dot, new LinearLayout.LayoutParams(dp(12), dp(12)));
        k.addView(top);

        FrameLayout pill = new FrameLayout(this);
        GradientDrawable pg = solid(0xFF05060B, dp(32));
        pg.setStroke(dp(1), 0x33FFFFFF);
        pill.setBackground(pg);
        LinearLayout in = new LinearLayout(this);
        in.setGravity(Gravity.CENTER);
        in.setPadding(dp(22), 0, dp(22), 0);
        pill.addView(in, new FrameLayout.LayoutParams(MATCH, MATCH));
        fillPreview(in);
        pill.setOnClickListener(v -> { pv = (pv + 1) % 4; fillPreview(in); });
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(dp(290), dp(64));
        pl.gravity = Gravity.CENTER_HORIZONTAL;
        pl.topMargin = dp(26);
        pl.bottomMargin = dp(18);
        k.addView(pill, pl);
        TextView cap = text("Your island right now — tap to explore each state", 14, cSub, false);
        cap.setGravity(Gravity.CENTER);
        k.addView(cap);

        LinearLayout bn = new LinearLayout(this);
        bn.setGravity(Gravity.CENTER_VERTICAL);
        bn.setPadding(dp(16), dp(14), dp(16), dp(14));
        bn.setBackground(solid(dark ? 0x33F5A524 : 0x26F5A524, dp(18)));
        bn.addView(icon(R.drawable.ic_bolt, AMBER, 18));
        String msg = !ov ? "Overlay permission needed — tap to grant"
                : !na ? "Notification access needed — tap to grant"
                : running ? "All required permissions granted — the island is live over your screen."
                : "Permissions OK — tap Start to show the island.";
        TextView mt = text(msg, 15, cText, false);
        mt.setPadding(dp(12), 0, 0, 0);
        bn.addView(mt, new LinearLayout.LayoutParams(0, WRAP, 1f));
        bn.setOnClickListener(v -> {
            if (!ov) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            else if (!na) startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(MATCH, WRAP);
        bl.topMargin = dp(16);
        k.addView(bn, bl);
        btn(k, running ? "Stop Island" : "Start Island", !running, v -> {
            if (running) stopService(new Intent(this, IslandService.class));
            else {
                if (Build.VERSION.SDK_INT >= 33)
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
                send(null, 0);
            }
            content.postDelayed(this::open, 700);
        });
        LinearLayout.LayoutParams kl = new LinearLayout.LayoutParams(MATCH, WRAP);
        kl.bottomMargin = dp(14);
        c.addView(k, kl);

        LinearLayout tiles = new LinearLayout(this);
        String gp = glass ? PROFILES[profile] : "Flat";
        tiles.addView(tile(activeCount() + "", "Active", PURPLE), tileLp());
        tiles.addView(tile(gp, "Glass profile", ACCENT), tileLp());
        tiles.addView(tile(version(), "Version", CYAN), tileLp());
        c.addView(tiles);

        section(c, "SHORTCUTS");
        LinearLayout s = card(c);
        row(s, R.drawable.ic_tune, ACCENT, "Customize", "Shape the island to your taste", chevron(), () -> go(1));
        row(s, R.drawable.ic_puzzle, AMBER, "Plugins", "Enable or disable island features", trail(activeCount() + ""), () -> go(2));
        return scr(c);
    }

    View customize() {
        LinearLayout c = col("Customize", "Shape the island to your taste", false);
        section(c, "ISLAND");
        LinearLayout k = card(c);
        row(k, R.drawable.ic_size, ACCENT, "Size, Shape & Position", "Dimensions, corner radius, margins & placement", chevron(), () -> goSub(1));
        row(k, R.drawable.ic_timer, PURPLE, "Animation Physics", "Presets, timing & liquid motion", chevron(), () -> goSub(2));
        row(k, R.drawable.ic_grid, GREEN, "Event Sizes", "Width per plugin (Media, Battery…)", chevron(), () -> goSub(3));
        section(c, "DISPLAY");
        LinearLayout d = card(c);
        toggle(d, 0, 0, "Notch Mode", "Attach to the top edge with a flat top, for notch phones", "notch", false, null, null);
        toggle(d, 0, 0, "Hide in Landscape", "Hide the island when the screen rotates", "hideLand", false, null, null);
        toggle(d, 0, 0, "Hide When Idle", "Hide the pill while nothing is playing or running", "hideIdle", false, null, null);
        return scr(c);
    }

    View plugins() {
        LinearLayout c = col("Plugins", "Enable or disable island features", false);
        section(c, "FEATURES");
        LinearLayout k = card(c);
        for (int i = 0; i < PLUGINS.length; i++)
            toggle(k, P_ICON[i], P_TINT[i], PLUGINS[i][1], PLUGINS[i][2], "pl_" + PLUGINS[i][0], true, null, null);
        section(c, "QUICK START");
        LinearLayout q = card(c);
        row(q, R.drawable.ic_timer, AMBER, "Start timer", "Pick the minutes, it counts down in the island", chevron(), this::timerDialog);
        row(q, R.drawable.ic_timer, CYAN, "Start stopwatch", "Counts up in the island", chevron(), () -> send("STOPWATCH", 0));
        row(q, R.drawable.ic_check, GREEN, "Stop timer / stopwatch", "You can also long-press the island", chevron(), () -> send("STOP", 0));
        return scr(c);
    }

    View settings() {
        LinearLayout c = col("Settings", "Appearance & about", false);
        section(c, "APPEARANCE");
        LinearLayout k = card(c);
        row(k, R.drawable.ic_moon, ACCENT, "Theme", "Switch between light and dark, or follow the system", null, null);
        segmented(k, new String[]{"System", "Light", "Dark"}, sp.getInt("themeMode", 0),
                i -> { sp.edit().putInt("themeMode", i).apply(); rebuild(); });
        section(c, "LIQUID GLASS");
        LinearLayout g = card(c);
        row(g, R.drawable.ic_drop, PURPLE, "Liquid Glass", "Material, outline & strength of the glass look",
                trail(glass ? "Liquid" : "Flat"), () -> goSub(4));
        section(c, "NOTIFICATIONS");
        LinearLayout n = card(c);
        toggle(n, 0, 0, "Block original notifications", "Dismiss the originals and show them only in the island", "block", false, null, null);
        row(n, 0, 0, "Restore blocked notifications", NotifService.blocked.size() + " waiting · re-posts them to your shade",
                chevron(), () -> { NotifService.restoreAll(this); open(); });
        section(c, "ABOUT");
        LinearLayout a = card(c);
        row(a, R.drawable.ic_bolt, GREEN, "Version", version(), null, null);
        return scr(c);
    }

    View subSize() {
        LinearLayout c = col("Size, Shape & Position", "Dimensions, corner radius, margins & placement", true);
        section(c, "SIZE");
        LinearLayout a = card(c);
        seek(a, "Collapsed width (dp)", "idleW", 60, 200, 110);
        seek(a, "Height (dp)", "height", 24, 60, 34);
        section(c, "SHAPE");
        LinearLayout b = card(c);
        seek(b, "Corner radius (dp)", "radius", 0, 40, 22);
        seek(b, "Opacity (%)", "opacity", 30, 100, 100);
        seek(b, "Text size (sp)", "textSize", 10, 18, 14);
        TextView ic = text("Island color", 16, cText, true);
        ic.setPadding(0, dp(14), 0, 0);
        b.addView(ic);
        swatches(b, Look.ISLAND, "islandColor");
        section(c, "POSITION");
        LinearLayout p = card(c);
        seek(p, "Top margin (dp)", "posY", 0, 120, 6);
        seek(p, "Horizontal offset (dp)", "posX", -150, 150, 0);
        btn(c, "Reset island settings", false, v -> {
            SharedPreferences.Editor ed = sp.edit();
            for (String key : ISLAND_KEYS) ed.remove(key);
            ed.apply();
            open();
        });
        return scr(c);
    }

    View subAnim() {
        LinearLayout c = col("Animation Physics", "Presets, timing & liquid motion", true);
        section(c, "PRESETS");
        LinearLayout a = card(c);
        final int[][] pr = {{320, 0}, {250, 4}, {300, 8}, {140, 2}};
        int cs = sp.getInt("speed", 250), cb = sp.getInt("bounce", 4), sel = -1;
        for (int i = 0; i < pr.length; i++) if (pr[i][0] == cs && pr[i][1] == cb) sel = i;
        segmented(a, new String[]{"Smooth", "Balanced", "Bouncy", "Snappy"}, sel, i -> {
            sp.edit().putInt("speed", pr[i][0]).putInt("bounce", pr[i][1]).apply();
            open();
        });
        section(c, "MOTION");
        LinearLayout b = card(c);
        toggle(b, 0, 0, "Liquid motion", "Springy overshoot and fade instead of a plain ease", "liquidMotion", true, null, null);
        seek(b, "Duration (ms)", "speed", 100, 600, 250);
        seek(b, "Bounce", "bounce", 0, 10, 4);
        section(c, "TIMING");
        LinearLayout t = card(c);
        seek(t, "Notification time (s)", "notifSec", 2, 10, 4);
        return scr(c);
    }

    View subEvents() {
        LinearLayout c = col("Event Sizes", "Compact size per plugin (Media, Battery…)", true);
        section(c, "EXPANDED WIDTH (DP)");
        LinearLayout a = card(c);
        seek(a, "Notifications", "w_notif", 160, 340, 260);
        seek(a, "Music", "w_media", 160, 340, 260);
        seek(a, "Timer", "w_timer", 160, 340, 260);
        seek(a, "Stopwatch", "w_stopwatch", 160, 340, 260);
        seek(a, "Charging", "w_charge", 160, 340, 260);
        return scr(c);
    }

    View subGlass() {
        LinearLayout c = col("Liquid Glass", "Material, outline & quality profiles", true);
        section(c, "MATERIAL");
        LinearLayout a = card(c);
        toggle(a, R.drawable.ic_drop, PURPLE, "Liquid glass", "Glass cards and a soft outline on the island. Off gives a flat solid look.", "glass", true, this::rebuild, null);
        section(c, "PROFILE");
        LinearLayout b = card(c);
        row(b, 0, 0, "Glass strength", "How see-through the cards are", null, null);
        segmented(b, PROFILES, profile, i -> { sp.edit().putInt("profile", i).apply(); rebuild(); });
        return scr(c);
    }

    // ================= building blocks =================

    int a(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    GradientDrawable solid(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        return g;
    }

    GradientDrawable cardBg(int r) {
        GradientDrawable g;
        if (!glass) {
            g = new GradientDrawable();
            g.setColor(dark ? 0xFF151A30 : 0xFFFFFFFF);
            g.setStroke(dp(1), dark ? 0x14FFFFFF : 0x14000000);
        } else {
            int[] fill = dark
                    ? new int[][]{{0xF0171C36, 0xF0121730}, {0xCC1D2448, 0xB3141A38}, {0x992C3868, 0x66171E40}}[profile]
                    : new int[][]{{0xFFFFFFFF, 0xFFF4F6FD}, {0xE6FFFFFF, 0xCCF1F4FF}, {0xB3FFFFFF, 0x80EAEFFF}}[profile];
            g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, fill);
            g.setStroke(dp(1), dark ? new int[]{0x22FFFFFF, 0x33FFFFFF, 0x66FFFFFF}[profile]
                    : new int[]{0x1A000000, 0x22000000, 0x2A000000}[profile]);
        }
        g.setCornerRadius(r);
        return g;
    }

    TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif-medium", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    ImageView icon(int res, int tint, int sizeDp) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(res);
        iv.setColorFilter(tint);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return iv;
    }

    ImageView chevron() { return icon(R.drawable.ic_chevron, cSub, 24); }

    LinearLayout trail(String badge) {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = text(badge, 13, GREEN, true);
        t.setPadding(dp(10), dp(3), dp(10), dp(3));
        GradientDrawable g = solid(a(GREEN, 0x26), dp(10));
        g.setStroke(dp(1), a(GREEN, 0x66));
        t.setBackground(g);
        l.addView(t);
        l.addView(chevron());
        return l;
    }

    LinearLayout col(String title, String subtitle, boolean back) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(20), dp(20), dp(120));
        if (back) {
            TextView bk = text("←  Back", 16, ACCENT, true);
            bk.setPadding(0, 0, 0, dp(8));
            bk.setOnClickListener(v -> onBackPressed());
            c.addView(bk);
        }
        c.addView(text(title, 34, cText, true));
        TextView s = text(subtitle, 17, cSub, false);
        s.setPadding(0, dp(2), 0, dp(6));
        c.addView(s);
        return c;
    }

    View scr(LinearLayout c) {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(c);
        return sv;
    }

    void section(LinearLayout c, String name) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(6), dp(22), 0, dp(10));
        View d = new View(this);
        d.setBackground(solid(ACCENT, dp(2)));
        r.addView(d, new LinearLayout.LayoutParams(dp(20), dp(3)));
        TextView t = text(name, 14, cSub, true);
        t.setLetterSpacing(0.18f);
        t.setPadding(dp(12), 0, 0, 0);
        r.addView(t);
        c.addView(r);
    }

    LinearLayout card(LinearLayout parent) {
        LinearLayout k = new LinearLayout(this);
        k.setOrientation(LinearLayout.VERTICAL);
        k.setPadding(dp(18), dp(6), dp(18), dp(6));
        k.setBackground(cardBg(dp(28)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(14);
        parent.addView(k, lp);
        return k;
    }

    LinearLayout row(LinearLayout card, int icon, int tint, String title, String desc, View trailing, Runnable click) {
        if (card.getChildCount() > 0) {
            View ln = new View(this);
            ln.setBackgroundColor(cLine);
            card.addView(ln, new LinearLayout.LayoutParams(MATCH, dp(1)));
        }
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(14), 0, dp(14));
        if (icon != 0) {
            FrameLayout tile = new FrameLayout(this);
            GradientDrawable g = solid(a(tint, 0x30), dp(15));
            g.setStroke(dp(1), a(tint, 0x66));
            tile.setBackground(g);
            tile.addView(icon(icon, tint, 24), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(dp(50), dp(50));
            tl.rightMargin = dp(16);
            r.addView(tile, tl);
        }
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.addView(text(title, 18, cText, true));
        if (desc != null) tx.addView(text(desc, 14, cSub, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, WRAP, 1f));
        if (trailing != null) r.addView(trailing);
        if (click != null) r.setOnClickListener(v -> click.run());
        card.addView(r);
        return r;
    }

    Switch sw(boolean on) {
        Switch s = new Switch(this);
        s.setChecked(on);
        s.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        s.setTrackTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{ACCENT, dark ? 0xFF3A4060 : 0xFFC4CBE2}));
        return s;
    }

    LinearLayout toggle(LinearLayout card, int icon, int tint, String title, String desc, String key, boolean def,
                        Runnable after, Runnable tap) {
        Switch s = sw(sp.getBoolean(key, def));
        s.setOnCheckedChangeListener((v, on) -> {
            sp.edit().putBoolean(key, on).apply();
            if (after != null) after.run();
        });
        Runnable click = tap;
        if (click == null) click = () -> s.toggle();
        return row(card, icon, tint, title, desc, s, click);
    }

    void segmented(LinearLayout card, String[] opts, int sel, IntConsumer on) {
        LinearLayout seg = new LinearLayout(this);
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        GradientDrawable g = solid(dark ? 0xFF121730 : 0xFFE9EDF8, dp(26));
        g.setStroke(dp(1), cLine);
        seg.setBackground(g);
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            TextView t = text(opts[i], opts.length > 3 ? 14 : 16, i == sel ? Color.WHITE : cSub, true);
            t.setGravity(Gravity.CENTER);
            if (i == sel) t.setBackground(solid(ACCENT, dp(22)));
            t.setOnClickListener(v -> on.accept(idx));
            seg.addView(t, new LinearLayout.LayoutParams(0, dp(44), 1f));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(12);
        card.addView(seg, lp);
    }

    void seek(LinearLayout card, String name, String key, int min, int max, int def) {
        LinearLayout head = new LinearLayout(this);
        head.setPadding(0, dp(14), 0, 0);
        TextView val = text("" + sp.getInt(key, def), 16, ACCENT, true);
        head.addView(text(name, 16, cText, true), new LinearLayout.LayoutParams(0, WRAP, 1f));
        head.addView(val);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(sp.getInt(key, def) - min);
        sb.setProgressTintList(ColorStateList.valueOf(ACCENT));
        sb.setThumbTintList(ColorStateList.valueOf(ACCENT));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean user) {
                val.setText("" + (v + min));
                if (user) sp.edit().putInt(key, v + min).apply();
            }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { }
        });
        card.addView(head);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(8);
        card.addView(sb, lp);
    }

    void swatches(LinearLayout p, int[] fills, String key) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(10), 0, dp(10));
        final GradientDrawable[] ds = new GradientDrawable[fills.length];
        int sel = sp.getInt(key, 0);
        for (int i = 0; i < fills.length; i++) {
            final int idx = i;
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(fills[i]);
            g.setStroke(dp(3), i == sel ? cText : 0x33808080);
            ds[i] = g;
            View s = new View(this);
            s.setBackground(g);
            s.setOnClickListener(x -> {
                sp.edit().putInt(key, idx).apply();
                for (int j = 0; j < ds.length; j++) ds[j].setStroke(dp(3), j == idx ? cText : 0x33808080);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.rightMargin = dp(12);
            row.addView(s, lp);
        }
        p.addView(row);
    }

    Button btn(LinearLayout p, String label, boolean primary, View.OnClickListener c) {
        Button bt = new Button(this);
        bt.setText(label);
        bt.setAllCaps(false);
        bt.setTextSize(16);
        bt.setTextColor(primary ? Color.WHITE : cText);
        bt.setStateListAnimator(null);
        bt.setBackground(primary ? solid(ACCENT, dp(18)) : cardBg(dp(18)));
        bt.setOnClickListener(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, dp(52));
        lp.topMargin = dp(14);
        p.addView(bt, lp);
        return bt;
    }

    LinearLayout tile(String big, String small, int color) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(6), dp(18), dp(6), dp(18));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{a(color, dark ? 0x40 : 0x26), a(color, dark ? 0x14 : 0x0D)});
        g.setCornerRadius(dp(24));
        g.setStroke(dp(1), a(color, 0x55));
        t.setBackground(g);
        TextView b = text(big, 21, color, true);
        b.setSingleLine(true);
        t.addView(b);
        t.addView(text(small, 14, cSub, true));
        return t;
    }

    LinearLayout.LayoutParams tileLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    void fillPreview(LinearLayout in) {
        in.removeAllViews();
        if (pv == 0) {
            int lvl = battery();
            in.addView(icon(R.drawable.ic_bolt, GREEN, 22));
            FrameLayout bar = new FrameLayout(this);
            bar.setBackground(solid(0xFF2A2B33, dp(10)));
            View fill = new View(this);
            fill.setBackground(solid(GREEN, dp(10)));
            bar.addView(fill, new FrameLayout.LayoutParams(dp(150) * lvl / 100, MATCH));
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(dp(150), dp(18));
            bl.setMargins(dp(12), 0, dp(12), 0);
            in.addView(bar, bl);
            in.addView(text(lvl + "%", 16, GREEN, true));
        } else {
            String[][] st = {{}, {"🎵 Now playing", "Song title · Artist"}, {"⏳ Timer", "04:59"}, {"💬 Notification", "New message"}};
            LinearLayout v = new LinearLayout(this);
            v.setOrientation(LinearLayout.VERTICAL);
            v.setGravity(Gravity.CENTER);
            v.addView(text(st[pv][0], 15, Color.WHITE, true));
            v.addView(text(st[pv][1], 12, 0xFFB0B4C4, false));
            in.addView(v);
        }
    }

    LinearLayout buildNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackground(cardBg(dp(36)));
        String[] names = {"Home", "Customize", "Plugins", "Settings"};
        int[] icons = {R.drawable.ic_home, R.drawable.ic_tune, R.drawable.ic_puzzle, R.drawable.ic_gear};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout t = new LinearLayout(this);
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER);
            ImageView iv = icon(icons[i], cSub, 24);
            TextView lb = text(names[i], 12, cSub, true);
            t.addView(iv);
            t.addView(lb);
            t.setOnClickListener(v -> go(idx));
            bar.addView(t, new LinearLayout.LayoutParams(0, MATCH, 1f));
            navItems[i] = t;
            navIcons[i] = iv;
            navLabels[i] = lb;
        }
        return bar;
    }

    // ================= helpers =================

    int activeCount() {
        int n = 0;
        for (String[] p : PLUGINS) if (sp.getBoolean("pl_" + p[0], true)) n++;
        return n;
    }

    String version() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (Exception e) { return "1.0"; }
    }

    boolean notifAccess() {
        String s = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(getPackageName());
    }

    int battery() {
        Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return 0;
        return b.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / Math.max(1, b.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
    }

    void timerDialog() {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText("5");
        new AlertDialog.Builder(this).setTitle("Timer (minutes)").setView(e)
                .setPositiveButton("Start", (d, w) -> {
                    int m;
                    try { m = Integer.parseInt(e.getText().toString()); } catch (Exception ex) { m = 5; }
                    send("TIMER", m);
                })
                .setNegativeButton("Cancel", null).show();
    }

    void send(String action, int min) {
        Intent i = new Intent(this, IslandService.class);
        if (action != null) { i.setAction(action); i.putExtra("min", min); }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }
}
