package com.mobilejazz.android.diskcache.library;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

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

/**
 * A {@link FileProvider} implementation that additionally clears files that
 * exceed a certain age and ensures that the cache size does not exceed a
 * certain value. Both maximum age and size can be defined by metadata
 * properties ({@code com.mobilejazz.android.diskcache.META_MAX_AGE} and
 * {@code com.mobilejazz.android.diskcache.META_MAX_SIZE}).
 *
 * An alarm is used to schedule a clearing of the cache once a day.
 *
 * @see AlarmManager
 */
public class CacheProvider extends FileProvider {

    /**
     * Meta-data defining the maximum age of a file in hours. When a file
     * exceeds that age, it will be removed. Note that it will not be removed
     * immediately, but within the next day. If a file is modified it will reset
     * it's age (see {@link File#lastModified()}.
     */
    public static final String META_DATA_MAX_AGE = "com.mobilejazz.android.diskcache.META_MAX_AGE";

    /**
     * Meta-data defining the maximum size of the cache in kilobytes. When all
     * files collectively exceed the maximum cache size, files are deleted until
     * the cache size is smaller than the maximum cache sizes. The order in
     * which files are deleted is a combination of size and age.
     */
    public static final String META_DATA_MAX_SIZE = "com.mobilejazz.android.diskcache.META_MAX_SIZE";

    public static final String ACTION_CLEAR_CACHE = "com.mobilejazz.android.diskcache.ACTION_CLEAR_CACHE";

    static final String TAG = "diskcache";

    static final int REQUEST_CLEAR_CACHE = 44477905;
    static final String EXTRA_AUTHORITY = "com.mobilejazz.android.diskcache.EXTRA_AUTHORITY";

    @Override
    public void attachInfo(Context context, final ProviderInfo info) {
        super.attachInfo(context, info);
        Intent clearCacheIntent = new Intent(ACTION_CLEAR_CACHE).setPackage(getContext().getPackageName()).putExtra(EXTRA_AUTHORITY, info.authority);
        PendingIntent clearCache = PendingIntent.getBroadcast(getContext(), REQUEST_CLEAR_CACHE, clearCacheIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) (getContext().getSystemService(Context.ALARM_SERVICE));
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, clearCache);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return super.openFile(uri, mode);
    }

    /**
     * Manually clear the cache. Clearing the cache will remove all files that
     * are older than the maximum age (see {@link #META_DATA_MAX_AGE} and it
     * will remove selected files until the sum of all file sizes is smaller
     * than the maximum size (see {@link #META_DATA_MAX_SIZE}). Files will be
     * selected based on their age and their sizes.
     * 
     * @param context
     *            The current {@link Context}.
     * @param authority
     *            The authority of the cache provider for which to clear the
     *            files.
     * @throws IOException
     *             If there is an error while reading the meta file that defines
     *             the directories.
     */
    public static void clear(Context context, String authority) throws IOException {
        final ProviderInfo info = context.getPackageManager().resolveContentProvider(authority, PackageManager.GET_META_DATA);
        // delete all files that are older than the maximum allowed age:
        FileCleaner cleaner = new FileCleaner(info.metaData.getInt(META_DATA_MAX_AGE, 0), info.metaData.getInt(META_DATA_MAX_SIZE, 0));
        visitFiles(context, authority, cleaner);
        cleaner.purge();
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
        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    visitDirectory(f, visitor);
                } else {
                    visitor.visit(f);
                }
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

    private static class FileCleaner implements Visitor<File> {

        private int size;
        private PriorityQueue<File> purgeCandidates;

        private final int maxAge;
        private final int maxSize;

        public FileCleaner(int maxAge, int maxSize) {
            this.maxAge = maxAge * 3600000; // hours
            this.maxSize = maxSize * 1024; // kb
            size = 0;
            purgeCandidates = new PriorityQueue<>(32, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    double sa = score(lhs);
                    double sb = score(rhs);
                    if (sa > sb) {
                        return -1;
                    } else if (sa < sb) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
        }

        private double score(File f) {
            return ((double) (f.length()) / (double) maxSize) * 5.0 + ((double) (System.currentTimeMillis() - f.lastModified()) / (double) maxAge);
        }

        @Override
        public void visit(File f) {
            if (maxAge > 0 && System.currentTimeMillis() - f.lastModified() > maxAge) {
                long fileSize = f.length();
                boolean res = f.delete();
                if (!res) {
                    size += fileSize;
                }
                Log.i(TAG, String.format("[AGE] Removing file %s (%.1f kb) - %s", f.getAbsolutePath(), fileSize / 1024.0, (res) ? "SUCCESS" : "FAILURE"));
            } else if (maxSize > 0) {
                size += f.length();
                purgeCandidates.add(f);
            }
        }

        public void purge() {
            if (maxSize > 0) {
                // remove files until the size of the cache is lower than
                // MAX_SIZE:
                while (size > maxSize && purgeCandidates.size() > 0) {
                    File next = purgeCandidates.poll();
                    long fileSize = next.length();
                    boolean res = next.delete();
                    if (res) {
                        size -= fileSize;
                    }
                    Log.i(TAG,
                            String.format("[SIZE] Removing file %s (%.1f kb) - %s", next.getAbsolutePath(), fileSize / 1024.0, (res) ? "SUCCESS" : "FAILURE"));
                }
            }
            purgeCandidates.clear();
            Log.i(TAG, String.format("Remaining cache size: %.1f kb (%.0f %% used)", size / 1024.0, size * 100.0 / maxSize));
        }

    }

}
