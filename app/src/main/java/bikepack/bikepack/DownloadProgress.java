package bikepack.bikepack;

import bikepack.bikepack.databinding.DownloadMapActivityBinding;

class DownloadProgress
{
    private  static final String LOG_TAG = "DownloadProgress";

    final int numTilesTotal;
    int numTilesDowloaded=0;
    float downloadedSizeMb=0;
    long downloadTimeMs=0;
    long writeTimeMs=0;
    long totalTimeMs=0;

    DownloadProgress(int numTilesTotal)
    {
        this.numTilesTotal = numTilesTotal;
    }

    int getPercent()
    {
        if ( numTilesTotal == 0 ) return 100;
        return (int) (100 * numTilesDowloaded / (float) numTilesTotal);
    }

    float getEstimatedTime() // seconds
    {
        if ( numTilesDowloaded == 0 || numTilesDowloaded > numTilesTotal ) return 0;
        return (numTilesTotal - numTilesDowloaded) * (totalTimeMs/1000) / numTilesDowloaded;
    }

    float getEstimatedSizeMb()
    {
        if ( numTilesDowloaded == 0 ) return 0;
        return numTilesTotal * downloadedSizeMb / numTilesDowloaded;
    }

    void populateDialog( DownloadMapActivityBinding dialog )
    {
        int downloadTimePercent = (int)( 100*downloadTimeMs/(float)totalTimeMs );
        int writeTimePercent = (int)( 100*writeTimeMs/(float)totalTimeMs );

        dialog.progressPercent.setText( String.format( "%d%%", getPercent() ) );
        dialog.progressPercentBar.setProgress( getPercent() );
        dialog.progressMessage.setText( String.format( "Downloaded %d/%d tiles",
                numTilesDowloaded, numTilesTotal ) );

        dialog.estimatedTime.setText( String.format("Estimated time: %s",
                StringFormatter.formatDuration( getEstimatedTime() ) ) );

        dialog.estimatedSize.setText( String.format("Estimated size: %s",
                StringFormatter.formatFileSize( getEstimatedSizeMb() ) ) );

        dialog.downloadLoadPercent.setText( String.format( "%d%%", downloadTimePercent ) );
        dialog.downloadPercentBar.setProgress( downloadTimePercent);

        dialog.writeLoadPercent.setText( String.format( "%d%%", writeTimePercent ) );
        dialog.writeLoadBar.setProgress( writeTimePercent );
    }
}
