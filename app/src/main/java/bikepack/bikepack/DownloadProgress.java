package bikepack.bikepack;

import bikepack.bikepack.databinding.DowloadProgressBinding;

class DownloadProgress
{
    private  static final String LOG_TAG = "DownloadProgress";

    final int numTilesDowloaded;
    final int numTilesTotal;
    final float downloadedSizeMb;
    final long downloadTimeMs;
    final long writeTimeMs;
    final long totalTimeMs;

    DownloadProgress(int numTilesDowloaded, int numTilesTotal, float downloadedSizeMb, long downloadTimeMs, long writeTimeMs, long totalTimeMs) {
        this.numTilesDowloaded = numTilesDowloaded;
        this.numTilesTotal = numTilesTotal;
        this.downloadedSizeMb = downloadedSizeMb;
        this.downloadTimeMs = downloadTimeMs;
        this.writeTimeMs = writeTimeMs;
        this.totalTimeMs = totalTimeMs;
    }

    int getPercent()
    {
        return (int) (100 * numTilesDowloaded / (float) numTilesTotal);
    }

    float getEstimatedTime() // seconds
    {
        return (numTilesTotal - numTilesDowloaded) * (totalTimeMs/1000) / numTilesDowloaded;
    }

    float getEstimatedSizeMb()
    {
        return numTilesTotal * downloadedSizeMb / numTilesDowloaded;
    }

    void populateDialog( DowloadProgressBinding dialog )
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
