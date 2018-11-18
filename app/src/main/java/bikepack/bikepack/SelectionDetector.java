package bikepack.bikepack;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

class SelectionDetector
{
    interface Listener
    {
        void onStartSelection( float x );
        void onAddToSelection( float x );
        void onEndSelection( float x1, float x2 );
    }

    static final String LOG_TAG = "SelectionDetector";

    private boolean longPress =false;
    private boolean inSelection =false;
    private float startX =0;

    private final GestureDetector gestureDetector;
    private final Listener listener;

    SelectionDetector(@NonNull Listener listener )
    {
        this.listener = listener;

        gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener()
        {
            @Override
            public void onLongPress(MotionEvent e) {
                Log.i(LOG_TAG, "onLongPress");
                longPress = true;
            }
        });
    }

    boolean onTouchEvent( MotionEvent event )
    {
        gestureDetector.onTouchEvent(event);
        float currentX = event.getX();

        if ( longPress && !inSelection)
        {
            listener.onStartSelection(currentX);
            startX = currentX;
            inSelection =true;
        }

        switch ( event.getAction() )
        {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                longPress = false;
                if (inSelection) listener.onEndSelection(startX, currentX );
                inSelection = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (inSelection) listener.onAddToSelection(currentX);
                break;
        }

        return true;
    }
}