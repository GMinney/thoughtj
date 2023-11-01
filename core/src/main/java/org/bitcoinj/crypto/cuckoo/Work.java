package org.bitcoinj.crypto.cuckoo;


import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bitcoinj.crypto.cuckoo.algo.SHA256d;
import org.bitcoinj.crypto.cuckoo.data.BlockImpl;
import org.bitcoinj.crypto.cuckoo.data.CoinbaseTransaction;
import org.bitcoinj.crypto.cuckoo.data.DataUtils;
import org.bitcoinj.crypto.cuckoo.data.PaymentObject;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.BlockTemplate;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.Masternode;
import org.bitcoinj.crypto.cuckoo.algo.SHA256d;
import org.bitcoinj.core.Block;

public class Work
{
    private static final Logger LOG = Logger.getLogger(Work.class.getCanonicalName());

    long                        height;
    private BigInteger          target;

    private BlockImpl           block;
    private CoinbaseTransaction coinbaseTransaction;
    SHA256d localHasher    = new SHA256d(32);


    public Work(BlockTemplate blt)
    {
        height = blt.height();
        block = new BlockImpl(blt);
        coinbaseTransaction = new CoinbaseTransaction(blt.height(), blt.coinbasevalue(), Miner.getInstance().getCoinbaseAddress());
        if (null != Miner.getInstance().getScript())
        {
            coinbaseTransaction.setCoinbaseScript(Miner.getInstance().getScript().getBytes());
        }

        try
        {
            if (blt.masternode_payments_started())
            {
                List<Masternode> outputs = blt.masternode();
                for (Masternode m : outputs)
                {
                    PaymentObject p = new PaymentObject();
                    p.setPayee(m.payee());
                    p.setScript(m.script());
                    p.setValue(m.amount());
                    coinbaseTransaction.addExtraPayment(p);
                }
            }
        }
        catch (Exception e)
        {
            // Not thought_dash daemon, so ignore this error
        }

        try
        {
            String payload = blt.coinbase_payload();
            if (null != payload && payload.length() > 0)
            {
                coinbaseTransaction.setExtraPayload(payload);
            }
        }
        catch (Exception e)
        {
            // Not thought_dash daemon, so ignore this error
        }

        block.setCoinbaseTransaction(coinbaseTransaction);

        BigInteger lBits = new BigInteger(DataUtils.hexStringToByteArray(blt.bits()));
        target = DataUtils.decodeCompactBits(lBits.longValue());
        LOG.setLevel(Level.ALL);
    }


    public boolean meetsTarget(int nonce, int[] solution, SHA256d hasher) throws GeneralSecurityException
    {
        boolean retval = false;
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < solution.length; n++)
        {
            sb.append(String.format("%08X", Integer.reverseBytes(solution[n])));
        }

        hasher.update(DataUtils.hexStringToByteArray(sb.toString()));
        byte[] hash = hasher.doubleDigest();

        BigInteger hashValue = new BigInteger(DataUtils.reverseBytes(hash));

        if (hashValue.compareTo(BigInteger.ZERO) == -1)
        {
            retval = false;
        }
        else
        {
            retval = (hashValue.compareTo(target) == 1) ? false : true;
        }

        return retval;
    }

    public BlockImpl getBlock()
    {
        return block;
    }

    public BigInteger getTarget()
    {
        return target;
    }

}