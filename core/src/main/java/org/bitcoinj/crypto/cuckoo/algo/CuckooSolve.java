package org.bitcoinj.crypto.cuckoo.algo;

import java.util.HashSet;
import java.util.Set;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.cuckoo.algo.Cuckoo;
import org.bitcoinj.crypto.cuckoo.algo.Edge;

public class CuckooSolve
{
    public static final int MAXPATHLEN = 4096;
    Cuckoo                  graph;
    int                     easiness;
    int[]                   cuckoo;
    int                     nthreads;

    public CuckooSolve(byte[] hdr, int en, int nt)
    {
        graph = new Cuckoo(hdr);
        easiness = en;
        cuckoo = new int[1 + (int) Cuckoo.nNodes];
        assert cuckoo != null;
        nthreads = nt;
    }

    public int path(int u, int[] us)
    {
        int nu;
        for (nu = 0; u != 0; u = cuckoo[u])
        {
            if (++nu >= MAXPATHLEN)
            {
                String msg = null;
                while (nu-- != 0 && us[nu] != u)
                    ;
                if (nu < 0)
                    msg = "maximum path length exceeded";
                else
                    msg = "illegal " + (MAXPATHLEN - nu) + "-cycle";
                throw new RuntimeException(msg);
            }
            us[nu] = u;
        }
        return nu;
    }

    public synchronized int[] solution(int[] us, int nu, int[] vs, int nv)
    {
        int[] retval = null;
        int[] sol = new int[NetworkParameters.CUCKOO_PROOF_SIZE];
        Set<Edge> cycle = new HashSet<Edge>();
        int n;
        cycle.add(new Edge(us[0], vs[0] - Cuckoo.nEdges));
        while (nu-- != 0) // u's in even position; v's in odd
            cycle.add(new Edge(us[(nu + 1) & ~1], us[nu | 1] - Cuckoo.nEdges));
        while (nv-- != 0) // u's in odd position; v's in even
            cycle.add(new Edge(vs[nv | 1], vs[(nv + 1) & ~1] - Cuckoo.nEdges));
        for (int nonce = n = 0; nonce < easiness; nonce++)
        {
            Edge e = graph.sipedge(nonce);
            if (cycle.contains(e))
            {
                sol[n++] = nonce;
                cycle.remove(e);
            }
        }
        if (n == NetworkParameters.CUCKOO_PROOF_SIZE)
        {
            retval = sol;
        }
        // else
        // System.out.println("Only recovered " + n + " nonces");
        return retval;
    }

    public int getEasiness()
    {
        return easiness;
    }

    public int[] getCuckoo()
    {
        return cuckoo;
    }

    public int getNthreads()
    {
        return nthreads;
    }

    public Cuckoo getGraph()
    {
        return graph;
    }
}