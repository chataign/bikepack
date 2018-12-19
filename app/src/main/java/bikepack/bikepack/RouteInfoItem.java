package bikepack.bikepack;

import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.View;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import bikepack.bikepack.databinding.RouteInfoItemBinding;

public class RouteInfoItem
{
    private static DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");

    final String label;
    final String value;
    final int iconResId;

    public RouteInfoItem( String label, String value, int iconResId )
    {
        this.label = label;
        this.value = value;
        this.iconResId = iconResId;
    }

    public View getView( LayoutInflater inflater )
    {
        View view = inflater.inflate( R.layout.route_info_item, null );
        RouteInfoItemBinding binding = DataBindingUtil.bind(view);

        binding.label.setText(label);
        binding.value.setText(value);
        binding.icon.setImageResource(iconResId);

        return view;
    }

    public static List<RouteInfoItem> createRouteItems( Route route )
    {
        String totalDistance = StringFormatter.formatDistance(route.totalDistance,false);
        String totalAscent = StringFormatter.formatDistance(route.totalAscent,true);
        String totalDescent = StringFormatter.formatDistance(route.totalDescent,true);
        String highestElevation = StringFormatter.formatDistance(route.highestElevation,true);
        String lowestElevation = StringFormatter.formatDistance(route.lowestElevation,true);
        String dateCreated = DATE_FORMAT.format(route.dateCreated);

        List<RouteInfoItem> infoItems = new ArrayList<>();

        infoItems.add( new RouteInfoItem( "Total distance", totalDistance, R.drawable.ic_distance_black_24px ) );
        infoItems.add( new RouteInfoItem( "Date created", dateCreated, R.drawable.ic_date_black_24px) );
        infoItems.add( new RouteInfoItem( "Total ascent", totalAscent, R.drawable.ic_ascent_black_24px) );
        infoItems.add( new RouteInfoItem( "Total descent", totalDescent, R.drawable.ic_descent_black_24px) );
        infoItems.add( new RouteInfoItem( "Highest elevation", highestElevation, R.drawable.ic_elevation_top_black_24px ) );
        infoItems.add( new RouteInfoItem( "Lowest elevation", lowestElevation, R.drawable.ic_elevation_bottom_black_24px ));

        return infoItems;
    }
}
