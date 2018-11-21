package bikepack.bikepack;

import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
        void onSelectionStart(Trackpoint trackpoint);
        void onSelectionUpdated(List<Trackpoint> selection, List<LatLng> points);
        void onSelectionEnd(List<Trackpoint> selection, List<LatLng> points);
    }

    static private final String LOG_TAG = "ElevationView";
    private ElevationViewBinding layoutBinding = null;

    private Listener listener = null;

    private static class ElevationData
    {
        private final List<Trackpoint> trackpoints;
        private final float distances[];
        private final int canvasLeft;
        private final float canvasRatio;

        ElevationData( @NonNull List<Trackpoint> trackpoints, int canvasLeft, int canvasWidth )
        {
            this.trackpoints = trackpoints;
            this.canvasLeft = canvasLeft;
            this.canvasRatio = trackpoints.size() / (float) canvasWidth;

            distances = new float[trackpoints.size()];
            distances[0] = 0.0f;

            for (int i = 1; i < trackpoints.size(); ++i)
                distances[i] = distances[i - 1] + (float) SphericalUtil.computeDistanceBetween(
                        trackpoints.get(i-1).pos, trackpoints.get(i).pos);
        }

        Trackpoint getTrackpoint(float pixelX) throws IndexOutOfBoundsException
        {
            return trackpoints.get( getPixelIndex(pixelX) );
        }

        float getDistance(float pixelX) throws IndexOutOfBoundsException
        {
            int index = getPixelIndex(pixelX);
            if ( index < 0 || index >= distances.length )
                throw new IndexOutOfBoundsException("invalid pixel for distance");
            return distances[index];
        }

        private int getPixelIndex(float pixelX)
        {
            return Math.round( (pixelX - canvasLeft) * canvasRatio );
        }
    }

    private ElevationData data = null;
    private SelectionDetector selectionDetector = null;
    private ScaleGestureDetector scaleGestureDetector = null;
    private Vibrator vibrator = null;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            scaleGestureDetector.getScaleFactor();
            return true;
        }
    }

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
    }

    void setTrackpoints(@NonNull final List<Trackpoint> trackpoints, final Listener listener) {
        this.data = null;

        if (trackpoints.isEmpty()) {
            Log.w(LOG_TAG, "ignoring empty trackpoints");
            return;
        }

        int canvasLeft = getPaddingLeft();
        int canvasRight = getWidth() - getPaddingRight();
        int canvasWidth = canvasRight - canvasLeft;

        this.listener = listener;
        this.data = new ElevationData( trackpoints, canvasLeft, canvasWidth );

        layoutBinding.plot.setTrackpoints(trackpoints);

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        selectionDetector = new SelectionDetector(new SelectionDetector.Listener() {
            private float startX;
            private List<Trackpoint> selection = new ArrayList<>();
            private List<LatLng> selectionPoints = new ArrayList<>();

            @Override
            public void onStartSelection(float pixelX) {
                if (vibrator != null)
                    vibrator.vibrate(100);

                startX = pixelX;

                if (listener != null) {
                    Trackpoint trackpoint = data.getTrackpoint(pixelX);
                    listener.onSelectionStart(trackpoint);

                    selection.clear();
                    selection.add(trackpoint);
                    selectionPoints.add(trackpoint.pos);
                }
            }

            @Override
            public void onAddToSelection(float pixelX) {
                if (listener != null) {
                    Trackpoint trackpoint = data.getTrackpoint(pixelX);
                    selection.add(trackpoint);
                    selectionPoints.add(trackpoint.pos);
                    listener.onSelectionUpdated(selection, selectionPoints);
                }

                float scaleX = (pixelX - startX) / layoutBinding.selectionMarker.getWidth();

                layoutBinding.selectionMarker.setX((pixelX + startX) / 2);
                layoutBinding.selectionMarker.setScaleX(scaleX);
                layoutBinding.selectionMarker.setVisibility(VISIBLE);
            }

            @Override
            public void onEndSelection(final float pixel1_x, final float pixel2_x) {
                if (listener != null) {
                    Trackpoint trackpoint2 = data.getTrackpoint(pixel2_x);
                    selection.add(trackpoint2);
                    selectionPoints.add(trackpoint2.pos);
                    listener.onSelectionEnd(selection, selectionPoints);
                }
                layoutBinding.selectionMarker.setVisibility(GONE);
            }
        });
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                onPixelTouched( event.getX() );
                break;
            case MotionEvent.ACTION_UP:
                if (listener != null) listener.onValueReleased();
                onPixelReleased();
                break;
        }

        if ( data != null )
        {
            scaleGestureDetector.onTouchEvent(event);
            selectionDetector.onTouchEvent(event);
        }

        return true;
    }

    private void onPixelTouched( float pixelX )
    {
        layoutBinding.touchMarker.setX(pixelX);
        layoutBinding.touchMarker.setVisibility(VISIBLE);

        if ( data != null )
        {
            Trackpoint trackpoint = data.getTrackpoint(pixelX);
            float distance = data.getDistance(pixelX);

            if (listener != null) listener.onValueTouched(trackpoint);

            layoutBinding.elevationText.setText(new DistanceFormater().forceMeters(true).format(trackpoint.elevation));
            layoutBinding.elevationText.setVisibility(VISIBLE);
            layoutBinding.elevationLabel.setVisibility(VISIBLE);

            layoutBinding.distanceText.setText(new DistanceFormater().format(distance));
            layoutBinding.distanceText.setVisibility(VISIBLE);
            layoutBinding.distanceLabel.setVisibility(VISIBLE);
        }
    }

    private void onPixelReleased()
    {
        layoutBinding.touchMarker.setVisibility(GONE);

        layoutBinding.elevationText.setVisibility(GONE);
        layoutBinding.elevationLabel.setVisibility(GONE);

        layoutBinding.distanceText.setVisibility(GONE);
        layoutBinding.distanceLabel.setVisibility(GONE);
    }
}