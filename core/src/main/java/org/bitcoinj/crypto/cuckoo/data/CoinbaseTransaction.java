package org.bitcoinj.crypto.cuckoo.data;

import java.util.ArrayList;
import java.util.List;

public class CoinbaseTransaction implements Hexable
{
    protected static final int COINBASE_SCRIPT_LENGTH = 95;

    protected int    version    = 3;
    protected int    type       = 5;
    protected long   height;
    protected long   value;
    protected String address;
    protected List<PaymentObject> extraPayments;
    protected int    lockTime   = 0;
    protected String extraPayload;
    protected byte[] coinbaseScript;

    public CoinbaseTransaction(long height, long value, String coinbaseAddress)
    {
        this.height = height;
        this.value = value;
        this.address = coinbaseAddress;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public long getHeight()
    {
        return height;
    }

    public void setHeight(long height)
    {
        this.height = height;
    }

    public long getValue()
    {
        return value;
    }

    public void setValue(long value)
    {
        this.value = value;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public byte[] getCoinbaseScript()
    {
        return coinbaseScript;
    }

    public void setArbitraryData(byte[] coinbaseScript)
    {
        this.coinbaseScript = coinbaseScript;
    }

    public void setExtraPayments(List<PaymentObject> extraPayments)
    {
        this.extraPayments = extraPayments;
    }

    public void addExtraPayment(PaymentObject payment)
    {
        if (null == extraPayments)
        {
            extraPayments = new ArrayList<PaymentObject>();
        }
        extraPayments.add(payment);
    }

    public List<PaymentObject> getExtraPayments()
    {
        return extraPayments;
    }

    public int getLockTime()
    {
        return lockTime;
    }

    public void setLockTime(int lockTime)
    {
        this.lockTime = lockTime;
    }

    public void setExtraPayload(String extraPayload)
    {
        this.extraPayload = extraPayload;
    }

    public String getExtraPayload()
    {
        return this.extraPayload;
    }

    public void setCoinbaseScript(byte[] coinbaseScript)
    {
        this.coinbaseScript = coinbaseScript;
    }

    @Override
    public byte[] getHex()
    {
        ByteArray cbtx = new ByteArray();
        cbtx.append(DataUtils.hexStringToByteArray(String.format("%04X", Integer.reverseBytes(version))), 0, 2);
        cbtx.append(DataUtils.hexStringToByteArray(String.format("%04X", Integer.reverseBytes(type))), 0, 2);
        cbtx.append((byte) 0x01);

        byte[] empty = new byte[32];
        cbtx.append(empty);

        byte[] seq = DataUtils.hexStringToByteArray("ffffffff");
        cbtx.append(seq);

        String heightHex = Long.toHexString(height);
        byte[] heightBytes = DataUtils.reverseBytes(DataUtils.hexStringToByteArray(heightHex));
        int trailingZeroes = 0;
        if (heightBytes[heightBytes.length - 1] < 0x00)
        {
            trailingZeroes = 1;
        }
        int scriptSize = heightBytes.length + trailingZeroes + 1;
        int coinbaseScriptLength = 0;
        if (null != coinbaseScript && coinbaseScript.length > 0)
        {
            coinbaseScriptLength = coinbaseScript.length > COINBASE_SCRIPT_LENGTH ? COINBASE_SCRIPT_LENGTH : coinbaseScript.length;
            scriptSize += coinbaseScriptLength;
        }
        // BIP-34 height
        cbtx.append(DataUtils.hexStringToByteArray(String.format("%02X", scriptSize)));
        cbtx.append(DataUtils.hexStringToByteArray(String.format("%02X", heightBytes.length + trailingZeroes)));
        cbtx.append(heightBytes);
        for (int x = 0; x < trailingZeroes; x++)
        {
            cbtx.append(Byte.valueOf((byte) 0x00));
        }
        if (coinbaseScriptLength > 0)
        {
            cbtx.append(coinbaseScript, 0, coinbaseScriptLength);
        }

        // Sequence
        seq = DataUtils.hexStringToByteArray("00000000");
        cbtx.append(seq);

        // tx_out counter
        int outCounter = 1;
        if (null != extraPayments)
        {
            outCounter += extraPayments.size();
        }
        cbtx.append(DataUtils.hexStringToByteArray(String.format("%01X", outCounter)));

        // First output should be miner payment, which is what's left after the extra payments, so get that value first
        long minerReward = value;
        if (null != extraPayments)
        {
            for (PaymentObject p : extraPayments)
            {
                minerReward -= p.getValue();
            }
        }

        // Do the miner reward output
        byte[] val = DataUtils.hexStringToByteArray(String.format("%016X", Long.reverseBytes(minerReward)));
        cbtx.append(val);

        // Address size
        byte[] addr = DataUtils.addressToScript(address);
        byte[] len = DataUtils.encodeCompact(addr.length);

        cbtx.append(len);
        // Payment Address
        cbtx.append(addr);

        // Now do the extra payments
        if (null != extraPayments)
        {
            for (PaymentObject p : extraPayments)
            {
                val = DataUtils.hexStringToByteArray(String.format("%016X", Long.reverseBytes(p.getValue())));
                cbtx.append(val);

                // Address size
                addr = DataUtils.addressToScript(p.getPayee());
                len = DataUtils.encodeCompact(addr.length);

                cbtx.append(len);
                // Payment Address
                cbtx.append(addr);
            }
        }

        // Lock time
        byte[] loc = { 0x00, 0x00, 0x00, 0x00 };
        cbtx.append(loc);

        // Extra payload
        if (null != extraPayload)
        {
            val = DataUtils.hexStringToByteArray(extraPayload);
            len = DataUtils.encodeCompact(val.length);
            cbtx.append(len);
            cbtx.append(val);
        }
        //System.out.println(DataUtils.byteArrayToHexString(cbtx.get()));
        return cbtx.get();
    }
}