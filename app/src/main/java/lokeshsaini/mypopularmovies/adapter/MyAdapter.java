package lokeshsaini.mypopularmovies.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import lokeshsaini.mypopularmovies.R;

public class MyAdapter extends ArrayAdapter<String> {

    private final Context mContext;

    public MyAdapter(Context context, String[] names) {
        super(context, R.layout.movies_grid_item_layout, names);
        this.mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater myInflater = LayoutInflater.from(getContext());
            view = myInflater.inflate(R.layout.movies_grid_item_layout, parent, false);
        }

        ImageView imageView = (ImageView) view.findViewById(R.id.movie_image);
        String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185";
        String imageURL = IMAGE_BASE_URL + getItem(position);

        Picasso.with(mContext)
                .load(imageURL)
                .error(R.drawable.no_image_available)
                .placeholder(R.drawable.loading)
                .into(imageView);

        return view;
    }
}
