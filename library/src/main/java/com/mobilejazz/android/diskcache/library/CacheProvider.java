package com.mobilejazz.android.diskcache.library;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.FileProvider;
import android.util.Log;

public class CacheProvider extends FileProvider {

    public static final String TAG = "diskcache";

    public static final int REQUEST_CLEAR_CACHE = 44477905;
    public static final String ACTION_CLEAR_CACHE = "com.mobilejazz.android.diskcache.library.ACTION_CLEAR_CACHE";
    public static final String EXTRA_AUTHORITY = "com.mobilejazz.android.diskcache.library.EXTRA_AUTHORITY";

    public static final int MAX_AGE = 24 * 3600; // one day

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        Intent clearCacheIntent = new Intent(ACTION_CLEAR_CACHE).setClass(getContext(), CacheBroadcastReceiver.class).putExtra(EXTRA_AUTHORITY, info.authority);
        PendingIntent clearCache = PendingIntent.getBroadcast(getContext(), REQUEST_CLEAR_CACHE, clearCacheIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) (getContext().getSystemService(Context.ALARM_SERVICE));
        am.cancel(clearCache);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, clearCache);

        try {
            getContext().registerReceiver(new CacheBroadcastReceiver(), IntentFilter.create(ACTION_CLEAR_CACHE, "*/*"));
        } catch (IllegalArgumentException e) {
            // already registered
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return super.openFile(uri, mode);
    }

    public static void clear(Context context, String authority) throws IOException, XmlPullParserException {
        // delete all files that are older than the maximum allowed age:
        visitFiles(context, authority, new Visitor<File>() {
            @Override
            public void visit(File f) {
                if (System.currentTimeMillis() - f.lastModified() > MAX_AGE) {
                    boolean res = f.delete();
                    Log.i(TAG, String.format("Removing file %s - %s", f.getAbsolutePath(), (res) ? "SUCCESS" : "FAILURE"));
                }
            }
        });
    }

    private static final String META_DATA_FILE_PROVIDER_PATHS = "android.support.FILE_PROVIDER_PATHS";
    private static final String ATTR_PATH = "path";
    private static final String TAG_ROOT_PATH = "root-path";
    private static final String TAG_FILES_PATH = "files-path";
    private static final String TAG_CACHE_PATH = "cache-path";
    private static final String TAG_EXTERNAL = "external-path";
    private static final File DEVICE_ROOT = new File("/");

    private static Map<String, List<File>> directories = new HashMap<>();

    /**
     * Gets all cache directories that the {@link CacheProvider} with the given
     * authority supports. If you are only interested in creating a file that is
     * monitored by this provider, or if the provider has only one directory,
     * {@link #getCacheDirectory(Context, String)} is provided for ease of
     * usage.
     * 
     * @param context
     *            The current {@link Context}.
     * @param authority
     *            The authority of the provider you want to query the list of
     *            files for.
     * @return A list of directories that are monitored by this cache provider.
     * @throws IOException
     *             If there is an error while reading the meta file that defines
     *             the directories.
     */
    public static List<File> getCacheDirectories(Context context, String authority) throws IOException {
        List<File> result = directories.get(authority);
        if (result == null) {
            result = getDirectoriesFromXml(context, authority);
            directories.put(authority, result);
        }
        return result;
    }

    private static List<File> getDirectoriesFromXml(Context context, String authority) throws IOException {
        try {
            List<File> result = new ArrayList<>();

            final ProviderInfo info = context.getPackageManager().resolveContentProvider(authority, PackageManager.GET_META_DATA);
            final XmlResourceParser in = info.loadXmlMetaData(context.getPackageManager(), META_DATA_FILE_PROVIDER_PATHS);
            if (in == null) {
                throw new IllegalArgumentException("Missing " + META_DATA_FILE_PROVIDER_PATHS + " meta-data");
            }

            int type;
            while ((type = in.next()) != END_DOCUMENT) {
                if (type == START_TAG) {
                    final String tag = in.getName();

                    String path = in.getAttributeValue(null, ATTR_PATH);

                    File target = null;
                    if (TAG_ROOT_PATH.equals(tag)) {
                        target = new File(DEVICE_ROOT, path);
                    } else if (TAG_FILES_PATH.equals(tag)) {
                        target = new File(context.getFilesDir(), path);
                    } else if (TAG_CACHE_PATH.equals(tag)) {
                        target = new File(context.getCacheDir(), path);
                    } else if (TAG_EXTERNAL.equals(tag)) {
                        target = new File(Environment.getExternalStorageDirectory(), path);
                    }

                    if (target != null) {
                        result.add(target);
                    }
                }
            }

            return result;
        } catch (XmlPullParserException e) {
            throw new IOException("Error while parsing xml file: " + e.getLocalizedMessage());
        }
    }

    public static void visitFiles(Context context, String authority, Visitor<File> visitor) throws IOException {
        for (File f : getCacheDirectories(context, authority)) {
            visitDirectory(f, visitor);
        }
    }

    private static void visitDirectory(File directory, Visitor<File> visitor) {
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                visitDirectory(f, visitor);
            } else {
                visitor.visit(f);
            }
        }
    }

    /**
     * Returns the first of the cache provider's registered cache directories.
     * See {@link #getCacheDirectories(Context, String)} for more details.
     *
     * @param context
     *            The current {@link Context}.
     * @param authority
     *            The authority of the content provider.
     * @return the first of this provider's registered cache directories
     * @throws IOException
     *             If there is an error while reading the meta file that defines
     *             the directories.
     */
    public static File getCacheDirectory(Context context, String authority) throws IOException {
        List<File> ds = getCacheDirectories(context, authority);
        if (ds.size() == 0) {
            throw new IllegalStateException("This cache provider has no directory defined.");
        }
        return ds.get(0);
    }

}
