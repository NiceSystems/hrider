package hrider.io;

import java.io.Closeable;
import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public abstract class PathWatcher extends TimerTask implements Closeable {

    private       long  timeStamp;
    private final File  path;
    private final Timer timer;

    protected PathWatcher(File path) {
        this.path = path;
        this.timeStamp = path.lastModified();
        this.timer = new Timer();
    }

    public void start(long period) {
        this.timer.schedule(this, new Date(), period);
    }

    @Override
    public final void run() {
        long timestamp = this.path.lastModified();

        if (this.timeStamp != timestamp) {
            this.timeStamp = timestamp;

            onChange(this.path);
        }
    }

    @Override
    public void close() {
        this.timer.cancel();
    }

    protected abstract void onChange(File file);
}
