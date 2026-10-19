package com.forja.chat;

import android.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Pantallas de contenido: Inicio, Prompts, Historial y Ajustes. */
final class Screens {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final MainActivity m;
    private final Prefs prefs;

    Screens(MainActivity m) {
        this.m = m;
        this.prefs = m.prefs;
    }

    // ------------------------------------------------------------ utilidades

    private int d(int v) {
        return Ui.dp(m, v);
    }

    private LinearLayout vert() {
        LinearLayout l = new LinearLayout(m);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horiz() {
        LinearLayout l = new LinearLayout(m);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout card() {
        LinearLayout c = vert();
        c.setBackground(Ui.shape(m, Ui.CARD, Ui.LINE, 1, 18));
        return c;
    }

    private TextView label(String s) {
        TextView t = Ui.bold(Ui.text(m, s, 13, Ui.SUB));
        t.setPadding(d(4), d(18), 0, d(6));
        return t;
    }

    private ScrollView scroll(LinearLayout col) {
        ScrollView sv = new ScrollView(m);
        sv.addView(col, new ViewGroup.LayoutParams(MATCH, WRAP));
        return sv;
    }

    private LinearLayout page() {
        LinearLayout col = vert();
        col.setPadding(d(14), d(6), d(14), d(16));
        return col;
    }

    private View empty(String title, String sub) {
        LinearLayout col = vert();
        col.setGravity(Gravity.CENTER);
        col.setPadding(d(36), d(40), d(36), d(40));
        ImageView iv = Ui.icon(m, R.drawable.ic_history, 44, Ui.OFF);
        col.addView(iv);
        TextView t = Ui.bold(Ui.text(m, title, 17, Ui.TEXT));
        t.setPadding(0, d(14), 0, d(6));
        t.setGravity(Gravity.CENTER);
        col.addView(t);
        TextView s = Ui.text(m, sub, 14, Ui.SUB);
        s.setGravity(Gravity.CENTER);
        col.addView(s);
        return col;
    }

    private EditText field(String hint, String value, boolean multiline) {
        EditText e = new EditText(m);
        e.setHint(hint);
        e.setText(value);
        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(4);
            e.setGravity(Gravity.TOP);
        }
        return e;
    }

    private String when(long ts) {
        return DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString();
    }

    // ------------------------------------------------------------ asistentes (tarjeta y fila)

    private void modelActions(final Assistant a) {
        new AlertDialog.Builder(m)
                .setItems(new String[]{"Editar", "Eliminar"}, (dlg, which) -> {
                    if (which == 0) m.editModel(a);
                    else m.deleteModel(a);
                }).show();
    }

    View assistantRow(final Assistant a, boolean selected) {
        LinearLayout r = horiz();
        r.setPadding(d(10), d(9), d(10), d(9));
        r.setBackground(selected ? Ui.shape(m, Ui.CARD, Ui.SEL, 2, 18)
                : Ui.shape(m, Ui.CARD, Ui.LINE, 1, 18));
        r.addView(Ui.logo(m, a, 42));
        LinearLayout col = vert();
        col.setPadding(d(12), 0, 0, 0);
        col.addView(Ui.bold(Ui.text(m, a.name, 16, Ui.TEXT)));
        col.addView(Ui.text(m, a.vendor, 12, Ui.SUB));
        r.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));
        r.addView(star(a));
        r.setClickable(true);
        r.setOnClickListener(v -> m.showAssistant(a.id));
        if (a.custom) {
            r.setOnLongClickListener(v -> {
                modelActions(a);
                return true;
            });
        }
        return r;
    }

    private View star(final Assistant a) {
        boolean fav = prefs.favs().contains(a.id);
        ImageView star = Ui.icon(m, fav ? R.drawable.ic_star_fill : R.drawable.ic_star, 22,
                fav ? Ui.ACCENT : Ui.OFF);
        star.setContentDescription("Favorito");
        star.setPadding(d(4), d(4), d(4), d(4));
        star.setOnClickListener(v -> {
            prefs.toggleFav(a.id);
            m.refresh();
        });
        return star;
    }

    private View assistantCard(final Assistant a) {
        LinearLayout c = vert();
        c.setPadding(d(12), d(12), d(12), d(14));
        c.setBackground(Ui.shape(m, Ui.CARD, Ui.LINE, 1, 18));
        LinearLayout top = horiz();
        top.addView(Ui.logo(m, a, 44));
        top.addView(new View(m), new LinearLayout.LayoutParams(0, 1, 1f));
        top.addView(star(a));
        c.addView(top);
        TextView n = Ui.bold(Ui.text(m, a.name, 16, Ui.TEXT));
        n.setPadding(0, d(10), 0, 0);
        n.setSingleLine();
        n.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(n);
        TextView v2 = Ui.text(m, a.vendor, 12, Ui.SUB);
        v2.setSingleLine();
        c.addView(v2);
        c.setClickable(true);
        c.setOnClickListener(v -> m.showAssistant(a.id));
        if (a.custom) {
            c.setOnLongClickListener(v -> {
                modelActions(a);
                return true;
            });
        }
        return c;
    }

    private void renderGrid(LinearLayout grid, String query) {
        grid.removeAllViews();
        int cols = m.tablet ? 3 : 2;
        String q = query.trim().toLowerCase();
        Set<String> favs = prefs.favs();
        List<Assistant> shown = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++) {
            for (Assistant a : prefs.all()) {
                if ((pass == 0) != favs.contains(a.id)) continue;
                if (!q.isEmpty() && !a.name.toLowerCase().contains(q)) continue;
                shown.add(a);
            }
        }
        if (shown.isEmpty()) {
            grid.addView(Ui.text(m, "Sin resultados", 14, Ui.SUB));
            return;
        }
        for (int i = 0; i < shown.size(); i += cols) {
            LinearLayout row = new LinearLayout(m);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < cols; j++) {
                int idx = i + j;
                View cell = idx < shown.size() ? assistantCard(shown.get(idx)) : new View(m);
                row.addView(cell, Ui.lp(0, WRAP, 1f, j == 0 ? 0 : d(10), 0, 0, 0));
            }
            grid.addView(row, Ui.lp(MATCH, WRAP, 0f, 0, d(10), 0, 0));
        }
    }

    // ------------------------------------------------------------ INICIO

    View home() {
        LinearLayout col = page();

        LinearLayout hero = card();
        hero.setPadding(d(18), d(16), d(18), d(16));
        hero.addView(Ui.bold(Ui.text(m, "¿Con qué IA trabajamos hoy?", 20, Ui.TEXT)));
        boolean on = m.isOnline();
        TextView sub = Ui.text(m, (on ? "Conectado" : "Sin conexión")
                + "  ·  Ruta " + Prefs.routeName(prefs.route()), 13, on ? Ui.ONLINE : Ui.SUB);
        sub.setPadding(0, d(4), 0, 0);
        hero.addView(sub);
        col.addView(hero, Ui.lp(MATCH, WRAP, 0f, 0, d(4), 0, d(12)));

        final EditText search = new EditText(m);
        search.setHint("Buscar asistente");
        search.setHintTextColor(Ui.SUB);
        search.setTextColor(Ui.TEXT);
        search.setSingleLine(true);
        search.setBackground(Ui.shape(m, Ui.CARD, Ui.LINE, 1, 14));
        search.setPadding(d(14), d(12), d(14), d(12));
        col.addView(search);

        final LinearLayout grid = vert();
        col.addView(grid);
        renderGrid(grid, "");
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable e) {
                renderGrid(grid, e.toString());
            }
        });

        List<Prefs.Prompt> ps = prefs.prompts();
        if (!ps.isEmpty()) {
            col.addView(label("Prompts rápidos"));
            HorizontalScrollView hs = new HorizontalScrollView(m);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout chips = horiz();
            for (int i = 0; i < ps.size() && i < 8; i++) {
                final Prefs.Prompt p = ps.get(i);
                chips.addView(Ui.chip(m, p.title, v -> m.useText(p.text)),
                        Ui.lp(WRAP, WRAP, 0f, 0, 0, d(8), 0));
            }
            hs.addView(chips);
            col.addView(hs);
        }

        List<Prefs.Hist> hist = prefs.history();
        if (!hist.isEmpty()) {
            col.addView(label("Recientes"));
            for (int i = 0; i < hist.size() && i < 3; i++) {
                col.addView(historyRow(hist.get(i)), Ui.lp(MATCH, WRAP, 0f, 0, d(6), 0, 0));
            }
        }

        View add = Ui.pill(m, "+  Agregar modelo", false, v -> m.editModel(null));
        col.addView(add, Ui.lp(MATCH, WRAP, 0f, 0, d(18), 0, 0));
        return scroll(col);
    }

    private View historyRow(final Prefs.Hist h) {
        Assistant a = prefs.find(h.assistantId);
        LinearLayout r = horiz();
        r.setPadding(d(12), d(10), d(12), d(10));
        r.setBackground(Ui.shape(m, Ui.CARD, Ui.LINE, 1, 16));
        if (a != null) r.addView(Ui.logo(m, a, 32));
        LinearLayout col = vert();
        col.setPadding(d(10), 0, 0, 0);
        TextView t = Ui.text(m, h.text.replace('\n', ' ').trim(), 14, Ui.TEXT);
        t.setSingleLine();
        t.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(t);
        col.addView(Ui.text(m, (a != null ? a.name : "Asistente") + "  ·  " + when(h.ts), 12, Ui.SUB));
        r.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));
        r.setClickable(true);
        r.setOnClickListener(v -> historyActions(h));
        return r;
    }

    // ------------------------------------------------------------ PROMPTS

    View prompts() {
        LinearLayout col = page();
        LinearLayout top = horiz();
        TextView hint = Ui.text(m, "Plantillas para empezar más rápido. Toca una para usarla.", 13, Ui.SUB);
        top.addView(hint, new LinearLayout.LayoutParams(0, WRAP, 1f));
        top.addView(Ui.pill(m, "+ Nuevo", true, v -> promptDialog(null)),
                Ui.lp(WRAP, WRAP, 0f, d(10), 0, 0, 0));
        col.addView(top);

        List<Prefs.Prompt> ps = prefs.prompts();
        if (ps.isEmpty()) {
            col.addView(empty("Sin prompts", "Crea tu primera plantilla con el botón Nuevo."));
        }
        for (final Prefs.Prompt p : ps) {
            LinearLayout c = card();
            c.setPadding(d(14), d(12), d(14), d(10));
            c.addView(Ui.bold(Ui.text(m, p.title, 16, Ui.TEXT)));
            TextView body = Ui.text(m, p.text.trim(), 13, Ui.SUB);
            body.setMaxLines(3);
            body.setEllipsize(TextUtils.TruncateAt.END);
            body.setPadding(0, d(4), 0, d(8));
            c.addView(body);
            LinearLayout acts = horiz();
            acts.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            acts.addView(Ui.pill(m, "Editar", false, v -> promptDialog(p)),
                    Ui.lp(WRAP, WRAP, 0f, 0, 0, d(8), 0));
            acts.addView(Ui.pill(m, "Eliminar", false, v -> confirmDeletePrompt(p)),
                    Ui.lp(WRAP, WRAP, 0f, 0, 0, d(8), 0));
            acts.addView(Ui.pill(m, "Usar", true, v -> m.useText(p.text)));
            c.addView(acts);
            c.setClickable(true);
            c.setOnClickListener(v -> m.useText(p.text));
            col.addView(c, Ui.lp(MATCH, WRAP, 0f, 0, d(10), 0, 0));
        }
        return scroll(col);
    }

    private void promptDialog(final Prefs.Prompt existing) {
        LinearLayout box = vert();
        box.setPadding(d(16), d(12), d(16), 0);
        final EditText title = field("Título", existing != null ? existing.title : "", false);
        final EditText text = field("Texto del prompt", existing != null ? existing.text : "", true);
        box.addView(title);
        box.addView(text);
        new AlertDialog.Builder(m)
                .setTitle(existing == null ? "Nuevo prompt" : "Editar prompt")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", (dlg, w) -> {
                    String t = title.getText().toString().trim();
                    String x = text.getText().toString();
                    if (t.isEmpty() || x.trim().isEmpty()) {
                        m.toast("Escribe un título y un texto");
                        return;
                    }
                    List<Prefs.Prompt> ps = prefs.prompts();
                    if (existing != null) {
                        for (int i = 0; i < ps.size(); i++) {
                            if (ps.get(i).id.equals(existing.id)) {
                                ps.set(i, new Prefs.Prompt(existing.id, t, x));
                            }
                        }
                    } else {
                        ps.add(0, new Prefs.Prompt("p_" + System.currentTimeMillis(), t, x));
                    }
                    prefs.savePrompts(ps);
                    m.refresh();
                }).show();
    }

    private void confirmDeletePrompt(final Prefs.Prompt p) {
        new AlertDialog.Builder(m)
                .setMessage("¿Eliminar el prompt «" + p.title + "»?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dlg, w) -> {
                    List<Prefs.Prompt> ps = prefs.prompts();
                    for (int i = ps.size() - 1; i >= 0; i--) if (ps.get(i).id.equals(p.id)) ps.remove(i);
                    prefs.savePrompts(ps);
                    m.refresh();
                }).show();
    }

    // ------------------------------------------------------------ HISTORIAL

    View history() {
        List<Prefs.Hist> hist = prefs.history();
        if (hist.isEmpty()) {
            return empty("Aún no hay historial",
                    "Las preguntas que envíes desde ForjaChat aparecerán aquí para reutilizarlas.");
        }
        LinearLayout col = page();
        LinearLayout top = horiz();
        top.addView(Ui.text(m, hist.size() + (hist.size() == 1 ? " pregunta" : " preguntas"), 13, Ui.SUB),
                new LinearLayout.LayoutParams(0, WRAP, 1f));
        top.addView(Ui.pill(m, "Borrar todo", false, v -> new AlertDialog.Builder(m)
                .setMessage("¿Borrar todo el historial?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Borrar", (dlg, w) -> {
                    prefs.clearHistory();
                    m.refresh();
                }).show()));
        col.addView(top);
        for (Prefs.Hist h : hist) {
            col.addView(historyCard(h), Ui.lp(MATCH, WRAP, 0f, 0, d(10), 0, 0));
        }
        return scroll(col);
    }

    private View historyCard(final Prefs.Hist h) {
        Assistant a = prefs.find(h.assistantId);
        LinearLayout c = card();
        c.setPadding(d(14), d(12), d(14), d(12));
        TextView t = Ui.text(m, h.text.trim(), 14, Ui.TEXT);
        t.setMaxLines(4);
        t.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(t);
        LinearLayout meta = horiz();
        if (a != null) meta.addView(Ui.logo(m, a, 22));
        TextView mt = Ui.text(m, (a != null ? a.name : "Asistente") + "  ·  "
                + Prefs.routeName(h.route) + "  ·  " + when(h.ts), 12, Ui.SUB);
        mt.setPadding(a != null ? d(8) : 0, 0, 0, 0);
        meta.addView(mt);
        c.addView(meta, Ui.lp(MATCH, WRAP, 0f, 0, d(8), 0, 0));
        c.setClickable(true);
        c.setOnClickListener(v -> historyActions(h));
        return c;
    }

    private void historyActions(final Prefs.Hist h) {
        new AlertDialog.Builder(m)
                .setItems(new String[]{"Usar de nuevo", "Copiar", "Eliminar"}, (dlg, which) -> {
                    if (which == 0) {
                        if (prefs.find(h.assistantId) != null) m.showAssistant(h.assistantId);
                        m.useText(h.text);
                    } else if (which == 1) {
                        m.copy(h.text);
                        m.toast("Copiado");
                    } else {
                        prefs.removeHistory(h.ts);
                        m.refresh();
                    }
                }).show();
    }

    // ------------------------------------------------------------ AJUSTES

    View settings() {
        LinearLayout col = page();

        col.addView(label("Envío"));
        LinearLayout send = card();
        send.addView(settingRow("Ruta predeterminada", Prefs.routeName(prefs.route()), v -> routeDialog()));
        send.addView(switchRow("Rellenado automático (ruta Web)",
                "Escribe tu pregunta en el chat; tú pulsas enviar", prefs.inject(),
                (b, on) -> prefs.setInject(on)));
        send.addView(switchRow("Abrir el último asistente al iniciar", "",
                prefs.openLast(), (b, on) -> prefs.setOpenLast(on)));
        col.addView(send);

        col.addView(label("Datos"));
        LinearLayout data = card();
        data.addView(settingRow("Exportar copia", "Modelos, prompts y favoritos (JSON)", v -> m.exportData()));
        data.addView(settingRow("Importar copia", "Restaura desde un archivo JSON", v -> m.importData()));
        data.addView(settingRow("Borrar historial", prefs.history().size() + " guardadas", v ->
                new AlertDialog.Builder(m).setMessage("¿Borrar todo el historial?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Borrar", (dlg, w) -> {
                            prefs.clearHistory();
                            m.refresh();
                        }).show()));
        col.addView(data);

        col.addView(label("Sesiones web"));
        LinearLayout web = card();
        web.addView(settingRow("Recargar todos los asistentes", "", v -> {
            m.pool.reloadAll();
            m.toast("Asistentes recargados");
        }));
        web.addView(settingRow("Cerrar todas las sesiones", "Borra cookies y datos de los sitios", v ->
                new AlertDialog.Builder(m)
                        .setMessage("Se cerrará la sesión en todos los asistentes. ¿Continuar?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Cerrar sesiones", (dlg, w) -> m.clearSessions()).show()));
        col.addView(web);

        col.addView(label("Acerca de"));
        LinearLayout about = card();
        about.setPadding(d(16), d(14), d(16), d(14));
        about.addView(Ui.bold(Ui.text(m, "ForjaChat 1.6", 16, Ui.TEXT)));
        TextView info = Ui.text(m, "Reúne varias IA en un solo lugar. Cada asistente guarda su historial "
                + "en su propia web; ForjaChat no envía tus chats a ningún servidor propio. Tus prompts, "
                + "historial y ajustes se quedan en este dispositivo.\n\nRuta Web: rellena el chat y tú "
                + "pulsas enviar.\nRuta Asistencia: copia tu pregunta para pegarla.\nRuta Local: próximamente.",
                13, Ui.SUB);
        info.setPadding(0, d(6), 0, 0);
        info.setLineSpacing(0, 1.2f);
        about.addView(info);
        col.addView(about);
        return scroll(col);
    }

    private View settingRow(String title, String sub, View.OnClickListener l) {
        LinearLayout r = horiz();
        r.setPadding(d(16), d(14), d(16), d(14));
        LinearLayout t = vert();
        t.addView(Ui.text(m, title, 15, Ui.TEXT));
        if (!sub.isEmpty()) t.addView(Ui.text(m, sub, 12, Ui.SUB));
        r.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
        r.addView(Ui.icon(m, R.drawable.ic_chevron, 18, Ui.OFF));
        r.setClickable(true);
        r.setOnClickListener(l);
        return r;
    }

    private View switchRow(String title, String sub, boolean checked,
                           android.widget.CompoundButton.OnCheckedChangeListener l) {
        LinearLayout r = horiz();
        r.setPadding(d(16), d(10), d(12), d(10));
        LinearLayout t = vert();
        t.addView(Ui.text(m, title, 15, Ui.TEXT));
        if (!sub.isEmpty()) t.addView(Ui.text(m, sub, 12, Ui.SUB));
        r.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
        Switch sw = new Switch(m);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(l);
        r.addView(sw);
        return r;
    }

    private void routeDialog() {
        final String[] keys = {"AUTO", "A", "C"};
        String[] names = {"Auto (el coordinador decide)", "Web (rellenar el chat)", "Asistencia (copiar y pegar)"};
        new AlertDialog.Builder(m).setTitle("Ruta predeterminada")
                .setItems(names, (dlg, which) -> {
                    prefs.setRoute(keys[which]);
                    m.refresh();
                }).show();
    }
}
