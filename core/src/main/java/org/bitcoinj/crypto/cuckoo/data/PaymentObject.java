package org.bitcoinj.crypto.cuckoo.data;

public class PaymentObject
{
    protected String payee;
    protected String script;
    protected long value;

    public PaymentObject()
    {

    }

    public String getPayee()
    {
        return payee;
    }
    public void setPayee(String payee)
    {
        this.payee = payee;
    }
    public String getScript()
    {
        return script;
    }
    public void setScript(String script)
    {
        this.script = script;
    }
    public long getValue()
    {
        return value;
    }
    public void setValue(long value)
    {
        this.value = value;
    }


}
