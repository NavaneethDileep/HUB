package com.academic.hub;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.CalendarContract;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView web;
    private static final int REQ_NOTIF = 44;

    private boolean pageReady = false;
    private int pendingImeBottom = 0;
    private int pendingNavBottom = 0;
    private int pendingStatusTop = 0;

    @Override
    public void onCreate(android.os.Bundle b) {
        super.onCreate(b);

        // Keep keyboard handling enabled.
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        );

        web = new WebView(this);
        web.setBackgroundColor(0xfff4f1e8);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);

        web.addJavascriptInterface(new Bridge(this), "Android");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageReady = true;
                sendInsetsToWeb(
                        pendingImeBottom,
                        pendingNavBottom,
                        pendingStatusTop
                );
            }
        });

        // Android 15 / target SDK 35 uses edge-to-edge.
        // Read the real IME (keyboard) inset and tell the WebView page
        // exactly how much space is occupied by the keyboard.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            web.setOnApplyWindowInsetsListener(
                    new View.OnApplyWindowInsetsListener() {
                        @Override
                        public WindowInsets onApplyWindowInsets(
                                View v,
                                WindowInsets insets
                        ) {
                            int imeBottom = 0;
                            int navBottom = 0;
                            int statusTop = 0;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                android.graphics.Insets ime =
                                        insets.getInsets(
                                                WindowInsets.Type.ime()
                                        );

                                android.graphics.Insets bars =
                                        insets.getInsets(
                                                WindowInsets.Type.systemBars()
                                        );

                                imeBottom = ime.bottom;
                                navBottom = bars.bottom;
                                statusTop = bars.top;
                            } else {
                                // Fallback for older Android versions.
                                android.graphics.Rect r =
                                        new android.graphics.Rect();

                                web.getWindowVisibleDisplayFrame(r);

                                int rootHeight =
                                        web.getRootView().getHeight();

                                imeBottom =
                                        Math.max(0, rootHeight - r.bottom);
                            }

                            pendingImeBottom = imeBottom;
                            pendingNavBottom = navBottom;
                            pendingStatusTop = statusTop;

                            if (pageReady) {
                                sendInsetsToWeb(
                                        imeBottom,
                                        navBottom,
                                        statusTop
                                );
                            }

                            // We are only observing the insets here.
                            // Let WebView continue its normal processing.
                            return insets;
                        }
                    }
            );
        }

        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    REQ_NOTIF
            );
        }
    }

    private void sendInsetsToWeb(
            int imeBottomPx,
            int navBottomPx,
            int statusTopPx
    ) {
        if (web == null) return;

        float density =
                getResources().getDisplayMetrics().density;

        if (density <= 0) density = 1f;

        // Android reports physical pixels; WebView CSS uses
        // density-independent CSS pixels.
        final float ime =
                imeBottomPx / density;

        final float nav =
                navBottomPx / density;

        final float status =
                statusTopPx / density;

        String js =
                "(function(){" +

                "document.documentElement.style.setProperty(" +
                "'--android-ime-inset'," +
                "'" + ime + "px');" +

                "document.documentElement.style.setProperty(" +
                "'--android-nav-inset'," +
                "'" + nav + "px');" +

                "document.documentElement.style.setProperty(" +
                "'--android-status-inset'," +
                "'" + status + "px');" +

                "var modal=document.getElementById('modal');" +

                "if(modal){" +

                // This is the important part:
                // when the keyboard is open, the modal's bottom is
                // moved to the top edge of the keyboard.
                "var h=Math.max(1,window.innerHeight-" +
                ime + ");" +

                "modal.style.height=h+'px';" +
                "modal.style.top='0px';" +
                "modal.style.bottom='auto';" +

                "var sheet=modal.querySelector('.sheet');" +

                "if(sheet){" +
                "sheet.style.maxHeight=" +
                "Math.max(160,h-16)+'px';" +
                "}" +

                "}" +

                // If an input is currently focused, keep it visible.
                "var a=document.activeElement;" +

                "if(a&&a.matches&&" +
                "a.matches('input,select,textarea')){" +

                "setTimeout(function(){" +
                "a.scrollIntoView({" +
                "block:'center'," +
                "behavior:'smooth'" +
                "});" +
                "},80);" +

                "}" +

                "})();";

        web.post(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript(js, null);
            }
        });
    }

    public static class Bridge {

        private final Context c;

        Bridge(Context c) {
            this.c = c;
        }

        @JavascriptInterface
        public void addCalendar(
                String title,
                String start,
                String end,
                String notes
        ) {
            try {
                long s = Long.parseLong(start);
                long e = Long.parseLong(end);

                Intent i =
                        new Intent(Intent.ACTION_INSERT)
                                .setData(
                                        CalendarContract.Events.CONTENT_URI
                                )
                                .putExtra(
                                        CalendarContract.Events.TITLE,
                                        title
                                )
                                .putExtra(
                                        CalendarContract.Events.EVENT_LOCATION,
                                        "HUB"
                                )
                                .putExtra(
                                        CalendarContract.Events.DESCRIPTION,
                                        notes
                                )
                                .putExtra(
                                        CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                        s
                                )
                                .putExtra(
                                        CalendarContract.EXTRA_EVENT_END_TIME,
                                        e
                                );

                c.startActivity(i);

            } catch (Exception ignored) {
            }
        }

        @JavascriptInterface
        public void scheduleReminder(
                String title,
                long when
        ) {
            try {
                Intent i =
                        new Intent(
                                c,
                                ReminderReceiver.class
                        )
                                .putExtra("title", title);

                PendingIntent p =
                        PendingIntent.getBroadcast(
                                c,
                                (int) (
                                        when % Integer.MAX_VALUE
                                ),
                                i,
                                PendingIntent.FLAG_UPDATE_CURRENT
                                        | PendingIntent.FLAG_IMMUTABLE
                        );

                AlarmManager a =
                        (AlarmManager)
                                c.getSystemService(
                                        Context.ALARM_SERVICE
                                );

                if (Build.VERSION.SDK_INT >= 31
                        && !a.canScheduleExactAlarms()) {

                    a.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            when,
                            p
                    );

                } else {

                    a.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            when,
                            p
                    );
                }

            } catch (Exception ignored) {
            }
        }
    }
}

