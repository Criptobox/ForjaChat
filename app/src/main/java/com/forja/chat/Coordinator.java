package com.forja.chat;

import java.util.List;
import java.util.Locale;

/** Coordinador por reglas: decide la ruta (A web, C asistencia) y qué IA conviene. */
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

    /** {palabras clave, asistentes por orden de preferencia, motivo} */
    private static final String[][] RULES = {
            {"código|codigo|code|función|funcion|bug|error|python|java|script|sql|html|css|api|compilar|regex",
                    "claude|deepseek|chatgpt", "parece código"},
            {"calcula|matemática|matematica|ecuación|ecuacion|demuestra|lógica|logica|razona",
                    "deepseek|chatgpt|claude", "requiere razonamiento"},
            {"busca|noticias|hoy|precio|última|ultima|actual|clima|resultado|quién ganó",
                    "gemini|chatgpt", "necesita datos actuales"},
            {"imagen|foto|dibuja|logo|ilustración|ilustracion",
                    "gemini|chatgpt", "tema visual"},
            {"traduce|traducción|traduccion|translate",
                    "chatgpt|gemini|claude", "traducción"},
            {"redacta|corrige|ortografía|ortografia|correo|carta|resume|resumen|ensayo",
                    "claude|chatgpt|gemini", "escritura"},
    };

    static Decision decide(String text, String pref, boolean online, boolean inject,
                           List<Assistant> all, String currentId) {
        if (!online) {
            return new Decision('X', null,
                    "Sin conexión. La IA local (ruta Local) llegará en una próxima versión.");
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
            for (String[] rule : RULES) {
                if (!hasAny(lower, rule[0].split("\\|"))) continue;
                Assistant t = firstOf(all, rule[1].split("\\|"));
                if (t != null) {
                    target = t;
                    why += rule[2];
                    break;
                }
            }
        }
        if (target == null) {
            target = current != null ? current : firstOf(all, "chatgpt", "claude", "gemini");
            if (target == null) target = all.get(0);
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
        return null;
    }
}
