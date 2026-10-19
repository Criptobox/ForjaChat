package com.forja.chat;

import java.util.List;
import java.util.Locale;

/** Coordinador por reglas simples: decide la ruta (A web, C asistencia) y qué IA sugerir. */
final class Coordinator {

    static final class Decision {
        final char route;       // 'A', 'C' o 'X' (bloqueado)
        final Assistant target;
        final String reason;

        Decision(char route, Assistant target, String reason) {
            this.route = route;
            this.target = target;
            this.reason = reason;
        }
    }

    private static final String[] CODE = {"código", "codigo", "code", "función", "funcion", "bug", "error",
            "python", "java", "script", "sql", "html", "css", "api", "compilar"};
    private static final String[] SEARCH = {"busca", "noticias", "hoy", "precio", "última", "ultima",
            "actual", "clima", "resultado"};

    static Decision decide(String text, String pref, boolean online, boolean inject,
                           List<Assistant> all, String currentId) {
        if (!online) {
            return new Decision('X', null,
                    "Sin conexión. La IA local (ruta B) llegará en una próxima versión.");
        }

        char route;
        String why = "";
        switch (pref) {
            case "A":
                route = 'A';
                break;
            case "C":
                route = 'C';
                break;
            case "B":
                route = 'C';
                why = "IA local no disponible, uso asistencia. ";
                break;
            default:
                route = inject ? 'A' : 'C';
                break;
        }

        Assistant current = byId(all, currentId);
        Assistant target = null;
        String lower = text.toLowerCase(Locale.ROOT);

        if ("AUTO".equals(pref) || current == null) {
            if (hasAny(lower, CODE)) {
                target = firstOf(all, "claude", "deepseek", "chatgpt");
                why += "parece código";
            } else if (hasAny(lower, SEARCH)) {
                target = firstOf(all, "gemini", "chatgpt");
                why += "parece búsqueda actual";
            }
        }
        if (target == null) {
            target = current != null ? current : firstOf(all, "chatgpt", "claude", "gemini");
            if (why.isEmpty()) why = current != null ? "asistente actual" : "por defecto";
        }
        return new Decision(route, target, why);
    }

    private static boolean hasAny(String text, String[] words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    private static Assistant byId(List<Assistant> all, String id) {
        if (id == null) return null;
        for (Assistant a : all) if (a.id.equals(id)) return a;
        return null;
    }

    private static Assistant firstOf(List<Assistant> all, String... ids) {
        for (String id : ids) {
            Assistant a = byId(all, id);
            if (a != null) return a;
        }
        return all.get(0);
    }
}
