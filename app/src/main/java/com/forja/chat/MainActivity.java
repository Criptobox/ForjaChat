package com.forja.chat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity implements WebViewPool.Host {

    private static final int RQ_EXPORT = 1, RQ_IMPORT = 2, RQ_FILE = 3, RQ_PERM = 4;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final String[] ROUTES = {"AUTO", "A", "B", "C"};
    private static final String[] ROUTE_LABELS = {"Auto", "Web", "Local", "Asistencia"};
    private static final int[] ROUTE_ICONS = {R.drawable.ic_auto, R.drawable.ic_globe,
            R.drawable.ic_cpu, R.drawable.ic_handoff};

    private Prefs prefs;
    private WebViewPool pool;
    private boolean tablet;

    private LinearLayout sidebar, sideList, navRow;
    private FrameLayout headerBox, content;
    private View statusDot;
    private TextView statusText;
    private EditText input;
    private final View[] segBox = new View[4];
    private final ImageView[] segIcon = new ImageView[4];
    private final TextView[] segLabel = new TextView[4];

    private String current;   // null = inicio
    private String second;
    private boolean split;
    private ValueCallback<Uri[]> fileCb;
    private String statusMsg = "Tu espacio para todas las IA";

    private int d(int v) {
        return Ui.dp(this, v);
    }

    // ------------------------------------------------------------ ciclo de vida

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        pool = new WebViewPool(this, this);
        buildUi();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        buildUi();
    }

    @Override
    protected void onDestroy() {
        pool.destroyAll();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (current != null) {
            WebView w = pool.peek(current);
            if (w != null && w.canGoBack()) w.goBack();
            else showHome();
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------ estructura

    private void buildUi() {
        tablet = getResources().getConfiguration().screenWidthDp >= 700;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(body, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        sidebar = null;
        sideList = null;
        navRow = null;
        if (tablet) {
            sidebar = new LinearLayout(this);
            sidebar.setOrientation(LinearLayout.VERTICAL);
            sidebar.setPadding(d(14), d(18), d(10), d(14));
            sidebar.addView(brand(44));
            ScrollView sv = new ScrollView(this);
            sideList = new LinearLayout(this);
            sideList.setOrientation(LinearLayout.VERTICAL);
            sv.addView(sideList);
            LinearLayout.LayoutParams svp = new LinearLayout.LayoutParams(MATCH, 0, 1f);
            svp.setMargins(0, d(16), 0, d(8));
            sidebar.addView(sv, svp);
            sidebar.addView(wideButton("Agregar modelo", R.drawable.ic_plus, v -> editDialog(null)));
            body.addView(sidebar, new LinearLayout.LayoutParams(d(300), MATCH));
            View line = new View(this);
            line.setBackgroundColor(Ui.LINE);
            body.addView(line, new LinearLayout.LayoutParams(d(1), MATCH));
        }

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        body.addView(main, new LinearLayout.LayoutParams(0, MATCH, 1f));
        headerBox = new FrameLayout(this);
        main.addView(headerBox, new LinearLayout.LayoutParams(MATCH, WRAP));
        content = new FrameLayout(this);
        main.addView(content, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        main.addView(buildDock());

        if (tablet) {
            navRow = new LinearLayout(this);
            navRow.setOrientation(LinearLayout.HORIZONTAL);
            navRow.setBackgroundColor(Ui.DOCK);
            root.addView(navRow, new LinearLayout.LayoutParams(MATCH, d(66)));
        }
        setContentView(root);
        refresh();
    }

    private void refresh() {
        pool.detachAll();
        headerBox.removeAllViews();
        content.removeAllViews();
        if (current == null) {
            headerBox.addView(homeHeader());
            content.addView(tablet ? welcome() : phoneHome());
        } else {
            headerBox.addView(chatHeader());
            content.addView(chatBody());
        }
        if (tablet) {
            refreshSidebar();
            refreshNav();
        }
        styleSegs();
        statusText.setText(statusMsg);
        statusDot.setBackground(Ui.circle(isOnline() ? Ui.ONLINE : Ui.OFF));
    }

    // ------------------------------------------------------------ cabeceras

    private View brand(int logoDp) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_forja);
        r.addView(logo, new LinearLayout.LayoutParams(d(logoDp), d(logoDp)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(d(10), 0, 0, 0);
        SpannableString ss = new SpannableString("Forja Chat");
        ss.setSpan(new ForegroundColorSpan(0xFFB38CFF), 6, 10, 0);
        TextView t = Ui.bold(Ui.text(this, "", 22, Ui.TEXT));
        t.setText(ss);
        col.addView(t);
        TextView s = Ui.text(this, "TU ESPACIO PARA TODAS LAS IA", 9, 0xFF9AA1C8);
        s.setLetterSpacing(0.06f);
        col.addView(s);
        r.addView(col);
        return r;
    }

    private View homeHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(d(16), d(14), d(12), d(8));
        if (tablet) {
            h.addView(Ui.bold(Ui.text(this, "Bienvenido", 20, Ui.TEXT)),
                    new LinearLayout.LayoutParams(0, WRAP, 1f));
        } else {
            h.addView(brand(40), new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        h.addView(Ui.iconBtn(this, R.drawable.ic_dots, "Menú", Ui.TEXT, v -> showMenu()));
        h.setLayoutParams(new FrameLayout.LayoutParams(MATCH, WRAP));
        return h;
    }

    private View chatHeader() {
        Assistant a = prefs.find(current);
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(d(12), d(10), d(10), d(10));
        h.setBackground(Ui.shape(this, Ui.PANEL, Ui.LINE, 1, 22));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(MATCH, WRAP);
        hp.setMargins(d(8), d(8), d(8), d(2));
        h.setLayoutParams(hp);

        if (!tablet) {
            ImageView back = Ui.icon(this, R.drawable.ic_back, 24, 0xFFC9CDF5);
            back.setContentDescription("Volver");
            back.setOnClickListener(v -> showHome());
            back.setPadding(0, 0, d(8), 0);
            h.addView(back);
        }
        h.addView(Ui.logo(this, a, tablet ? 52 : 42));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(d(12), 0, 0, 0);
        col.addView(Ui.bold(Ui.text(this, a.name, tablet ? 20 : 17, Ui.TEXT)));
        boolean on = isOnline();
        col.addView(Ui.text(this, a.vendor + (on ? "  ·  En línea" : "  ·  Sin conexión"), 12,
                on ? Ui.ONLINE : Ui.SUB));
        h.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));

        if (tablet) {
            h.addView(Ui.iconBtn(this, R.drawable.ic_plus, "Nuevo chat", Ui.TEXT, v -> {
                WebView w = pool.peek(current);
                Assistant cur = prefs.find(current);
                if (w != null && cur != null) w.loadUrl(cur.url);
            }));
        }
        h.addView(Ui.iconBtn(this, R.drawable.ic_split, "Vista dividida",
                split ? Ui.ACCENT_TXT : Ui.TEXT, v -> toggleSplit()));
        h.addView(Ui.iconBtn(this, R.drawable.ic_dots, "Menú", Ui.TEXT, v -> showMenu()));
        return h;
    }

    // ------------------------------------------------------------ contenido

    private View pane(Assistant a) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Ui.shape(this, Ui.PANEL, Ui.LINE, 1, 18));
        f.setClipToOutline(true);
        f.addView(pool.get(a), new FrameLayout.LayoutParams(MATCH, MATCH));
        return f;
    }

    private View chatBody() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(d(8), d(6), d(8), d(4));
        Assistant a = prefs.find(current);
        col.addView(pane(a), new LinearLayout.LayoutParams(MATCH, 0, 1f));
        Assistant b = split ? prefs.find(second) : null;
        if (b != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, 0, 1f);
            lp.setMargins(0, d(8), 0, 0);
            col.addView(pane(b), lp);
        }
        return col;
    }

    private View welcome() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(d(40), d(20), d(40), d(20));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_forja);
        col.addView(logo, new LinearLayout.LayoutParams(d(88), d(88)));
        TextView t = Ui.bold(Ui.text(this, "Tu espacio para todas las IA", 22, Ui.TEXT));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, d(16), 0, d(8));
        col.addView(t);
        TextView s = Ui.text(this, "Elige un asistente en la barra lateral. Cada uno guarda su historial en su propia web.", 15, Ui.SUB);
        s.setGravity(Gravity.CENTER);
        col.addView(s);
        TextView r = Ui.text(this, "Ruta Web: rellena el chat y tú pulsas enviar\nRuta Asistencia: copia tu pregunta para pegarla\nRuta Local: próximamente", 13, 0xFFB6BCE4);
        r.setPadding(d(18), d(14), d(18), d(14));
        r.setBackground(Ui.shape(this, Ui.CARD, Ui.LINE, 1, 16));
        r.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(WRAP, WRAP);
        rp.setMargins(0, d(24), 0, 0);
        col.addView(r, rp);
        return col;
    }

    private View phoneHome() {
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(d(14), d(4), d(14), d(10));

        final EditText search = new EditText(this);
        search.setHint("Buscar asistente");
        search.setHintTextColor(Ui.SUB);
        search.setTextColor(Ui.TEXT);
        search.setSingleLine(true);
        search.setBackground(Ui.shape(this, Ui.CARD, Ui.LINE, 1, 14));
        search.setPadding(d(14), d(12), d(14), d(12));
        col.addView(search);

        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);
        renderPhoneList(list, "");
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable e) {
                renderPhoneList(list, e.toString());
            }
        });
        col.addView(wideButton("Agregar modelo", R.drawable.ic_plus, v -> editDialog(null)),
                rowLp(d(10)));
        sv.addView(col);
        return sv;
    }

    private void renderPhoneList(LinearLayout list, String query) {
        list.removeAllViews();
        String q = query.trim().toLowerCase();
        Set<String> favs = prefs.favs();
        TextView lab = Ui.bold(Ui.text(this, "Asistentes", 13, Ui.SUB));
        lab.setPadding(d(4), d(12), 0, d(2));
        list.addView(lab);
        boolean any = false;
        for (int pass = 0; pass < 2; pass++) {
            for (Assistant a : prefs.all()) {
                boolean f = favs.contains(a.id);
                if ((pass == 0) != f) continue;
                if (!q.isEmpty() && !a.name.toLowerCase().contains(q)) continue;
                any = true;
                list.addView(assistantRow(a, false), rowLp(d(7)));
            }
        }
        if (!any) list.addView(Ui.text(this, "Sin resultados", 14, Ui.SUB));
    }

    private LinearLayout.LayoutParams rowLp(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.setMargins(0, top, 0, 0);
        return lp;
    }

    private View wideButton(String label, int icon, View.OnClickListener l) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(d(16), d(12), d(16), d(12));
        b.setBackground(Ui.shape(this, 0xFF121838, Ui.LINE, 1, 16));
        b.setClickable(true);
        b.setOnClickListener(l);
        b.addView(Ui.icon(this, icon, 20, Ui.TEXT));
        TextView t = Ui.text(this, label, 15, Ui.TEXT);
        t.setPadding(d(10), 0, 0, 0);
        b.addView(t);
        return b;
    }

    // ------------------------------------------------------------ lista de asistentes

    private View assistantRow(final Assistant a, boolean selected) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(d(10), d(9), d(10), d(9));
        r.setBackground(selected ? Ui.shape(this, Ui.CARD, Ui.SEL, 2, 18)
                : Ui.shape(this, Ui.CARD, Ui.LINE, 1, 18));
        r.addView(Ui.logo(this, a, 42));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(d(12), 0, 0, 0);
        col.addView(Ui.bold(Ui.text(this, a.name, 16, Ui.TEXT)));
        col.addView(Ui.text(this, a.vendor, 12, Ui.SUB));
        r.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));

        boolean fav = prefs.favs().contains(a.id);
        ImageView star = Ui.icon(this, fav ? R.drawable.ic_star_fill : R.drawable.ic_star, 20,
                fav ? Ui.ACCENT : Ui.OFF);
        star.setContentDescription("Favorito");
        star.setPadding(d(4), d(4), d(4), d(4));
        star.setOnClickListener(v -> {
            prefs.toggleFav(a.id);
            refresh();
        });
        r.addView(star);
        View dot = new View(this);
        dot.setBackground(Ui.circle(Ui.ONLINE));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(d(9), d(9));
        dp.setMargins(d(8), 0, d(2), 0);
        r.addView(dot, dp);

        r.setClickable(true);
        r.setOnClickListener(v -> showAssistant(a.id));
        r.setOnLongClickListener(v -> {
            if (!a.custom) return false;
            new AlertDialog.Builder(this)
                    .setItems(new String[]{"Editar", "Eliminar"}, (dlg, which) -> {
                        if (which == 0) editDialog(a);
                        else deleteCustom(a);
                    }).show();
            return true;
        });
        return r;
    }

    private void refreshSidebar() {
        sideList.removeAllViews();
        TextView lab = Ui.bold(Ui.text(this, "Asistentes", 14, Ui.SUB));
        lab.setPadding(d(6), 0, 0, d(2));
        sideList.addView(lab);
        Set<String> favs = prefs.favs();
        for (int pass = 0; pass < 2; pass++) {
            for (Assistant a : prefs.all()) {
                if ((pass == 0) != favs.contains(a.id)) continue;
                sideList.addView(assistantRow(a, a.id.equals(current)), rowLp(d(8)));
            }
        }
        List<String> rec = prefs.recents();
        if (!rec.isEmpty()) {
            TextView l2 = Ui.bold(Ui.text(this, "Recientes", 14, Ui.SUB));
            l2.setPadding(d(6), d(20), 0, d(4));
            sideList.addView(l2);
            for (final String id : rec) {
                final Assistant a = prefs.find(id);
                if (a == null) continue;
                LinearLayout r = new LinearLayout(this);
                r.setOrientation(LinearLayout.HORIZONTAL);
                r.setGravity(Gravity.CENTER_VERTICAL);
                r.setPadding(d(4), d(6), d(4), d(6));
                FrameLayout c = new FrameLayout(this);
                c.setBackground(Ui.circle(0xFF141A3A));
                ImageView ic = new ImageView(this);
                ic.setImageResource(R.drawable.ic_chat);
                ic.setColorFilter(0xFFC4C9F0);
                c.addView(ic, new FrameLayout.LayoutParams(d(18), d(18), Gravity.CENTER));
                r.addView(c, new LinearLayout.LayoutParams(d(38), d(38)));
                TextView t = Ui.text(this, a.name, 15, 0xFFDDE0FA);
                t.setPadding(d(12), 0, 0, 0);
                r.addView(t);
                r.setClickable(true);
                r.setOnClickListener(v -> showAssistant(id));
                sideList.addView(r);
            }
        }
    }

    // ------------------------------------------------------------ navegación inferior (tablet)

    private void refreshNav() {
        navRow.removeAllViews();
        addTab("Inicio", R.drawable.ic_home, current == null, v -> showHome());
        addTab("Chats", R.drawable.ic_chat, current != null, v -> {
            if (current != null) return;
            String id = prefs.last();
            if (prefs.find(id) == null) id = Prefs.BUILTIN[0].id;
            showAssistant(id);
        });
        addTab("Ajustes", R.drawable.ic_settings, false, v -> showMenu());
    }

    private void addTab(String label, int icon, boolean active, View.OnClickListener l) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(l);
        FrameLayout box = new FrameLayout(this);
        if (active) box.setBackground(Ui.shape(this, 0xFF6B5CFF, 0, 0, 14));
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(active ? 0xFFFFFFFF : Ui.SUB);
        box.addView(iv, new FrameLayout.LayoutParams(d(24), d(24), Gravity.CENTER));
        t.addView(box, new LinearLayout.LayoutParams(d(56), d(34)));
        TextView tv = Ui.bold(Ui.text(this, label, 11, active ? Ui.ACCENT_TXT : Ui.SUB));
        t.addView(tv);
        navRow.addView(t, new LinearLayout.LayoutParams(0, MATCH, 1f));
    }

    // ------------------------------------------------------------ dock (rutas + entrada)

    private View buildDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.VERTICAL);
        dock.setBackgroundColor(Ui.DOCK);
        dock.setPadding(d(14), d(10), d(14), d(12));

        LinearLayout sr = new LinearLayout(this);
        sr.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this);
        sr.addView(statusDot, new LinearLayout.LayoutParams(d(7), d(7)));
        statusText = Ui.text(this, "", 12, 0xFFB6BCE4);
        statusText.setPadding(d(8), 0, 0, 0);
        statusText.setMaxLines(2);
        sr.addView(statusText, new LinearLayout.LayoutParams(0, WRAP, 1f));
        dock.addView(sr);

        LinearLayout segs = new LinearLayout(this);
        segs.setOrientation(LinearLayout.HORIZONTAL);
        segs.setPadding(d(4), d(4), d(4), d(4));
        segs.setBackground(Ui.shape(this, Ui.CARD, Ui.LINE, 1, 16));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout s = new LinearLayout(this);
            s.setOrientation(LinearLayout.VERTICAL);
            s.setGravity(Gravity.CENTER);
            s.setPadding(0, d(8), 0, d(8));
            s.setOnClickListener(v -> onSeg(idx));
            ImageView iv = new ImageView(this);
            s.addView(iv, new LinearLayout.LayoutParams(d(18), d(18)));
            iv.setImageResource(ROUTE_ICONS[i]);
            TextView tv = Ui.bold(Ui.text(this, ROUTE_LABELS[i], 11, Ui.SUB));
            tv.setPadding(0, d(3), 0, 0);
            s.addView(tv);
            segBox[i] = s;
            segIcon[i] = iv;
            segLabel[i] = tv;
            segs.addView(s, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        dock.addView(segs, rowLp(d(10)));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(d(16), d(5), d(5), d(5));
        bar.setBackground(Ui.shape(this, Ui.INPUT, 0xFF232A5A, 1, 26));
        input = new EditText(this);
        input.setBackground(null);
        input.setHint("Escribe tu pregunta…");
        input.setHintTextColor(0xFF7F87AD);
        input.setTextColor(Ui.TEXT);
        input.setTextSize(15);
        input.setMaxLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        bar.addView(input, new LinearLayout.LayoutParams(0, WRAP, 1f));
        FrameLayout send = new FrameLayout(this);
        send.setBackground(Ui.sendGradient());
        send.setContentDescription("Enviar");
        send.setClickable(true);
        send.setOnClickListener(v -> onSend());
        ImageView si = new ImageView(this);
        si.setImageResource(R.drawable.ic_send);
        si.setColorFilter(0xFFFFFFFF);
        send.addView(si, new FrameLayout.LayoutParams(d(22), d(22), Gravity.CENTER));
        bar.addView(send, new LinearLayout.LayoutParams(d(44), d(44)));
        dock.addView(bar, rowLp(d(10)));
        return dock;
    }

    private void styleSegs() {
        String sel = prefs.route();
        for (int i = 0; i < 4; i++) {
            boolean on = ROUTES[i].equals(sel);
            boolean off = "B".equals(ROUTES[i]);
            segBox[i].setBackground(on ? Ui.shape(this, Ui.ACCENT_SOFT, Ui.ACCENT_LINE, 1, 12) : null);
            int col = on ? 0xFFD9D3FF : Ui.SUB;
            segIcon[i].setColorFilter(col);
            segLabel[i].setTextColor(col);
            segBox[i].setAlpha(off ? 0.45f : 1f);
        }
    }

    private void onSeg(int idx) {
        if ("B".equals(ROUTES[idx])) {
            toast("IA local: próximamente");
            return;
        }
        prefs.setRoute(ROUTES[idx]);
        statusMsg = "Ruta: " + ROUTE_LABELS[idx];
        styleSegs();
        statusText.setText(statusMsg);
    }

    private void setStatus(String s) {
        statusMsg = s;
        statusText.setText(s);
    }

    // ------------------------------------------------------------ acciones

    private void showHome() {
        current = null;
        refresh();
    }

    private void showAssistant(String id) {
        if (prefs.find(id) == null) {
            showHome();
            return;
        }
        current = id;
        prefs.addRecent(id);
        refresh();
    }

    private void toggleSplit() {
        if (split) {
            split = false;
            second = null;
            refresh();
            return;
        }
        if (current == null) {
            toast("Abre primero un asistente");
            return;
        }
        final List<Assistant> opts = new ArrayList<>();
        for (Assistant a : prefs.all()) if (!a.id.equals(current)) opts.add(a);
        String[] names = new String[opts.size()];
        for (int i = 0; i < names.length; i++) names[i] = opts.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle("Elige el segundo asistente")
                .setItems(names, (dlg, which) -> {
                    split = true;
                    second = opts.get(which).id;
                    refresh();
                }).show();
    }

    private void editDialog(final Assistant existing) {
        int p = d(16);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(p, p, p, 0);
        final EditText name = new EditText(this);
        name.setHint("Nombre del modelo");
        final EditText url = new EditText(this);
        url.setHint("URL del chat (https://…)");
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        if (existing != null) {
            name.setText(existing.name);
            url.setText(existing.url);
        }
        box.addView(name);
        box.addView(url);
        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Agregar modelo" : "Editar modelo")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dlg, w) -> {
                    String n = name.getText().toString().trim();
                    String u = url.getText().toString().trim();
                    if (n.isEmpty()) {
                        toast("Ponle un nombre al modelo.");
                        return;
                    }
                    if (!u.matches("^https://.+")) {
                        toast("La URL debe empezar con https://");
                        return;
                    }
                    List<Assistant> cs = prefs.customs();
                    if (existing != null) {
                        for (int i = 0; i < cs.size(); i++) {
                            if (cs.get(i).id.equals(existing.id)) {
                                cs.set(i, new Assistant(existing.id, n, u));
                            }
                        }
                    } else {
                        cs.add(new Assistant("c_" + System.currentTimeMillis(), n, u));
                    }
                    prefs.saveCustoms(cs);
                    toast(existing == null ? "Modelo agregado" : "Modelo actualizado");
                    refresh();
                }).show();
    }

    private void deleteCustom(Assistant a) {
        List<Assistant> cs = prefs.customs();
        for (int i = cs.size() - 1; i >= 0; i--) if (cs.get(i).id.equals(a.id)) cs.remove(i);
        prefs.saveCustoms(cs);
        if (a.id.equals(second)) {
            split = false;
            second = null;
        }
        if (a.id.equals(current)) current = null;
        refresh();
    }

    // ------------------------------------------------------------ envío (coordinador)

    private void onSend() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            toast("Escribe tu pregunta");
            return;
        }
        Coordinator.Decision d = Coordinator.decide(text, prefs.route(), isOnline(),
                prefs.inject(), prefs.all(), current);
        if (d.route == 'X') {
            setStatus(d.reason);
            toast(d.reason);
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("ForjaChat", text));

        List<Assistant> targets = new ArrayList<>();
        if (split && current != null && prefs.find(second) != null) {
            targets.add(prefs.find(current));
            targets.add(prefs.find(second));
        } else {
            targets.add(d.target);
        }
        statusMsg = "";
        showAssistant(targets.get(0).id);

        StringBuilder names = new StringBuilder();
        for (Assistant a : targets) {
            if (names.length() > 0) names.append(" + ");
            names.append(a.name);
        }
        if (d.route == 'A') {
            for (Assistant a : targets) pool.fill(a, text);
            setStatus("Ruta Web · " + names + " (" + d.reason + ") · texto rellenado, pulsa enviar en el chat");
        } else {
            setStatus("Ruta Asistencia · " + names + " (" + d.reason + ") · pregunta copiada");
            toast("Pregunta copiada. Pega en el chat");
        }
        input.setText("");
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities c = cm.getNetworkCapabilities(n);
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // ------------------------------------------------------------ menú

    private void showMenu() {
        String[] items = {
                "Recargar todos los asistentes",
                "Cerrar todas las sesiones",
                "Exportar modelos (JSON)",
                "Importar modelos (JSON)",
                "Rellenado automático (ruta Web): " + (prefs.inject() ? "activado" : "desactivado"),
                "Acerca de"
        };
        new AlertDialog.Builder(this).setItems(items, (dlg, which) -> {
            switch (which) {
                case 0:
                    pool.reloadAll();
                    toast("Asistentes recargados");
                    break;
                case 1:
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    WebStorage.getInstance().deleteAllData();
                    pool.reloadAll();
                    toast("Sesiones borradas");
                    break;
                case 2:
                    Intent e = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    e.addCategory(Intent.CATEGORY_OPENABLE);
                    e.setType("application/json");
                    e.putExtra(Intent.EXTRA_TITLE, "forjachat-modelos.json");
                    startActivityForResult(e, RQ_EXPORT);
                    break;
                case 3:
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, RQ_IMPORT);
                    break;
                case 4:
                    prefs.setInject(!prefs.inject());
                    toast("Rellenado automático " + (prefs.inject() ? "activado" : "desactivado"));
                    break;
                default:
                    new AlertDialog.Builder(this)
                            .setTitle("ForjaChat 1.5")
                            .setMessage("Reúne varias IA en un solo lugar. Cada asistente guarda su "
                                    + "historial en su propia web; ForjaChat no envía tus chats a "
                                    + "ningún servidor propio.\n\nRuta Web: rellena el chat (tú "
                                    + "pulsas enviar).\nRuta Asistencia: copia tu pregunta para "
                                    + "pegarla.\nRuta Local: próximamente.")
                            .setPositiveButton("OK", null).show();
            }
        }).show();
    }

    // ------------------------------------------------------------ resultados y permisos

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == RQ_FILE) {
            if (fileCb != null) {
                fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
                fileCb = null;
            }
            return;
        }
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (req == RQ_EXPORT) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(Prefs.customsJson(prefs.customs()).getBytes(StandardCharsets.UTF_8));
                os.close();
                toast("Modelos exportados");
            } else if (req == RQ_IMPORT) {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONArray arr = new JSONArray(sb.toString());
                List<Assistant> cs = prefs.customs();
                int added = 0;
                for (int k = 0; k < arr.length(); k++) {
                    JSONObject o = arr.getJSONObject(k);
                    String n = o.getString("name"), u = o.getString("url");
                    if (!u.startsWith("https://")) continue;
                    boolean dup = false;
                    for (Assistant a : cs) if (a.url.equals(u)) dup = true;
                    if (dup) continue;
                    cs.add(new Assistant("c_" + System.currentTimeMillis() + "_" + k, n, u));
                    added++;
                }
                prefs.saveCustoms(cs);
                toast("Importados: " + added);
                refresh();
            }
        } catch (Exception e) {
            toast(req == RQ_EXPORT ? "No se pudo exportar" : "Archivo no válido");
        }
    }

    @Override
    public void pickFile(Intent intent, ValueCallback<Uri[]> cb) {
        if (fileCb != null) fileCb.onReceiveValue(null);
        fileCb = cb;
        try {
            startActivityForResult(intent, RQ_FILE);
        } catch (Exception e) {
            fileCb = null;
            cb.onReceiveValue(null);
        }
    }

    @Override
    public void ensurePerms() {
        List<String> need = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.RECORD_AUDIO);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.CAMERA);
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), RQ_PERM);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
