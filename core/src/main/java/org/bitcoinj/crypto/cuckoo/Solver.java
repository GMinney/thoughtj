package org.bitcoinj.crypto.cuckoo;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.cuckoo.algo.SHA256d;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface;
import org.bitcoinj.crypto.cuckoo.algo.Cuckoo;
import org.bitcoinj.crypto.cuckoo.algo.CuckooSolve;
import org.bitcoinj.crypto.cuckoo.algo.SHA256d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Solver extends Observable implements Observer, Runnable
{
    private int                    index;
    private CuckooSolve solve;
    private Work                   curWork;
    private SHA256d hasher             = new SHA256d(32);
    private AtomicBoolean          stop               = new AtomicBoolean();

    public Solver(Work curWork, int index, CuckooSolve solve)
    {
        this.solve = solve;
        this.index = index;
        this.curWork = curWork;
        stop.set(false);
    }

    private static final Logger log = LoggerFactory.getLogger(Solver.class);

    public synchronized void stop()
    {
        log.debug("Stopping solver " + index, 2);
        this.stop.set(true);
    }

    public void cleanup()
    {
        Miner.getInstance().getPoller().deleteObserver(this);
        deleteObserver(Miner.getInstance());
    }

    public void run()
    {
        log.debug("Starting solver " + index, 2);
        int[] cuckoo = solve.getCuckoo();
        int[] us = new int[CuckooSolve.MAXPATHLEN], vs = new int[CuckooSolve.MAXPATHLEN];
        try
        {
            for (int nonce = index; nonce < solve.getEasiness(); nonce += solve.getNthreads())
            {
                if (stop.get())
                {
                    Thread.currentThread().interrupt();
                    break;
                }
                int u = cuckoo[us[0] = (int) solve.getGraph().sipnode(nonce, 0)];
                int v = cuckoo[vs[0] = (int) (Cuckoo.nEdges + solve.getGraph().sipnode(nonce, 1))];
                if (u == vs[0] || v == us[0])
                    continue; // ignore duplicate edges
                int nu = solve.path(u, us), nv = solve.path(v, vs);
                if (us[nu] == vs[nv])
                {
                    int min = nu < nv ? nu : nv;
                    for (nu -= min, nv -= min; us[nu] != vs[nv]; nu++, nv++)
                        ;
                    int len = nu + nv + 1;
                    Miner.getInstance().incrementCycles();
                    if (len == NetworkParameters.CUCKOO_PROOF_SIZE)
                    {
                        int[] soln = solve.solution(us, nu, vs, nv);
                        if (null != soln)
                        {
                            Miner.getInstance().incrementSolutions();
                            try
                            {
                                if (solve.getGraph().verify(soln, Cuckoo.nNodes))
                                {
                                    if (curWork.meetsTarget(index, soln, hasher))
                                    {
                                        setChanged();
                                        notifyObservers(Notification.POW_TRUE);
                                        stop();
                                        break;
                                    }
                                }
                                else
                                {
                                    Miner.getInstance().incrementErrors();
                                }
                            }
                            catch (GeneralSecurityException e)
                            {
                                log.debug(e.toString());
                            }
                        }
                    }
                    continue;
                }
                if (nu < nv)
                {
                    while (nu-- != 0)
                        cuckoo[us[nu + 1]] = us[nu];
                    cuckoo[us[0]] = vs[0];
                }
                else
                {
                    while (nv-- != 0)
                        cuckoo[vs[nv + 1]] = vs[nv];
                    cuckoo[vs[0]] = us[0];
                }
            }
        }
        catch (RuntimeException re)
        {
            log.debug("Illegal cycle.", 2);
        }
        cleanup();
        log.debug("Exiting solver " + index, 2);
    }

    @Override
    public void update(Observable o, Object arg)
    {
        Notification n = (Notification) arg;
        if (n == Notification.NEW_WORK)
        {
            stop();
        }
    }
}