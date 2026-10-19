package com.forja.chat;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class Prefs {

    static final class Prompt {
        final String id, title, text;

        Prompt(String id, String title, String text) {
            this.id = id;
            this.title = title;
            this.text = text;
        }
    }

    static final class Hist {
        final long ts;
        final String text, assistantId, route;

        Hist(long ts, String text, String assistantId, String route) {
            this.ts = ts;
            this.text = text;
            this.assistantId = assistantId;
            this.route = route;
        }
    }

    static final Assistant[] BUILTIN = {
            new Assistant("chatgpt", "ChatGPT", "https://chatgpt.com", "OpenAI", 0xFF12A77F, "logo_openai"),
            new Assistant("claude", "Claude", "https://claude.ai", "Anthropic", 0xFFE8744F, "logo_claude"),
            new Assistant("gemini", "Gemini", "https://gemini.google.com", "Google", 0xFF5663F5, "logo_gemini"),
            new Assistant("deepseek", "DeepSeek", "https://chat.deepseek.com", "DeepSeek AI", 0xFF4D6BFE, "logo_deepseek"),
            new Assistant("qwen", "Qwen", "https://chat.qwen.ai", "Alibaba", 0xFF8E5CF0, "logo_qwen"),
            new Assistant("glm", "GLM", "https://chat.z.ai", "Z.ai", 0xFF1CB89A, "logo_glm"),
            new Assistant("arena", "Arena", "https://lmarena.ai", "Modelos IA", 0xFFF2A93B, "ic_scale"),
    };

    private static final String[][] DEFAULT_PROMPTS = {
            {"Resumir texto", "Resume el siguiente texto en 5 puntos claros:\n\n"},
            {"Explicar fácil", "Explícame esto como si tuviera 12 años, con un ejemplo:\n\n"},
            {"Corregir y mejorar", "Corrige la ortografía y mejora la redacción manteniendo el tono:\n\n"},
            {"Traducir", "Traduce al inglés de forma natural:\n\n"},
            {"Revisar código", "Revisa este código, encuentra errores y propón mejoras:\n\n"},
            {"Plan de proyecto", "Crea un plan paso a paso con tareas, tiempos y riesgos para:\n\n"},
            {"Comparar opciones", "Compara estas opciones en una tabla con pros, contras y tu recomendación:\n\n"},
            {"Ideas creativas", "Dame 10 ideas originales sobre:\n\n"},
    };

    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getSharedPreferences("forja", Context.MODE_PRIVATE);
    }

    static String routeName(String r) {
        switch (r) {
            case "A":
                return "Web";
            case "B":
                return "Local";
            case "C":
                return "Asistencia";
            default:
                return "Auto";
        }
    }

    // ---------------------------------------------------------------- modelos

    List<Assistant> customs() {
        List<Assistant> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString("customs", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Assistant(o.getString("id"), o.getString("name"), o.getString("url")));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    void saveCustoms(List<Assistant> list) {
        sp.edit().putString("customs", customsJson(list)).apply();
    }

    static String customsJson(List<Assistant> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Assistant a : list) {
                if (!a.custom) continue;
                JSONObject o = new JSONObject();
                o.put("id", a.id);
                o.put("name", a.name);
                o.put("url", a.url);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        return arr.toString();
    }

    List<Assistant> all() {
        List<Assistant> out = new ArrayList<>();
        for (Assistant a : BUILTIN) out.add(a);
        out.addAll(customs());
        return out;
    }

    Assistant find(String id) {
        if (id == null) return null;
        for (Assistant a : all()) if (a.id.equals(id)) return a;
        return null;
    }

    // ---------------------------------------------------------------- favoritos y recientes

    Set<String> favs() {
        return new HashSet<>(sp.getStringSet("favs", new HashSet<String>()));
    }

    void toggleFav(String id) {
        Set<String> f = favs();
        if (!f.remove(id)) f.add(id);
        sp.edit().putStringSet("favs", f).apply();
    }

    void addRecent(String id) {
        sp.edit().putString("last", id).apply();
    }

    String last() {
        return sp.getString("last", null);
    }

    // ---------------------------------------------------------------- ajustes

    String route() {
        return sp.getString("route", "AUTO");
    }

    void setRoute(String r) {
        sp.edit().putString("route", r).apply();
    }

    boolean inject() {
        return sp.getBoolean("inject", true);
    }

    void setInject(boolean v) {
        sp.edit().putBoolean("inject", v).apply();
    }

    boolean openLast() {
        return sp.getBoolean("openLast", false);
    }

    void setOpenLast(boolean v) {
        sp.edit().putBoolean("openLast", v).apply();
    }

    // ---------------------------------------------------------------- biblioteca de prompts

    List<Prompt> prompts() {
        List<Prompt> out = new ArrayList<>();
        String raw = sp.getString("prompts", null);
        if (raw == null) {
            for (int i = 0; i < DEFAULT_PROMPTS.length; i++) {
                out.add(new Prompt("d" + i, DEFAULT_PROMPTS[i][0], DEFAULT_PROMPTS[i][1]));
            }
            savePrompts(out);
            return out;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Prompt(o.getString("id"), o.getString("title"), o.getString("text")));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    void savePrompts(List<Prompt> list) {
        sp.edit().putString("prompts", promptsArray(list).toString()).apply();
    }

    private static JSONArray promptsArray(List<Prompt> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Prompt p : list) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("title", p.title);
                o.put("text", p.text);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        return arr;
    }

    // ---------------------------------------------------------------- historial

    List<Hist> history() {
        List<Hist> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp.getString("history", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Hist(o.getLong("ts"), o.getString("text"), o.getString("a"), o.getString("r")));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void saveHistory(List<Hist> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Hist h : list) {
                JSONObject o = new JSONObject();
                o.put("ts", h.ts);
                o.put("text", h.text);
                o.put("a", h.assistantId);
                o.put("r", h.route);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        sp.edit().putString("history", arr.toString()).apply();
    }

    void addHistory(String text, String assistantId, String route) {
        List<Hist> h = history();
        h.add(0, new Hist(System.currentTimeMillis(), text, assistantId, route));
        while (h.size() > 100) h.remove(h.size() - 1);
        saveHistory(h);
    }

    void removeHistory(long ts) {
        List<Hist> h = history();
        for (int i = h.size() - 1; i >= 0; i--) if (h.get(i).ts == ts) h.remove(i);
        saveHistory(h);
    }

    void clearHistory() {
        sp.edit().putString("history", "[]").apply();
    }

    // ---------------------------------------------------------------- copia de seguridad

    String exportAll() {
        try {
            JSONObject o = new JSONObject();
            o.put("version", 2);
            o.put("models", new JSONArray(customsJson(customs())));
            o.put("prompts", promptsArray(prompts()));
            o.put("favs", new JSONArray(favs()));
            return o.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Importa modelos y prompts. Acepta también el formato antiguo (lista de modelos). */
    int importAll(String raw) throws Exception {
        raw = raw.trim();
        JSONArray models;
        JSONArray prompts = null;
        JSONArray favs = null;
        if (raw.startsWith("[")) {
            models = new JSONArray(raw);
        } else {
            JSONObject o = new JSONObject(raw);
            models = o.optJSONArray("models");
            prompts = o.optJSONArray("prompts");
            favs = o.optJSONArray("favs");
        }
        int added = 0;
        long stamp = System.currentTimeMillis();
        if (models != null) {
            List<Assistant> cs = customs();
            for (int k = 0; k < models.length(); k++) {
                JSONObject o = models.getJSONObject(k);
                String n = o.getString("name"), u = o.getString("url");
                if (!u.startsWith("https://")) continue;
                boolean dup = false;
                for (Assistant a : cs) if (a.url.equals(u)) dup = true;
                if (dup) continue;
                cs.add(new Assistant("c_" + stamp + "_" + k, n, u));
                added++;
            }
            saveCustoms(cs);
        }
        if (prompts != null) {
            List<Prompt> ps = prompts();
            for (int k = 0; k < prompts.length(); k++) {
                JSONObject o = prompts.getJSONObject(k);
                String t = o.getString("title"), x = o.getString("text");
                boolean dup = false;
                for (Prompt p : ps) if (p.title.equals(t) && p.text.equals(x)) dup = true;
                if (dup) continue;
                ps.add(new Prompt("p_" + stamp + "_" + k, t, x));
                added++;
            }
            savePrompts(ps);
        }
        if (favs != null) {
            Set<String> f = favs();
            for (int k = 0; k < favs.length(); k++) f.add(favs.getString(k));
            sp.edit().putStringSet("favs", f).apply();
        }
        return added;
    }
}
