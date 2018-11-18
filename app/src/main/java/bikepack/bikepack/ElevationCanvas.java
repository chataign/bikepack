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

    private final Paint line_paint;
    private final Paint guide_paint;
    private final int num_guides;
    private Bitmap bitmap=null;

    ElevationCanvas(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.setClickable(false);
        this.setSaveEnabled(true);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ElevationCanvas,0, 0);

        int line_color = attributes.getColor( R.styleable.ElevationCanvas_line_color, DEFAULT_LINE_COLOR );
        int line_width = attributes.getColor( R.styleable.ElevationCanvas_line_width, DEFAULT_LINE_WIDTH );
        int guide_color = attributes.getColor( R.styleable.ElevationCanvas_guide_color, DEFAULT_GUIDE_COLOR );
        int guide_width = attributes.getColor( R.styleable.ElevationCanvas_guide_width, DEFAULT_GUIDE_WIDTH );
        num_guides = attributes.getColor( R.styleable.ElevationCanvas_num_guides, DEFAULT_NUM_GUIDES );

        line_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        line_paint.setColor( line_color );
        line_paint.setStrokeWidth( line_width );

        guide_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guide_paint.setColor( guide_color );
        guide_paint.setStrokeWidth( guide_width );
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

    void setTrackpoints( @NonNull List<Trackpoint> trackpoints )
    {
        Log.i( LOG_TAG, String.format( "setTrackpoints: %d trackpoints", trackpoints.size() ) );

        DrawBitmap draw_bitmap = new DrawBitmap( this, trackpoints, new DrawBitmap.Listener()
        {
            @Override
            public void onBitmapReady( Bitmap bitmap )
            {
                Log.i( LOG_TAG, "onBitmapReady" );
                ElevationCanvas.this.bitmap = bitmap;
                ElevationCanvas.this.invalidate(); // calls onDraw()
            }
        } );

        draw_bitmap.execute();
    }

    static private class DrawBitmap extends AsyncTask< Void, Void, Bitmap >
    {
        interface Listener
        {
            void onBitmapReady( Bitmap bitmap );
        }

        final ElevationCanvas plot_canvas;
        final List<Trackpoint> trackpoints;
        final Listener listener;

        DrawBitmap(ElevationCanvas plot_canvas, List<Trackpoint> trackpoints, Listener listener )
        {
            this.plot_canvas = plot_canvas;
            this.trackpoints = trackpoints;
            this.listener = listener;
        }

        protected Bitmap doInBackground(Void... data)
        {
            long start_time = System.currentTimeMillis();

            float min_elevation = Float.MAX_VALUE;
            float max_elevation = Float.MIN_VALUE;

            for ( Trackpoint trackpoint: trackpoints )
            {
                min_elevation = Math.min( trackpoint.elevation, min_elevation );
                max_elevation = Math.max( trackpoint.elevation, max_elevation );
            }

            Log.i( LOG_TAG, String.format( "values: min=%.2f max=%.2f",
                    min_elevation, max_elevation ) );

            int canvas_left = plot_canvas.getPaddingLeft();
            int canvas_top = plot_canvas.getPaddingTop();
            int canvas_right = plot_canvas.getWidth() - plot_canvas.getPaddingRight();
            int canvas_bottom = plot_canvas.getHeight() - plot_canvas.getPaddingBottom();
            int canvas_width = canvas_right - canvas_left;
            int canvas_height = canvas_bottom - canvas_top;

            Log.i( LOG_TAG, String.format( "canvas: left=%d top=%d right=%d bottom=%d",
                    canvas_left, canvas_top, canvas_right, canvas_bottom ) );

            Bitmap bitmap = Bitmap.createBitmap( plot_canvas.getWidth(), plot_canvas.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            int interval = (int) ( canvas_height / (float) (plot_canvas.num_guides-1) );

            Log.i( LOG_TAG, String.format( "canvas: guides=%d interval=%d",
                    plot_canvas.num_guides, interval ) );

            for ( int y= canvas_top; y <= canvas_bottom; y += interval )
                canvas.drawLine( canvas_left, y, canvas_right, y, plot_canvas.guide_paint );

            for ( int i=1; i< trackpoints.size(); ++i )
            {
                float elevation1 = trackpoints.get(i-1).elevation;
                float x1 = canvas_left + (i-1) * canvas_width / (float) trackpoints.size();
                float y1 = canvas_bottom - canvas_height * (elevation1 - min_elevation) / (max_elevation - min_elevation);

                float elevation2 = trackpoints.get(i).elevation;
                float x2 = canvas_left + i * canvas_width / (float) trackpoints.size();
                float y2 = canvas_bottom - canvas_height * (elevation2 - min_elevation) / (max_elevation - min_elevation);

                canvas.drawLine( x1, y1, x2, y2, plot_canvas.line_paint );
            }

            long end_time = System.currentTimeMillis();
            Log.i( LOG_TAG, "drawing time=" + (end_time-start_time) );

            return bitmap;
        }

        @Override
        protected void onPostExecute( Bitmap bitmap )
        {
            listener.onBitmapReady(bitmap);
        }
    }
}
