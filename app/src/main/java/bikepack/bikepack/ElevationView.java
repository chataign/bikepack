package bikepack.bikepack;

import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
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
        void onValueTouched( Trackpoint trackpoint );
        void onValueReleased();
        void onSelectionStart( Trackpoint trackpoint );
        void onSelectionUpdated( List<Trackpoint> selection, List<LatLng> points );
        void onSelectionEnd( List<Trackpoint> selection, List<LatLng> points );
    }

    static private final String LOG_TAG = "ElevationView";
    private final ElevationViewBinding ui;

    private Listener listener = null;
    private List<Trackpoint> trackpoints = null;

    private SelectionDetector selectionDetector = null;
    private ScaleGestureDetector scaleGestureDetector=null;

    private Vibrator vibrator = null;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector )
        {
            scaleGestureDetector.getScaleFactor();
            return true;
        }
    }

    ElevationView(Context context, AttributeSet attrs )
    {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        ui = DataBindingUtil.inflate( inflater, R.layout.elevation_view, this, true );

        setClickable(true);
        setLongClickable(true);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ElevationView,0, 0);

        boolean vibrate = attributes.getBoolean( R.styleable.ElevationView_vibrate, false );
        if (vibrate) vibrator = (Vibrator) getContext().getSystemService(VIBRATOR_SERVICE);
    }

    void setTrackpoints(@NonNull final List<Trackpoint> trackpoints, final Listener listener )
    {
        float distance=0;

        for ( int i=1; i< trackpoints.size(); ++i )
            distance += SphericalUtil.computeDistanceBetween( trackpoints.get(i-1).pos, trackpoints.get(i).pos );

        ui.distance.setText( new DistanceFormater().format(distance) );
        ui.distance.setVisibility(VISIBLE);
        ui.distanceLabel.setVisibility(VISIBLE);

        this.listener = listener;
        this.trackpoints = trackpoints;

        ui.plot.setTrackpoints(trackpoints);

        scaleGestureDetector = new ScaleGestureDetector( getContext(), new ScaleListener() );

        selectionDetector = new SelectionDetector(new SelectionDetector.Listener()
        {
            private float startX;
            private int firstIndex, lastIndex;
            private List<Trackpoint> selection = new ArrayList<>();
            private List<LatLng> points = new ArrayList<>();

            @Override
            public void onStartSelection(float pixelX)
            {
                if ( vibrator != null )
                    vibrator.vibrate(100);

                startX = pixelX;

                if ( listener != null )
                {
                    int index = ElevationView.this.getPixelIndex(pixelX);
                    listener.onSelectionStart( trackpoints.get(index) );

                    selection.clear();
                    selection.add( trackpoints.get(index) );
                    points.add( trackpoints.get(index).pos );
                    firstIndex = lastIndex = index;
                }
            }

            @Override
            public void onAddToSelection(float pixelX)
            {
                if ( listener != null )
                {
                    int index = ElevationView.this.getPixelIndex(pixelX);
                    Trackpoint trackpoint = trackpoints.get(index);

                    if ( index > lastIndex )
                    {
                        for ( int i= lastIndex+1; i <= index; ++i )
                        {
                            selection.add(trackpoint);
                            points.add(trackpoint.pos);
                        }

                        lastIndex = index;
                    }
                    if ( index < firstIndex )
                    {
                        for ( int i=index; i < firstIndex; ++i )
                        {
                            selection.add( 0, trackpoint );
                            points.add( 0, trackpoint.pos );
                        }

                        firstIndex = index;
                    }

                    listener.onSelectionUpdated(selection,points);
                }

                float scaleX = ( pixelX - startX ) / ui.selection.getWidth();

                ui.selection.setX( ( pixelX + startX ) / 2 );
                ui.selection.setScaleX( scaleX );
                ui.selection.setVisibility(VISIBLE);
            }

            @Override
            public void onEndSelection(final float pixel1_x, final float pixel2_x)
            {
                if ( listener != null )
                {
                    int index1 = ElevationView.this.getPixelIndex( pixel1_x );
                    int index2 = ElevationView.this.getPixelIndex( pixel2_x );
                    selection.add( trackpoints.get(index2) );
                    points.add( trackpoints.get(index2).pos );
                    listener.onSelectionEnd( selection, points );
                }
                ui.selection.setVisibility(GONE);
            }
        } );
    }

    @Override
    public boolean performClick()
    {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent( MotionEvent event )
    {
        switch ( event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                int index = this.getPixelIndex( event.getX() );
                if ( listener != null ) listener.onValueTouched(trackpoints.get(index));
                ui.marker.setX( event.getX() );
                ui.marker.setVisibility(VISIBLE);
                ui.elevation.setText( new DistanceFormater().forceMeters(true).format(trackpoints.get(index).elevation) );
                ui.elevation.setVisibility(VISIBLE);
                ui.elevationLabel.setVisibility(VISIBLE);
                break;
            case MotionEvent.ACTION_UP:
                if ( listener != null ) listener.onValueReleased();
                ui.marker.setVisibility(GONE);
                ui.elevation.setVisibility(GONE);
                ui.elevationLabel.setVisibility(GONE);
                break;
        }

        scaleGestureDetector.onTouchEvent(event);
        selectionDetector.onTouchEvent(event);
        return true;
    }

    private int getPixelIndex( float pixelX )
    {
        int canvasLeft = getPaddingLeft();
        int canvasRight = getWidth() - getPaddingRight();
        int canvasWidth = canvasRight - canvasLeft;

        return Math.round( ( pixelX - canvasLeft ) * trackpoints.size() / (float) canvasWidth );
    }
}
