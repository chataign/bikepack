package bikepack.bikepack;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.List;

public class ElevationCanvas extends View
{
    static private final String LOG_TAG = "ElevationCanvas";

    private final Paint linePaint;
    private final Paint guidePaint;
    private final int num_guides;
    private Bitmap bitmap=null;
    private DrawBitmapTask drawBitmapTask=null;

    ElevationCanvas(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.setClickable(false);
        this.setSaveEnabled(true);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ElevationCanvas,0, 0);

        final int DEFAULT_LINE_COLOR = Color.RED;
        final int DEFAULT_LINE_WIDTH = 2;
        final int DEFAULT_GUIDE_COLOR = Color.GRAY;
        final int DEFAULT_GUIDE_WIDTH = 1;
        final int DEFAULT_NUM_GUIDES = 4;

        int lineColor = attributes.getColor( R.styleable.ElevationCanvas_line_color, DEFAULT_LINE_COLOR);
        int lineWidth = attributes.getColor( R.styleable.ElevationCanvas_line_width, DEFAULT_LINE_WIDTH);
        int guideColor = attributes.getColor( R.styleable.ElevationCanvas_guide_color, DEFAULT_GUIDE_COLOR );
        int guideWidth = attributes.getColor( R.styleable.ElevationCanvas_guide_width, DEFAULT_GUIDE_WIDTH );
        num_guides = attributes.getColor( R.styleable.ElevationCanvas_num_guides, DEFAULT_NUM_GUIDES );

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor( lineColor );
        linePaint.setStrokeWidth( lineWidth );

        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setStyle(Paint.Style.STROKE);
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

    void drawTrackpoints( @NonNull List<Trackpoint> trackpoints )
    {
        drawBitmapTask = new DrawBitmapTask( this, trackpoints, new DrawBitmapTask.Listener()
        {
            @Override
            public void onBitmapReady( Bitmap bitmap )
            {
                //Log.i( LOG_TAG, "onBitmapReady" );
                ElevationCanvas.this.bitmap = bitmap;
                ElevationCanvas.this.invalidate(); // calls onDraw()
            }
        } );

        drawBitmapTask.execute();
    }

    @Override
    protected void onDetachedFromWindow ()
    {
        if ( drawBitmapTask != null ) drawBitmapTask.cancel(true);
        super.onDetachedFromWindow();
    }

    static private class DrawBitmapTask extends AsyncTask< Void, Void, Bitmap >
    {
        interface Listener
        {
            void onBitmapReady( Bitmap bitmap );
        }

        private final ElevationCanvas plotCanvas;
        private final List<Trackpoint> trackpoints;
        private final Listener listener;

        DrawBitmapTask(ElevationCanvas plotCanvas, List<Trackpoint> trackpoints, Listener listener )
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
                if ( isCancelled() ) return null;
                minElevation = Math.min( trackpoint.elevation, minElevation );
                maxElevation = Math.max( trackpoint.elevation, maxElevation );
            }

            //Log.i( LOG_TAG, String.format( "values: min=%.2f max=%.2f",
            //        minElevation, maxElevation ) );

            int canvasLeft = plotCanvas.getPaddingLeft();
            int canvasTop = plotCanvas.getPaddingTop();
            int canvasRight = plotCanvas.getWidth() - plotCanvas.getPaddingRight();
            int canvasBottom = plotCanvas.getHeight() - plotCanvas.getPaddingBottom();
            int canvasWidth = canvasRight - canvasLeft;
            int canvasHeight = canvasBottom - canvasTop;

            //Log.i( LOG_TAG, String.format( "canvas: left=%d top=%d right=%d bottom=%d",
            //        canvasLeft, canvasTop, canvasRight, canvasBottom ) );

            Bitmap bitmap = Bitmap.createBitmap( plotCanvas.getWidth(), plotCanvas.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            int interval = (int) ( canvasHeight / (float) (plotCanvas.num_guides-1) );

            //Log.i( LOG_TAG, String.format( "canvas: guides=%d interval=%d",
            //        plotCanvas.num_guides, interval ) );

            for ( int y= canvasTop; y <= canvasBottom; y += interval )
                canvas.drawLine( canvasLeft, y, canvasRight, y, plotCanvas.guidePaint );

            Path elevationPath = new Path();

            for ( int i=0; i< trackpoints.size(); ++i )
            {
                if ( isCancelled() ) return null;
                float elevation = trackpoints.get(i).elevation;

                float x = canvasLeft + i*canvasWidth / (float) trackpoints.size();
                float y = canvasBottom - canvasHeight * (elevation - minElevation) / (maxElevation - minElevation);

                if ( i == 0 ) elevationPath.moveTo(x,y);
                else elevationPath.lineTo(x,y);
            }

            canvas.drawPath( elevationPath, plotCanvas.linePaint );

            long endTime = System.currentTimeMillis();

            Log.i( LOG_TAG, String.format( "drawn %d trackpoints in %dms",
                    trackpoints.size(), endTime-startTime ) );

            return bitmap;
        }

        @Override
        protected void onPostExecute( Bitmap bitmap )
        {
            listener.onBitmapReady(bitmap);
        }
    }
}
