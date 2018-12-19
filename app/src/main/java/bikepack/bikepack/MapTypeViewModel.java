package bikepack.bikepack;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

public class MapTypeViewModel extends AndroidViewModel
{
    private SharedPreferences preferences;
    private String mapTypeKey;
    private MutableLiveData<String> mapType = new MutableLiveData<>();

    public MapTypeViewModel(Application application )
    {
        super(application);
    }

    public void init( SharedPreferences preferences, String mapTypeKey, String defaultMapType )
    {
        this.preferences = preferences;
        this.mapTypeKey = mapTypeKey;

        mapType.setValue( preferences.getString( mapTypeKey, defaultMapType ) );
    }

    public LiveData<String> getMapType() { return mapType; }

    public void setMapType( String newMapType )
    {
        preferences.edit().putString( mapTypeKey, newMapType ).apply();
        mapType.setValue(newMapType);
    }
}
