package lokeshsaini.mypopularmovies;

import android.content.Context;
import android.database.Cursor;

import lokeshsaini.mypopularmovies.data.MovieContract;

public class Helper {

    public static int isFavorite(Context context, int id) {
        Cursor cursor = context.getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,   // projection
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?", // selection
                new String[] { Integer.toString(id) },   // selectionArgs
                null    // sort order
        );
        int i = cursor.getCount();
        cursor.close();
        return i;
    }
}
