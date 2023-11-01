package org.bitcoinj.crypto.cuckoo.data;

import org.bitcoinj.crypto.cuckoo.ThoughtClientInterface.BlockTemplateTransaction;

public class TransactionImpl implements Hexable
{
    String data;
    String hash;

    public TransactionImpl(BlockTemplateTransaction trans)
    {
        data = trans.data();
        hash = trans.hash();
    }

    public TransactionImpl()
    {

    }

    public byte[] getHex()
    {
        return DataUtils.hexStringToByteArray(data);
    }

    public void setHex(String hex)
    {
        this.data = hex;
    }

    public byte[] getHash()
    {
        return DataUtils.hexStringToByteArray(hash);
    }

    public void setHash(String hash)
    {
        this.hash = hash;
    }
}
