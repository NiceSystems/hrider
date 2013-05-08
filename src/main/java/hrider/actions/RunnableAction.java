package hrider.actions;

import hrider.io.Log;
import org.apache.commons.lang.time.StopWatch;

/**
 * Copyright (C) 2012 NICE Systems ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Igor Cher
 * @version %I%, %G%
 *          <p/>
 *          This class represents a runnable action.
 */
public class RunnableAction<R> implements Runnable {

    //region Constants
    private final static Log logger = Log.getLogger(RunnableAction.class);
    //endregion

    //region Variables
    private String    name;
    private Action<R> action;
    private Thread    thread;
    private boolean   isRunning;
    private boolean   interrupted;
    private R         result;
    //endregion

    //region Constructor
    public RunnableAction(String name, Action<R> action) {
        this.name = name;
        this.action = action;
    }
    //endregion

    //region Public Properties
    public R getResult() {
        return result;
    }

    public boolean isCompleted() {
        return !isRunning && !interrupted;
    }

    public boolean isRunning() {
        return isRunning;
    }
    //endregion

    //region Public Methods
    public static <R> RunnableAction<R> run(String name, Action<R> action) {
        RunnableAction<R> runnableAction = new RunnableAction<R>(name, action);
        runnableAction.start();

        return runnableAction;
    }

    public static <R> R runAndWait(String name, Action<R> action, long timeout) {
        RunnableAction<R> runnableAction = new RunnableAction<R>(name, action);
        runnableAction.start();

        runnableAction.waitOrAbort(timeout);
        return runnableAction.result;
    }

    public void start() {
        if (this.thread == null) {
            this.thread = new Thread(this);
            this.thread.setName(this.name);
            this.thread.setDaemon(true);
            this.thread.start();

            this.isRunning = true;

            logger.info("Action '%s' started.", this.name);
        }
    }

    public void abort() {
        if (this.isRunning) {
            this.interrupted = true;

            this.thread.interrupt();
            this.thread = null;
        }
    }

    public void waitUntil(long timeout) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        long localTimeout = timeout / 10;

        while (this.isRunning) {
            try {
                Thread.sleep(localTimeout);

                if (stopWatch.getTime() > timeout) {
                    break;
                }
            }
            catch (InterruptedException ignore) {
            }
        }

        stopWatch.stop();
    }

    public void waitOrAbort(long timeout) {
        waitUntil(timeout);

        if (this.isRunning) {
            abort();
        }
    }

    @Override
    public void run() {
        try {
            this.result = action.run();
            this.isRunning = false;

            logger.info("Action '%s' completed.", this.name);
        }
        catch (Exception e) {
            this.isRunning = false;

            if (this.interrupted) {
                logger.info("Action '%s' aborted.", this.name);
            }
            else {
                logger.info("Action '%s' failed.", this.name);
                this.action.onError(e);
            }
        }
    }
    //endregion
}
