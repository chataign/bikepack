package bikepack.bikepack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import bikepack.bikepack.databinding.RouteEditBinding;

class PolylineSelection implements ActionMode.Callback, ElevationView.OnSelectionListener
{
    private final AppCompatActivity activity;
    private final GoogleMap googleMap;

    private Polyline selectionPolyline;
    private List<Trackpoint> selectionTrackpoints=null;
    private int originalStatusBarColor;

    PolylineSelection( AppCompatActivity activity, GoogleMap googleMap )
    {
        this.activity = activity;
        this.googleMap = googleMap;

        this.selectionPolyline = googleMap.addPolyline( new PolylineOptions()
                .visible(false)
                .color(Color.BLUE) );
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu)
    {
        MenuInflater inflater = activity.getMenuInflater();
        inflater.inflate(R.menu.route_selection_menu, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            originalStatusBarColor = activity.getWindow().getStatusBarColor();
            int actionModeStatusBarColor = activity.getResources().getColor(R.color.black);
            activity.getWindow().setStatusBarColor(actionModeStatusBarColor);
        }

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
                //if ( selectionTrackpoints != null ) saveRouteSelection(selectionTrackpoints);
                mode.finish();
                return true;
        }
        return false ;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            activity.getWindow().setStatusBarColor(originalStatusBarColor);

        selectionPolyline.remove();
        //binding.elevationView.exitSelection();
    }

    public void onSelectionCreated( final List<Trackpoint> selection, List<LatLng> points )
    {
        this.selectionTrackpoints = selection;
        selectionPolyline.setPoints( points );
        selectionPolyline.setVisible(true);

        //float selectionDistance = (float) SphericalUtil.computeLength(points);
        //actionMode.setTitle( "Selection: " + StringFormatter.formatDistance(selectionDistance,false) );
        //actionMode.setSubtitle(R.string.route_save_selection_subtitle);
    }

    public void onSelectionUpdated( final List<Trackpoint> selection, List<LatLng> points )
    {
        selectionTrackpoints = selection;
        selectionPolyline.setPoints( points );
        selectionPolyline.setVisible(true);

        //float selectionDistance = (float) SphericalUtil.computeLength(points);
        //actionMode.setTitle( "Selection: " + StringFormatter.formatDistance( selectionDistance,false ) );
    }

    public void onZoomEntered()
    {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for ( LatLng point : selectionPolyline.getPoints() ) boundsBuilder.include(point);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds( boundsBuilder.build(), 100 )); // TODO magic number
    }

    private void saveSelection( final List<Trackpoint> selection, final Route route, final List<Waypoint> waypoints )
    {
        View routeEditorView = activity.getLayoutInflater().inflate(R.layout.route_edit, null );
        final RouteEditBinding routeEditor = DataBindingUtil.bind(routeEditorView);

        routeEditor.name.setText(route.routeName);
        routeEditor.author.setText(route.authorName);
        routeEditor.authorLink.setText(route.authorLink);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity )
                .setTitle(R.string.route_save_selection_title)
                .setView(routeEditorView)
                .setNegativeButton( R.string.dialog_cancel, null )
                .setPositiveButton( R.string.dialog_ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        Metadata metadata = new Metadata(
                                routeEditor.name.getText().toString(),
                                routeEditor.author.getText().toString(),
                                routeEditor.authorLink.getText().toString(),
                                route.dateCreated);

                        List<GlobalPosition> trackpointsData= new ArrayList<>(selection.size());
                        LatLngBounds.Builder selectionBoundsBuilder = new LatLngBounds.Builder();

                        for ( Trackpoint trackpoint : selection )
                        {
                            selectionBoundsBuilder.include( trackpoint.latlng );

                            trackpointsData.add(
                                    new GlobalPosition( trackpoint.latitude,
                                            trackpoint.longitude,
                                            trackpoint.elevation ) );
                        }

                        LatLngBounds selectionBounds = selectionBoundsBuilder.build();
                        List<NamedGlobalPosition> waypointsData = new ArrayList<>(waypoints.size());

                        for ( Waypoint waypoint : waypoints )
                        {
                            if ( selectionBounds.contains(waypoint.latlng) )
                                waypointsData.add( waypoint.namedPosition );
                        }

                        /*
                        CreateRouteQuery createRoute = new CreateRouteQuery(
                                database, metadata, trackpointsData, waypointsData , new CreateRouteQuery.Listener() {

                            public void onRouteCreated(Route route) {
                                Toast toast = Toast.makeText( RouteMapActivity.this,
                                        R.string.route_created, Toast.LENGTH_SHORT );
                                toast.show();
                            }
                            public void onCreateRouteError(String error_message) {
                                Toast toast = Toast.makeText( RouteMapActivity.this,
                                        R.string.route_creation_failed, Toast.LENGTH_SHORT );
                                toast.show();
                            }
                        });

                        createRoute.execute();
                         */
                    }
                });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }
}
