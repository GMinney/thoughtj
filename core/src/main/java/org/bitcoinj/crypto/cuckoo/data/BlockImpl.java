package org.bitcoinj.crypto.cuckoo.data;

import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.Block;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.BlockTemplate;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.BlockTemplateTransaction;
import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.Transaction;

public class BlockImpl implements Hexable
{
    private Block block;

    protected String hash;
    protected int confirmations;
    protected int size;
    protected long height;
    protected long version;
    protected String merkleRoot;
    protected List<TransactionImpl> transactions;
    protected long time;
    protected long nonce;
    protected String bits;
    protected double difficulty;
    protected String previousHash;
    protected String chainwork;
    protected int[] cuckooSolution;

    protected int[] voteBits;

    protected CoinbaseTransaction coinbase;

    public BlockImpl(Block block)
    {
        this.block = block;
        setHash(block.hash());
        setConfirmations(block.confirmations());
        setSize(block.size());
        setHeight(block.height());
        setVersion(block.version());
        setMerkleRoot(block.merkleRoot());
        // TODO: Transactions
        setTime(block.time().getTime());
        setNonce(block.nonce());
        setBits(block.bits());
        setDifficulty(block.difficulty());
        setPreviousHash(block.previousHash());
        setChainwork(block.chainwork());
    }

    public BlockImpl(BlockTemplate blt)
    {
        setHeight(blt.height());
        setVersion(blt.version());
        setTime(blt.curtime());
        setBits(blt.bits());
        setPreviousHash(blt.previousblockhash());
        List<BlockTemplateTransaction> trans = blt.transactions();
        transactions = new ArrayList<TransactionImpl>(trans.size());

        for (BlockTemplateTransaction t: trans)
        {
            transactions.add(new TransactionImpl(t));
        }
    }

    public BlockImpl(BlockTemplate blt, List<Integer> voteBits)
    {
        setHeight(blt.height());
        setVersion(blt.version());
        setTime(blt.curtime());
        setBits(blt.bits());
        setPreviousHash(blt.previousblockhash());
        List<BlockTemplateTransaction> trans = blt.transactions();
        transactions = new ArrayList<TransactionImpl>(trans.size());

        for (BlockTemplateTransaction t: trans)
        {
            transactions.add(new TransactionImpl(t));
        }

        if (null != voteBits)
        {
            for (int i : voteBits)
            {
                this.addVoteBit(i);
            }
        }

    }


    public String getHash()
    {
        return hash;
    }

    public void setHash(String hash)
    {
        this.hash = hash;
    }

    public int getConfirmations()
    {
        return confirmations;
    }

    public void setConfirmations(int confirmations)
    {
        this.confirmations = confirmations;
    }

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
    }

    public long getHeight()
    {
        return height;
    }

    public void setHeight(long height)
    {
        this.height = height;
    }

    public long getVersion()
    {
        return version;
    }

    public void setVersion(long version)
    {
        this.version = version;
    }

    public String getMerkleRoot()
    {
        return merkleRoot;
    }

    public void setMerkleRoot(String merkleRoot)
    {
        this.merkleRoot = merkleRoot;
    }

    public List<TransactionImpl> getTransactions()
    {
        return transactions;
    }

    public void setTransactions(List<TransactionImpl> transactions)
    {
        this.transactions = transactions;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public long getNonce()
    {
        return nonce;
    }

    public void setNonce(long nonce)
    {
        this.nonce = nonce;
    }

    public String getBits()
    {
        return bits;
    }

    public void setBits(String bits)
    {
        this.bits = bits;
    }

    public double getDifficulty()
    {
        return difficulty;
    }

    public void setDifficulty(double difficulty)
    {
        this.difficulty = difficulty;
    }

    public String getPreviousHash()
    {
        return previousHash;
    }

    public void setPreviousHash(String previousHash)
    {
        this.previousHash = previousHash;
    }

    public String getChainwork()
    {
        return chainwork;
    }

    public void setChainwork(String chainwork)
    {
        this.chainwork = chainwork;
    }

    public Block getBlock()
    {
        return block;
    }

    public void setCoinbaseTransaction(CoinbaseTransaction trans)
    {
        coinbase = trans;
    }

    public CoinbaseTransaction getCoinbaseTransaction()
    {
        return coinbase;
    }

    public int[] getCuckooSolution()
    {
        return cuckooSolution;
    }

    public void setCuckooSolution(int[] cuckooSolution)
    {
        this.cuckooSolution = cuckooSolution;
    }

    public void addVoteBit(int bit)
    {
        if (null == voteBits)
        {
            voteBits = new int[1];
            voteBits[0] = bit;
        }
        else
        {
            List<Integer> ints = new ArrayList<Integer>();
            for (int i : voteBits)
            {
                ints.add(i);
            }
            ints.add(bit);
            voteBits = new int[ints.size()];
            int j = 0;
            for (int i : ints)
            {
                voteBits[j] = i;
                j++;
            }
        }
    }


    public byte[] getHeader()
    {
        byte[] data = new byte[80];

        int offset = 0;
        if (null == voteBits)
        {
            DataUtils.uint32ToByteArrayLE(version, data, offset);
        }
        else
        {
            long t = 1 << 27;

            for (int i : voteBits)
            {
                t = (t | (1 << i));
            }

            long mask = 0xF0000000 & version;
            t = t | mask;

            data[offset + 3] = (byte) (0xFF & (t >> 24));
            data[offset + 2] = (byte) (0xFF & (t >> 16));
            data[offset + 1] = (byte) (0xFF & (t >> 8));
            data[offset + 0] = (byte) (0xFF & (t >> 0));

        }
        offset += 4;

        byte[] prev = DataUtils.hexStringToByteArray(previousHash);
        System.arraycopy(DataUtils.reverseBytes(prev), 0, data, offset, prev.length);
        offset += prev.length;

        MerkleTree mt = new MerkleTree(coinbase, getTransactions());

        byte[] merkle_bytes = mt.getRoot();
        System.arraycopy(DataUtils.reverseBytes(merkle_bytes), 0, data, offset, merkle_bytes.length);
        offset += merkle_bytes.length;

        DataUtils.uint32ToByteArrayLE(time, data, offset);
        offset += 4;
        byte[] bitsHex = DataUtils.reverseBytes(DataUtils.hexStringToByteArray(bits));
        System.arraycopy(bitsHex, 0, data, offset, bitsHex.length);

        if (nonce != 0)
        {
            data[79] = (byte) (nonce >> 0);
            data[78] = (byte) (nonce >> 8);
            data[77] = (byte) (nonce >> 16);
            data[76] = (byte) (nonce >> 24);
        }

        return data;
    }

    @Override
    public byte[] getHex()
    {
        byte[] header = getHeader();

        ByteArray bytes = new ByteArray(header);
        //
        // Cuckoo nonces
        byte[] tmp = new byte[4 * 42];
        int off = 0;
        for (int i = 0; i < 42; i++)
        {
            byte[] cnon = DataUtils.hexStringToByteArray(String.format("%08X", Integer.reverseBytes(cuckooSolution[i])));
            System.arraycopy(cnon, 0, tmp, off, 4);
            off += 4;
        }
        bytes.append(tmp);

        //
        // Transactions
        //
        byte[] numTrans = DataUtils.encodeCompact(transactions.size() + 1);
        bytes.append(numTrans);
        bytes.append(coinbase.getHex());
        for (TransactionImpl t : transactions)
            bytes.append(t.getHex());

        return bytes.get();
    }

}
