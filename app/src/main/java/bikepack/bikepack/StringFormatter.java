package bikepack.bikepack;

public class StringFormatter {
    static String formatDistance(float distanceMeters, boolean forceMeters) {
        if (forceMeters || distanceMeters < 1000)
            return String.format("%d m", Math.round(distanceMeters));
        if (distanceMeters < 20) return String.format("%.1f km", distanceMeters / 1000);
        return String.format("%d km", Math.round(distanceMeters / 1000));
    }

    static String formatDuration(float durationSeconds) {
        int hours = (int) (durationSeconds / 3600);
        int minutes = (int) ((durationSeconds - hours * 3600) / 60);
        int seconds = (int) (durationSeconds - hours * 3600 - minutes * 60);

        if (hours > 0) return String.format("%dh %d minutes", hours, minutes);
        else if (minutes > 0) return String.format("%d minutes", minutes);
        else return String.format("%d seconds", seconds);
    }

    static String formatFileSize(float sizeMb) {
        if (sizeMb > 1e3) return String.format("%.1f Gb", sizeMb / 1e3);
        if (sizeMb > 10) return String.format("%d Mb", (int)sizeMb);
        if (sizeMb > 0) return String.format("%.1f Mb", sizeMb);
        return String.format("%d Kb", (int) (sizeMb*1e3));
    }
}