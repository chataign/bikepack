package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RouteListActivity extends AppCompatActivity
{
    private static final int ACTION_SELECT_ROUTE_FILE = 1;
    private static final String LOG_TAG = "RouteListActivity";

    private RouteAdapter routesAdapter;
    RouteListViewModel routesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_list_activity);

        routesAdapter = new RouteAdapter( getApplicationContext(), new ArrayList<Route>() );

        ListView routeListView = findViewById(R.id.route_list);
        routeListView.setAdapter(routesAdapter);
        routeListView.setOnItemClickListener( new ListView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> list, View view, int position, long id )
            {
            Route route = (Route) list.getItemAtPosition(position);
            Intent intent = new Intent( RouteListActivity.this, RouteInfoActivity2.class);
            intent.putExtra( getString(R.string.route_extra), route );
            startActivity(intent);
            }
        });
        routeListView.setOnItemLongClickListener( new ListView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick( AdapterView<?> list, View view, int position, long id )
            {
            final Route route = (Route) list.getItemAtPosition(position);
            new AlertDialog.Builder(RouteListActivity.this)
                    .setTitle(R.string.route_delete_dialog_title)
                    .setMessage(R.string.route_delete_dialog_message)
                    .setNegativeButton(R.string.dialog_ok, null )
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            routesViewModel.deleteRoute(route);
                        }
                    })
                    .create()
                    .show();
            return true;
            }
        });

        routesViewModel = ViewModelProviders.of(this).get(RouteListViewModel.class);
        routesViewModel.getRoutes().observe(this, new Observer<List<Route>>() {
            @Override
            public void onChanged(@Nullable List<Route> routes)
            {
                routesAdapter.clear();
                routesAdapter.addAll(routes);
            }
        } );

        FloatingActionButton addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType( getString(R.string.routes_mime_types) );
                startActivityForResult(Intent.createChooser(intent,"Select route file"), ACTION_SELECT_ROUTE_FILE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_sort: {
                routesAdapter.sort(new Comparator<Route>()
                {
                    @Override
                    public int compare(Route route1, Route route2) {
                    return ( route1.totalDistance < route2.totalDistance ? -1 : +1 );
                    }
                });
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int request_code, int result_code, Intent intent)
    {
        if (request_code == ACTION_SELECT_ROUTE_FILE && result_code == RESULT_OK )
        {
            final Uri routeUri = intent.getData();
            if (routeUri == null ) Log.w( LOG_TAG, "invalid file URI" );
            else importRoute(routeUri);
        }
    }

    private void importRoute( Uri routeUri )
    {
        View dialogView = getLayoutInflater().inflate(R.layout.progress_dialog, null );

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                .setTitle(R.string.route_import_dialog_title)
                .setNegativeButton( R.string.dialog_cancel, null )
                .setView(dialogView);

        final AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        final TextView progressMessage = dialogView.findViewById(R.id.progress_message);
        progressMessage.setText( getString(R.string.route_import_dialog_parse_gpx) );

        new GpxFileParser(getContentResolver(), routeUri, new GpxFileParser.Listener()
        {
            @Override
            public void onGpxFileRead( Metadata metadata,
                                       List<GlobalPosition> trackpoints,
                                       List<NamedGlobalPosition> waypoints ) {
                progressMessage.setText( getString(R.string.route_import_dialog_write_db) );
                routesViewModel.createRoute( metadata, trackpoints, waypoints );
                dialog.dismiss();
            }

            @Override
            public void onGpxReadError( String errorMessage ) {
                dialog.dismiss();
                showErrorMessage( getString(R.string.route_creation_failed), errorMessage );
            }

            @Override
            public void onGpxReadCancelled()
            {
                dialog.dismiss();
            }
        }).execute();
    }

    private void showErrorMessage( String title, String message )
    {
        Log.e( LOG_TAG, message );

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_ok, null )
            .create()
            .show();
    }
}
