package bikepack.bikepack;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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

    private final ElevationViewBinding viewBinding;
    private ElevationData elevationData = null;

    private MutableLiveData<List<Trackpoint>> selectionLiveData = new MutableLiveData<>();
    private MutableLiveData<Trackpoint> trackpointLiveData = new MutableLiveData<>();

    ElevationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        viewBinding = DataBindingUtil.inflate(inflater, R.layout.elevation_view, this, true);

        setClickable(true);
        setLongClickable(true);

        this.setOnTouchListener(new OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector( new GestureDetector.SimpleOnGestureListener()
            {
                public void onLongPress( MotionEvent event )
                {
                    float leftX  = event.getX() - viewBinding.selectionView.getMinWidth()/2;
                    float rightX = event.getX() + viewBinding.selectionView.getMinWidth()/2;
                    onSelectionCreated( leftX, rightX );
                }
            } );

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    LiveData<Trackpoint> getTrackpoint() { return trackpointLiveData; }
    LiveData<List<Trackpoint>> getSelection() { return selectionLiveData; }

    void clearSelection()
    {
        selectionLiveData.setValue(null);

        viewBinding.selectionView.setVisibility(INVISIBLE);
        viewBinding.selectionView.setEnabled(false);
        viewBinding.selectionView.setListener(null);
    }

    private void onTrackpointChanged( float pixelX )
    {
        try
        {
            Trackpoint trackpoint = elevationData.getTrackpoint(pixelX);
            float distance = elevationData.getDistance(pixelX);

            this.viewBinding.elevationText.setText( StringFormatter.formatDistance(trackpoint.elevation,true) );
            this.viewBinding.distanceText.setText( StringFormatter.formatDistance(distance,false) );

            this.viewBinding.touchMarker.setX(pixelX);
            this.viewBinding.trackpointInfo.setVisibility(VISIBLE);
            this.viewBinding.touchMarker.setVisibility(VISIBLE);

            trackpointLiveData.setValue(trackpoint);
        }
        catch ( Exception e )
        {
            Log.e( LOG_TAG, "onTrackpointChanged: error=" + e.getMessage() );
            onTrackpointReleased();
        }
    }

    private void onTrackpointReleased()
    {
        this.viewBinding.trackpointInfo.setVisibility(INVISIBLE);
        this.viewBinding.touchMarker.setVisibility(INVISIBLE);
        trackpointLiveData.setValue(null);
    }

    void onSelectionCreated( float leftX, float rightX )
    {
        Log.i( LOG_TAG, "onSelectionCreated: left=" + leftX + " right=" + rightX );
        if ( elevationData == null ) { Log.e( LOG_TAG, "onSelectionCreated: null elevationData"); return; }

        selectionLiveData.setValue( elevationData.getTrackpointsBetween( leftX, rightX ) );

        viewBinding.selectionView.updateBounds( leftX, rightX );
        viewBinding.selectionView.setVisibility(VISIBLE);
        viewBinding.selectionView.setEnabled(true);
        viewBinding.selectionView.setListener(new SelectionView.Listener() {
            public void onSelectionTouched(float pixelX) {
                trackpointLiveData.setValue( elevationData.getTrackpoint(pixelX) ); }
            public void onSelectionDoubleClicked(float leftX, float rightX) { }
            public void onSelectionUpdated(float leftX, float rightX) {
                selectionLiveData.setValue( elevationData.getTrackpointsBetween( leftX, rightX ) ); }
        });
    }

    void setTrackpoints(@NonNull final List<Trackpoint> trackpoints )
    {
        Log.i( LOG_TAG, "drawTrackpoints size=" + trackpoints.size() );

        int canvasLeft = getPaddingLeft();
        int canvasRight = getWidth() - getPaddingRight();
        int canvasWidth = canvasRight - canvasLeft;

        try
        {
            elevationData = new ElevationData( trackpoints, canvasLeft, canvasWidth ); // TODO slow & blocks
            viewBinding.canvas.drawTrackpoints(trackpoints);
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
                    onTrackpointChanged( event.getX() );
                return true;
            case MotionEvent.ACTION_UP:
                onTrackpointReleased();
                return true;
        }

        return false;
    }
}