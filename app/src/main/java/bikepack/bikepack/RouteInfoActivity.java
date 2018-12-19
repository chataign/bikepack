package bikepack.bikepack;

import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import bikepack.bikepack.databinding.RouteEditBinding;
import bikepack.bikepack.databinding.RouteInfoActivityBinding;

public class RouteInfoActivity extends AppCompatActivity
{
    static final private String LOG_TAG = "RouteInfoActivity";

    private RouteViewModel routeViewModel;
    private RouteInfoActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView( this, R.layout.route_info_activity );

        Intent intent = getIntent();
        Route routeIn = null;

        if ( intent == null || ( routeIn = intent.getParcelableExtra( getString(R.string.route_extra) ) ) == null )
        {
            Log.e( LOG_TAG, "route input is null" );
            finish();
            return;
        }

        routeViewModel = ViewModelProviders.of(this).get(RouteViewModel.class);
        routeViewModel.init( routeIn.routeId);
        routeViewModel.getRoute().observe(this, new Observer<Route>() {
            @Override
            public void onChanged(@Nullable Route route) {
                if ( route == null ) return; // route was deleted
                Log.i( LOG_TAG, "route changed" );
                binding.name.setText(route.routeName);
                binding.author.setText(route.authorName);
                binding.grid.removeAllViews();
                for ( RouteInfoItem infoItem : RouteInfoItem.createRouteItems(route) )
                    binding.grid.addView( infoItem.getView( getLayoutInflater() ) );
            }
        });
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
                intent.putExtra( getString(R.string.route_extra), routeViewModel.getRoute().getValue() );
                startActivity(intent);
                return true;
            }
            case R.id.action_delete: {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.route_delete_dialog_title)
                        .setMessage(R.string.route_delete_dialog_message)
                        .setNegativeButton(R.string.dialog_cancel, null )
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        routeViewModel.deleteRoute();
                        RouteInfoActivity.this.finish();
                    }
                }).create().show();
                return true;
            }
            case R.id.action_edit: {
                View routeEditView = getLayoutInflater().inflate(R.layout.route_edit, null );
                final RouteEditBinding editor = DataBindingUtil.bind(routeEditView);

                Route route = routeViewModel.getRoute().getValue();

                editor.name.setText(route.routeName);
                editor.author.setText(route.authorName);
                editor.authorLink.setText(route.authorLink);

                new AlertDialog.Builder(this)
                        .setTitle(R.string.route_edit_dialog_title)
                        .setView(routeEditView)
                        .setNegativeButton( R.string.dialog_cancel, null )
                        .setPositiveButton( R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                            String routeName  = editor.name.getText().toString();
                            String authorName = editor.author.getText().toString();
                            String authorLink = editor.authorLink.getText().toString();
                            routeViewModel.updateRoute( routeName, authorName, authorLink );
                        }
                    }).create()
                      .show();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
