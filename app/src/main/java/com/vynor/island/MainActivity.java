package com.vynor.island;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    SharedPreferences sp;
    EditText et;
    Button restoreBtn;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("island", MODE_PRIVATE);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(40, 60, 40, 40);

        add(l, getString(R.string.grant_overlay), v -> startActivity(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        add(l, getString(R.string.grant_notif), v -> startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        add(l, getString(R.string.start), v -> {
            if (Build.VERSION.SDK_INT >= 33)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
            send(null, 0);
        });
        add(l, getString(R.string.stop_island), v -> stopService(new Intent(this, IslandService.class)));

        header(l, R.string.timer_section);
        et = new EditText(this);
        et.setHint(R.string.minutes_hint);
        et.setText("5");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        l.addView(et);
        add(l, getString(R.string.start_timer), v -> {
            int m;
            try { m = Integer.parseInt(et.getText().toString()); } catch (Exception e) { m = 5; }
            send("TIMER", m);
        });
        add(l, getString(R.string.start_stopwatch), v -> send("STOPWATCH", 0));
        add(l, getString(R.string.stop_timer), v -> send("STOP", 0));

        header(l, R.string.size_section);
        seek(l, R.string.idle_width, "idleW", 60, 200, 110);
        seek(l, R.string.expanded_width, "expW", 200, 340, 260);
        seek(l, R.string.height, "height", 24, 60, 34);
        seek(l, R.string.vertical_pos, "posY", 0, 120, 6);

        header(l, R.string.notif_section);
        Switch sw = new Switch(this);
        sw.setText(R.string.block_original);
        sw.setChecked(sp.getBoolean("block", false));
        sw.setOnCheckedChangeListener((v, on) -> sp.edit().putBoolean("block", on).apply());
        l.addView(sw);
        restoreBtn = add(l, "", v -> { NotifService.restoreAll(this); updateRestore(); });

        ScrollView sv = new ScrollView(this);
        sv.addView(l);
        setContentView(sv);
    }

    @Override protected void onResume() { super.onResume(); updateRestore(); }

    void updateRestore() { restoreBtn.setText(getString(R.string.restore, NotifService.blocked.size())); }

    void header(LinearLayout l, int res) {
        TextView t = new TextView(this);
        t.setText(res);
        t.setTextSize(18);
        t.setPadding(0, 40, 0, 10);
        l.addView(t);
    }

    void seek(LinearLayout l, int label, String key, int min, int max, int def) {
        final String name = getString(label);
        TextView tv = new TextView(this);
        tv.setText(name + ": " + sp.getInt(key, def));
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(sp.getInt(key, def) - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                tv.setText(name + ": " + (p + min));
                if (user) sp.edit().putInt(key, p + min).apply();
            }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { }
        });
        l.addView(tv);
        l.addView(sb);
    }

    Button add(LinearLayout l, String text, View.OnClickListener c) {
        Button bt = new Button(this);
        bt.setText(text);
        bt.setAllCaps(false);
        bt.setOnClickListener(c);
        l.addView(bt);
        return bt;
    }

    void send(String action, int min) {
        Intent i = new Intent(this, IslandService.class);
        if (action != null) { i.setAction(action); i.putExtra("min", min); }
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }
}
