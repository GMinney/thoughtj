package org.bitcoinj.crypto.cuckoo;

import org.bitcoinj.core.Block;
import org.bitcoinj.crypto.cuckoo.algo.Cuckoo;
import org.bitcoinj.crypto.cuckoo.data.BlockImpl;
import org.bitcoinj.crypto.cuckoo.algo.CuckooSolve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Miner implements Observer {
    /** RELEASE VERSION */
    public static final String               VERSION             = "v0.4";

    /** Longpoll client */
    private Poller                           poller;
    /** Performance metrics */
    private long                             lastWorkTime        = 0L;
    private long                             lastWorkCycles      = 0L;
    private long                             lastWorkSolutions   = 0L;
    private long                             lastWorkErrors;
    private AtomicLong                       cycles              = new AtomicLong(0L);
    private AtomicLong                       errors              = new AtomicLong(0L);
    private AtomicLong                       solutions           = new AtomicLong(0L);
    private long                             attempted           = 0L;
    private long                             accepted            = 0L;

    /** Work in progress */
    private volatile Work                    curWork             = null;
    private AtomicInteger                    cycleIndex          = new AtomicInteger(0);

    /** Single instance */
    private static Miner                     instance;
    /** Control printing of debug messages */
    private static int                       debugLevel          = 1;

    /** Runtime parameters */
    protected String                         coinbaseAddr;
    protected boolean                        moreElectricity;
    protected int                            nThreads;
    protected String                         script;


    private static final Logger log = LoggerFactory.getLogger(Block.class);

    protected Miner(String coinbase, int nThread, String script)
    {
        log.debug(String.format("@|bg_blue,fg_white jtminer %s: A Java block miner for Thought Network.|@", VERSION));
        Miner.instance = this;
        if (nThread < 1)
        {
            throw new IllegalArgumentException("Invalid number of threads: " + nThread);
        }
        else
        {
            this.nThreads = nThread;
        }
        this.coinbaseAddr = coinbase;
        this.script = script;


        // Set up timer for performance metric reporting
        TimerTask reporter = new TimerTask()
        {
            public void run()
            {
                report();
            }
        };
        Timer timer = new Timer("Timer");

        long delay = 15000L;
        long period = 15000L;
        timer.scheduleAtFixedRate(reporter, delay, period);
    }

    protected void report()
    {
        if (lastWorkTime > 0L)
        {
            long currentCycles = cycles.get() - lastWorkCycles;
            long currentErrors = errors.get() - lastWorkErrors;
            long currentSolutions = solutions.get() - lastWorkSolutions;
            float speed = (float) currentCycles / Math.max(1, System.currentTimeMillis() - lastWorkTime);
            log.debug(String.format("%d cycles, %d solutions, %d errors, %.2f kilocycles/sec", currentCycles, currentSolutions,
                    currentErrors, speed));
        }
        lastWorkTime = System.currentTimeMillis();
        lastWorkCycles = cycles.get();
        lastWorkErrors = errors.get();
        lastWorkSolutions = solutions.get();
    }

    public static Miner getInstance()
    {
        return instance;
    }

    public int getDebugLevel()
    {
        return debugLevel;
    }

    public Poller getPoller()
    {
        return poller;
    }

    public String getCoinbaseAddress()
    {
        return coinbaseAddr;
    }

    public String getScript()
    {
        return script;
    }

    public void incrementCycles()
    {
        cycles.incrementAndGet();
    }

    public void incrementSolutions()
    {
        solutions.incrementAndGet();
    }

    public void incrementErrors()
    {
        errors.incrementAndGet();
    }

    public void update(Observable o, Object arg)
    {
        Notification n = (Notification) arg;
        if (n == Notification.SYSTEM_ERROR)
        {
            log.debug("@|red System error|@");
            moreElectricity = false;
        }
        else if (n == Notification.PERMISSION_ERROR)
        {
            log.debug("@|red Permission Error|@");
            moreElectricity = false;
        }
        else if (n == Notification.AUTHENTICATION_ERROR)
        {
            log.debug("@|red Invalid worker username or password|@");
            moreElectricity = false;
        }
        else if (n == Notification.TERMINATED)
        {
            log.debug("@|red Poller terminated. Exiting.|@");
            moreElectricity = false;
        }
        else if (n == Notification.CONNECTION_ERROR)
        {
            log.debug("@|yellow Connection error, retrying in " + poller.getRetryPause() / 1000L + " seconds|@");
        }
        else if (n == Notification.COMMUNICATION_ERROR)
        {
            log.debug("@|red Communication error|@");
        }
        else if (n == Notification.LONG_POLLING_FAILED)
        {
            log.debug("@|red Long polling failed|@");
        }
        else if (n == Notification.LONG_POLLING_ENABLED)
        {
            log.debug("@|bold,white Long polling activated|@");
        }
        else if (n == Notification.NEW_BLOCK_DETECTED)
        {
            log.debug("LONGPOLL detected new block", 2);
        }
        else if (n == Notification.POW_TRUE)
        {
            attempted++;
            accepted++;
            log.debug(String.format("@|bold,white Accepted block %d of %d|@ @|bold,green (yay!!!)|@", accepted, attempted));
        }
        else if (n == Notification.POW_FALSE)
        {
            attempted++;
            log.debug(String.format("@|white Rejected block attempt %d|@ @|bold,red (boo...)|@", attempted));
        }
        else if (n == Notification.NEW_WORK)
        {
            log.debug("Getting new work.", 2);
            curWork = getPoller().getWork();
            if (null != curWork)
            {
                log.debug("New work retrieved.", 2);
            }
            cycleIndex.set(0);
        }
    }

    public void run()
    {

    log.debug("Using " + nThreads + " threads.");

        ArrayList<Thread> threads = new ArrayList<Thread>(nThreads);
        if (null != curWork)
        {
            log.debug(String.format("Target: %064x", curWork.getTarget()), 2);
            log.debug("Starting " + nThreads + " solvers.", 2);

            BlockImpl block = curWork.getBlock();
            int blockNonce = cycleIndex.getAndIncrement();
            block.setNonce(blockNonce);
            CuckooSolve solve = new CuckooSolve(block.getHeader(), Cuckoo.nNodes, nThreads);
            for (int n = 0; n < nThreads; n++)
            {
                Solver solver = new Solver(curWork, cycleIndex.getAndIncrement(), solve);
                solver.addObserver(Miner.getInstance());
                Miner.getInstance().getPoller().addObserver(solver);

                Thread t = new Thread(solver);
                threads.add(t);
                t.start();
            }

            for (Thread t : threads)
            {
                try
                {
                    t.join();
                }
                catch (InterruptedException e)
                {
                    // Swallow
                }
            }
        }

    }
}
