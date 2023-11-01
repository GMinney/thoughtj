package org.bitcoinj.crypto.cuckoo.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.bitcoinj.crypto.cuckoo.algo.SHA256d;

public class DataUtils
{

    private static SHA256d hasher = new SHA256d();

    public static String byteArrayToHexString(byte[] b)
    {
        StringBuilder sb = new StringBuilder(80);
        for (int i = 0; i < b.length; i++)
            sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        String source = null;
        if (len % 2 != 0)
        {
            source = "0" + s;
            len++;
        }
        else
        {
            source = s;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(source.charAt(i), 16) << 4) + Character.digit(source.charAt(i + 1), 16));
        }
        return data;
    }

    private final static char[] BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static int[]        toInt           = new int[128];

    static
    {
        for (int i = 0; i < BASE64_ALPHABET.length; i++)
        {
            toInt[BASE64_ALPHABET[i]] = i;
        }
    }

    public static String stringToBase64(String str)
    {
        byte[] buf = str.getBytes();
        int size = buf.length;
        char[] ar = new char[((size + 2) / 3) * 4];
        int a = 0;
        int i = 0;
        while (i < size)
        {
            byte b0 = buf[i++];
            byte b1 = (i < size) ? buf[i++] : 0;
            byte b2 = (i < size) ? buf[i++] : 0;
            ar[a++] = BASE64_ALPHABET[(b0 >> 2) & 0x3f];
            ar[a++] = BASE64_ALPHABET[((b0 << 4) | ((b1 & 0xFF) >> 4)) & 0x3f];
            ar[a++] = BASE64_ALPHABET[((b1 << 2) | ((b2 & 0xFF) >> 6)) & 0x3f];
            ar[a++] = BASE64_ALPHABET[b2 & 0x3f];
        }
        switch (size % 3)
        {
            case 1:
                ar[--a] = '=';
            case 2:
                ar[--a] = '=';
        }
        return new String(ar);
    }

    public static void uint32ToByteArrayBE(long val, byte[] out, int offset)
    {
        out[offset + 0] = (byte) (0xFF & (val >> 24));
        out[offset + 1] = (byte) (0xFF & (val >> 16));
        out[offset + 2] = (byte) (0xFF & (val >> 8));
        out[offset + 3] = (byte) (0xFF & (val >> 0));
    }

    public static void uint32ToByteArrayLE(long val, byte[] out, int offset)
    {
        out[offset + 0] = (byte) (0xFF & (val >> 0));
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }

    public static byte reverseBitsByte(byte x) {
        int intSize = 8;
        byte y=0;
        for(int position=intSize-1; position>0; position--){
            y+=((x&1)<<position);
            x >>= 1;
        }
        return y;
    }

    public static long readUint32(byte[] bytes, int offset)
    {
        return ((bytes[offset++] & 0xFFL) << 0) | ((bytes[offset++] & 0xFFL) << 8) | ((bytes[offset++] & 0xFFL) << 16)
                | ((bytes[offset] & 0xFFL) << 24);
    }

    public static long readUint32BE(byte[] bytes, int offset)
    {
        return ((bytes[offset + 0] & 0xFFL) << 24) | ((bytes[offset + 1] & 0xFFL) << 16) | ((bytes[offset + 2] & 0xFFL) << 8)
                | ((bytes[offset + 3] & 0xFFL) << 0);
    }

    /**
     * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They
     * consist of a 4 byte big endian length field, followed by the stated number of
     * bytes representing the number in big endian format.
     */
    public static BigInteger decodeMPI(byte[] mpi)
    {
        int length = (int) readUint32BE(mpi, 0);
        byte[] buf = new byte[length];
        System.arraycopy(mpi, 4, buf, 0, length);
        return new BigInteger(buf);
    }

    // The representation of nBits uses another home-brew encoding, as a way to
    // represent a large
    // hash value in only 32 bits.
    public static BigInteger decodeCompactBits(long compact)
    {
        int size = ((int) (compact >> 24)) & 0xFF;
        byte[] bytes = new byte[4 + size];
        bytes[3] = (byte) size;
        if (size >= 1)
            bytes[4] = (byte) ((compact >> 16) & 0xFF);
        if (size >= 2)
            bytes[5] = (byte) ((compact >> 8) & 0xFF);
        if (size >= 3)
            bytes[6] = (byte) ((compact >> 0) & 0xFF);
        return decodeMPI(bytes);
    }

    public static byte[] reverseBytes(byte[] bytes)
    {
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }

    //TODO - Use this for AbstractBitcoinNetParams
    public static byte[] encodeCompact(long height)
    {
        byte[] retval = null;
        if (height <= 252)
        {
            retval = new byte[1];
            retval[0] = (byte) (height & 0xff);
        }
        else if (height < 0xffff)
        {
            retval = new byte[3];
            retval[0] = (byte) 0xfd;
            retval[1] = (byte) (height & 0xff);
            retval[2] = (byte) ((height >> 8) & 0xff);
        }
        else if (height < 0xffffffff)
        {
            retval = new byte[5];
            retval[0] = (byte) 0xf3;
            retval[1] = (byte) (height & 0xff);
            retval[2] = (byte) ((height >> 8) & 0xff);
            retval[3] = (byte) ((height >> 16) & 0xff);
            retval[4] = (byte) ((height >> 24) & 0xff);
        }
        return retval;
    }

    //Adds the checksum and converts to Base58Check. Note that the caller needs to prepend the version byte(s).
    public static String bytesToBase58(byte[] data) {
        return rawBytesToBase58(addCheckHash(data));
    }


    // Directly converts to Base58Check without adding a checksum.
    static String rawBytesToBase58(byte[] data) {
        // Convert to base-58 string
        StringBuilder sb = new StringBuilder();
        BigInteger num = new BigInteger(1, data);
        while (num.signum() != 0) {
            BigInteger[] quotrem = num.divideAndRemainder(ALPHABET_SIZE);
            sb.append(ALPHABET.charAt(quotrem[1].intValue()));
            num = quotrem[0];
        }

        // Add '1' characters for leading 0-value bytes
        for (int i = 0; i < data.length && data[i] == 0; i++)
            sb.append(ALPHABET.charAt(0));
        return sb.reverse().toString();
    }


    // Returns a new byte array by concatenating the given array with its checksum.
    static byte[] addCheckHash(byte[] data) {
        try {
            hasher.update(data);
            byte[] hash = Arrays.copyOf(hasher.doubleDigest(), 4);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write(data);
            buf.write(hash);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }


    // Converts the given Base58Check string to a byte array, verifies the checksum, and removes the checksum to return the payload.
    // The caller is responsible for handling the version byte(s).
    public static byte[] base58ToBytes(String s) {
        byte[] concat = base58ToRawBytes(s);
        byte[] data = Arrays.copyOf(concat, concat.length - 4);
        byte[] hash = Arrays.copyOfRange(concat, concat.length - 4, concat.length);
        hasher.update(data);
        byte[] rehash = Arrays.copyOf(hasher.doubleDigest(), 4);
        if (!Arrays.equals(rehash, hash))
            throw new IllegalArgumentException("Checksum mismatch");
        return data;
    }


    // Converts the given Base58Check string to a byte array, without checking or removing the trailing 4-byte checksum.
    protected static byte[] base58ToRawBytes(String s) {
        // Parse base-58 string
        BigInteger num = BigInteger.ZERO;
        for (int i = 0; i < s.length(); i++) {
            num = num.multiply(ALPHABET_SIZE);
            int digit = ALPHABET.indexOf(s.charAt(i));
            if (digit == -1)
                throw new IllegalArgumentException("Invalid character for Base58Check");
            num = num.add(BigInteger.valueOf(digit));
        }

        // Strip possible leading zero due to mandatory sign bit
        byte[] b = num.toByteArray();
        if (b[0] == 0)
            b = Arrays.copyOfRange(b, 1, b.length);

        try {
            // Convert leading '1' characters to leading 0-value bytes
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            for (int i = 0; i < s.length() && s.charAt(i) == ALPHABET.charAt(0); i++)
                buf.write(0);
            buf.write(b);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";  // Everything except 0OIl
    private static final BigInteger ALPHABET_SIZE = BigInteger.valueOf(ALPHABET.length());

    public static byte[] addressToScript(String addr)
    {
        byte[] retval = null;

        byte[] addrbin = base58ToBytes(addr);
        byte addrver = addrbin[0];

        switch (addrver) {
            case (byte)9:    /* Mainnet script hash */
            case (byte)193:  /* Testnet script hash */
                retval = new byte[23];
                retval[0] = (byte) 0xa9;  /* OP_HASH160 */
                retval[1] = (byte) 0x14;  /* push 20 bytes */
                System.arraycopy(addrbin, 1, retval, 2, 20);
                retval[22] = (byte)0x87;  /* OP_EQUAL */
            default:
                retval = new byte[25];
                retval[0] = (byte) 0x76;  /* OP_DUP */
                retval[1] = (byte) 0xa9;  /* OP_HASH160 */
                retval[2] = (byte) 0x14;  /* push 20 bytes */
                System.arraycopy(addrbin, 1, retval, 3, 20);
                retval[23] = (byte) 0x88;  /* OP_EQUALVERIFY */
                retval[24] = (byte) 0xac;  /* OP_CHECKSIG */
        }
        return retval;
    }

}