package bikepack.bikepack;

import android.util.Log;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

class FileStorageManager
{
    private static final String LOG_TAG = "FileStorageManager";

    private final float maxStorageMb;
    private final Queue<File> fileQueue = new ArrayDeque<>();
    private double sizeMb=0;

    private class DateComparator implements Comparator<File>
    {
        public int compare( File file1, File file2 )
        {
            if ( file1.lastModified() < file2.lastModified() ) return -1;
            if ( file1.lastModified() > file2.lastModified() ) return +1;
            return 0;
        }
    }

    FileStorageManager( float maxStorageMb, final List<File> files )
    {
        Collections.sort( files, new DateComparator() );

        this.maxStorageMb = maxStorageMb;
        this.fileQueue.addAll(files);

        for ( File file : files )
            sizeMb += file.length() / 1e6;
    }

    int getFileCount() { return fileQueue.size(); }
    double getSizeMb() { return sizeMb; }

    void add( File file )
    {
        double fileMb = file.length() / 1e6;

        while( !fileQueue.isEmpty() && ( sizeMb + fileMb ) > maxStorageMb )
        {
            File oldestFile = fileQueue.remove();
            double oldestMb = oldestFile.length() / 1e6;
            Log.i( LOG_TAG, "deleting=" + oldestFile.getName() + " with size=" + oldestMb + "Mb" );
            if ( oldestFile.delete() == false ) Log.e( LOG_TAG, "failed to delete file=" + oldestFile.getName() );
            sizeMb -= oldestMb;
        }

        fileQueue.add(file);
        sizeMb += fileMb;

        Log.i( LOG_TAG, String.format( "added=%s, storage: %.1f/%.1fMb (%d%%)",
                file.getName(), sizeMb, maxStorageMb,
                Math.round(100*sizeMb/maxStorageMb) ) );
    }
}
