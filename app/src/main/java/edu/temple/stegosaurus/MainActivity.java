package edu.temple.stegosaurus;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    ViewPager viewPager;
    PagerAdapter mPagerAdapter;

    private static final int NUM_PAGES = 3;
    private static final int GET_PERMISSION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get permissions
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                GET_PERMISSION);

        // Instantiate a ViewPager and a PagerAdapter.
        viewPager = findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(mPagerAdapter);
        viewPager.setCurrentItem(1);    // how to set first page loaded
    }

    /**
     * A simple pager adapter containing 3 ScreenSlidePageFragment objects
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment retval;

            switch (position) {
                case 1: retval = new HomeFragment(); break;
                case 2: retval = new ExtractFragment(); break;
                default: retval = new InsertFragment(); break;
            }
            return retval;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    /**
     * The Menu linking to information about the app
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.info_link:
                Intent startInfoIntent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(startInfoIntent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}