package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.net.Uri;
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
    public  static final int ACTION_SELECT_ROUTE_FILE = 1;
    private static final String LOG_TAG = "RouteListActivity";

    AppDatabase database;
    RouteAdapter routesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.route_list_activity );

        database = Room.databaseBuilder(this, AppDatabase.class, AppDatabase.DB_NAME)
                .fallbackToDestructiveMigration()
                .build();
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
    public void onStart()
    {
        super.onStart();

        routesAdapter = new RouteAdapter( getApplicationContext(), new ArrayList<Route>() );

        new GetRoutesQuery(database, new GetRoutesQuery.Listener()
        {
            @Override
            public void onRoutesReceived(List<Route> routes) {
                routesAdapter.addAll(routes);
            }

            @Override
            public void onGetRoutesError(String errorMessage) {
                Log.e( LOG_TAG, errorMessage );
                showErrorMessage( getString(R.string.route_loading_failed), errorMessage );
            }
        }).execute();

        ListView routeList = findViewById(R.id.route_list);
        routeList.setAdapter(routesAdapter);
        routeList.setOnItemClickListener( new ListView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> list, View view, int position, long id )
            {
                Route route = (Route) list.getItemAtPosition(position);
                Intent intent = new Intent( RouteListActivity.this, RouteInfoActivity.class);
                intent.putExtra( getString(R.string.route_extra), route );
                startActivity(intent);
            }
        });

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
        View progressDialogView = getLayoutInflater().inflate(R.layout.progress_dialog, null );

        AlertDialog.Builder progressBuilder = new AlertDialog.Builder(this)
                .setTitle(R.string.route_import_dialog_title)
                .setNegativeButton( R.string.dialog_cancel, null )
                .setView(progressDialogView);

        final AlertDialog progressDialog = progressBuilder.create();
        progressDialog.show();

        final TextView progressMessage = progressDialogView.findViewById(R.id.progress_message);
        progressMessage.setText( getString(R.string.route_import_dialog_parse_gpx) );

        GpxFileParser gpxFileParser = new GpxFileParser(getContentResolver(), routeUri, new GpxFileParser.Listener()
        {
            @Override
            public void onGpxFileRead( Metadata metadata,
                                       List<GlobalPosition> trackpoints,
                                       List<NamedGlobalPosition> waypoints )
            {
                progressMessage.setText( getString(R.string.route_import_dialog_write_db) );

                CreateRouteQuery createRouteTask = new CreateRouteQuery(
                        database, metadata, trackpoints, waypoints, new CreateRouteQuery.Listener()
                {
                    public void onRouteCreated(Route route)
                    {
                        progressDialog.dismiss();
                        routesAdapter.add(route);
                    }

                    public void onCreateRouteError( String errorMessage )
                    {
                        progressDialog.dismiss();
                        showErrorMessage( getString(R.string.route_creation_failed), errorMessage );
                    }
                });

                createRouteTask.execute();
            }

            @Override
            public void onGpxReadError( String errorMessage )
            {
                progressDialog.dismiss();
                showErrorMessage( getString(R.string.route_creation_failed), errorMessage );
            }

            @Override
            public void onGpxReadCancelled()
            {
                progressDialog.dismiss();
            }
        });

        gpxFileParser.execute();
    }

    private void showErrorMessage( String title, String message )
    {
        Log.e( LOG_TAG, message );

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.dialog_ok, null );

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
