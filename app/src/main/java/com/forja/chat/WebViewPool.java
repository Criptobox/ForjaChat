package com.forja.chat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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

    interface PageListener {
        void progress(int percent);

        void error(boolean failed);
    }

    private static final class Pending {
        final String text;
        final ValueCallback<String> cb;

        Pending(String text, ValueCallback<String> cb) {
            this.text = text;
            this.cb = cb;
        }
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
    private final Map<String, PageListener> listeners = new HashMap<>();
    private final Set<String> started = new HashSet<>();
    private final Set<String> ready = new HashSet<>();
    private final Set<String> errored = new HashSet<>();
    private final Map<String, Pending> pending = new HashMap<>();

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

    /** Registra quién escucha el progreso y los errores de carga de este asistente. */
    void listen(String id, PageListener l) {
        listeners.put(id, l);
        WebView w = views.get(id);
        l.progress(w != null ? w.getProgress() : 0);
        l.error(errored.contains(id));
    }

    void detachAll() {
        for (WebView w : views.values()) {
            if (w.getParent() instanceof ViewGroup) {
                ((ViewGroup) w.getParent()).removeView(w);
            }
        }
    }

    void reload(String id) {
        WebView w = views.get(id);
        if (w == null) return;
        errored.remove(id);
        ready.remove(id);
        PageListener l = listeners.get(id);
        if (l != null) l.error(false);
        w.reload();
    }

    void reloadAll() {
        for (String id : new HashSet<>(views.keySet())) reload(id);
    }

    /** Rellena la caja de texto del asistente. No pulsa enviar: lo hace la persona. */
    void fill(Assistant a, String text, ValueCallback<String> result) {
        WebView w = views.get(a.id);
        if (w == null) return;
        if (ready.contains(a.id)) {
            final WebView fw = w;
            fw.postDelayed(() -> inject(fw, text, result), 300);
        } else {
            pending.put(a.id, new Pending(text, result));
        }
    }

    private void inject(WebView w, String text, ValueCallback<String> result) {
        w.evaluateJavascript(FILL_JS + JSONObject.quote(text) + ")", result);
    }

    void destroyAll() {
        for (WebView w : views.values()) w.destroy();
        views.clear();
        listeners.clear();
        started.clear();
        ready.clear();
        errored.clear();
        pending.clear();
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
            public void onPageStarted(WebView v, String url, Bitmap icon) {
                errored.remove(a.id);
                PageListener l = listeners.get(a.id);
                if (l != null) l.error(false);
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    errored.add(a.id);
                    PageListener l = listeners.get(a.id);
                    if (l != null) l.error(true);
                }
            }

            @Override
            public void onPageFinished(final WebView v, String url) {
                if (errored.contains(a.id)) return;
                ready.add(a.id);
                final Pending p = pending.remove(a.id);
                if (p != null) v.postDelayed(() -> inject(v, p.text, p.cb), 1500);
            }
        });

        w.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int newProgress) {
                PageListener l = listeners.get(a.id);
                if (l != null) l.progress(newProgress);
            }

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
