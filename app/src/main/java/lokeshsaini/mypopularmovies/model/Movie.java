package lokeshsaini.mypopularmovies.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import lokeshsaini.mypopularmovies.MainActivity;

public class Movie implements Parcelable {

    public static final Creator<Movie> CREATOR = new Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel in) {
            return new Movie(in);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };
    private String mID;
    private String mName;
    private String mImage;
    private String mDesc;
    private Double mRating;
    private String mDate;

    // Empty Constructor
    public Movie() {
    }

    // Parcelable code generated using this plugin https://plugins.jetbrains.com/plugin/7332?pr=idea

    public Movie(String mID, String mName, String mImage, String mDesc, Double mRating, String mDate) {
        this.mID = mID;
        this.mName = mName;
        this.mImage = mImage;
        this.mDesc = mDesc;
        this.mRating = mRating;
        this.mDate = mDate;
    }

    public Movie(Cursor cursor) {
        this.mID = cursor.getString(MainActivity.COL_MOVIE_ID);
        this.mName = cursor.getString(MainActivity.COL_TITLE);
        this.mImage = cursor.getString(MainActivity.COL_IMAGE);
        this.mDesc = cursor.getString(MainActivity.COL_OVERVIEW);
        this.mRating = cursor.getDouble(MainActivity.COL_RATING);
        this.mDate = cursor.getString(MainActivity.COL_DATE);
    }

    public Movie(Parcel in) {
        mID = in.readString();
        mName = in.readString();
        mImage = in.readString();
        mDesc = in.readString();
        mDate = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mID);
        parcel.writeString(mName);
        parcel.writeString(mImage);
        parcel.writeString(mDesc);
        parcel.writeValue(mRating);
        parcel.writeString(mDate);
    }

    public String getJSONString() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", mID);
            obj.put("name", mName);
            obj.put("image", mImage);
            obj.put("desc", mDesc);
            obj.put("rating", mRating);
            obj.put("date", mDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj.toString();
    }

    public String getmID() {
        return mID;
    }

    public String getmName() {
        return mName;
    }

    public String getmImage() {
        return mImage;
    }

    public String getmDesc() {
        return mDesc;
    }

    public Double getmRating() {
        return mRating;
    }

    public String getmDate() {
        return mDate;
    }
}
