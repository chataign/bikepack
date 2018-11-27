package bikepack.bikepack;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

class FileData
{
    final File file;
    final long fileSize; // bytes

    FileData( File file )
    {
        this.file = file;
        this.fileSize = file.length();
    }
}

class DateComparator implements Comparator<FileData>
{
    public int compare( FileData file1, FileData file2 )
    {
        if ( file1.file.lastModified() < file2.file.lastModified() ) return -1;
        if ( file1.file.lastModified() > file2.file.lastModified() ) return +1;
        return 0;
    }
}

class CreationTask extends AsyncTask< Void, Void, List<FileData> >
{
    interface Listener
    {
        void onStorageReady(List<FileData> files, float totalSizeMb );
    }

    private final File fileDirectory;
    private final String filePrefix;
    private final Listener listener;

    CreationTask( File fileDirectory, String filePrefix, Listener listener )
    {
        this.fileDirectory = fileDirectory;
        this.filePrefix = filePrefix;
        this.listener = listener;
    }

    @Override
    protected List<FileData> doInBackground(Void ... e )
    {
        long startTime = System.currentTimeMillis();
        List<FileData> files = new ArrayList<>();

        for ( File file : fileDirectory.listFiles() )
        {
            if ( filePrefix.isEmpty() || file.getName().startsWith(filePrefix) )
                files.add( new FileData(file) );
        }

        Collections.sort( files, new DateComparator() );

        long endTime = System.currentTimeMillis();
        Log.i( "FileStorageManager", String.format( "created in %dms", endTime-startTime ) );
        return files;
    }

    @Override
    protected void onPostExecute( List<FileData> files )
    {
        float totalSizeMb=0;
        for ( FileData file : files ) totalSizeMb += file.fileSize / 1e6;

        if ( listener != null )
            listener.onStorageReady( files, totalSizeMb );
    }
}

class FileStorageManager implements CreationTask.Listener
{
    private static final String LOG_TAG = "FileStorageManager";

    private final Queue<FileData> fileQueue = new ArrayDeque<>();
    private final float maxStorageMb;
    private double totalSizeMb=0;
    private CreationTask creationTask=null;

    FileStorageManager( final File fileDirectory, final float maxStorageMb, final String filePrefix )
    {
        this.maxStorageMb = maxStorageMb;
        this.creationTask = new CreationTask( fileDirectory, filePrefix, this );
        this.creationTask.execute();
    }

    @Override
    public void onStorageReady( List<FileData> files, float totalSizeMb )
    {
        this.fileQueue.addAll(files);
        this.totalSizeMb = totalSizeMb;

        Log.i( LOG_TAG, String.format( "files=%d size=%s",
            files.size(), StringFormatter.formatFileSize(totalSizeMb) ) );
    }

    int getFileCount() { return fileQueue.size(); }
    double getTotalSizeMb() { return totalSizeMb; }

    void add( File file )
    {
        double fileMb = file.length() / 1e6;

        while( !fileQueue.isEmpty() && ( totalSizeMb + fileMb ) > maxStorageMb )
        {
            FileData oldest = fileQueue.remove();
            double oldestMb = oldest.fileSize / 1e6;
            Log.i( LOG_TAG, "deleting=" + oldest.file.getName() + " with size=" + oldestMb + "Mb" );
            if ( oldest.file.delete() == false ) Log.e( LOG_TAG, "failed to delete file=" + oldest.file.getName() );
            totalSizeMb -= oldestMb;
        }

        fileQueue.add( new FileData(file) );
        totalSizeMb += fileMb;

        Log.i( LOG_TAG, String.format( "added=%s, storage: %.1f/%.1fMb (%d%%)",
                file.getName(), totalSizeMb, maxStorageMb,
                Math.round(100*totalSizeMb/maxStorageMb) ) );
    }
}
