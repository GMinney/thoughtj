package org.bitcoinj.crypto.cuckoo;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bitcoinj.core.Block;
import org.bitcoinj.crypto.cuckoo.data.BlockImpl;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.BlockTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Poller extends Observable implements Runnable
{
    protected long                   retryPause      = 10000;
    protected ThoughtClientInterface client;

    protected AtomicBoolean          moreElectricity = new AtomicBoolean(false);
    protected Work                   currentWork     = null;
    protected long                   currentHeight   = 0;
    protected int[]                  workMutex       = new int[0];

    protected List<Integer>          voteBits;

    private static final Logger log = LoggerFactory.getLogger(Poller.class);

    public Poller(ThoughtClientInterface client)
    {
        this.client = client;
    }

    public Poller(ThoughtClientInterface client, List<Integer> voteBits)
    {
        this.client = client;
        this.voteBits = voteBits;
    }

    public synchronized void shutdown()
    {
        moreElectricity.set(false);
        this.notifyAll();
    }

    public synchronized Work getWork()
    {
        Work retval = null;

        retval = currentWork;

        return retval;
    }

    public long getRetryPause()
    {
        return retryPause;
    }

    @Override
    public void run()
    {
        log.debug("Starting poller.", 2);
        moreElectricity.set(true);
        boolean notified = false;
        // Make initial connection for poller
        BlockTemplate bl = null;
        String longpollid = null;

        while (moreElectricity.get())
        {
            try
            {
                bl = client.getBlockTemplate(longpollid);
                longpollid = bl.longpollid();
                if (null != bl.longpollid())
                {
                    if (!notified)
                    {
                        setChanged();
                        notifyObservers(Notification.LONG_POLLING_ENABLED);
                        notified = true;
                    }
                    if (bl.height() > currentHeight)
                    {
                        setChanged();
                        notifyObservers(Notification.NEW_BLOCK_DETECTED);
                        log.debug(String.format("@|cyan Current block is %d|@", bl.height()));

                        Work w = new Work(bl);
                        if (null != voteBits)
                        {
                            BlockImpl b = w.getBlock();
                            for (int i : voteBits)
                            {
                                b.addVoteBit(i);
                            }
                        }
                        synchronized (workMutex)
                        {
                            currentWork = w;
                            currentHeight = bl.height();
                        }
                        setChanged();
                        notifyObservers(Notification.NEW_WORK);
                    }
                }
                else
                {
                    setChanged();
                    notifyObservers(Notification.LONG_POLLING_FAILED);
                }
            }
            catch (Exception e)
            {
                setChanged();
                if (e instanceof IllegalArgumentException)
                {
                    notifyObservers(Notification.AUTHENTICATION_ERROR);
                    shutdown();
                    break;
                }
                else if (e instanceof IllegalStateException)
                {
                    notifyObservers(Notification.PERMISSION_ERROR);
                    shutdown();
                    break;
                }
                else if (e instanceof IOException)
                {
                    notifyObservers(Notification.CONNECTION_ERROR);
                }
                else
                {
                    //e.printStackTrace();
                    notifyObservers(Notification.COMMUNICATION_ERROR);
                }
                try
                {
                    currentWork = null;
                    Thread.sleep(retryPause);
                }
                catch (InterruptedException ie)
                {
                }
            }
        }
    }

}