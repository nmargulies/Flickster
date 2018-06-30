package me.nmargulies.flickster;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import me.nmargulies.flickster.models.Config;
import me.nmargulies.flickster.models.Movie;

public class MainActivity extends AppCompatActivity {

    //constants
    //the base URL for the API
    public final static String API_BASE_URL = "http://api.themoviedb.org/3";

    // the parameter name for the API key
    public final static String API_KEY_PARAM = "api_key";

    // tag for logging from this activity
    public final static String TAG = "MovieFinder";

    // instance fields
    AsyncHttpClient client;
    // the list of currently playing movies
    ArrayList<Movie> movies;
    // the recycler view
    @BindView(R.id.rvMovies) RecyclerView rvMovies;
    // the adapter wired to the recycler view
    MovieAdapter adapter;
    // image config
    Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // initialize the client
        client = new AsyncHttpClient();

        // initialize the list of movies
        movies = new ArrayList<>();

        // initialize the adapter-- movies array cannot be reinitialized after this
        adapter = new MovieAdapter(movies);

        // recieve the recycler view and connect a layout manager and the adapter
        rvMovies = (RecyclerView) rvMovies;
        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);

        //get the configuration on app creation
        getConfiguration();
    }

    // get the list of currently playing movies from the API
    private void getNowPlaying() {
        // create the url
        String url = API_BASE_URL + "/movie/now_playing";

        // set the request parameters
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key));

        //execute a GET request expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler(){

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // load the results into movie list
                try {
                    JSONArray results = response.getJSONArray("results");

                    //iterate through result set and create Movie objections
                    for (int i = 0; i < results.length(); i++) {
                        Movie movie = new Movie(results.getJSONObject(i));
                        movies.add(movie);

                        // notify adapter that a row was added
                        adapter.notifyItemInserted(movies.size() - 1);
                    }
                    Log.i(TAG, String.format("Loaded %d movies", results.length()));

                } catch (JSONException e) {
                    logError("Failed to parse now playing movies", e, true);
                }

            }
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from now playing endpoint", throwable, true);
            }
        });
    }

    // get the configuration from the API
    private void getConfiguration(){
        // created the url
        String url = API_BASE_URL + "/configuration";

        // set the request parameters
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, getString(R.string.api_key));

        //execute a GET request, expecting a JSON object response
        client.get(url, params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    config = new Config(response);
                    Log.i(TAG,
                            String.format("Loaded configuration with imageBaseUrl %s and posterSize %s",
                                    config.getImageBaseUrl(),
                                    config.getPosterSize()));

                    // pass config to adapter
                    adapter.setConfig(config);

                    // get the now playing movie list
                    getNowPlaying();

                } catch (JSONException e) {
                    logError("Failed parsing configuration", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
            }
        });
    }

    // handle errors, log and alert user
    private void logError(String message, Throwable error, boolean alertUser) {
        //always log the error
        Log.e(TAG, message, error);

        // alert the user to avoid silent errors
        if (alertUser) {
            // show a long toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
