package com.forja.chat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity implements WebViewPool.Host {

    static final int T_HOME = 0, T_CHAT = 1, T_PROMPTS = 2, T_HIST = 3, T_SET = 4;
    private static final int RQ_EXPORT = 1, RQ_IMPORT = 2, RQ_FILE = 3, RQ_PERM = 4, RQ_VOICE = 5;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final String[] ROUTES = {"AUTO", "A", "B", "C"};
    private static final String[] ROUTE_LABELS = {"Auto", "Web", "Local", "Asistencia"};
    private static final int[] ROUTE_ICONS = {R.drawable.ic_auto, R.drawable.ic_globe,
            R.drawable.ic_cpu, R.drawable.ic_handoff};
    private static final String[] TAB_LABELS = {"Inicio", "Chat", "Prompts", "Historial", "Ajustes"};
    private static final int[] TAB_ICONS = {R.drawable.ic_home, R.drawable.ic_chat,
            R.drawable.ic_prompts, R.drawable.ic_history, R.drawable.ic_settings};

    Prefs prefs;
    WebViewPool pool;
    boolean tablet;

    private Screens screens;
    private LinearLayout sideList, navRow, dock;
    private FrameLayout headerBox, content;
    private View statusDot;
    private TextView statusText;
    private EditText input;
    private final View[] segBox = new View[4];
    private final ImageView[] segIcon = new ImageView[4];
    private final TextView[] segLabel = new TextView[4];

    private int tab = T_HOME;
    private String current;
    private String second;
    private boolean split;
    private ValueCallback<Uri[]> fileCb;
    private String statusMsg = "Tu espacio para todas las IA";
    private ConnectivityManager.NetworkCallback netCb;

    int d(int v) {
        return Ui.dp(this, v);
    }

    // ------------------------------------------------------------ ciclo de vida

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        pool = new WebViewPool(this, this);
        screens = new Screens(this);
        if (prefs.openLast() && prefs.find(prefs.last()) != null) {
            current = prefs.last();
            tab = T_CHAT;
        }
        buildUi();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        netCb = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network n) {
                runOnUiThread(() -> refreshChrome());
            }

            @Override
            public void onLost(Network n) {
                runOnUiThread(() -> refreshChrome());
            }
        };
        try {
            cm.registerDefaultNetworkCallback(netCb);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        try {
            ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE))
                    .unregisterNetworkCallback(netCb);
        } catch (Exception ignored) {
        }
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        String keep = input != null ? input.getText().toString() : "";
        buildUi();
        input.setText(keep);
    }

    @Override
    protected void onDestroy() {
        pool.destroyAll();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (tab == T_CHAT && current != null) {
            WebView w = pool.peek(current);
            if (w != null && w.canGoBack()) {
                w.goBack();
                return;
            }
        }
        if (tab != T_HOME) setTab(T_HOME);
        else super.onBackPressed();
    }

    private void handleIntent(Intent i) {
        if (i == null) return;
        String shared = null;
        if (Intent.ACTION_SEND.equals(i.getAction())) {
            shared = i.getStringExtra(Intent.EXTRA_TEXT);
        } else if (Intent.ACTION_PROCESS_TEXT.equals(i.getAction())) {
            CharSequence cs = i.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            if (cs != null) shared = cs.toString();
        }
        if (shared != null && !shared.trim().isEmpty()) {
            input.setText(shared);
            input.setSelection(input.length());
            tab = current != null ? T_CHAT : T_HOME;
            refresh();
            setStatus("Texto recibido. Elige una ruta y pulsa enviar.");
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

        sideList = null;
        if (tablet) {
            LinearLayout sidebar = new LinearLayout(this);
            sidebar.setOrientation(LinearLayout.VERTICAL);
            sidebar.setPadding(d(14), d(18), d(10), d(14));
            sidebar.addView(brand(44));
            ScrollView sv = new ScrollView(this);
            sideList = new LinearLayout(this);
            sideList.setOrientation(LinearLayout.VERTICAL);
            sv.addView(sideList, new ViewGroup.LayoutParams(MATCH, WRAP));
            sidebar.addView(sv, Ui.lp(MATCH, 0, 1f, 0, d(16), 0, d(8)));
            sidebar.addView(Ui.pill(this, "+  Agregar modelo", false, v -> editModel(null)));
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
        dock = buildDock();
        main.addView(dock, new LinearLayout.LayoutParams(MATCH, WRAP));

        navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setBackgroundColor(Ui.DOCK);
        root.addView(navRow, new LinearLayout.LayoutParams(MATCH, d(64)));

        setContentView(root);
        refresh();
    }

    void refresh() {
        pool.detachAll();
        headerBox.removeAllViews();
        content.removeAllViews();
        if (tab == T_CHAT && current == null) current = defaultAssistantId();
        headerBox.addView(buildHeader());
        content.addView(buildBody());
        dock.setVisibility(tab == T_HOME || tab == T_CHAT ? View.VISIBLE : View.GONE);
        if (tablet) refreshSidebar();
        refreshNav();
        styleSegs();
        statusText.setText(statusMsg);
        statusDot.setBackground(Ui.circle(isOnline() ? Ui.ONLINE : Ui.OFF));
    }

    private void refreshChrome() {
        if (statusDot == null) return;
        statusDot.setBackground(Ui.circle(isOnline() ? Ui.ONLINE : Ui.OFF));
        if (tab == T_CHAT && current != null) {
            headerBox.removeAllViews();
            headerBox.addView(chatHeader());
        }
    }

    private String defaultAssistantId() {
        String id = prefs.last();
        return prefs.find(id) != null ? id : Prefs.BUILTIN[0].id;
    }

    private void setTab(int t) {
        tab = t;
        refresh();
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

    private View buildHeader() {
        View h;
        switch (tab) {
            case T_CHAT:
                h = chatHeader();
                break;
            case T_PROMPTS:
                h = titleHeader("Prompts", "Tu biblioteca de plantillas");
                break;
            case T_HIST:
                h = titleHeader("Historial", "Preguntas que has enviado");
                break;
            case T_SET:
                h = titleHeader("Ajustes", "Rutas, datos y privacidad");
                break;
            default:
                h = tablet ? titleHeader("Inicio", "Elige un asistente para empezar") : brandHeader();
        }
        return h;
    }

    private View brandHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(d(16), d(14), d(16), d(8));
        h.addView(brand(40), new LinearLayout.LayoutParams(0, WRAP, 1f));
        h.setLayoutParams(new FrameLayout.LayoutParams(MATCH, WRAP));
        return h;
    }

    private View titleHeader(String title, String sub) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(d(18), d(16), d(16), d(6));
        h.addView(Ui.bold(Ui.text(this, title, 22, Ui.TEXT)));
        h.addView(Ui.text(this, sub, 13, Ui.SUB));
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

        h.addView(Ui.logo(this, a, tablet ? 52 : 42));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(d(12), 0, 0, 0);
        col.addView(Ui.bold(Ui.text(this, a.name, tablet ? 20 : 17, Ui.TEXT)));
        boolean on = isOnline();
        col.addView(Ui.text(this, a.vendor + (on ? "  ·  En línea" : "  ·  Sin conexión"), 12,
                on ? Ui.ONLINE : Ui.SUB));
        h.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));
        h.addView(Ui.iconBtn(this, R.drawable.ic_split, "Vista dividida",
                split ? Ui.ACCENT_TXT : Ui.TEXT, v -> toggleSplit()));
        h.addView(Ui.iconBtn(this, R.drawable.ic_dots, "Opciones del chat", Ui.TEXT,
                v -> showChatMenu()));
        return h;
    }

    // ------------------------------------------------------------ contenido

    private View buildBody() {
        switch (tab) {
            case T_CHAT:
                return chatBody();
            case T_PROMPTS:
                return screens.prompts();
            case T_HIST:
                return screens.history();
            case T_SET:
                return screens.settings();
            default:
                return screens.home();
        }
    }

    private View chatBody() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        if (!tablet) col.addView(switcher(), new LinearLayout.LayoutParams(MATCH, d(54)));
        LinearLayout panes = new LinearLayout(this);
        panes.setOrientation(LinearLayout.VERTICAL);
        panes.setPadding(d(8), d(4), d(8), d(4));
        panes.addView(pane(prefs.find(current)), new LinearLayout.LayoutParams(MATCH, 0, 1f));
        Assistant b = split ? prefs.find(second) : null;
        if (b != null) {
            panes.addView(pane(b), Ui.lp(MATCH, 0, 1f, 0, d(8), 0, 0));
        }
        col.addView(panes, new LinearLayout.LayoutParams(MATCH, 0, 1f));
        return col;
    }

    /** Franja de logos para cambiar de asistente sin volver al inicio. */
    private View switcher() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(d(10), d(4), d(10), d(4));
        for (final Assistant a : prefs.all()) {
            boolean sel = a.id.equals(current);
            FrameLayout cell = new FrameLayout(this);
            cell.setContentDescription(a.name);
            cell.setClickable(true);
            cell.setOnClickListener(v -> showAssistant(a.id));
            if (sel) cell.setBackground(Ui.ring(this, Ui.SEL, 2));
            View logo = Ui.logo(this, a, 38);
            cell.addView(logo, new FrameLayout.LayoutParams(d(38), d(38), Gravity.CENTER));
            row.addView(cell, Ui.lp(d(46), d(46), 0f, 0, 0, d(6), 0));
        }
        hs.addView(row);
        return hs;
    }

    private View pane(Assistant a) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Ui.shape(this, Ui.PANEL, Ui.LINE, 1, 18));
        f.setClipToOutline(true);
        f.addView(pool.get(a), new FrameLayout.LayoutParams(MATCH, MATCH));

        final ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgressTintList(ColorStateList.valueOf(Ui.ACCENT));
        pb.setProgressBackgroundTintList(ColorStateList.valueOf(0x00000000));
        f.addView(pb, new FrameLayout.LayoutParams(MATCH, d(3), Gravity.TOP));

        final LinearLayout err = new LinearLayout(this);
        err.setOrientation(LinearLayout.VERTICAL);
        err.setGravity(Gravity.CENTER);
        err.setBackgroundColor(Ui.PANEL);
        err.setPadding(d(30), d(20), d(30), d(20));
        err.setClickable(true);
        TextView et = Ui.bold(Ui.text(this, "No se pudo cargar " + a.name, 17, Ui.TEXT));
        et.setGravity(Gravity.CENTER);
        err.addView(et);
        TextView es = Ui.text(this, "Revisa tu conexión e inténtalo de nuevo.", 14, Ui.SUB);
        es.setGravity(Gravity.CENTER);
        es.setPadding(0, d(6), 0, d(16));
        err.addView(es);
        final String id = a.id;
        err.addView(Ui.pill(this, "Reintentar", true, v -> pool.reload(id)));
        err.setVisibility(View.GONE);
        f.addView(err, new FrameLayout.LayoutParams(MATCH, MATCH));

        pool.listen(a.id, new WebViewPool.PageListener() {
            @Override
            public void progress(int p) {
                pb.setProgress(p);
                pb.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void error(boolean failed) {
                err.setVisibility(failed ? View.VISIBLE : View.GONE);
            }
        });
        return f;
    }

    // ------------------------------------------------------------ barra lateral y navegación

    private void refreshSidebar() {
        sideList.removeAllViews();
        TextView lab = Ui.bold(Ui.text(this, "Asistentes", 14, Ui.SUB));
        lab.setPadding(d(6), 0, 0, d(2));
        sideList.addView(lab);
        Set<String> favs = prefs.favs();
        for (int pass = 0; pass < 2; pass++) {
            for (Assistant a : prefs.all()) {
                if ((pass == 0) != favs.contains(a.id)) continue;
                boolean sel = tab == T_CHAT && a.id.equals(current);
                sideList.addView(screens.assistantRow(a, sel), Ui.lp(MATCH, WRAP, 0f, 0, d(8), 0, 0));
            }
        }
    }

    private void refreshNav() {
        navRow.removeAllViews();
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int idx = i;
            boolean active = tab == i;
            LinearLayout t = new LinearLayout(this);
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER);
            t.setClickable(true);
            t.setOnClickListener(v -> setTab(idx));
            FrameLayout box = new FrameLayout(this);
            if (active) box.setBackground(Ui.shape(this, 0xFF6B5CFF, 0, 0, 14));
            ImageView iv = new ImageView(this);
            iv.setImageResource(TAB_ICONS[i]);
            iv.setColorFilter(active ? 0xFFFFFFFF : Ui.SUB);
            box.addView(iv, new FrameLayout.LayoutParams(d(22), d(22), Gravity.CENTER));
            t.addView(box, new LinearLayout.LayoutParams(d(52), d(32)));
            TextView tv = Ui.bold(Ui.text(this, TAB_LABELS[i], 10.5f, active ? Ui.ACCENT_TXT : Ui.SUB));
            tv.setPadding(0, d(2), 0, 0);
            t.addView(tv);
            navRow.addView(t, new LinearLayout.LayoutParams(0, MATCH, 1f));
        }
    }

    // ------------------------------------------------------------ dock (rutas + entrada)

    private LinearLayout buildDock() {
        LinearLayout dk = new LinearLayout(this);
        dk.setOrientation(LinearLayout.VERTICAL);
        dk.setBackgroundColor(Ui.DOCK);
        dk.setPadding(d(14), d(10), d(14), d(10));

        LinearLayout sr = new LinearLayout(this);
        sr.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this);
        sr.addView(statusDot, new LinearLayout.LayoutParams(d(7), d(7)));
        statusText = Ui.text(this, "", 12, 0xFFB6BCE4);
        statusText.setPadding(d(8), 0, 0, 0);
        statusText.setMaxLines(2);
        statusText.setEllipsize(TextUtils.TruncateAt.END);
        sr.addView(statusText, new LinearLayout.LayoutParams(0, WRAP, 1f));
        dk.addView(sr);

        LinearLayout segs = new LinearLayout(this);
        segs.setOrientation(LinearLayout.HORIZONTAL);
        segs.setPadding(d(4), d(4), d(4), d(4));
        segs.setBackground(Ui.shape(this, Ui.CARD, Ui.LINE, 1, 16));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout s = new LinearLayout(this);
            s.setOrientation(LinearLayout.VERTICAL);
            s.setGravity(Gravity.CENTER);
            s.setPadding(0, d(7), 0, d(7));
            s.setClickable(true);
            s.setOnClickListener(v -> onSeg(idx));
            ImageView iv = new ImageView(this);
            iv.setImageResource(ROUTE_ICONS[i]);
            s.addView(iv, new LinearLayout.LayoutParams(d(18), d(18)));
            TextView tv = Ui.bold(Ui.text(this, ROUTE_LABELS[i], 11, Ui.SUB));
            tv.setPadding(0, d(2), 0, 0);
            s.addView(tv);
            segBox[i] = s;
            segIcon[i] = iv;
            segLabel[i] = tv;
            segs.addView(s, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        dk.addView(segs, Ui.lp(MATCH, WRAP, 0f, 0, d(8), 0, 0));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(d(16), d(4), d(4), d(4));
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

        FrameLayout mic = new FrameLayout(this);
        mic.setContentDescription("Dictar");
        mic.setClickable(true);
        mic.setOnClickListener(v -> startVoice());
        ImageView mi = new ImageView(this);
        mi.setImageResource(R.drawable.ic_mic);
        mi.setColorFilter(0xFFAEB4E0);
        mic.addView(mi, new FrameLayout.LayoutParams(d(22), d(22), Gravity.CENTER));
        bar.addView(mic, new LinearLayout.LayoutParams(d(42), d(42)));

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
        dk.addView(bar, Ui.lp(MATCH, WRAP, 0f, 0, d(8), 0, 0));
        return dk;
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
        setStatus("Ruta: " + ROUTE_LABELS[idx]);
        styleSegs();
    }

    private void setStatus(String s) {
        statusMsg = s;
        if (statusText != null) statusText.setText(s);
    }

    // ------------------------------------------------------------ acciones

    void showAssistant(String id) {
        if (prefs.find(id) == null) return;
        current = id;
        tab = T_CHAT;
        prefs.addRecent(id);
        refresh();
    }

    /** Coloca un texto en la barra de entrada y lleva a una pantalla donde se puede enviar. */
    void useText(String text) {
        input.setText(text);
        input.setSelection(input.length());
        if (tab != T_HOME && tab != T_CHAT) {
            tab = current != null ? T_CHAT : T_HOME;
            refresh();
        }
        input.requestFocus();
        toast("Listo: completa tu pregunta y envía");
    }

    void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("ForjaChat", text));
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
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

    private void showChatMenu() {
        final Assistant a = prefs.find(current);
        if (a == null) return;
        String[] items = {
                "Recargar",
                "Nuevo chat",
                "Abrir en el navegador",
                "Copiar enlace",
                split ? "Cerrar vista dividida" : "Vista dividida"
        };
        new AlertDialog.Builder(this).setTitle(a.name).setItems(items, (dlg, which) -> {
            WebView w = pool.peek(a.id);
            String url = w != null && w.getUrl() != null ? w.getUrl() : a.url;
            switch (which) {
                case 0:
                    pool.reload(a.id);
                    break;
                case 1:
                    if (w != null) w.loadUrl(a.url);
                    break;
                case 2:
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        toast("No hay navegador disponible");
                    }
                    break;
                case 3:
                    copy(url);
                    toast("Enlace copiado");
                    break;
                default:
                    toggleSplit();
            }
        }).show();
    }

    void editModel(final Assistant existing) {
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

    void deleteModel(final Assistant a) {
        new AlertDialog.Builder(this)
                .setMessage("¿Eliminar «" + a.name + "»?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dlg, w) -> {
                    List<Assistant> cs = prefs.customs();
                    for (int i = cs.size() - 1; i >= 0; i--) if (cs.get(i).id.equals(a.id)) cs.remove(i);
                    prefs.saveCustoms(cs);
                    if (a.id.equals(second)) {
                        split = false;
                        second = null;
                    }
                    if (a.id.equals(current)) {
                        current = null;
                        tab = T_HOME;
                    }
                    refresh();
                }).show();
    }

    void clearSessions() {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        pool.reloadAll();
        toast("Sesiones cerradas");
    }

    // ------------------------------------------------------------ envío (coordinador)

    private void onSend() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            toast("Escribe tu pregunta");
            return;
        }
        Coordinator.Decision dec = Coordinator.decide(text, prefs.route(), isOnline(),
                prefs.inject(), prefs.all(), current);
        if (dec.route == 'X') {
            setStatus(dec.reason);
            toast(dec.reason);
            return;
        }
        copy(text);

        List<Assistant> targets = new ArrayList<>();
        if (split && current != null && prefs.find(second) != null) {
            targets.add(prefs.find(current));
            targets.add(prefs.find(second));
        } else {
            targets.add(dec.target);
        }
        prefs.addHistory(text, targets.get(0).id, String.valueOf(dec.route));
        showAssistant(targets.get(0).id);

        StringBuilder names = new StringBuilder();
        for (Assistant a : targets) {
            if (names.length() > 0) names.append(" + ");
            names.append(a.name);
        }
        if (dec.route == 'A') {
            for (final Assistant a : targets) {
                pool.fill(a, text, r -> {
                    if (r != null && r.contains("nofield")) {
                        setStatus("No encontré la caja de " + a.name + ". Tu pregunta está copiada: pégala.");
                        toast("Pregunta copiada. Pega en el chat");
                    }
                });
            }
            setStatus("Ruta Web · " + names + " (" + dec.reason + ") · texto rellenado, pulsa enviar en el chat");
        } else {
            setStatus("Ruta Asistencia · " + names + " (" + dec.reason + ") · pregunta copiada");
            toast("Pregunta copiada. Pega en el chat");
        }
        input.setText("");
    }

    boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities c = cm.getNetworkCapabilities(n);
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    // ------------------------------------------------------------ voz, copia de seguridad, permisos

    private void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora");
        try {
            startActivityForResult(i, RQ_VOICE);
        } catch (Exception e) {
            toast("El dictado por voz no está disponible");
        }
    }

    void exportData() {
        Intent e = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        e.addCategory(Intent.CATEGORY_OPENABLE);
        e.setType("application/json");
        e.putExtra(Intent.EXTRA_TITLE, "forjachat-copia.json");
        startActivityForResult(e, RQ_EXPORT);
    }

    void importData() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, RQ_IMPORT);
    }

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
        if (res != RESULT_OK || data == null) return;
        if (req == RQ_VOICE) {
            ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty()) {
                String cur = input.getText().toString();
                input.setText(cur.isEmpty() ? r.get(0) : cur + " " + r.get(0));
                input.setSelection(input.length());
            }
            return;
        }
        if (data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (req == RQ_EXPORT) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(prefs.exportAll().getBytes(StandardCharsets.UTF_8));
                os.close();
                toast("Copia exportada");
            } else if (req == RQ_IMPORT) {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                br.close();
                int added = prefs.importAll(sb.toString());
                toast("Elementos importados: " + added);
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
}
