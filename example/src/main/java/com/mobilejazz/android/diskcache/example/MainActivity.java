package com.mobilejazz.android.diskcache.example;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.mobilejazz.android.diskcache.library.CacheProvider;

public class MainActivity extends Activity implements DatePickerDialog.OnDateSetListener {

    public static final String TAG = "diskcache-example";

    Random random;
    TextView text;
    String authority;

    List<File> cacheDirectories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        random = new Random();
        text = (TextView) findViewById(R.id.text);
        authority = getString(R.string.authority_cache);

        try {
            cacheDirectories = CacheProvider.getCacheDirectories(this, authority);
            StringBuilder b = new StringBuilder();
            for (File f : cacheDirectories) {
                b.append(f.getAbsolutePath().substring(getApplicationInfo().dataDir.length()));
                b.append('\n');
            }
            text.setText(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addFile(View v) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog d = new DatePickerDialog(this, this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    public void clear(View v) {
        try {
            CacheProvider.clear(this, authority);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        try {
            File directory = cacheDirectories.get(random.nextInt(cacheDirectories.size()));
            directory.mkdirs();
            File newFile = new File(directory, String.format("%05X", System.currentTimeMillis()));
            String srcFile = String.format("%d.pdf", random.nextInt(3) + 1);
            Log.i(TAG, String.format("Copying data from %s to %s", srcFile, newFile.getName()));
            FileUtils.copyInputStreamToFile(getAssets().open(srcFile), newFile);
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, monthOfYear);
            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            if (!newFile.setLastModified(c.getTimeInMillis())) {
                Log.e(TAG, "Could not set last modified on " + newFile.getAbsolutePath());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
