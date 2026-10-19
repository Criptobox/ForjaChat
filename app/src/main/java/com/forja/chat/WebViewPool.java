package com.forja.chat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Mantiene una WebView viva por asistente para conservar sesión y contexto. */
final class WebViewPool {

    interface Host {
        void pickFile(Intent intent, ValueCallback<Uri[]> cb);

        void ensurePerms();
    }

    private static final String FILL_JS =
            "(function(t){"
                    + "var el=document.querySelector('textarea')"
                    + "||document.querySelector('[contenteditable=\"true\"]')"
                    + "||document.querySelector('input[type=text]');"
                    + "if(!el)return 'nofield';"
                    + "el.focus();"
                    + "if(el.tagName==='TEXTAREA'||el.tagName==='INPUT'){"
                    + "var p=el.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;"
                    + "Object.getOwnPropertyDescriptor(p,'value').set.call(el,t);"
                    + "el.dispatchEvent(new Event('input',{bubbles:true}));"
                    + "}else{document.execCommand('selectAll',false,null);"
                    + "document.execCommand('insertText',false,t);}"
                    + "return 'ok';})(";

    private final Activity act;
    private final Host host;
    private final Map<String, WebView> views = new HashMap<>();
    private final Set<String> started = new HashSet<>();
    private final Set<String> ready = new HashSet<>();
    private final Map<String, String> pending = new HashMap<>();

    WebViewPool(Activity act, Host host) {
        this.act = act;
        this.host = host;
        CookieManager.getInstance().setAcceptCookie(true);
    }

    WebView peek(String id) {
        return views.get(id);
    }

    WebView get(Assistant a) {
        WebView w = views.get(a.id);
        if (w == null) {
            w = make(a);
            views.put(a.id, w);
        }
        if (w.getParent() instanceof ViewGroup) {
            ((ViewGroup) w.getParent()).removeView(w);
        }
        if (started.add(a.id)) w.loadUrl(a.url);
        return w;
    }

    void detachAll() {
        for (WebView w : views.values()) {
            if (w.getParent() instanceof ViewGroup) {
                ((ViewGroup) w.getParent()).removeView(w);
            }
        }
    }

    /** Rellena la caja de texto del asistente. No pulsa enviar: lo hace la persona. */
    void fill(Assistant a, String text) {
        WebView w = views.get(a.id);
        if (w == null) return;
        if (ready.contains(a.id)) {
            final WebView fw = w;
            fw.postDelayed(() -> inject(fw, text), 300);
        } else {
            pending.put(a.id, text);
        }
    }

    private void inject(WebView w, String text) {
        w.evaluateJavascript(FILL_JS + JSONObject.quote(text) + ")", null);
    }

    void reloadAll() {
        ready.clear();
        for (WebView w : views.values()) w.reload();
    }

    void destroyAll() {
        for (WebView w : views.values()) w.destroy();
        views.clear();
        started.clear();
        ready.clear();
    }

    private WebView make(final Assistant a) {
        WebView w = new WebView(act);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setSupportMultipleWindows(false);
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        w.setBackgroundColor(Ui.BG);

        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http://") || u.startsWith("https://")) return false;
                try {
                    act.startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl()));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(final WebView v, String url) {
                ready.add(a.id);
                final String t = pending.remove(a.id);
                if (t != null) v.postDelayed(() -> inject(v, t), 1500);
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest r) {
                act.runOnUiThread(() -> {
                    host.ensurePerms();
                    r.grant(r.getResources());
                });
            }

            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                             FileChooserParams p) {
                host.pickFile(p.createIntent(), cb);
                return true;
            }
        });

        w.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
            }
        });
        return w;
    }
}
