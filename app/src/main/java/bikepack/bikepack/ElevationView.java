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
import android.widget.RelativeLayout;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.LinkedList;
import java.util.List;

import bikepack.bikepack.databinding.ElevationViewBinding;

import static android.content.Context.VIBRATOR_SERVICE;

public class ElevationView extends RelativeLayout
{
    interface Listener
    {
        void onValueTouched(Trackpoint trackpoint);
        void onValueReleased();
        void onSelectionUpdated(List<Trackpoint> selection, List<LatLng> points);
        void onSelectionEnd(List<Trackpoint> selection, List<LatLng> points);
    }

    private class ElevationData {
        private final Trackpoint trackpoints[];
        private final float distances[];
        private final int canvasLeft;
        private final float canvasRatio;

        private LinkedList<Trackpoint> selectionTrackpoints = new LinkedList<>();
        private LinkedList<LatLng> selectionPoints = new LinkedList<>();
        private int leftIndex = 0, rightIndex = 0;

        ElevationData(@NonNull List<Trackpoint> trackpoints, int canvasLeft, int canvasWidth)
                throws Exception {
            if (trackpoints.isEmpty())
                throw new Exception("invalid elevation data: trackpoints are empty");

            this.canvasLeft = canvasLeft;
            this.canvasRatio = trackpoints.size() / (float) canvasWidth;
            this.trackpoints = new Trackpoint[trackpoints.size()];

            distances = new float[trackpoints.size()];
            distances[0] = 0.0f;

            for (int i = 1; i < trackpoints.size(); ++i)
            {
                this.trackpoints[i] = trackpoints.get(i);

                distances[i] = distances[i - 1] + (float) SphericalUtil.computeDistanceBetween(
                        trackpoints.get(i - 1).latlng, trackpoints.get(i).latlng);
            }
        }

        Trackpoint getTrackpoint(float pixelX) {
            return trackpoints[getPixelIndex(pixelX)];
        }

        float getDistance(float pixelX) {
            return distances[getPixelIndex(pixelX)];
        }

        int getPixelIndex(float pixelX) {
            int index = Math.round((pixelX - canvasLeft) * canvasRatio);
            return Math.max(Math.min(index, trackpoints.length-1), 0);
        }

        private void updateBounds(float leftX, float rightX)
        {
            if (leftX > rightX)
            {
                updateBounds(rightX, leftX);
                return;
            }

            int newLeftIndex = getPixelIndex(leftX);
            int newRightIndex = getPixelIndex(rightX);

            if ( selectionTrackpoints.isEmpty() )
            {
                for ( int index = newLeftIndex; index <= newRightIndex; ++index )
                {
                    selectionTrackpoints.add( trackpoints[index] );
                    selectionPoints.add( trackpoints[index].latlng );
                }
            }
            else
            {
                if ( newLeftIndex < leftIndex)
                {
                    for ( int index = leftIndex-1; index >= newLeftIndex; --index )
                        selectionPoints.addFirst( trackpoints[index].latlng );
                }
                else if ( newLeftIndex > leftIndex )
                {
                    for ( int index = leftIndex; index < newLeftIndex; ++index )
                        selectionPoints.removeFirst();
                }

                if ( newRightIndex > rightIndex )
                {
                    for ( int index = rightIndex+1; index <= newRightIndex; ++index )
                        selectionPoints.addFirst( trackpoints[index].latlng );
                }
                else if ( newRightIndex < rightIndex )
                {
                    for ( int index = newRightIndex; index < rightIndex; ++index )
                        selectionPoints.removeLast();
                }
            }

            leftIndex = newLeftIndex;
            rightIndex = newRightIndex;
        }

        List<LatLng> getPointsBetween( float leftX, float rightX )
        {
            updateBounds( leftX, rightX );
            return selectionPoints;
        }

        List<Trackpoint> getTrackpointsBetween( float leftX, float rightX )
        {
            updateBounds( leftX, rightX );
            return selectionTrackpoints;
        }
    }

    static private final String LOG_TAG = "ElevationView";

    private ElevationData data = null;
    private final GestureDetector gestureDetector;
    //private ScaleGestureDetector scaleGestureDetector = null;
    private Vibrator vibrator = null;
    private ElevationViewBinding layoutBinding = null;
    private Listener listener = null;
    private boolean longPress=false;

    /*
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            scaleGestureDetector.getScaleFactor();
            return true;
        }
    }
     */

    ElevationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        layoutBinding = DataBindingUtil.inflate(inflater, R.layout.elevation_view, this, true);

        setClickable(true);
        setLongClickable(true);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ElevationView, 0, 0);

        boolean vibrate = attributes.getBoolean(R.styleable.ElevationView_vibrate, false);
        if (vibrate) vibrator = (Vibrator) getContext().getSystemService(VIBRATOR_SERVICE);

        //scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public void onLongPress(MotionEvent e) {
                Log.i(LOG_TAG, "onLongPress");
                longPress = true;
            }
        });
    }

    void setTrackpoints(@NonNull final List<Trackpoint> trackpoints, final Listener listener)
    {
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
                if ( longPress ) onEnterSelection( pixelX-100, pixelX+100 );
                longPress=false;
            case MotionEvent.ACTION_DOWN:
                layoutBinding.touchMarker.setX(pixelX);
                layoutBinding.touchMarker.setVisibility(VISIBLE);
                onPixelTouched(pixelX);
                break;
            case MotionEvent.ACTION_UP:
                longPress=false;
                if (listener != null) listener.onValueReleased();
                layoutBinding.touchMarker.setVisibility(GONE);
                onPixelReleased();
                break;
        }

        gestureDetector.onTouchEvent(event);
        //scaleGestureDetector.onTouchEvent(event);

        return true;
    }

    private void onEnterSelection( float leftX, float rightX )
    {
        if ( data == null || listener == null )
            return;

        layoutBinding.selectionView.update( leftX, rightX );
        layoutBinding.selectionView.setVisibility(VISIBLE);
        layoutBinding.selectionView.setListener(new SelectionView.Listener()
        {
            @Override
            public void onSelectionClicked(float leftX, float rightX) {

                listener.onSelectionEnd(
                        data.getTrackpointsBetween( leftX, rightX ),
                        data.getPointsBetween( leftX, rightX ) );
            }

            @Override
            public void onSelectionUpdated(float leftX, float rightX)
            {
                            /*
                            listener.onSelectionEnd(
                                    data.getTrackpointsBetween( leftX, rightX ),
                                    data.getPointsBetween( leftX, rightX ) );
                            */
            }
        });
    }

    private void onPixelTouched(float pixelX)
    {
        if ( data == null ) return;

        Trackpoint trackpoint = data.getTrackpoint(pixelX);
        layoutBinding.elevationText.setText(new DistanceFormater().forceMeters(true).format(trackpoint.elevation));
        layoutBinding.elevationText.setVisibility(VISIBLE);
        layoutBinding.elevationLabel.setVisibility(VISIBLE);

        float distance = data.getDistance(pixelX);
        layoutBinding.distanceText.setText(new DistanceFormater().format(distance));
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