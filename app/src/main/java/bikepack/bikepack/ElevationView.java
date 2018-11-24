package bikepack.bikepack;

import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.os.Vibrator;
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

import static android.content.Context.VIBRATOR_SERVICE;

public class ElevationView extends RelativeLayout
{
    interface Listener
    {
        void onValueTouched(Trackpoint trackpoint);
        void onValueReleased();
        void onSelectionCreated(List<Trackpoint> selection, List<LatLng> points);
        void onSelectionUpdated(List<Trackpoint> selection, List<LatLng> points);
        void onSelectionDoubleClicked(List<Trackpoint> selection, List<LatLng> points);
    }

    private class ElevationData {
        private final List<Trackpoint> trackpoints;
        private final List<LatLng> points;
        private final List<Float> distances;
        private final int canvasLeft;
        private final float canvasRatio;

        ElevationData(@NonNull List<Trackpoint> trackpoints, int canvasLeft, int canvasWidth)
                throws Exception {
            if (trackpoints.isEmpty())
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

    private ElevationData data = null;
    private final ElevationViewBinding layoutBinding;
    private Listener listener = null;

    ElevationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        layoutBinding = DataBindingUtil.inflate(inflater, R.layout.elevation_view, this, true);

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
                    if ( vibrate )
                    {
                        Vibrator vibrator = (Vibrator) getContext().getSystemService(VIBRATOR_SERVICE);
                        vibrator.vibrate(50);
                    }

                    onEnterSelection( event.getX()-100, event.getX()+100 );
                }
            } );

            public boolean onTouch( View view, MotionEvent event )
            {
                return gestureDetector.onTouchEvent(event);
            }
        } );
    }

    void setTrackpoints(@NonNull final List<Trackpoint> trackpoints, final Listener listener)
    {
        if ( trackpoints == null )
        {
            Log.e( LOG_TAG, "trackpoints are null");
            return;
        }

        Log.i( LOG_TAG, "setTrackpoints size=" + trackpoints.size() );

        int canvasLeft = getPaddingLeft();
        int canvasRight = getWidth() - getPaddingRight();
        int canvasWidth = canvasRight - canvasLeft;

        try
        {
            this.data = new ElevationData( trackpoints, canvasLeft, canvasWidth );
            this.listener = listener;

            layoutBinding.plot.drawTrackpoints(trackpoints);
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
        float pixelX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_DOWN:
                layoutBinding.touchMarker.setX(pixelX);
                layoutBinding.touchMarker.setVisibility(VISIBLE);
                onPixelTouched(pixelX);
                return true;
            case MotionEvent.ACTION_UP:
                if (listener != null) listener.onValueReleased();
                layoutBinding.touchMarker.setVisibility(GONE);
                onPixelReleased();
                return true;
        }

        return false;
    }

    private void onEnterSelection( float leftX, float rightX )
    {
        if ( data == null || listener == null )
            return;

        listener.onSelectionCreated(
                data.getTrackpointsBetween( leftX, rightX ),
                data.getPointsBetween( leftX, rightX ) );

        layoutBinding.selectionView.updateBounds( leftX, rightX );
        layoutBinding.selectionView.setVisibility(VISIBLE);
        layoutBinding.selectionView.setListener(new SelectionView.Listener()
        {
            @Override
            public void onSelectionTouched(float pixelX)
            {
                listener.onValueTouched( data.getTrackpoint(pixelX) );
            }

            @Override
            public void onSelectionClicked(float leftX, float rightX) {

                Log.i( LOG_TAG, "onSelectionClicked" );

                listener.onSelectionDoubleClicked(
                        data.getTrackpointsBetween( leftX, rightX ),
                        data.getPointsBetween( leftX, rightX ) );

                layoutBinding.selectionView.setVisibility(INVISIBLE);
                layoutBinding.selectionView.setListener(null);
            }

            @Override
            public void onSelectionUpdated(float leftX, float rightX)
            {
                //Log.i( LOG_TAG, "onSelectionUpdated" );

                long time1 = System.currentTimeMillis();
                List<Trackpoint> trackpointBetween = data.getTrackpointsBetween( leftX, rightX );
                List<LatLng> pointsBetween = data.getPointsBetween( leftX, rightX );
                long time2 = System.currentTimeMillis();

                listener.onSelectionUpdated( trackpointBetween, pointsBetween );
                long time3 = System.currentTimeMillis();

                Log.i( LOG_TAG, "subList="+(time2-time1)+ " onSelectionUpdated="+(time3-time2) );
            }
        });
    }

    void onExitSelection()
    {
        layoutBinding.selectionView.setVisibility(INVISIBLE);
    }

    private void onPixelTouched(float pixelX)
    {
        if ( data == null ) return;

        Trackpoint trackpoint = data.getTrackpoint(pixelX);
        layoutBinding.elevationText.setText(new DistanceFormatter().forceMeters(true).format(trackpoint.elevation));
        layoutBinding.elevationText.setVisibility(VISIBLE);
        layoutBinding.elevationLabel.setVisibility(VISIBLE);

        float distance = data.getDistance(pixelX);
        layoutBinding.distanceText.setText(new DistanceFormatter().format(distance));
        layoutBinding.distanceText.setVisibility(VISIBLE);
        layoutBinding.distanceLabel.setVisibility(VISIBLE);

        if (listener != null) listener.onValueTouched(trackpoint);
    }

    private void onPixelReleased()
    {
        layoutBinding.elevationText.setVisibility(GONE);
        layoutBinding.elevationLabel.setVisibility(GONE);

        layoutBinding.distanceText.setVisibility(GONE);
        layoutBinding.distanceLabel.setVisibility(GONE);
    }

}