package com.academic.hub;

import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.net.Uri;import android.os.*;import android.provider.CalendarContract;import android.webkit.*;import java.util.*;

public class MainActivity extends Activity {
    WebView web; static final int REQ_NOTIF=44;
    @Override public void onCreate(Bundle b){super.onCreate(b); web=new WebView(this); web.setBackgroundColor(0xfff4f1e8); web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true); web.addJavascriptInterface(new Bridge(this),"Android"); web.loadUrl("file:///android_asset/index.html"); setContentView(web); if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIF);}
    public static class Bridge { Context c; Bridge(Context c){this.c=c;}
      @JavascriptInterface public void addCalendar(String title,String start,String end,String notes){try{long s=Long.parseLong(start),e=Long.parseLong(end);Intent i=new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE,title).putExtra(CalendarContract.Events.EVENT_LOCATION,"HUB").putExtra(CalendarContract.Events.DESCRIPTION,notes).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,s).putExtra(CalendarContract.EXTRA_EVENT_END_TIME,e);c.startActivity(i);}catch(Exception ignored){}}
      @JavascriptInterface public void scheduleReminder(String title,long when){try{Intent i=new Intent(c,ReminderReceiver.class).putExtra("title",title);PendingIntent p=PendingIntent.getBroadcast(c,(int)(when%Integer.MAX_VALUE),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE); if(Build.VERSION.SDK_INT>=31&&!a.canScheduleExactAlarms()){a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);}else{a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);}}catch(Exception ignored){}}
    }
}
