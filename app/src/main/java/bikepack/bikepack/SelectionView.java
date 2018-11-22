package bikepack.bikepack;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import bikepack.bikepack.databinding.SelectionViewBinding;

public class SelectionView extends RelativeLayout
{
    interface Listener
    {
        void onSelectionClicked( float leftX, float rightX );
        void onSelectionUpdated( float leftX, float rightX );
    }

    private final float MIN_WIDTH = 100;
    private final SelectionViewBinding binding;
    private float leftX=0, rightX=0;
    private Listener listener = null;

    SelectionView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DataBindingUtil.inflate(inflater, R.layout.selection_view, this, true);

        binding.rectangle.setOnClickListener( new ImageView.OnClickListener()
        {
            @Override
            public void onClick( View vieww )
            {
                if ( listener != null )
                    listener.onSelectionClicked(leftX,rightX);
            }
        } );

        binding.leftHandle.setOnTouchListener( new View.OnTouchListener()
        {
            @Override
            public boolean onTouch( View view, MotionEvent event )
            {
                float newLeftX = leftX + event.getX();
                if ( ( rightX - newLeftX ) < MIN_WIDTH ) return false;
                update( newLeftX, rightX );
                return true;
            }
        } );

        binding.rightHandle.setOnTouchListener( new View.OnTouchListener()
        {
            @Override
            public boolean onTouch( View view, MotionEvent event )
            {
                float newRightX = rightX + event.getX();
                if ( ( newRightX - leftX ) < MIN_WIDTH ) return false;
                update( leftX, newRightX );
                return true;
            }
        } );
    }

    void setListener( Listener listener )
    {
        this.listener = listener;
    }

    void update( float leftX, float rightX )
    {
        if ( leftX > rightX )
        {
            update( rightX, leftX );
            return;
        }

        this.leftX = leftX;
        this.rightX = rightX;

        binding.rectangle.setX( (leftX+rightX)/2 );
        binding.rectangle.setScaleX( (rightX-leftX) / binding.rectangle.getWidth() );
        binding.rectangle.setScaleY( this.getHeight() / binding.rectangle.getHeight() );

        binding.leftHandle.setX( leftX );
        binding.rightHandle.setX( rightX - binding.rightHandle.getWidth()/2 );
    }
}