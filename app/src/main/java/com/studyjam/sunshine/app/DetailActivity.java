package com.studyjam.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


public class DetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);


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
            Intent settingsIntent = new Intent(this, SettingsActivity.class);

            startActivity(settingsIntent);
        }else if(id == R.id.action_share){

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment {

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();
        private String forecast;

        public DetailFragment() {
        }

        @Override
        public void onCreate (Bundle savedInstanceState){
            super.onCreate(savedInstanceState);

            Intent intent = getActivity().getIntent();

            if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT)){
                forecast = intent.getStringExtra(Intent.EXTRA_TEXT);
            }

            this.setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            ((TextView)rootView.findViewById(R.id.detail_text)).setText(forecast);

            return rootView;
        }


        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);

            inflater.inflate(R.menu.detailfragment, menu);

            ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_share));
            shareActionProvider.setShareIntent(createShareIntent());

        }

        private Intent createShareIntent(){
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            shareIntent.putExtra(Intent.EXTRA_TEXT, forecast + "#SunshineApp");
            shareIntent.setType("text/plain");

            return shareIntent;
        }

    }
}
