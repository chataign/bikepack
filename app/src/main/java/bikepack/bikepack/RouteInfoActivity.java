package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.persistence.room.Room;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import bikepack.bikepack.databinding.RouteEditBinding;
import bikepack.bikepack.databinding.RouteInfoActivityBinding;

public class RouteInfoActivity extends AppCompatActivity
{
    static final private String LOG_TAG = "RouteInfoActivity";

    private AppRepository repository;
    private RouteInfoActivityBinding binding;

    private Route route=null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView( this, R.layout.route_info_activity );

        AppDatabase database = Room.databaseBuilder(this, AppDatabase.class, AppDatabase.DB_NAME)
                .fallbackToDestructiveMigration().build();

        repository = new AppRepository(database);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        Intent intent = getIntent();
        route = intent.getParcelableExtra( getString(R.string.route_extra) );
        onRouteReceived(route);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle)
    {
        Log.i( LOG_TAG, "onRestoreInstanceState" );
    }

    @Override
    public void onSaveInstanceState(Bundle bundle)
    {
        Log.i( LOG_TAG, "onSaveInstanceState" );
        super.onSaveInstanceState(bundle);
    }

    void onRouteReceived( Route route )
    {
        if ( route == null )
        {
            Log.e(LOG_TAG, "No route extra, aborting.");
            this.finish();
        }

        DistanceFormater formater = new DistanceFormater();

        String total_distance = formater.format(route.totalDistance);
        String total_ascent = formater.forceMeters(true).format(route.totalAscent);
        String total_descent = formater.forceMeters(true).format(route.totalDescent);
        String highest_elevation = formater.forceMeters(true).format(route.highestElevation);
        String lowest_elevation = formater.forceMeters(true).format(route.lowestElevation);
        String date_added = new SimpleDateFormat("dd/MM/yy").format(route.dateAdded);
        String date_created = new SimpleDateFormat("dd/MM/yy").format(route.dateCreated);

        List<RouteInfoItem> info_items = new ArrayList<>();

        info_items.add( new RouteInfoItem( "Total distance", total_distance, R.drawable.ic_distance_black_24px ) );
        info_items.add( new RouteInfoItem( "Date created", date_created, R.drawable.ic_date_black_24px) );
        info_items.add( new RouteInfoItem( "Total ascent", total_ascent, R.drawable.ic_ascent_black_24px) );
        info_items.add( new RouteInfoItem( "Total descent", total_descent, R.drawable.ic_descent_black_24px) );
        info_items.add( new RouteInfoItem( "Highest elevation", highest_elevation, R.drawable.ic_elevation_top_black_24px ) );
        info_items.add( new RouteInfoItem( "Lowest elevation", lowest_elevation, R.drawable.ic_elevation_bottom_black_24px ) );

        binding.name.setText(route.routeName);
        binding.author.setText(route.authorName);
        binding.grid.removeAllViews();

        for ( RouteInfoItem info_item : info_items )
            binding.grid.addView( info_item.getView( getLayoutInflater() ) );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_map: {
                Intent intent = new Intent( this, RouteMapActivity.class );
                intent.putExtra( getString(R.string.route_extra), route );
                startActivity(intent);
                return true;
            }
            case R.id.action_delete: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.route_delete_dialog_title);
                builder.setMessage(R.string.route_delete_dialog_message);
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        repository.deleteRoute(route);
                        RouteInfoActivity.this.finish();
                    }
                });
                builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            }
            case R.id.action_edit: {
                View routeEditView = getLayoutInflater().inflate(R.layout.route_edit, null );
                final RouteEditBinding routeEditor = DataBindingUtil.bind(routeEditView);

                routeEditor.name.setText(route.routeName);
                routeEditor.author.setText(route.authorName);
                routeEditor.authorLink.setText(route.authorLink);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.route_edit_dialog_title);
                builder.setView(routeEditView);
                builder.setPositiveButton( R.string.dialog_ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String routeName  = routeEditor.name.getText().toString();
                        String authorName = routeEditor.author.getText().toString();
                        String authorLink = routeEditor.authorLink.getText().toString();
                        Route updated = repository.updateRoute( route.routeId, routeName, authorName, authorLink );
                        onRouteReceived(updated);
                    }
                });
                builder.setNegativeButton( R.string.dialog_cancel, null );
                AlertDialog edit_dialog = builder.create();
                edit_dialog.show();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
