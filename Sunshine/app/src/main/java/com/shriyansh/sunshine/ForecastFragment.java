package com.shriyansh.sunshine;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
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
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ForecastFragment extends Fragment {

    ArrayAdapter<String> forecastAdapter;

    public ForecastFragment() {


    }

    /**
     * Called to do initial creation of a fragment.  This is called after
     * {@link #onAttach(android.app.Activity)} and before
     * {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * <p/>
     * <p>Note that this can be called while the fragment's activity is
     * still in the process of being created.  As such, you can not rely
     * on things like the activity's content view hierarchy being initialized
     * at this point.  If you want to do work once the activity itself is
     * created, see {@link #onActivityCreated(android.os.Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }


    /**
     * Called when the Fragment is visible to the user.  This is generally
     * tied to {@link android.app.Activity#onStart() Activity.onStart} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        /*String forecastList[]={
                "Today - Sunny  -  88 / 63",
                "Monday 16 - Clear  -  45 / 48",
                "Tuesday 17 - Rain  -  16 / 49",
                "Thursday 18- Sunny  -  88 / 63",
                "Friday 19 - Clear  -  45 / 48",
                "Saturday 20 - Rain  -  16 / 49",
                "Sunday 21 - Sunny  -  88 / 63",
                "Monday 22 - Clear  -  45 / 48",
                "Tuesday 23 - Rain  -  16 / 49",
                "Thursday 24 - Sunny  -  88 / 63",
                "Friday 25 - Clear  -  45 / 48",
                "Saturday 26 - Rain  -  16 / 49"
        };*/

        //List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastList));

        forecastAdapter= new ArrayAdapter<String>(getActivity(),
                                                   R.layout.list_item_forecast,
                                                   R.id.list_item_forecast_textview,
                                                   new ArrayList<String>());

        ListView listView=(ListView)rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(forecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast=forecastAdapter.getItem(position);
                Intent intent=new Intent(getActivity(),DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT,forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link #setHasOptionsMenu}.  See
     * {@link android.app.Activity#onCreateOptionsMenu(android.view.Menu) Activity.onCreateOptionsMenu}
     * for more information.
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater
     * @see #setHasOptionsMenu
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment,menu);
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     * <p/>
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id=item.getItemId();

        if(id==R.id.action_refresh){
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateWeather(){
        FetchWeatherTask weatherTask =new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.string_location_key),getString(R.string.location_default_value));
        weatherTask.execute(location);
    }




    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>{

       String LOG_TAG=ForecastFragment.class.getSimpleName();


        /* The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        */
        private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = prefs.getString(getString(R.string.pref_units_key),getString(R.string.pref_default_units_metric));

            if(unitType.equals(getString(R.string.pref_default_units_imperial))){
                high=(high*1.8)+ 32;
                low= (low*1.8) + 32;

            }else if(!unitType.equals(getString(R.string.pref_default_units_metric))){
                Log.d(LOG_TAG,"Unit Type not found"+unitType);
            }

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);


            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy: constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();
            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
        // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

        // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

        // The date/time is returned as a long. We need to convert that
        // into something human-readable, since most people won't read "1400356800" as
        // "this saturday".
                long dateTime;
        // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

        // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

        // Temperatures are in a child object called "temp". Try not to name variables
        // "temp" when working with temperature. It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            return resultStrs;

        }

        /**
            * Override this method to perform a computation on a background thread. The
            * specified parameters are the parameters passed to {@link #execute}
            * by the caller of this task.
            * <p/>
            * This method can call {@link #publishProgress} to publish updates
            * on the UI thread.
            *
            * @param params The parameters of the task.
            * @return A result, defined by the subclass of this task.
            * @see #onPreExecute()
            * @see #onPostExecute
            * @see #publishProgress
            */
       @Override
       protected String[] doInBackground(String... params) {

           // These two need to be declared outside the try/catch
           // so that they can be closed in the finally block.

           if(params.length==0){
               return null;
           }

           HttpURLConnection urlConnection = null;
           BufferedReader reader = null;

           // Will contain the raw JSON response as a string.
           String forecastJsonStr = null;
           String format= "json";
           String units="metric";
           int numDays=7;

           try {
               // Construct the URL for the OpenWeatherMap query
               // Possible parameters are avaiable at OWM's forecast API page, at
               // http://openweathermap.org/API#forecast
               final String FORECAST_BASE_URL="http://api.openweathermap.org/data/2.5/forecast/daily?";
               final String QUERY_PARAM="q";
               final String FORMAT_PARAM="mode";
               final String UNITS_PARAM="units";
               final String DAYS_PARAM="cnt";

               Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                       .appendQueryParameter(QUERY_PARAM, params[0])
                       .appendQueryParameter(FORMAT_PARAM,format)
                       .appendQueryParameter(UNITS_PARAM,units)
                       .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays))
                       .build();

               URL url = new URL(builtUri.toString());

               Log.d(LOG_TAG,"Built Uri "+builtUri.toString());
               // Create the request to OpenWeatherMap, and open the connection
               urlConnection = (HttpURLConnection) url.openConnection();
               urlConnection.setRequestMethod("GET");
               urlConnection.connect();

               // Read the input stream into a String
               InputStream inputStream = urlConnection.getInputStream();
               StringBuffer buffer = new StringBuffer();
               if (inputStream == null) {
                   // Nothing to do.
                   return null;
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
                   // Stream was empty. No point in parsing.
                   return null;
               }
               forecastJsonStr = buffer.toString();

               //Logging the response
               Log.d(LOG_TAG,forecastJsonStr);

           } catch (IOException e) {
               Log.e(LOG_TAG, "Error ", e);
               // If the code didn't successfully get the weather data, there's no point in attemping
               // to parse it.
               return null;
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
               return getWeatherDataFromJson(forecastJsonStr,numDays);
           } catch (JSONException e) {
               Log.e(LOG_TAG,e.getMessage(),e);
               e.printStackTrace();
           }


           return null;
       }

        /**
         * <p>Runs on the UI thread after {@link #doInBackground}. The
         * specified result is the value returned by {@link #doInBackground}.</p>
         * <p/>
         * <p>This method won't be invoked if the task was cancelled.</p>
         *

         */
        @Override
        protected void onPostExecute(String[] result) {
            if(result!=null){
                //clearing the forecast adapter
                forecastAdapter.clear();
                //adding each string in result array to forecastAdapter
                for(String dayForecastStr : result){
                    forecastAdapter.add(dayForecastStr);
                }

            }
        }
    }

}
