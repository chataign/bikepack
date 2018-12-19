package bikepack.bikepack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import bikepack.bikepack.databinding.WaypointEditBinding;

public class AddWaypointActionMode implements ActionMode.Callback
{
    private final AppCompatActivity activity;
    private final GoogleMap googleMap;
    private final WaypointListViewModel waypoints;

    private int originalStatusBarColor;
    private final int actionModeStatusBarColor;
    private CharSequence originalTitle;
    private Marker waypointMarker;

    public AddWaypointActionMode(final AppCompatActivity activity, final GoogleMap googleMap, WaypointListViewModel waypoints )
    {
        this.activity = activity;
        this.googleMap = googleMap;
        this.waypoints = waypoints;

        this.actionModeStatusBarColor = activity.getResources().getColor(R.color.black);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            // set the status bar color to black to match the action mode bar
            originalStatusBarColor = activity.getWindow().getStatusBarColor();
            activity.getWindow().setStatusBarColor(actionModeStatusBarColor);

            originalTitle = mode.getTitle();
            mode.setTitle(R.string.add_waypoint_mode_title);
        }

        MenuInflater menuInflater = activity.getMenuInflater();
        menuInflater.inflate(R.menu.add_waypoint_menu, menu);

        waypointMarker = googleMap.addMarker( new MarkerOptions()
            .position( new LatLng(0,0) )
            .visible(true)
            .icon( BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) )
            .anchor(0.5f,1.0f) // middle-bottom of pin marker
            .draggable(false)
            .position( googleMap.getCameraPosition().target ) );

        googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() { // keep the marker at the center of the googleMap
                waypointMarker.setPosition( googleMap.getCameraPosition().target );
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu)
    {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_save:
                View waypointEditView = activity.getLayoutInflater().inflate(R.layout.waypoint_edit, null );
                final WaypointEditBinding waypointEditor = DataBindingUtil.bind(waypointEditView);

                new AlertDialog.Builder(activity )
                        .setTitle(R.string.waypoint_create_dialog_title)
                        .setView(waypointEditView)
                        .setNegativeButton( R.string.dialog_cancel, null )
                        .setPositiveButton( R.string.dialog_ok, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(final DialogInterface dialog, int id)
                            {
                                String name  = waypointEditor.name.getText().toString();
                                String description = waypointEditor.description.getText().toString();

                                waypoints.createWaypoint( new NamedGlobalPosition(
                                        new GlobalPosition(
                                                waypointMarker.getPosition().latitude,
                                                waypointMarker.getPosition().longitude, 0 ),
                                        name, description ) );
                            }
                        }).create()
                        .show();

                mode.finish();
                return true;
        }
        return false ;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode)
    {
        // set status bar back to its original color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            activity.getWindow().setStatusBarColor(originalStatusBarColor);

        mode.setTitle(originalTitle);

        // clear the googleMap
        waypointMarker.remove();
        googleMap.setOnCameraMoveListener(null);
    }
}