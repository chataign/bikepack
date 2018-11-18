package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import bikepack.bikepack.databinding.RouteListActivityBinding;

public class RouteListActivity extends AppCompatActivity
{
    public  static final int ACTION_SELECT_ROUTE_FILE = 1;
    private static final String LOG_TAG = "RouteListActivity";

    AppDatabase database;
    AppRepository repository;
    RouteAdapter routesAdapter;
    RouteListActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.route_list_activity );

        database = Room.databaseBuilder(this, AppDatabase.class, AppDatabase.DB_NAME)
                .fallbackToDestructiveMigration()
                .build();

        repository = new AppRepository(database);
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
                routesAdapter.sort(new Comparator<Route>() {
                    @Override
                    public int compare(Route route1, Route route2) {
                        if ( route1.totalDistance < route2.totalDistance) return -1;
                        return 1;
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

        routesAdapter = new RouteAdapter( getApplicationContext(), repository.getRoutes() );

        binding.routesList.setAdapter(routesAdapter);
        binding.routesList.setOnItemClickListener( new ListView.OnItemClickListener()
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

        binding.addButton.setOnClickListener(new View.OnClickListener()
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
            binding.layout.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);

            Uri routeUri = intent.getData();
            Metadata metadata=null;
            List<GlobalPosition> trackpoints = new ArrayList<>();
            //List<XmlUtils.XmlObject> xml_waypoints;

            try
            {
                XmlUtils.XmlObject metadataXml = XmlUtils.readFirst( getContentResolver(), routeUri, Metadata.GPX_TAG);
                if ( metadataXml == null ) throw new NoSuchFieldException("File has no route metadata");
                metadata = Metadata.buildFromXml(metadataXml);

                List<XmlUtils.XmlObject> trackpointsXml = XmlUtils.readAll( getContentResolver(), routeUri, Trackpoint.GPX_TAG);
                if ( trackpointsXml.isEmpty() ) throw new Exception("Route has no trackpoints");

                for (int i = 0; i < trackpointsXml.size(); ++i)
                    trackpoints.add( GlobalPosition.buildFromXml( trackpointsXml.get(i) ) );

                //xml_waypoints = XmlUtils.readAll( getContentResolver(), routeUri, Waypoint.GPX_TAG);
            }
            catch( Exception e )
            {
                Log.e( LOG_TAG, "Failed to read GPX file, error=" + e.getMessage() );
                return;
            }

            repository.createRoute( metadata, trackpoints, new AppRepository.CreateRouteListener()
            {
                public void onRouteCreated(Route route)
                {
                    binding.layout.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    routesAdapter.add(route);
                }
                public void onError( String error_message )
                {
                    Log.e( LOG_TAG, "Failed to create route, error=" + error_message );

                    binding.layout.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);

                    AlertDialog.Builder builder = new AlertDialog.Builder(RouteListActivity.this);
                    builder.setTitle(R.string.route_creation_failed);
                    builder.setMessage(error_message);
                    builder.setPositiveButton(R.string.dialog_ok, null );

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            });
        }
    }
}
