package com.mobilejazz.android.diskcache.library;

import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;

import static org.assertj.core.api.Assertions.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.KITKAT, manifest = "src/test/AndroidManifest.xml")
public class CacheProviderTest {

    private static final String AUTHORITY = "com.mobilejazz.android.diskcache.library.CACHE_PROVIDER";

    private CacheProvider mProvider;

    @Before
    public void setUp() {
        mProvider = new CacheProvider();
        mProvider.onCreate();
        ShadowContentResolver.registerProvider(AUTHORITY, mProvider);
    }

    @Test
    public void testClearing() throws Exception {
        CacheProvider.clear(RuntimeEnvironment.application.getApplicationContext(), AUTHORITY);
    }

    public void tearDown() {

    }

}
