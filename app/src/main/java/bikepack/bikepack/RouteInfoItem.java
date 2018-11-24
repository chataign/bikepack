package bikepack.bikepack;

import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.View;

import bikepack.bikepack.databinding.RouteInfoItemBinding;

class RouteInfoItem
{
    final String label;
    final String value;
    final int iconResId;

    RouteInfoItem( String label, String value, int iconResId )
    {
        this.label = label;
        this.value = value;
        this.iconResId = iconResId;
    }

    View getView( LayoutInflater inflater )
    {
        View view = inflater.inflate( R.layout.route_info_item, null );
        RouteInfoItemBinding binding = DataBindingUtil.bind(view);

        binding.label.setText(label);
        binding.value.setText(value);
        binding.icon.setImageResource(iconResId);

        return view;
    }
}
