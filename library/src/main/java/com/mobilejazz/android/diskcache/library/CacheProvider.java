package com.mobilejazz.android.diskcache.library;

import java.io.FileNotFoundException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.FileProvider;

public class CacheProvider extends FileProvider {

    public static final int REQUEST_CLEAR_CACHE = 44477905;
    public static final String ACTION_CLEAR_CACHE = "com.mobilejazz.android.diskcache.library.ACTION_CLEAR_CACHE";

    @Override
    public boolean onCreate() {
        super.onCreate();
        PendingIntent clearCache = PendingIntent.getBroadcast(getContext(), REQUEST_CLEAR_CACHE,
                new Intent(ACTION_CLEAR_CACHE).setClass(getContext(), CacheBroadcastReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) (getContext().getSystemService(Context.ALARM_SERVICE));
        am.cancel(clearCache);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, clearCache);

        try {
            getContext().registerReceiver(new CacheBroadcastReceiver(), IntentFilter.create(ACTION_CLEAR_CACHE, "*/*"));
        } catch (IllegalArgumentException e) {
            // already registered
        }

        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return super.openFile(uri, mode);
    }
}
