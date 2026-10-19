package com.forja.chat;

final class Assistant {
    private static final int[] PALETTE = {0xFF8E5CF0, 0xFF1CB89A, 0xFFE8744F, 0xFF4D6BFE, 0xFFF2A93B, 0xFF5663F5};

    final String id;
    final String name;
    final String url;
    final boolean custom;
    final String vendor;
    final int color;
    final String logo; // nombre del drawable o null

    /** Asistente integrado. */
    Assistant(String id, String name, String url, String vendor, int color, String logo) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.custom = false;
        this.vendor = vendor;
        this.color = color;
        this.logo = logo;
    }

    /** Modelo propio del usuario. */
    Assistant(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.custom = true;
        this.vendor = "Personalizado";
        this.color = PALETTE[Math.abs(name.hashCode()) % PALETTE.length];
        this.logo = null;
    }

    String host() {
        try {
            return android.net.Uri.parse(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
