package bikepack.bikepack;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import bikepack.bikepack.databinding.ElevationViewBinding;

public class ElevationView extends RelativeLayout
{
    private class ElevationData {
        private final List<Trackpoint> trackpoints;
        private final List<LatLng> points;
        private final List<Float> distances;
        private final int canvasLeft;
        private final float canvasRatio;

        ElevationData(@NonNull List<Trackpoint> trackpoints, int canvasLeft, int canvasWidth)
                throws Exception {
            if (trackpoints==null || trackpoints.isEmpty())
                throw new Exception("invalid elevation data: trackpoints are empty");

            this.canvasLeft = canvasLeft;
            this.canvasRatio = trackpoints.size() / (float) canvasWidth;
            this.trackpoints = trackpoints;
            this.points = new ArrayList<>(trackpoints.size());
            this.distances = new ArrayList<>(trackpoints.size());

            for (int i=0; i < trackpoints.size(); ++i)
            {
                LatLng curr = trackpoints.get(i).latlng;
                points.add(curr);

                if ( i>0 )
                {
                    LatLng prev = trackpoints.get(i-1).latlng;
                    float distance = distances.get(i-1) + (float) SphericalUtil.computeDistanceBetween(prev, curr);
                    distances.add(distance);
                }
                else distances.add(0.0f);
            }
        }

        int getPixelIndex(float pixelX) {
            int index = Math.round((pixelX - canvasLeft) * canvasRatio);
            return Math.max(Math.min(index, trackpoints.size() - 1), 0);
        }

        final Trackpoint getTrackpoint(float pixelX)
        {
            return trackpoints.get( getPixelIndex(pixelX) );
        }

        float getDistance(float pixelX)
        {
            return distances.get( getPixelIndex(pixelX) );
        }

        List<LatLng> getPointsBetween( float leftX, float rightX )
        {
            return points.subList( getPixelIndex(leftX), getPixelIndex(rightX) );
        }

        List<Trackpoint> getTrackpointsBetween( float leftX, float rightX )
        {
            return trackpoints.subList( getPixelIndex(leftX), getPixelIndex(rightX) );
        }
    }

    static private final String LOG_TAG = "ElevationView";

    private final ElevationViewBinding binding;
    private ElevationData elevationData = null;
    private MutableLiveData<Trackpoint> trackpointLiveData = new MutableLiveData<>();

    ElevationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DataBindingUtil.inflate(inflater, R.layout.elevation_view, this, true);

        setClickable(true);
        setLongClickable(true);
    }

    LiveData<Trackpoint> getTrackpoint() { return trackpointLiveData; }

    private void onTrackpointChanged( Trackpoint trackpoint )
    {
        if (trackpoint == null)
        {
            this.binding.trackpointInfo.setVisibility(INVISIBLE);
        }
        else
        {
            this.binding.elevationText.setText( StringFormatter.formatDistance(trackpoint.elevation,true) );
            this.binding.trackpointInfo.setVisibility(VISIBLE);
        }

        trackpointLiveData.setValue(trackpoint);
    }

    void drawTrackpoints(@NonNull final List<Trackpoint> trackpoints )
    {
        Log.i( LOG_TAG, "drawTrackpoints size=" + trackpoints.size() );

        int canvasLeft = getPaddingLeft();
        int canvasRight = getWidth() - getPaddingRight();
        int canvasWidth = canvasRight - canvasLeft;

        try
        {
            elevationData = new ElevationData( trackpoints, canvasLeft, canvasWidth ); // TODO slow & blocks
            binding.canvas.drawTrackpoints(trackpoints);
        }
        catch( Exception e )
        {
            elevationData = null;
            Log.w( LOG_TAG, "failed to set trackpoints, error=" + e.getMessage() );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                if ( elevationData != null )
                    onTrackpointChanged( elevationData.getTrackpoint(event.getX() ) );
                return true;
            case MotionEvent.ACTION_UP:
                onTrackpointChanged(null);
                return true;
        }

        return false;
    }
}