package lokeshsaini.mypopularmovies;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import lokeshsaini.mypopularmovies.adapter.MyAdapter;
import lokeshsaini.mypopularmovies.data.MovieContract;
import lokeshsaini.mypopularmovies.model.Movie;

public class MainActivity extends AppCompatActivity {

    /* Add your own API key
        Get your own API key from https://api.themoviedb.org
        Create file "gradle.properties" in root folder if doesn't exist.
        Add MyPopularMoviesApiKey="MY_API_KEY" at the end of the file.
        Replace MY_API_KEY with your API key.
     */
    public final String apiKey = BuildConfig.POPULAR_MOVIES_API_KEY;
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    public String popular = "popular";
    public String rating = "top_rated";
    public String favorite = "favorite";
    public String movieSortMethod = popular;

    public ListAdapter adapter;
    public GridView mListView;

    public Movie[] movies;
    public boolean favoriteTrue = false;

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_IMAGE,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_DATE
    };

    public static final int COL_ID = 0;
    public static final int COL_MOVIE_ID = 1;
    public static final int COL_TITLE = 2;
    public static final int COL_IMAGE = 3;
    public static final int COL_OVERVIEW = 4;
    public static final int COL_RATING = 5;
    public static final int COL_DATE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            movieSortMethod = savedInstanceState.getString("sortMethod");
        }

        mListView = (GridView) findViewById(R.id.gridview);

        fetchAndShowMovies();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), MovieActivity.class);
                intent.putExtra("movie", movies[i].getJSONString());
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item1 = menu.findItem(R.id.sort_by_popularity);
        MenuItem item2 = menu.findItem(R.id.sort_by_rating);
        MenuItem item3 = menu.findItem(R.id.sort_by_favorite);
        if (movieSortMethod == popular) {
            item1.setChecked(true);
        } else if (movieSortMethod == rating) {
            item2.setChecked(true);
        } else if (movieSortMethod == favorite) {
            item3.setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.sort_by_popularity:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                movieSortMethod = popular;
                favoriteTrue = false;
                Toast.makeText(getApplicationContext(), "Sort by Popularity", Toast.LENGTH_SHORT).show();
                fetchAndShowMovies();
                return true;
            case R.id.sort_by_rating:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                movieSortMethod = rating;
                favoriteTrue = false;
                Toast.makeText(getApplicationContext(), "Sort by Rating", Toast.LENGTH_SHORT).show();
                fetchAndShowMovies();
                return true;
            case R.id.sort_by_favorite:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                movieSortMethod = favorite;
                favoriteTrue = true;
                Toast.makeText(getApplicationContext(), "Sort by Favorite", Toast.LENGTH_SHORT).show();
                fetchAndShowMovies();
                return true;
            case R.id.action_refresh:
                fetchAndShowMovies();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("sortMethod", movieSortMethod);
        super.onSaveInstanceState(outState);
    }

    // Checks Internet availablity and API Key and executes FetchMoviesTask
    public void fetchAndShowMovies() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            if (apiKey != null) {
                if (favoriteTrue) {
                    new  FetchFavoriteMoviesTask(getApplicationContext()).execute();
                } else {
                    new FetchMoviesTask().execute();
                }
            } else {
                Log.e(LOG_TAG, "Invalid API Key!");
            }
        } else {
            Toast.makeText(getApplicationContext(), "Connect to the Internet!", Toast.LENGTH_SHORT).show();
        }

    }

    public class FetchFavoriteMoviesTask extends AsyncTask<Void, Void, List<Movie>> {

        private final String LOG_TAG = FetchFavoriteMoviesTask.class.getSimpleName();

        private Context mContext;

        public FetchFavoriteMoviesTask(Context context) {
            mContext = context;
        }

        @Override
        protected List<Movie> doInBackground(Void... voids) {
            Cursor cursor = mContext.getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    null
            );
            return getFavoriteMoviesDataFromCursor(cursor);
        }

        @Override
        protected void onPostExecute(List<Movie> movies) {
            if (movies != null) {
                String[] s = new String[movies.size()];
                for (int i = 0; i< movies.size(); i++) {
                    s[i] = movies.get(i).getmImage();
                }
                adapter = new MyAdapter(getApplicationContext(), s);
                mListView.setAdapter(adapter);
            } else {
                Toast.makeText(getApplicationContext(), "Something went Wrong.\nTry Again.", Toast.LENGTH_SHORT).show();
            }
        }

        private List<Movie> getFavoriteMoviesDataFromCursor(Cursor cursor) {
            List<Movie> results = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Movie movie = new Movie(cursor);
                    results.add(movie);
                } while (cursor.moveToNext());
                cursor.close();
            }
            return results;
        }
    }

    // Fetch Movies data and display it.
    public class FetchMoviesTask extends AsyncTask<Void, Void, String[]> {
        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();
        ProgressDialog progDailog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progDailog = new ProgressDialog(MainActivity.this);
            progDailog.setMessage("Loading...");
            progDailog.setIndeterminate(false);
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDailog.setCancelable(true);
            progDailog.show();
        }

        @Override
        protected String[] doInBackground(Void... voids) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String moviesJsonStr = null;

            try {
                URL url = getApiUrl();

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
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                return getMoviesDataFromJson(moviesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);

            progDailog.dismiss();

            if (strings != null) {
                adapter = new MyAdapter(getApplicationContext(), strings);
                mListView.setAdapter(adapter);
            } else {
                Toast.makeText(getApplicationContext(), "Something went Wrong.\nTry Again.", Toast.LENGTH_SHORT).show();
            }

        }

        // Returns String with Movies data fetched from Json String
        private String[] getMoviesDataFromJson(String moviesJsonStr) throws JSONException {
            final String MOVIE_RESULTS = "results";
            final String MOVIE_ID = "id";
            final String MOVIE_NAME = "original_title";
            final String MOVIE_IMAGE = "poster_path";
            final String MOVIE_DESC = "overview";
            final String MOVIE_RATING = "vote_average";
            final String MOVIE_DATE = "release_date";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray resultsArray = moviesJson.getJSONArray(MOVIE_RESULTS);

            movies = new Movie[resultsArray.length()];
            String[] imageURL = new String[resultsArray.length()];

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject movieInfo = resultsArray.getJSONObject(i);

                movies[i] = new Movie(
                        movieInfo.getString(MOVIE_ID),
                        movieInfo.getString(MOVIE_NAME),
                        movieInfo.getString(MOVIE_IMAGE),
                        movieInfo.getString(MOVIE_DESC),
                        movieInfo.getDouble(MOVIE_RATING),
                        movieInfo.getString(MOVIE_DATE)
                );

                imageURL[i] = movieInfo.getString(MOVIE_IMAGE);
            }

            return imageURL;
        }

        // Returns URL to fetch data from
        private URL getApiUrl() throws MalformedURLException {
            final String BASE_URL = "https://api.themoviedb.org/3/movie/";
            final String API_KEY = "api_key";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendPath(movieSortMethod)
                    .appendQueryParameter(API_KEY, apiKey)
                    .build();

//            Log.e(LOG_TAG, "" + builtUri.toString());
            return new URL(builtUri.toString());
        }
    }

}
