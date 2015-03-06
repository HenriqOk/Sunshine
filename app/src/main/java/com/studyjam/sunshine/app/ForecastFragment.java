package com.studyjam.sunshine.app;



import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static android.view.View.OnClickListener;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> forecastAdapter;
    FetchWeatherTask weatherTask = new FetchWeatherTask();

    public ForecastFragment() {
    }

//    public boolean onCreateMenuOptions(Menu menu){
//
//
//        return true;
//    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        this.setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        forecastAdapter = new ArrayAdapter<String>(
                this.getActivity(),
                R.layout.list_item_forecast, //arquivo que contem o componente
                R.id.list_item_forecast_textview,//componente
                new ArrayList<String>()
        );

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = (String) parent.getItemAtPosition(position);

                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra(Intent.EXTRA_TEXT, forecast);

                startActivity(detailIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
//        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather(){
        weatherTask = new FetchWeatherTask();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String postalCode = sharedPreferences.getString(
                getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        weatherTask.execute(postalCode);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>{

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private final String REQUEST_SCHEME = "http";
        private final String REQUEST_AUTHORITY = "api.openweathermap.org";
        private final String REQUEST_THING = "data";
        private final String REQUEST_API_VERSION = "2.5";
        private final String REQUEST_TYPE = "forecast";
        private final String REQUEST_FREQUENCY = "daily";

        private final String REQUEST_KEY_POSTALCODE = "q";
        private final String REQUEST_KEY_MODE = "mode";
        private final String REQUEST_KEY_UNITS = "units";
        private final String REQUEST_KEY_COUNT = "cnt";

        private final String REQUEST_PARAM_MODE = "json";
        private final String REQUEST_PARAM_UNITS = "metric";
        private final String REQUEST_PARAM_COUNT = "7";

        private final String OWM_LIST = "list";
        private final String OWM_WEATHER = "weather";
        private final String OWM_TEMPERATURE = "temp";
        private final String OWM_MAX = "max";
        private final String OWM_MIN = "min";
        private final String OWM_DATETIME = "dt";
        private final String OWM_DESCRIPTION = "main";


        /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String units = sharedPreferences.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_default));

            if(units.equals(getResources().getStringArray(R.array.pref_units_values)[1])){
                high = (high*9/5)+32;
                low = (low*9/5)+32;
            }

            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        private double convertFromCelsiusToFahrenheit(double celsius){
            return (celsius*9/5)+32;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.


            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }

        private String buildUrl(String postalCode){
            Uri.Builder builder = new Uri.Builder();

            builder.scheme(REQUEST_SCHEME).
                    authority(REQUEST_AUTHORITY).
                    appendPath(REQUEST_THING).
                    appendPath(REQUEST_API_VERSION).
                    appendPath(REQUEST_TYPE).
                    appendPath(REQUEST_FREQUENCY).
                    appendQueryParameter(REQUEST_KEY_POSTALCODE, postalCode).
                    appendQueryParameter(REQUEST_KEY_MODE, REQUEST_PARAM_MODE).
                    appendQueryParameter(REQUEST_KEY_UNITS, REQUEST_PARAM_UNITS).
                    appendQueryParameter(REQUEST_KEY_COUNT, REQUEST_PARAM_COUNT);

            return builder.build().toString();


        }

        @Override
        protected String[] doInBackground(String... params) {


//            These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                String postalCode = params[0];

                URL url = new URL(this.buildUrl(postalCode));

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    forecastJsonStr = null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return this.getWeatherDataFromJson(forecastJsonStr, Integer.parseInt(REQUEST_PARAM_COUNT));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON Error ", e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] forecasts) {
            if(forecasts != null) {
                super.onPostExecute(forecasts);

                forecastAdapter.clear();

                for (String forecast : forecasts) {
                    forecastAdapter.add(forecast);
                }
            }
        }
    }
}