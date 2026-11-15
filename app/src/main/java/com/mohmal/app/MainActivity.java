package com.mohmal.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final String HOME = "https://www.mohmal.com/ar";
    private static final String SITE = "https://www.mohmal.com";
    private static final int MAX_BATCH = 20;

    // مفتاح الإيقاف عن بعد: إذا انحذف هذا الملف من GitHub أو انكتب بيه off يتوقف التطبيق
    private static final String STATUS_URL =
            "https://raw.githubusercontent.com/7areq1-wq/mohmal/main/status.txt";
    private static final long GRACE_MS = 3L * 24 * 60 * 60 * 1000; // 3 أيام بدون إنترنت
    private static final String DEFAULT_OFF_MSG = "التطبيق متوقف حالياً.";

    private static final String[] OWN_HOSTS = {
            "mohmal.com", "mohmal.in", "mohmal.im",
            "mailna.co", "mailna.in", "mailna.me", "mozej.com"
    };

    private static final String[] BLOCKED_HOSTS = {
            "googlesyndication.com", "doubleclick.net", "googleadservices.com",
            "adservice.google.com", "google-analytics.com", "googletagmanager.com",
            "googletagservices.com", "adsterra.com", "propellerads.com",
            "popads.net", "popcash.net", "exoclick.com", "juicyads.com",
            "clickadu.com", "hilltopads.net", "adnxs.com", "taboola.com",
            "outbrain.com", "criteo.com", "amazon-adsystem.com",
            "rubiconproject.com", "mc.yandex.ru", "an.yandex.ru",
            "scorecardresearch.com", "adform.net", "mgid.com",
            "revcontent.com", "monetag.com", "a-ads.com",
            "coinzilla.com", "bidvertiser.com"
    };

    private static final String HIDE_ADS_JS =
            "(function(){"
                    + "var css='ins.adsbygoogle,.adsbygoogle,[id^=\"google_ads\"],[id*=\"ad-container\"],"
                    + "[class*=\"ad-container\"],[class*=\"adsbox\"],[id^=\"aswift\"],"
                    + "iframe[src*=\"doubleclick\"],iframe[src*=\"googlesyndication\"],"
                    + "iframe[src*=\"/ads/\"]"
                    + "{display:none!important;height:0!important;visibility:hidden!important}';"
                    + "var s=document.createElement('style');s.innerHTML=css;"
                    + "(document.head||document.documentElement).appendChild(s);"
                    + "})();";

    // يدور على عنوان الإيميل بالصفحة
    private static final String FIND_ADDRESS_JS =
            "(function(){"
                    + "function pick(t){var re=/[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+([.][A-Za-z0-9-]+)+/g;var m;"
                    + "while((m=re.exec(t))){var a=m[0];var l=a.toLowerCase();"
                    + "if(l.indexOf('@mohmal.com')<0&&l.indexOf('@www.mohmal.com')<0) return a;}"
                    + "return '';}"
                    + "var r=pick(document.body?document.body.innerText:'');"
                    + "if(r) return r;"
                    + "var inp=document.querySelectorAll('input,textarea');"
                    + "for(var i=0;i<inp.length;i++){var rr=pick(inp[i].value||''); if(rr) return rr;}"
                    + "return '';"
                    + "})()";

    // يضغط زر إنشاء إيميل عشوائي
    private static final String CLICK_CREATE_JS =
            "(function(){"
                    + "var els=document.querySelectorAll('button,a,input[type=button],input[type=submit],[role=button]');"
                    + "var pri=['إنشاء بريد باسم عشوائي','عشوائي','إنشاء'];"
                    + "for(var p=0;p<pri.length;p++){"
                    + "for(var i=0;i<els.length;i++){"
                    + "var tx=(els[i].innerText||els[i].value||'').trim();"
                    + "if(tx&&tx.indexOf(pri[p])>=0){els[i].click();return tx;}"
                    + "}}"
                    + "return '';"
                    + "})()";

    // نص الصفحة (للتشخيص إذا صار خطأ)
    private static final String DEBUG_JS =
            "(function(){var t=document.body?document.body.innerText:'';return t.substring(0,300);})()";

    // يستخرج الأكواد من الصندوق الوارد
    private static final String EXTRACT_JS =
            "(function(){"
                    + "function codes(t){var out=[];var m;"
                    + "var a=/(code|otp|verify|verification|confirm|رمز|كود|تحقق|تأكيد|التفعيل)[^0-9]{0,40}([0-9]{4,8})/ig;"
                    + "while((m=a.exec(t))){out.push(m[2]);}"
                    + "var b=/(^|[^0-9])([0-9]{6})([^0-9]|$)/g;"
                    + "while((m=b.exec(t))){out.push(m[2]);}"
                    + "return out;}"
                    + "var all=codes(document.body?document.body.innerText:'');"
                    + "var links=[];var seen={};"
                    + "var as=document.querySelectorAll('a[href]');"
                    + "for(var i=0;i<as.length;i++){var h=as[i].href;"
                    + "if(h.indexOf(location.origin)!==0||seen[h]||h===location.href) continue;"
                    + "if(h.indexOf('blog')>=0) continue;"
                    + "if(!/(message|inbox|mail|read|view|msg)/i.test(h)) continue;"
                    + "seen[h]=1;links.push(h);}"
                    + "var fetched=0;"
                    + "for(var j=0;j<links.length&&j<8;j++){try{var x=new XMLHttpRequest();x.open('GET',links[j],false);x.send();"
                    + "if(x.status===200){fetched++;var tx=x.responseText.replace(/<[^>]*>/g,' ');all=all.concat(codes(tx));}}catch(e){}}"
                    + "var uniq=[];for(var k=0;k<all.length;k++){if(uniq.indexOf(all[k])<0)uniq.push(all[k]);}"
                    + "var lp=[];for(var q=0;q<links.length&&q<3;q++){lp.push(links[q].replace(location.origin,''));}"
                    + "return JSON.stringify({codes:uniq,links:links.length,fetched:fetched,sample:lp});"
                    + "})()";

    private WebView webView;
    private TextView status;
    private TextView blockView;
    private LinearLayout bar;
    private boolean blocked = false;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // كل عنصر: [العنوان, الكوكيز]
    private final ArrayList<String[]> accounts = new ArrayList<String[]>();

    private boolean busy = false;
    private boolean pageLoaded = false;

    private int target = 0;
    private int createdThisRun = 0;

    private int extractIndex = 0;
    private StringBuilder results = new StringBuilder();
    private final ArrayList<String> allCodes = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("mohmal", MODE_PRIVATE);
        loadAccounts();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        Button btnCreate = makeButton("إنشاء إيميلات");
        Button btnCodes = makeButton("استخراج الأكواد");
        Button btnList = makeButton("القائمة");
        bar.addView(btnCreate);
        bar.addView(btnCodes);
        bar.addView(btnList);

        status = new TextView(this);
        status.setPadding(24, 12, 24, 12);
        status.setVisibility(View.GONE);

        webView = new WebView(this);

        root.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        blockView = new TextView(this);
        blockView.setGravity(Gravity.CENTER);
        blockView.setPadding(48, 48, 48, 48);
        blockView.setTextSize(18);
        blockView.setVisibility(View.GONE);
        root.addView(blockView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        btnCreate.setOnClickListener(v -> askCount());
        btnCodes.setOnClickListener(v -> startExtract());
        btnList.setOnClickListener(v -> showList());

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        ws.setSupportMultipleWindows(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                if (host != null && matches(host, BLOCKED_HOSTS)) {
                    return new WebResourceResponse("text/plain", "utf-8",
                            new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && matches(host, OWN_HOSTS)) {
                    return false;
                }
                if (request.hasGesture()) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) { }
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                view.evaluateJavascript(HIDE_ADS_JS, null);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                view.evaluateJavascript(HIDE_ADS_JS, null);
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME);
        }
    }

    // ---------- مفتاح الإيقاف عن بعد ----------

    @Override
    protected void onResume() {
        super.onResume();
        checkStatus();
    }

    private void checkStatus() {
        final long startTime = System.currentTimeMillis();
        if (!prefs.contains("lastOk")) {
            prefs.edit().putLong("lastOk", startTime).apply();
        }
        new Thread(() -> {
            // 0 = ما قدرنا نتأكد، 1 = شغال، 2 = متوقف
            int state = 0;
            String msg = DEFAULT_OFF_MSG;
            try {
                URL url = new URL(STATUS_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setUseCaches(false);
                int code = c.getResponseCode();
                if (code == 200) {
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(c.getInputStream(), "UTF-8"));
                    String first = r.readLine();
                    StringBuilder rest = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (rest.length() > 0) rest.append('\n');
                        rest.append(line);
                    }
                    r.close();
                    String f = first == null ? "" : first.trim();
                    if (f.equalsIgnoreCase("on")) {
                        state = 1;
                    } else if (f.equalsIgnoreCase("off")) {
                        state = 2;
                        if (rest.toString().trim().length() > 0) msg = rest.toString().trim();
                    }
                } else if (code == 404 || code == 403 || code == 410) {
                    state = 2;
                    msg = "هذا التطبيق لم يعد متاحاً.";
                }
                c.disconnect();
            } catch (Exception e) {
                state = 0;
            }

            final boolean fBlock;
            final String fMsg;
            if (state == 1) {
                prefs.edit().putLong("lastOk", System.currentTimeMillis()).apply();
                fBlock = false;
                fMsg = "";
            } else if (state == 2) {
                fBlock = true;
                fMsg = msg;
            } else {
                long last = prefs.getLong("lastOk", startTime);
                if (System.currentTimeMillis() - last > GRACE_MS) {
                    fBlock = true;
                    fMsg = "تعذر التحقق من حالة التطبيق. اتصل بالإنترنت ثم أعد فتحه.";
                } else {
                    fBlock = false;
                    fMsg = "";
                }
            }
            handler.post(() -> applyStatus(fBlock, fMsg));
        }).start();
    }

    private void applyStatus(boolean block, String msg) {
        if (block) {
            blocked = true;
            busy = false;
            handler.removeCallbacksAndMessages(null);
            status.setVisibility(View.GONE);
            bar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            webView.loadUrl("about:blank");
            blockView.setText(msg);
            blockView.setVisibility(View.VISIBLE);
        } else if (blocked) {
            blocked = false;
            blockView.setVisibility(View.GONE);
            bar.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(HOME);
        }
    }

    // ---------- واجهة ----------

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return b;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setVisibility(View.VISIBLE);
    }

    private void endBusy(String msg) {
        busy = false;
        status.setVisibility(View.GONE);
        if (msg != null) toast(msg);
    }

    private void copy(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("mohmal", text));
        toast("تم النسخ");
    }

    private static String joinLines(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(s);
        }
        return sb.toString();
    }

    // ---------- حفظ القائمة ----------

    private void saveAccounts() {
        JSONArray arr = new JSONArray();
        try {
            for (String[] a : accounts) {
                JSONObject o = new JSONObject();
                o.put("a", a[0]);
                o.put("c", a[1]);
                arr.put(o);
            }
        } catch (JSONException ignored) { }
        prefs.edit().putString("accounts", arr.toString()).apply();
    }

    private void loadAccounts() {
        try {
            JSONArray arr = new JSONArray(prefs.getString("accounts", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                accounts.add(new String[]{o.getString("a"), o.getString("c")});
            }
        } catch (JSONException ignored) { }
    }

    private boolean hasAddress(String addr) {
        for (String[] a : accounts) {
            if (a[0].equalsIgnoreCase(addr)) return true;
        }
        return false;
    }

    private static String unquote(String raw) {
        if (raw == null || raw.equals("null")) return "";
        try {
            Object o = new JSONTokener(raw).nextValue();
            return (o instanceof String) ? (String) o : "";
        } catch (JSONException e) {
            return "";
        }
    }

    // يمسح الجلسة الحالية (كوكيز + تخزين) ثم ينفذ الخطوة التالية
    private void clearSession(final Runnable next) {
        final CookieManager cm = CookieManager.getInstance();
        WebStorage.getInstance().deleteAllData();
        cm.removeAllCookies(v -> {
            cm.flush();
            next.run();
        });
    }

    // ---------- القائمة ----------

    private void showList() {
        if (accounts.isEmpty()) {
            toast("القائمة فاضية");
            return;
        }
        final String[] items = new String[accounts.size()];
        final ArrayList<String> all = new ArrayList<String>();
        for (int i = 0; i < items.length; i++) {
            items[i] = accounts.get(i)[0];
            all.add(items[i]);
        }
        new AlertDialog.Builder(this)
                .setTitle("الإيميلات (اضغط للنسخ)")
                .setItems(items, (d, which) -> copy(items[which]))
                .setNeutralButton("نسخ الكل", (d, w) -> copy(joinLines(all)))
                .setNegativeButton("مسح القائمة", (d, w) -> {
                    accounts.clear();
                    saveAccounts();
                    toast("انمسحت القائمة");
                })
                .setPositiveButton("إغلاق", null)
                .show();
    }

    // ---------- إنشاء إيميلات ----------

    private void askCount() {
        if (busy) {
            toast("في عملية شغالة، انتظر");
            return;
        }
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("العدد (1-" + MAX_BATCH + ")");
        new AlertDialog.Builder(this)
                .setTitle("كم إيميل تريد؟")
                .setView(input)
                .setPositiveButton("ابدأ", (d, w) -> {
                    int n = 0;
                    try {
                        n = Integer.parseInt(input.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        n = 0;
                    }
                    if (n < 1) {
                        toast("اكتب رقم صحيح");
                        return;
                    }
                    if (n > MAX_BATCH) n = MAX_BATCH;
                    startCreate(n);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void startCreate(int n) {
        if (busy) return;
        busy = true;
        target = n;
        createdThisRun = 0;
        createNext();
    }

    private void createNext() {
        if (createdThisRun >= target) {
            endBusy("تم إنشاء " + createdThisRun + " إيميل");
            showList();
            return;
        }
        setStatus("جاري إنشاء " + (createdThisRun + 1) + " من " + target + "...");
        clearSession(() -> {
            pageLoaded = false;
            webView.loadUrl(HOME);
            handler.postDelayed(() -> pollAddress(0, false), 3000);
        });
    }

    private void pollAddress(final int tries, final boolean clicked) {
        if (!pageLoaded && tries < 20) {
            handler.postDelayed(() -> pollAddress(tries + 1, clicked), 1000);
            return;
        }
        webView.evaluateJavascript(FIND_ADDRESS_JS, raw -> {
            String addr = unquote(raw);
            if (addr.length() > 0 && !hasAddress(addr)) {
                String cookies = CookieManager.getInstance().getCookie(SITE);
                accounts.add(new String[]{addr, cookies == null ? "" : cookies});
                saveAccounts();
                createdThisRun++;
                handler.postDelayed(() -> createNext(), 2000);
                return;
            }
            if (tries >= 25) {
                failCreate("ما كدرت أحصل على إيميل جديد.");
                return;
            }
            boolean c = clicked;
            if (tries == 6 && !clicked) {
                webView.evaluateJavascript(CLICK_CREATE_JS, null);
                c = true;
            }
            final boolean cc = c;
            handler.postDelayed(() -> pollAddress(tries + 1, cc), 1000);
        });
    }

    private void failCreate(final String reason) {
        webView.evaluateJavascript(DEBUG_JS, raw -> {
            String sample = unquote(raw);
            endBusy(null);
            new AlertDialog.Builder(this)
                    .setTitle("توقف الإنشاء")
                    .setMessage(reason + "\nتم إنشاء: " + createdThisRun
                            + "\n\nنص الصفحة:\n" + sample)
                    .setPositiveButton("تمام", null)
                    .show();
        });
    }

    // ---------- استخراج الأكواد ----------

    private void startExtract() {
        if (busy) {
            toast("في عملية شغالة، انتظر");
            return;
        }
        if (accounts.isEmpty()) {
            toast("سوي إيميلات أول");
            return;
        }
        busy = true;
        results = new StringBuilder();
        allCodes.clear();
        extractIndex = 0;
        extractNext();
    }

    private void extractNext() {
        if (extractIndex >= accounts.size()) {
            endBusy(null);
            showCodes();
            return;
        }
        final String[] acc = accounts.get(extractIndex);
        setStatus("فحص " + (extractIndex + 1) + " من " + accounts.size() + "...");
        clearSession(() -> {
            CookieManager cm = CookieManager.getInstance();
            for (String part : acc[1].split(";")) {
                String p = part.trim();
                if (p.length() > 0) cm.setCookie(SITE, p + "; Path=/");
            }
            cm.flush();
            pageLoaded = false;
            webView.loadUrl(HOME);
            handler.postDelayed(() -> pollExtract(0), 3000);
        });
    }

    private void pollExtract(final int tries) {
        if (!pageLoaded && tries < 20) {
            handler.postDelayed(() -> pollExtract(tries + 1), 1000);
            return;
        }
        handler.postDelayed(() -> webView.evaluateJavascript(EXTRACT_JS,
                raw -> onExtracted(unquote(raw))), 2500);
    }

    private void onExtracted(String json) {
        results.append(accounts.get(extractIndex)[0]).append('\n');
        try {
            JSONObject o = new JSONObject(json);
            JSONArray a = o.getJSONArray("codes");
            if (a.length() == 0) {
                results.append("   لا توجد أكواد (روابط: ").append(o.getInt("links")).append(")\n");
                JSONArray s = o.getJSONArray("sample");
                for (int i = 0; i < s.length(); i++) {
                    results.append("   ").append(s.getString(i)).append('\n');
                }
            } else {
                StringBuilder c = new StringBuilder();
                for (int i = 0; i < a.length(); i++) {
                    if (c.length() > 0) c.append(", ");
                    c.append(a.getString(i));
                    allCodes.add(a.getString(i));
                }
                results.append("   ").append(c).append('\n');
            }
        } catch (JSONException e) {
            results.append("   تعذر قراءة النتيجة\n");
        }
        results.append('\n');
        extractIndex++;
        extractNext();
    }

    private void showCodes() {
        new AlertDialog.Builder(this)
                .setTitle("الأكواد")
                .setMessage(results.toString())
                .setPositiveButton("تحديث", (d, w) -> startExtract())
                .setNeutralButton("نسخ الأكواد", (d, w) -> copy(joinLines(allCodes)))
                .setNegativeButton("إغلاق", null)
                .show();
    }

    // ---------- عام ----------

    private static boolean matches(String host, String[] list) {
        host = host.toLowerCase();
        for (String d : list) {
            if (host.equals(d) || host.endsWith("." + d)) return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
