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
    static final Assistant[] BUILTIN = {
            new Assistant("chatgpt", "ChatGPT", "https://chatgpt.com", "OpenAI", 0xFF12A77F, "logo_openai"),
            new Assistant("claude", "Claude", "https://claude.ai", "Anthropic", 0xFFE8744F, "logo_claude"),
            new Assistant("gemini", "Gemini", "https://gemini.google.com", "Google", 0xFF5663F5, "logo_gemini"),
            new Assistant("deepseek", "DeepSeek", "https://chat.deepseek.com", "DeepSeek AI", 0xFF4D6BFE, "logo_deepseek"),
            new Assistant("qwen", "Qwen", "https://chat.qwen.ai", "Alibaba", 0xFF8E5CF0, "logo_qwen"),
            new Assistant("glm", "GLM", "https://chat.z.ai", "Z.ai", 0xFF1CB89A, "logo_glm"),
            new Assistant("arena", "Arena", "https://lmarena.ai", "Modelos IA", 0xFFF2A93B, "ic_scale"),
    };

    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getSharedPreferences("forja", Context.MODE_PRIVATE);
    }

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

    Set<String> favs() {
        return new HashSet<>(sp.getStringSet("favs", new HashSet<String>()));
    }

    void toggleFav(String id) {
        Set<String> f = favs();
        if (!f.remove(id)) f.add(id);
        sp.edit().putStringSet("favs", f).apply();
    }

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

    List<String> recents() {
        List<String> out = new ArrayList<>();
        String raw = sp.getString("recents", "");
        if (!raw.isEmpty()) for (String x : raw.split(",")) out.add(x);
        return out;
    }

    void addRecent(String id) {
        List<String> r = recents();
        r.remove(id);
        r.add(0, id);
        while (r.size() > 4) r.remove(r.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String x : r) {
            if (sb.length() > 0) sb.append(',');
            sb.append(x);
        }
        sp.edit().putString("recents", sb.toString()).putString("last", id).apply();
    }

    String last() {
        return sp.getString("last", null);
    }
}
