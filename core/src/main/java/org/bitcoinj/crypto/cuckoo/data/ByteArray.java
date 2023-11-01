package org.bitcoinj.crypto.cuckoo.data;

public class ByteArray
{
    protected byte[] content;

    public ByteArray()
    {
        content = null;
    }

    public ByteArray(byte[] content)
    {
        if (null == content)
        {
            this.content = null;
        }
        else
        {
            this.content = new byte[content.length];
            System.arraycopy(content, 0, this.content, 0, content.length);
        }
    }

    public ByteArray(int size)
    {
        this.content = new byte[size];
    }

    public ByteArray append(byte b)
    {
        if (null != this.content)
        {
            int newLength = this.content.length + 1;
            byte[] newContent = new byte[newLength];
            System.arraycopy(this.content, 0, newContent, 0, this.content.length);
            newContent[newContent.length - 1] = b;
            this.content = newContent;
        }
        else
        {
            this.content = new byte[1];
            this.content[0] = b;
        }
        return this;
    }

    public ByteArray append(byte[] b)
    {
        if (null != b)
        {
            if (null != this.content)
            {
                int oldLength = this.content.length;
                int newLength = oldLength + b.length;
                byte[] newContent = new byte[newLength];
                System.arraycopy(this.content, 0, newContent, 0, this.content.length);
                System.arraycopy(b, 0, newContent, oldLength, b.length);
                this.content = newContent;
            }
            else
            {
                this.content = new byte[b.length];
                System.arraycopy(b, 0, this.content, 0, b.length);
            }
        }
        return this;
    }

    public ByteArray append(byte[] b, int index, int length)
    {
        if (null != b)
        {
            if (null != this.content)
            {
                int oldLength = this.content.length;
                int newLength = oldLength + length;
                byte[] newContent = new byte[newLength];
                System.arraycopy(this.content, 0, newContent, 0, this.content.length);
                System.arraycopy(b, index, newContent, oldLength, length);
                this.content = newContent;
            }
            else
            {
                this.content = new byte[length];
                System.arraycopy(b, index, this.content, 0, length);
            }
        }
        return this;
    }

    public ByteArray set(int index, byte b)
    {
        if (null == this.content || index > this.content.length - 1)
        {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        this.content[index] = b;
        return this;
    }

    public byte[] get()
    {
        return this.content;
    }
}