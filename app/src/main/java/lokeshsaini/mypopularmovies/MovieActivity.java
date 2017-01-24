package lokeshsaini.mypopularmovies;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.linearlistview.LinearListView;
import com.squareup.picasso.Picasso;

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

import lokeshsaini.mypopularmovies.adapter.ReviewAdapter;
import lokeshsaini.mypopularmovies.adapter.TrailerAdapter;
import lokeshsaini.mypopularmovies.data.MovieContract;
import lokeshsaini.mypopularmovies.model.Review;
import lokeshsaini.mypopularmovies.model.Trailer;

import static lokeshsaini.mypopularmovies.Helper.isFavorite;

public class MovieActivity extends AppCompatActivity {

    public final String apiKey = BuildConfig.POPULAR_MOVIES_API_KEY;
    private final String LOG_TAG = MovieActivity.class.getSimpleName();

    final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185";

    private View mTrailersViewGroup;
    private View mReviewsViewGroup;

    private TrailerAdapter mTrailerAdapter;
    private ReviewAdapter mReviewAdapter;

    private Toast mToast;

    private String movieID;
    private String movieName;
    private String movieDesc;
    private String movieRate;
    private String movieDate;
    private String movieImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie);

        ImageView movieImageView = (ImageView) findViewById(R.id.movie_image);
        TextView movieDateView = (TextView) findViewById(R.id.movie_date);
        TextView movieRatingView = (TextView) findViewById(R.id.movie_rating);
        TextView movieDescView = (TextView) findViewById(R.id.movie_desc);

        LinearListView mTrailersView = (LinearListView) findViewById(R.id.movie_trailers);
        LinearListView mReviewsView = (LinearListView) findViewById(R.id.movie_reviews);

        mTrailersViewGroup = findViewById(R.id.movie_trailers_viewgroup);
        mReviewsViewGroup = findViewById(R.id.movie_reviews_viewgroup);

        mTrailerAdapter = new TrailerAdapter(getApplicationContext(), new ArrayList<Trailer>());
        mTrailersView.setAdapter(mTrailerAdapter);

        mTrailersView.setOnItemClickListener(new LinearListView.OnItemClickListener() {
            @Override
            public void onItemClick(LinearListView linearListView, View view,
                                    int position, long id) {
                Trailer trailer = mTrailerAdapter.getItem(position);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://www.youtube.com/watch?v=" + trailer.getKey()));
                startActivity(intent);
            }
        });

        mReviewAdapter = new ReviewAdapter(getApplicationContext(), new ArrayList<Review>());
        mReviewsView.setAdapter(mReviewAdapter);

        String movie = getIntent().getStringExtra("movie");
        try {
            JSONObject obj = new JSONObject(movie);

            movieID = "" + obj.optString("id");
            movieName = "" + obj.optString("name");
            if (obj.optString("desc") != null) {
                movieDesc = "" + obj.optString("desc");
            }
            if (obj.optString("rating") != null) {
                movieRate = "" + obj.optString("rating") + "/10";
            }
            if (obj.optString("date") != null) {
                movieDate = "" + obj.optString("date");
            }
            movieImage = "" + obj.optString("image");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        fetchAndShowMovies();

        setTitle(movieName);
        movieDescView.setText(movieDesc);
        movieRatingView.setText(movieRate);
        movieDateView.setText(movieDate);
        String finalImageURL = IMAGE_BASE_URL + movieImage;
        Picasso.with(getApplicationContext())
                .load(finalImageURL)
                .error(R.drawable.no_image_available)
                .placeholder(R.drawable.loading)
                .into(movieImageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_movie, menu);
        MenuItem item1 = menu.findItem(R.id.action_favorite);
        int i = Helper.isFavorite(getApplicationContext(), Integer.parseInt(movieID));
        item1.setIcon(i == 1 ?
                R.drawable.ic_favorite_white_24dp :
                R.drawable.ic_favorite_border_white_24dp);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_favorite:
                new AsyncTask<Void, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(Void... params) {
                        return isFavorite(getApplicationContext(), Integer.parseInt(movieID));
                    }

                    @Override
                    protected void onPostExecute(Integer isFavorite) {
                        // if it is in favorites
                        if (isFavorite == 1) {
                            // delete from favorites
                            new AsyncTask<Void, Void, Integer>() {
                                @Override
                                protected Integer doInBackground(Void... params) {
                                    return getApplicationContext().getContentResolver().delete(
                                            MovieContract.MovieEntry.CONTENT_URI,
                                            MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                                            new String[]{Integer.toString(Integer.parseInt(movieID))}
                                    );
                                }

                                @Override
                                protected void onPostExecute(Integer rowsDeleted) {
                                    item.setIcon(R.drawable.ic_favorite_border_white_24dp);
                                    if (mToast != null) {
                                        mToast.cancel();
                                    }
                                    mToast = Toast.makeText(getApplicationContext(), "Removed from Favorites", Toast.LENGTH_SHORT);
                                    mToast.show();
                                }
                            }.execute();
                        }
                        // if it is not in favorites
                        else {
                            // add to favorites
                            new AsyncTask<Void, Void, Uri>() {
                                @Override
                                protected Uri doInBackground(Void... params) {
                                    ContentValues values = new ContentValues();

                                    values.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movieID);
                                    values.put(MovieContract.MovieEntry.COLUMN_TITLE, movieName);
                                    values.put(MovieContract.MovieEntry.COLUMN_IMAGE, movieImage);
                                    values.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, movieDesc);
                                    values.put(MovieContract.MovieEntry.COLUMN_RATING, movieRate);
                                    values.put(MovieContract.MovieEntry.COLUMN_DATE, movieDate);

                                    return getApplicationContext().getContentResolver().insert(MovieContract.MovieEntry.CONTENT_URI,
                                            values);
                                }

                                @Override
                                protected void onPostExecute(Uri returnUri) {
                                    item.setIcon(R.drawable.ic_favorite_white_24dp);
                                    if (mToast != null) {
                                        mToast.cancel();
                                    }
                                    mToast = Toast.makeText(getApplicationContext(), "Added to Favorites", Toast.LENGTH_SHORT);
                                    mToast.show();
                                }
                            }.execute();
                        }
                    }
                }.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Checks Internet availablity and API Key and executes FetchTrailersTask and FetchReviewsTask
    public void fetchAndShowMovies() {
        ConnectivityManager cm =
                (ConnectivityManager) getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
            if (apiKey != null) {
                FetchTrailersTask fetchTrailersTask = new FetchTrailersTask();
                fetchTrailersTask.execute();
                FetchReviewsTask fetchReviewsTask = new FetchReviewsTask();
                fetchReviewsTask.execute();
            } else {
                Log.e(LOG_TAG, "Invalid API Key!");
            }
        } else {
            Toast.makeText(getApplicationContext(), "Connect to the Internet for trailers and reviews!", Toast.LENGTH_SHORT).show();
        }

    }


    // Fetch Movie trailers and display it.
    public class FetchTrailersTask extends AsyncTask<Void, Void, Trailer[] > {
        private final String LOG_TAG = FetchTrailersTask.class.getSimpleName();
        ProgressDialog progDailog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progDailog = new ProgressDialog(MovieActivity.this);
            progDailog.setMessage("Loading...");
            progDailog.setIndeterminate(false);
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDailog.setCancelable(true);
            progDailog.show();
        }

        @Override
        protected Trailer[]  doInBackground(Void... voids) {

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
        protected void onPostExecute(Trailer[] trailers) {
            super.onPostExecute(trailers);

            progDailog.dismiss();

            if (trailers != null) {
                if (trailers.length > 0) {
                    mTrailersViewGroup.setVisibility(View.VISIBLE);
                    if (mTrailerAdapter != null) {
                        mTrailerAdapter.clear();
                        for (Trailer trailer : trailers) {
                            mTrailerAdapter.add(trailer);
                        }
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "Something went Wrong.\nTry Again.", Toast.LENGTH_SHORT).show();
            }

        }

        // Returns String with Movies data fetched from Json String
        private Trailer[] getMoviesDataFromJson(String moviesJsonStr) throws JSONException {
            final String TRAILER_RESULTS = "results";
            final String TRAILER_ID = "id";
            final String TRAILER_KEY = "key";
            final String TRAILER_NAME = "name";
            final String TRAILER_SITE = "site";
            final String TRAILER_TYPE = "type";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray resultsArray = moviesJson.getJSONArray(TRAILER_RESULTS);

            Trailer[] trailers = new Trailer[resultsArray.length()];

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject movieInfo = resultsArray.getJSONObject(i);

                trailers[i] = new Trailer(
                        movieInfo.getString(TRAILER_ID),
                        movieInfo.getString(TRAILER_KEY),
                        movieInfo.getString(TRAILER_NAME),
                        movieInfo.getString(TRAILER_SITE),
                        movieInfo.getString(TRAILER_TYPE)
                );
            }

            return trailers;
        }

        // Returns URL to fetch data from
        private URL getApiUrl() throws MalformedURLException {
            final String BASE_URL = "https://api.themoviedb.org/3/movie/";
            final String API_KEY = "api_key";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendPath(movieID)
                    .appendPath("videos")
                    .appendQueryParameter(API_KEY, apiKey)
                    .build();

//            Log.e(LOG_TAG, "" + builtUri.toString());
            return new URL(builtUri.toString());
        }
    }


    // Fetch Movie reviews and display it.
    public class FetchReviewsTask extends AsyncTask<Void, Void, Review[] > {
        private final String LOG_TAG = FetchReviewsTask.class.getSimpleName();
        ProgressDialog progDailog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progDailog = new ProgressDialog(MovieActivity.this);
            progDailog.setMessage("Loading...");
            progDailog.setIndeterminate(false);
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDailog.setCancelable(true);
            progDailog.show();
        }

        @Override
        protected Review[]  doInBackground(Void... voids) {

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
        protected void onPostExecute(Review[] reviews) {
            super.onPostExecute(reviews);

            progDailog.dismiss();

            if (reviews != null) {
                if (reviews.length > 0) {
                    mReviewsViewGroup.setVisibility(View.VISIBLE);
                    if (mReviewAdapter != null) {
                        mReviewAdapter.clear();
                        for (Review review : reviews) {
                            mReviewAdapter.add(review);
                        }
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "Something went Wrong.\nTry Again.", Toast.LENGTH_SHORT).show();
            }

        }

        // Returns String with Movies data fetched from Json String
        private Review[] getMoviesDataFromJson(String moviesJsonStr) throws JSONException {
            final String TRAILER_RESULTS = "results";
            final String TRAILER_ID = "id";
            final String TRAILER_AUTHOR = "author";
            final String TRAILER_CONTENT = "content";

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            JSONArray resultsArray = moviesJson.getJSONArray(TRAILER_RESULTS);

            Review[] reviews = new Review[resultsArray.length()];

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject movieInfo = resultsArray.getJSONObject(i);

                reviews[i] = new Review(
                        movieInfo.getString(TRAILER_ID),
                        movieInfo.getString(TRAILER_AUTHOR),
                        movieInfo.getString(TRAILER_CONTENT)
                );
            }

            return reviews;
        }

        // Returns URL to fetch data from
        private URL getApiUrl() throws MalformedURLException {
            final String BASE_URL = "https://api.themoviedb.org/3/movie/";
            final String API_KEY = "api_key";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendPath(movieID)
                    .appendPath("reviews")
                    .appendQueryParameter(API_KEY, apiKey)
                    .build();

//            Log.e(LOG_TAG, "" + builtUri.toString());
            return new URL(builtUri.toString());
        }
    }
}
