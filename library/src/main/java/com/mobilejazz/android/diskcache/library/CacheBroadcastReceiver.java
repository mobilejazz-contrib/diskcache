package com.mobilejazz.android.diskcache.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CacheBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            CacheProvider.clear(context, intent.getStringExtra(CacheProvider.EXTRA_AUTHORITY));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
