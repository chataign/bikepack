package bikepack.bikepack;

class DownloadProgress
{
    final int numTilesDowloaded;
    final int numTilesTotal;
    final double totalSizeMb;
    final double downloadTime;
    final double writeTime;
    final double totalTime;

    DownloadProgress( int numTilesDowloaded, int numTilesTotal, double totalSizeMb, double downloadTime, double writeTime, double totalTime )
    {
        this.numTilesDowloaded = numTilesDowloaded;
        this.numTilesTotal = numTilesTotal;
        this.totalSizeMb = totalSizeMb;
        this.downloadTime = downloadTime;
        this.writeTime = writeTime;
        this.totalTime = totalTime;
    }

    int getPercent() { return (int)(100*numTilesDowloaded/(float)numTilesTotal); }
}
