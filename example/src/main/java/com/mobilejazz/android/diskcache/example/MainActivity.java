package com.mobilejazz.android.diskcache.example;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;

import com.mobilejazz.android.diskcache.library.CacheProvider;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class MainActivity extends Activity implements DatePickerDialog.OnDateSetListener {

    Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        random = new Random();
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

        //noinspection SimplifiableIfStatement
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
            CacheProvider.clear(this, getString(R.string.authority_cache));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        try {
            File newFile = new File(getCacheDir(), String.format("test/%05X", System.currentTimeMillis()));
            if (newFile.mkdirs() && newFile.createNewFile()) {
                FileUtils.copyInputStreamToFile(getAssets().open(String.format("%d.pdf", random.nextInt(3) + 1)), newFile);
                Calendar c = Calendar.getInstance();
                c.set(Calendar.YEAR, year);
                c.set(Calendar.MONTH, monthOfYear);
                c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                if (!newFile.setLastModified(c.getTimeInMillis())) {
                    Log.e("diskcache-example", "Could not set last modified on " + newFile.getAbsolutePath());
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
