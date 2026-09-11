package com.academic.hub;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.CalendarContract;
import android.view.*;
import android.webkit.*;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView web;
    private static final int REQ_NOTIF = 44;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        web = new WebView(this);
        web.setBackgroundColor(0xfff4f1e8);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setBuiltInZoomControls(false);
        web.addJavascriptInterface(new Bridge(this), "Android");
        web.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                applyInsets(bars.top, bars.bottom);
            } else {
                applyInsets(0, 0);
            }
            return insets;
        });
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
    }

    private void applyInsets(int top, int bottom) {
        String js = "document.documentElement.style.setProperty('--android-status-inset','" + top + "px');" +
                    "document.documentElement.style.setProperty('--android-nav-inset','" + bottom + "px');";
        web.post(() -> web.evaluateJavascript(js, null));
    }

    public static class Bridge {
        Context c;
        Bridge(Context c){this.c=c;}
        @JavascriptInterface public void addCalendar(String title,String start,String end,String notes){
            try {
                long s=Long.parseLong(start),e=Long.parseLong(end);
                Intent i=new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE,title)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION,"HUB")
                    .putExtra(CalendarContract.Events.DESCRIPTION,notes)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,s)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,e);
                c.startActivity(i);
            } catch(Exception ignored){}
        }
        @JavascriptInterface public void scheduleReminder(String title,long when){
            try {
                Intent i=new Intent(c,ReminderReceiver.class).putExtra("title",title);
                PendingIntent p=PendingIntent.getBroadcast(c,(int)(when%Integer.MAX_VALUE),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
                if(Build.VERSION.SDK_INT>=31&&!a.canScheduleExactAlarms()) a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);
                else a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);
            } catch(Exception ignored){}
        }
    }
}
