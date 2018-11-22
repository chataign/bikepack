package bikepack.bikepack;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.List;

public class ElevationCanvas extends View
{
    static private final String LOG_TAG = "ElevationCanvas";

    private final int DEFAULT_LINE_COLOR = Color.RED;
    private final int DEFAULT_LINE_WIDTH = 2;
    private final int DEFAULT_GUIDE_COLOR = Color.GRAY;
    private final int DEFAULT_GUIDE_WIDTH = 1;
    private final int DEFAULT_NUM_GUIDES = 4;

    private final Paint linePaint;
    private final Paint guidePaint;
    private final int num_guides;
    private Bitmap bitmap=null;

    ElevationCanvas(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.setClickable(false);
        this.setSaveEnabled(true);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ElevationCanvas,0, 0);

        int lineColor = attributes.getColor( R.styleable.ElevationCanvas_line_color, DEFAULT_LINE_COLOR );
        int lineWidth = attributes.getColor( R.styleable.ElevationCanvas_line_width, DEFAULT_LINE_WIDTH );
        int guideColor = attributes.getColor( R.styleable.ElevationCanvas_guide_color, DEFAULT_GUIDE_COLOR );
        int guideWidth = attributes.getColor( R.styleable.ElevationCanvas_guide_width, DEFAULT_GUIDE_WIDTH );
        num_guides = attributes.getColor( R.styleable.ElevationCanvas_num_guides, DEFAULT_NUM_GUIDES );

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor( lineColor );
        linePaint.setStrokeWidth( lineWidth );

        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setColor( guideColor );
        guidePaint.setStrokeWidth( guideWidth );
    }

    @Override
    protected void onDraw( Canvas canvas )
    {
        Log.i( LOG_TAG, "onDraw" );

        if ( bitmap != null )
            canvas.drawBitmap( bitmap, 0, 0, null);

        super.onDraw(canvas);
    }

    // TODO setValues() with array of objects and value getter

    void drawTrackpoints( @NonNull List<Trackpoint> trackpoints )
    {
        Log.i( LOG_TAG, "trackpoints=" + trackpoints.size() );

        DrawBitmap drawBitmap = new DrawBitmap( this, trackpoints, new DrawBitmap.Listener()
        {
            @Override
            public void onBitmapReady( Bitmap bitmap )
            {
                Log.i( LOG_TAG, "onBitmapReady" );
                ElevationCanvas.this.bitmap = bitmap;
                ElevationCanvas.this.invalidate(); // calls onDraw()
            }
        } );

        drawBitmap.execute();
    }

    static private class DrawBitmap extends AsyncTask< Void, Void, Bitmap >
    {
        interface Listener
        {
            void onBitmapReady( Bitmap bitmap );
        }

        final ElevationCanvas plotCanvas;
        final List<Trackpoint> trackpoints;
        final Listener listener;

        DrawBitmap(ElevationCanvas plotCanvas, List<Trackpoint> trackpoints, Listener listener )
        {
            this.plotCanvas = plotCanvas;
            this.trackpoints = trackpoints;
            this.listener = listener;
        }

        protected Bitmap doInBackground(Void... data)
        {
            long startTime = System.currentTimeMillis();

            float minElevation = Float.MAX_VALUE;
            float maxElevation = Float.MIN_VALUE;

            for ( Trackpoint trackpoint: trackpoints )
            {
                minElevation = Math.min( trackpoint.elevation, minElevation );
                maxElevation = Math.max( trackpoint.elevation, maxElevation );
            }

            Log.i( LOG_TAG, String.format( "values: min=%.2f max=%.2f",
                    minElevation, maxElevation ) );

            int canvasLeft = plotCanvas.getPaddingLeft();
            int canvasTop = plotCanvas.getPaddingTop();
            int canvasRight = plotCanvas.getWidth() - plotCanvas.getPaddingRight();
            int canvasBottom = plotCanvas.getHeight() - plotCanvas.getPaddingBottom();
            int canvasWidth = canvasRight - canvasLeft;
            int canvasHeight = canvasBottom - canvasTop;

            Log.i( LOG_TAG, String.format( "canvas: left=%d top=%d right=%d bottom=%d",
                    canvasLeft, canvasTop, canvasRight, canvasBottom ) );

            Bitmap bitmap = Bitmap.createBitmap( plotCanvas.getWidth(), plotCanvas.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            int interval = (int) ( canvasHeight / (float) (plotCanvas.num_guides-1) );

            Log.i( LOG_TAG, String.format( "canvas: guides=%d interval=%d",
                    plotCanvas.num_guides, interval ) );

            for ( int y= canvasTop; y <= canvasBottom; y += interval )
                canvas.drawLine( canvasLeft, y, canvasRight, y, plotCanvas.guidePaint );

            for ( int i=1; i< trackpoints.size(); ++i )
            {
                float elevation1 = trackpoints.get(i-1).elevation;
                float x1 = canvasLeft + (i-1) * canvasWidth / (float) trackpoints.size();
                float y1 = canvasBottom - canvasHeight * (elevation1 - minElevation) / (maxElevation - minElevation);

                float elevation2 = trackpoints.get(i).elevation;
                float x2 = canvasLeft + i * canvasWidth / (float) trackpoints.size();
                float y2 = canvasBottom - canvasHeight * (elevation2 - minElevation) / (maxElevation - minElevation);

                canvas.drawLine( x1, y1, x2, y2, plotCanvas.linePaint );
            }

            long endTime = System.currentTimeMillis();
            Log.i( LOG_TAG, "drawing time=" + (endTime-startTime) );

            return bitmap;
        }

        @Override
        protected void onPostExecute( Bitmap bitmap )
        {
            listener.onBitmapReady(bitmap);
        }
    }
}
