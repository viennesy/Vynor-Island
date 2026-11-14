package com.vynor.island;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/** Swipeable pages with theme colors, optional liquid-glass cards, and live island settings. */
public class MainActivity extends Activity {
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final int N = 5;
    static final float[] SCALE = {1f, 1.3f, 1.6f, 1.3f, 1f};

    SharedPreferences sp;
    EditText et;
    Button restoreBtn;
    FrameLayout root, iconBox;
    HorizontalScrollView hsv;
    final View[] dots = new View[N];
    final ArgbEvaluator ev = new ArgbEvaluator();
    int pageW, cur, fg, theme;
    int[] cols;
    boolean liquid;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("island", MODE_PRIVATE);
        pageW = getResources().getDisplayMetrics().widthPixels;
        liquid = sp.getBoolean("liquid", true);
        theme = Math.min(sp.getInt("theme", 0), Look.THEMES.length - 1);
        cols = Look.THEMES[theme];
        fg = liquid ? Color.WHITE : 0xFF212121;

        root = new FrameLayout(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setClipChildren(false);

        TextView title = text(getString(R.string.app_name), 20, Color.WHITE, true);
        title.setPadding(dp(24), dp(16), dp(24), 0);
        col.addView(title);

        FrameLayout holder = new FrameLayout(this);
        holder.setClipChildren(false);
        holder.addView(buildIcon(), new FrameLayout.LayoutParams(dp(110), dp(110), Gravity.CENTER));
        col.addView(holder, new LinearLayout.LayoutParams(MATCH, dp(170)));

        LinearLayout pages = new LinearLayout(this);
        View[] ps = {pageSetup(), pageTimer(), pageAppearance(), pageIsland(), pageNotif()};
        for (View v : ps) pages.addView(v, new LinearLayout.LayoutParams(pageW, MATCH));
        hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hsv.addView(pages);
        hsv.setOnScrollChangeListener((v, x, y, ox, oy) -> onScrolled(x));
        hsv.setOnTouchListener((v, e) -> {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                int p = Math.round(hsv.getScrollX() / (float) pageW);
                hsv.post(() -> hsv.smoothScrollTo(p * pageW, 0));
            }
            return false;
        });
        col.addView(hsv, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        LinearLayout dotRow = new LinearLayout(this);
        dotRow.setGravity(Gravity.CENTER);
        for (int k = 0; k < N; k++) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(Color.WHITE);
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(8), dp(8));
            lp.setMargins(dp(6), 0, dp(6), 0);
            dotRow.addView(d, lp);
            dots[k] = d;
        }
        col.addView(dotRow, new LinearLayout.LayoutParams(MATCH, dp(40)));

        root.addView(col);
        setContentView(root);
        final int pg = sp.getInt("page", 0);
        sp.edit().remove("page").apply();
        onScrolled(0);
        if (pg > 0) hsv.post(() -> hsv.scrollTo(pg * pageW, 0));
    }

    @Override protected void onResume() { super.onResume(); updateRestore(); }

    void rebuild() { sp.edit().putInt("page", cur).apply(); recreate(); }

    void onScrolled(int x) {
        float pos = Math.max(0f, Math.min(N - 1, x / (float) pageW));
        int i = Math.min(N - 2, (int) pos);
        float f = pos - i;
        cur = Math.round(pos);
        int c = (int) ev.evaluate(f, cols[i], cols[i + 1]);
        root.setBackgroundColor(c);
        getWindow().setStatusBarColor(c);
        getWindow().setNavigationBarColor(c);
        iconBox.setRotation(pos * 90f);
        float sc = SCALE[i] + (SCALE[i + 1] - SCALE[i]) * f;
        iconBox.setScaleX(sc);
        iconBox.setScaleY(sc);
        for (int k = 0; k < N; k++) {
            float d = Math.max(0f, 1f - Math.abs(pos - k));
            dots[k].setAlpha(0.35f + 0.65f * d);
            dots[k].setScaleX(1f + 0.5f * d);
            dots[k].setScaleY(1f + 0.5f * d);
        }
    }

    FrameLayout buildIcon() {
        iconBox = new FrameLayout(this);
        View pill = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xE6FFFFFF);
        g.setCornerRadius(dp(19));
        pill.setBackground(g);
        iconBox.addView(pill, new FrameLayout.LayoutParams(dp(100), dp(38), Gravity.CENTER));
        View dot = new View(this);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(0xFF212121);
        dot.setBackground(d);
        dot.setTranslationX(dp(34));
        iconBox.addView(dot, new FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER));
        return iconBox;
    }

    // ---- pages ----

    View pageSetup() {
        LinearLayout c = page(R.string.page_setup, R.string.sub_setup);
        LinearLayout k = card(c);
        btn(k, getString(R.string.grant_overlay), v -> startActivity(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        btn(k, getString(R.string.grant_notif), v -> startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        LinearLayout k2 = card(c);
        btn(k2, getString(R.string.start), v -> {
            if (Build.VERSION.SDK_INT >= 33)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
            send(null, 0);
        });
        btn(k2, getString(R.string.stop_island), v -> stopService(new Intent(this, IslandService.class)));
        return wrap(c);
    }

    View pageTimer() {
        LinearLayout c = page(R.string.timer_section, R.string.sub_timer);
        LinearLayout k = card(c);
        et = new EditText(this);
        et.setHint(R.string.minutes_hint);
        et.setText("5");
        et.setTextColor(fg);
        et.setHintTextColor(0x88808080);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        k.addView(et);
        btn(k, getString(R.string.start_timer), v -> {
            int m;
            try { m = Integer.parseInt(et.getText().toString()); } catch (Exception e) { m = 5; }
            send("TIMER", m);
        });
        LinearLayout k2 = card(c);
        btn(k2, getString(R.string.start_stopwatch), v -> send("STOPWATCH", 0));
        btn(k2, getString(R.string.stop_timer), v -> send("STOP", 0));
        return wrap(c);
    }

    View pageAppearance() {
        LinearLayout c = page(R.string.appearance, R.string.sub_appearance);
        LinearLayout k = card(c);
        k.addView(text(getString(R.string.theme) + ": " + Look.THEME_NAMES[theme], 15, fg, true));
        int[] fills = new int[Look.THEMES.length];
        for (int i = 0; i < fills.length; i++) fills[i] = Look.THEMES[i][0];
        swatches(k, fills, "theme", this::rebuild);
        LinearLayout k2 = card(c);
        Switch sw = new Switch(this);
        sw.setText(R.string.liquid);
        sw.setTextColor(fg);
        sw.setChecked(liquid);
        sw.setOnCheckedChangeListener((v, on) -> { sp.edit().putBoolean("liquid", on).apply(); rebuild(); });
        k2.addView(sw);
        return wrap(c);
    }

    View pageIsland() {
        LinearLayout c = page(R.string.island_section, R.string.sub_island);
        LinearLayout k = card(c);
        k.addView(text(getString(R.string.size_pos), 15, fg, true));
        seek(k, R.string.idle_width, "idleW", 60, 200, 110);
        seek(k, R.string.expanded_width, "expW", 200, 340, 260);
        seek(k, R.string.height, "height", 24, 60, 34);
        seek(k, R.string.vertical_pos, "posY", 0, 120, 6);
        LinearLayout k2 = card(c);
        k2.addView(text(getString(R.string.look), 15, fg, true));
        seek(k2, R.string.corner, "radius", 0, 40, 22);
        seek(k2, R.string.opacity, "opacity", 30, 100, 100);
        seek(k2, R.string.text_size, "textSize", 10, 18, 14);
        TextView ic = text(getString(R.string.island_color), 14, fg, false);
        ic.setPadding(0, dp(10), 0, 0);
        k2.addView(ic);
        swatches(k2, Look.ISLAND, "islandColor", null);
        LinearLayout k3 = card(c);
        k3.addView(text(getString(R.string.motion), 15, fg, true));
        seek(k3, R.string.anim_ms, "speed", 100, 600, 250);
        seek(k3, R.string.bounce, "bounce", 0, 10, 4);
        seek(k3, R.string.notif_sec, "notifSec", 2, 10, 4);
        btn(k3, getString(R.string.reset), v -> {
            SharedPreferences.Editor ed = sp.edit();
            for (String key : new String[]{"idleW", "expW", "height", "posY", "radius", "opacity",
                    "textSize", "islandColor", "speed", "bounce", "notifSec"}) ed.remove(key);
            ed.apply();
            rebuild();
        });
        return wrap(c);
    }

    View pageNotif() {
        LinearLayout c = page(R.string.notif_section, R.string.sub_notif);
        LinearLayout k = card(c);
        Switch sw = new Switch(this);
        sw.setText(R.string.block_original);
        sw.setTextColor(fg);
        sw.setChecked(sp.getBoolean("block", false));
        sw.setOnCheckedChangeListener((v, on) -> sp.edit().putBoolean("block", on).apply());
        k.addView(sw);
        restoreBtn = btn(k, "", v -> { NotifService.restoreAll(this); updateRestore(); });
        return wrap(c);
    }

    // ---- building blocks ----

    LinearLayout page(int titleRes, int subRes) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(8), dp(20), dp(20));
        c.addView(text(getString(titleRes), 26, Color.WHITE, true));
        TextView s = text(getString(subRes), 14, 0xCCFFFFFF, false);
        s.setPadding(0, dp(4), 0, dp(14));
        c.addView(s);
        return c;
    }

    View wrap(LinearLayout c) {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(c);
        return sv;
    }

    GradientDrawable glass(int r, int top, int bottom, int stroke) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(r);
        g.setStroke(dp(1), stroke);
        return g;
    }

    GradientDrawable solid(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r);
        return g;
    }

    LinearLayout card(LinearLayout parent) {
        LinearLayout k = new LinearLayout(this);
        k.setOrientation(LinearLayout.VERTICAL);
        k.setPadding(dp(18), dp(16), dp(18), dp(16));
        k.setBackground(liquid ? glass(dp(28), 0x59FFFFFF, 0x1FFFFFFF, 0x66FFFFFF) : solid(0xF2FFFFFF, dp(22)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(14);
        parent.addView(k, lp);
        return k;
    }

    TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    Button btn(LinearLayout p, String label, View.OnClickListener c) {
        Button bt = new Button(this);
        bt.setText(label);
        bt.setAllCaps(false);
        bt.setTextColor(Color.WHITE);
        bt.setStateListAnimator(null);
        bt.setBackground(liquid ? glass(dp(16), 0x73FFFFFF, 0x33FFFFFF, 0x99FFFFFF) : solid(0xFF212121, dp(14)));
        bt.setOnClickListener(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, dp(48));
        lp.topMargin = dp(8);
        p.addView(bt, lp);
        return bt;
    }

    void swatches(LinearLayout p, int[] fills, String key, Runnable after) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(8), 0, dp(4));
        final GradientDrawable[] ds = new GradientDrawable[fills.length];
        int sel = sp.getInt(key, 0);
        for (int i = 0; i < fills.length; i++) {
            final int idx = i;
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(fills[i]);
            g.setStroke(dp(3), i == sel ? fg : 0x33808080);
            ds[i] = g;
            View sw = new View(this);
            sw.setBackground(g);
            sw.setOnClickListener(x -> {
                sp.edit().putInt(key, idx).apply();
                for (int j = 0; j < ds.length; j++) ds[j].setStroke(dp(3), j == idx ? fg : 0x33808080);
                if (after != null) after.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.rightMargin = dp(12);
            row.addView(sw, lp);
        }
        p.addView(row);
    }

    void seek(LinearLayout p, int label, String key, int min, int max, int def) {
        final String name = getString(label);
        TextView tv = text(name + ": " + sp.getInt(key, def), 14, fg, false);
        tv.setPadding(0, dp(8), 0, 0);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(sp.getInt(key, def) - min);
        sb.setThumbTintList(ColorStateList.valueOf(fg));
        sb.setProgressTintList(ColorStateList.valueOf(fg));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean user) {
                tv.setText(name + ": " + (v + min));
                if (user) sp.edit().putInt(key, v + min).apply();
            }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { }
        });
        p.addView(tv);
        p.addView(sb);
    }

    void updateRestore() {
        if (restoreBtn != null) restoreBtn.setText(getString(R.string.restore, NotifService.blocked.size()));
    }

    void send(String action, int min) {
        Intent i = new Intent(this, IslandService.class);
        if (action != null) { i.setAction(action); i.putExtra("min", min); }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }
}
