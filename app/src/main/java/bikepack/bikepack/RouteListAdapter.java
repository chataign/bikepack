package bikepack.bikepack;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import bikepack.bikepack.databinding.RouteListItemBinding;

public class RouteListAdapter extends ArrayAdapter<Route>
{
    public RouteListAdapter(@NonNull Context context, @NonNull List<Route> routes )
    {
        super( context, 0 , routes );
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent)
    {
        if( view == null )
            view = LayoutInflater.from(getContext()).inflate(
                    R.layout.route_list_item, parent,false);

        Route route = getItem(position);

        if ( route != null )
        {
            RouteListItemBinding listItem = DataBindingUtil.bind(view);
            listItem.routeName.setText( route.routeName );
            listItem.authorName.setText( route.authorName );
            listItem.routeInfo.setText( StringFormatter.formatDistance(route.totalDistance,false) );
        }

        return view;
    }
}
