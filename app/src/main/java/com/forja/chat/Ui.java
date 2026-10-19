package com.forja.chat;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = 0xFF070A1A;
    static final int DOCK = 0xFF080B1E;
    static final int PANEL = 0xFF0C1230;
    static final int CARD = 0xFF0E1330;
    static final int CARD2 = 0xFF161C42;
    static final int INPUT = 0xFF0A0F26;
    static final int TEXT = 0xFFEEF0FF;
    static final int SUB = 0xFF8C93B8;
    static final int ACCENT = 0xFF7B61FF;
    static final int ACCENT_TXT = 0xFF9C8CFF;
    static final int ACCENT_SOFT = 0x337B61FF;
    static final int ACCENT_LINE = 0xA68B78FF;
    static final int SEL = 0xFF6B66F5;
    static final int LINE = 0x297C86FF;
    static final int ONLINE = 0xFF2ED48A;
    static final int DANGER = 0xFFFF6B7A;
    static final int OFF = 0xFF5E6488;

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable shape(Context c, int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    static GradientDrawable circle(int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        return g;
    }

    static GradientDrawable ring(Context c, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(0);
        g.setStroke(dp(c, strokeDp), stroke);
        return g;
    }

    static GradientDrawable sendGradient() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF9A7BFF, 0xFF6A4DF4});
        g.setShape(GradientDrawable.OVAL);
        return g;
    }

    static LinearLayout.LayoutParams lp(int w, int h, float weight, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(l, t, r, b);
        return p;
    }

    static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    static TextView bold(TextView t) {
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }

    static ImageView icon(Context c, int res, int sizeDp, int color) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return iv;
    }

    static View iconBtn(Context c, int res, String desc, int tint, View.OnClickListener l) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(shape(c, CARD2, LINE, 1, 13));
        f.setContentDescription(desc);
        f.setClickable(true);
        f.setOnClickListener(l);
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        iv.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        f.addView(iv, new FrameLayout.LayoutParams(dp(c, 20), dp(c, 20), Gravity.CENTER));
        f.setLayoutParams(lp(dp(c, 40), dp(c, 40), 0f, dp(c, 4), 0, 0, 0));
        return f;
    }

    static TextView chip(Context c, String s, View.OnClickListener l) {
        TextView t = text(c, s, 13, TEXT);
        t.setSingleLine();
        t.setPadding(dp(c, 14), dp(c, 9), dp(c, 14), dp(c, 9));
        t.setBackground(shape(c, CARD, LINE, 1, 20));
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    static TextView pill(Context c, String s, boolean primary, View.OnClickListener l) {
        TextView t = bold(text(c, s, 13, primary ? 0xFFFFFFFF : TEXT));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 16), dp(c, 9), dp(c, 16), dp(c, 9));
        t.setBackground(primary ? shape(c, ACCENT, 0, 0, 12) : shape(c, CARD2, LINE, 1, 12));
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    /** Logo del asistente sobre un círculo con el color de su marca. */
    static View logo(Context c, Assistant a, int sizeDp) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(circle(a.color));
        int n = dp(c, sizeDp) * 58 / 100;
        if (a.logo != null) {
            ImageView iv = new ImageView(c);
            int id = c.getResources().getIdentifier(a.logo, "drawable", c.getPackageName());
            iv.setImageResource(id);
            if (a.logo.startsWith("ic_")) iv.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
            f.addView(iv, new FrameLayout.LayoutParams(n, n, Gravity.CENTER));
        } else {
            TextView t = bold(text(c, a.name.substring(0, 1).toUpperCase(), sizeDp * 0.42f, 0xFFFFFFFF));
            t.setGravity(Gravity.CENTER);
            f.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return f;
    }
}
