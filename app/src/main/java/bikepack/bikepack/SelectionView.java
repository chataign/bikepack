package bikepack.bikepack;

import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import bikepack.bikepack.databinding.SelectionViewBinding;

// Must be declared public to be used in layouts
public class SelectionView extends RelativeLayout
{
    interface Listener
    {
        void onSelectionTouched( float pixelX );
        void onSelectionDoubleClicked( float leftX, float rightX );
        void onSelectionUpdated( float leftX, float rightX );
    }

    private final static String LOG_TAG = "SelectionView";
    private final static float DEFAULT_MIN_WIDTH = 100;

    private final SelectionViewBinding binding;
    private float left =0, right =0;
    private Listener listener = null;
    private final float minWidth;
    private boolean updating=false;

    SelectionView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.SelectionView,0, 0);

        minWidth = attributes.getDimension( R.styleable.SelectionView_min_width, DEFAULT_MIN_WIDTH );

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DataBindingUtil.inflate(inflater, R.layout.selection_view, this, true);

        binding.rectangle.setOnTouchListener( new View.OnTouchListener()
        {
            GestureDetector gestureDetector = new GestureDetector( new GestureDetector.SimpleOnGestureListener()
            {
                public boolean onDoubleTapEvent(MotionEvent e)
                {
                    //Log.i( LOG_TAG, "onDoubleTapEvent" );
                    if ( listener == null ) return false;
                    listener.onSelectionDoubleClicked(left, right);
                    return true;
                }
            } );

            public boolean onTouch( View view, MotionEvent event )
            {
                return gestureDetector.onTouchEvent(event);
            }
        } );

        binding.leftHandle.setOnTouchListener( new View.OnTouchListener()
        {
            @Override
            public boolean onTouch( View view, MotionEvent event )
            {
                float newLeft = event.getRawX(); //left + event.getX();

                if ( ( right - newLeft ) < minWidth )
                    return false; // don't allow resizing beyond minWidth

                switch( event.getAction() )
                {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        if ( listener != null ) listener.onSelectionTouched(newLeft);
                        updateBounds( newLeft, right);
                        updating=true;
                        return true;
                    case MotionEvent.ACTION_UP:
                        if ( updating && listener != null ) listener.onSelectionUpdated(left, right);
                        updating=false;
                        return false;
                }

                return true;
            }
        } );

        binding.rightHandle.setOnTouchListener( new View.OnTouchListener()
        {
            @Override
            public boolean onTouch( View view, MotionEvent event )
            {
                float newRight = event.getRawX(); //right + event.getX();

                if ( ( newRight - left) < minWidth )
                    return false; // don't allow resizing beyond minWidth

                switch( event.getAction() )
                {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        if ( listener != null ) listener.onSelectionTouched(newRight);
                        updateBounds(left, newRight );
                        updating=true;
                        return true;
                    case MotionEvent.ACTION_UP:
                        if ( updating && listener != null ) listener.onSelectionUpdated(left, right);
                        updating=false;
                        return false;
                }

                return false;
            }
        } );
    }

    float getMinWidth() { return minWidth; }
    void setListener( Listener listener )
    {
        this.listener = listener;
    }

    void updateBounds( float newLeft, float newRight )
    {
        if ( newLeft > newRight )
        {
            updateBounds( newRight, newLeft );
            return;
        }

        this.left = newLeft;
        this.right = newRight;

        draw();
    }

    private void draw()
    {
        final float H = this.getMeasuredHeight() + this.getPaddingTop() + this.getPaddingBottom();

        binding.rectangle.setX( (left+right)/2 - binding.rectangle.getWidth()/2 );
        binding.rectangle.setScaleX( (right - left) / binding.rectangle.getWidth() );
        binding.rectangle.setScaleY( H / binding.rectangle.getHeight() );

        binding.leftHandle.setX( left - binding.leftHandle.getWidth()/2 );
        binding.leftHandle.setScaleY( H / binding.leftHandle.getHeight() );

        binding.rightHandle.setX( right - binding.rightHandle.getWidth()/2 );
        binding.rightHandle.setScaleY( H / binding.rightHandle.getHeight() );
    }
}