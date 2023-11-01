package org.bitcoinj.crypto.cuckoo;

/**
 *
 * @author Mikhail Yevchenko m.ṥῥẚɱ.ѓѐḿởύḙ@azazar.com
 */
public class GenericRpcException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>BitcoinException</code> without detail
     * message.
     */
    public GenericRpcException()
    {
    }

    /**
     * Constructs an instance of <code>BitcoinException</code> with the specified
     * detail message.
     *
     * @param msg
     *          the detail message.
     */
    public GenericRpcException(String msg)
    {
        super(msg);
    }

    public GenericRpcException(Throwable cause)
    {
        super(cause);
    }

    public GenericRpcException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
