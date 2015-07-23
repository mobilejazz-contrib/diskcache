package com.mobilejazz.android.diskcache.library;

import java.io.File;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

import com.mobilejazz.android.diskcache.library.CacheProvider;

public class CacheProviderTest extends AndroidTestCase {

    String mProviderAuthority;

    private IsolatedContext mProviderContext;
    private MockContentResolver mResolver;

    private class MockContext2 extends MockContext {

        @Override
        public Resources getResources() {
            return getContext().getResources();
        }

        @Override
        public File getDir(String name, int mode) {
            // name the directory so the directory will be separated from
            // one created through the regular Context
            return getContext().getDir("mockcontext2_" + name, mode);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }
    }

    public CacheProviderTest() {
        mProviderAuthority = "com.mobilejazz.android.diskcache.library.CACHE_PROVIDER";
    }

    private CacheProvider mProvider;

    public CacheProvider getProvider() {
        return mProvider;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mResolver = new MockContentResolver();
        final String filenamePrefix = "test.";
        RenamingDelegatingContext targetContextWrapper = new RenamingDelegatingContext(new MockContext2(), getContext(), filenamePrefix);
        mProviderContext = new IsolatedContext(mResolver, targetContextWrapper);
        mProvider = createProviderForTest(mProviderContext, mProviderAuthority);
        mResolver.addProvider(mProviderAuthority, getProvider());
    }

    static CacheProvider createProviderForTest(Context context, String authority) throws IllegalAccessException, InstantiationException {
        CacheProvider instance = new CacheProvider();
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = authority;
        providerInfo.metaData = new Bundle();
        providerInfo.metaData.putInt("android.support.FILE_PROVIDER_PATHS", R.xml.cache_filepath_single);
        instance.attachInfoForTesting(context, providerInfo);
        return instance;
    }

    @Override
    protected void tearDown() throws Exception {
        mProvider.shutdown();
        super.tearDown();
    }

    public MockContentResolver getMockContentResolver() {
        return mResolver;
    }

    public IsolatedContext getMockContext() {
        return mProviderContext;
    }

    public void testSomething() {
        assertEquals(true, false);
    }

}
