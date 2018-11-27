package bikepack.bikepack;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import static android.content.Context.VIBRATOR_SERVICE;

public class ElevationView extends RelativeLayout implements SelectionView.Listener
{
    interface OnSelectionListener
    {
        void onSelectionCreated(List<Trackpoint> selection, List<LatLng> points);
        void onSelectionUpdated(List<Trackpoint> selection, List<LatLng> points);
        void onZoomEntered();
    }

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
    private ElevationData data = null;
    private ElevationData dataBeforeZoom = null;
    private OnSelectionListener onSelectionListener = null;

    private MutableLiveData<Float> pixel = new MutableLiveData<>();
    private MutableLiveData<Trackpoint> trackpoint = new MutableLiveData<>();

    void setOnSelectionListener( OnSelectionListener onSelectionListener ) { this.onSelectionListener = onSelectionListener; }

    ElevationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DataBindingUtil.inflate(inflater, R.layout.elevation_view, this, true);

        setClickable(true);
        setLongClickable(true);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ElevationView, 0, 0);

        final boolean vibrate = attributes.getBoolean(R.styleable.ElevationView_vibrate, false);

        this.setOnTouchListener( new View.OnTouchListener()
        {
            GestureDetector gestureDetector = new GestureDetector( new GestureDetector.SimpleOnGestureListener()
            {
                public void onLongPress( MotionEvent event )
                {
                    if ( dataBeforeZoom != null )
                        return; // disable when zoomed

                    if ( vibrate )
                    {
                        Vibrator vibrator = (Vibrator) getContext().getSystemService(VIBRATOR_SERVICE);
                        vibrator.vibrate(50);
                    }

                    onEnterSelection(
                            event.getX()-binding.selectionView.getMinWidth()/2,
                            event.getX()+binding.selectionView.getMinWidth()/2 );
                }
            } );

            public boolean onTouch( View view, MotionEvent event )
            {
                return gestureDetector.onTouchEvent(event);
            }
        } );

        pixel.observe(this, new Observer<Float>() {
            @Override
            public void onChanged(@Nullable Float pixelX ) {
                if ( pixelX != null )
                {
                    float distance = data.getDistance(pixelX);
                    float elevation = data.getTrackpoint(pixelX).elevation;

                    binding.touchMarker.setX(pixelX);
                    binding.elevationText.setText( StringFormatter.formatDistance(elevation,true) );
                    binding.distanceText.setText( StringFormatter.formatDistance(distance,false) );
                    binding.trackpointInfo.setVisibility(VISIBLE);
                }
                else
                {
                    binding.trackpointInfo.setVisibility(GONE);
                }
            }
        });
    }

    LiveData<Trackpoint> getTrackpoint() { return trackpoint; }

    void setTrackpoints(@NonNull final List<Trackpoint> trackpoints )
    {
        Log.i( LOG_TAG, "setTrackpoints size=" + trackpoints.size() );

        int canvasLeft = getPaddingLeft();
        int canvasRight = getWidth() - getPaddingRight();
        int canvasWidth = canvasRight - canvasLeft;

        try
        {
            this.data = new ElevationData( trackpoints, canvasLeft, canvasWidth ); // TODO slow & blocks
            binding.plot.drawTrackpoints(trackpoints);
        }
        catch( Exception e )
        {
            this.data = null;
            Log.w( LOG_TAG, "failed to set trackpoints, error=" + e.getMessage() );
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                if ( data != null )
                {
                    trackpoint.setValue( data.getTrackpoint(event.getX() ) );
                    pixel.setValue( event.getX() );
                }
                return true;
            case MotionEvent.ACTION_UP:
                trackpoint.setValue(null);
                pixel.setValue(null);
                return true;
        }

        return false;
    }

    private void onEnterSelection( float leftX, float rightX )
    {
        if ( data == null || onSelectionListener == null )
            return;

        onSelectionListener.onSelectionCreated(
                data.getTrackpointsBetween( leftX, rightX ),
                data.getPointsBetween( leftX, rightX ) );

        binding.selectionView.updateBounds( leftX, rightX );
        binding.selectionView.setVisibility(VISIBLE);
        binding.selectionView.setEnabled(true);
        binding.selectionView.setListener(this);
    }

    @Override
    public void onSelectionTouched(float pixelX)
    {
        trackpoint.setValue( data.getTrackpoint(pixelX) );
    }

    @Override
    public void onSelectionDoubleClicked(float leftX, float rightX)
    {
        Log.i( LOG_TAG, "onSelectionDoubleClicked left=" + leftX + " right=" + rightX );

        binding.selectionView.setVisibility(GONE);
        binding.selectionView.setEnabled(false);

        List<Trackpoint> zoomTrackpoints = data.getTrackpointsBetween( leftX, rightX );
        List<LatLng> zoomPoints = data.getPointsBetween( leftX, rightX );

        dataBeforeZoom = data;

        setTrackpoints( zoomTrackpoints );
        if ( onSelectionListener != null ) onSelectionListener.onZoomEntered();
    }

    @Override
    public void onSelectionUpdated(float leftX, float rightX)
    {
        Log.i( LOG_TAG, "onSelectionUpdated left=" + leftX + " right=" + rightX );

        if ( onSelectionListener != null )
            onSelectionListener.onSelectionUpdated(
                data.getTrackpointsBetween( leftX, rightX ),
                data.getPointsBetween( leftX, rightX ) );
    }

    void exitSelection()
    {
        if ( dataBeforeZoom != null )
        {
            setTrackpoints( dataBeforeZoom.trackpoints );
            dataBeforeZoom = null;
        }

        binding.selectionView.setVisibility(GONE);
        binding.selectionView.setEnabled(false);
    }
}