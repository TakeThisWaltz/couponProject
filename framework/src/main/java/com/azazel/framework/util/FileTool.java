package com.azazel.framework.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Base64;

import com.azazel.framework.network.NetworkUtil.AzProgressListener;

public final class FileTool {
    private static final String TAG = "FileTool";
    private static MessageDigest mMessageDigest = null;

    public static synchronized String getMessageDigestFromString(final String msg)
            throws IOException, NoSuchAlgorithmException {

        final ByteArrayInputStream bis = new ByteArrayInputStream(msg.getBytes("UTF-8"));

        return getMessageDigest(bis);
    }

    public static synchronized String getMessageDigest(final String filepath)
            throws IOException, NoSuchAlgorithmException {

        final FileInputStream fis = new FileInputStream(filepath);

        return getMessageDigest(fis);
    }

    public static synchronized String getMessageDigest(final InputStream fis)
            throws IOException, NoSuchAlgorithmException {
        int bHex = 0;
        byte[] buff = new byte[8 * 1024]; // 8K
        if (mMessageDigest == null)
            mMessageDigest = MessageDigest.getInstance("MD5");
        else
            mMessageDigest.reset();

        int len = 0;
        while ((len = fis.read(buff)) > 0) {
            mMessageDigest.update(buff, 0, len);
        }
        fis.close();
        final byte md5Data[] = mMessageDigest.digest(); // Get MD5 bytes
        StringBuilder checksum = new StringBuilder(); // Create a Hex string

        for (int i = 0; i < md5Data.length; i++) {
            bHex = (0xFF & md5Data[i]);
            if (bHex <= 0xF) { // If it is a single digit, make sure it have 0
                // in front (proper padding)
                checksum.append("0");
            }
            checksum.append(Integer.toHexString(bHex)); // Add Number to String
        }
        return checksum.toString().toUpperCase(); // Hex string to Uppercase
    }

    public static boolean isSameFileInputStream(final FileInputStream fis, final String checkSum) {
        String fileChecksum;
        try {
            fileChecksum = getMessageDigest(fis);
        } catch (NoSuchAlgorithmException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        if (fileChecksum.equals(checkSum))
            return true;
        else
            return false;
    }

    public static boolean isSameFile(final String filepath, final String checkSum) {
        String fileChecksum;
        try {
            fileChecksum = getMessageDigest(filepath);
        } catch (NoSuchAlgorithmException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        if (fileChecksum.equals(checkSum))
            return true;
        else
            return false;
    }

    public static void writeToFile(final InputStream inputStream
            , long size, final FileOutputStream fileOpStream, AzProgressListener handler) throws IOException {
        try {
            LOG.d(TAG, "writeToFile - start Write with stream : " + fileOpStream);

            byte[] buffer = new byte[128 * 1024];    // 128 kbyte

            int len = 0;
            long sum = 0;
            while ((len = inputStream.read(buffer)) > 0) {
                sum += len;
                //LOG.d(TAG,	"writeToFile - writing : " + len + ", " + sum + "/" + size);
                if (handler != null)
                    handler.transferred(sum, size);
                fileOpStream.write(buffer, 0, len);
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if (null != inputStream)
                inputStream.close();
            if (null != fileOpStream)
                fileOpStream.close();
        }
    }

    public static void writeToFile(final InputStream inputStream
            , long size, final String filepath, AzProgressListener handler) throws IOException {
        LOG.d(TAG, "writeToFile - start Write with stream : " + filepath);
        /* Start - Logic to Check if the Folder already exists on the device */

        final String[] split = filepath.split("/");
        final String fileName = split[split.length - 1];
        final String folderPath = filepath.substring(0, filepath.length() - fileName.length());

        final File file = new File(folderPath);
        if (!file.exists()) {
            LOG.i(TAG, "Creating folder : " + folderPath);
            final boolean result = file.mkdirs();

            if (result == false) {
                LOG.f(TAG, "ORSMetaResponse.fromBinaryFile(): Can not create directory. ");
                throw new IOException();
            }
        }

        FileOutputStream fileOpStream = new FileOutputStream(filepath, false);

        writeToFile(inputStream, size, fileOpStream, handler);

    }

    public static void writeToFile(final String inputFile
            , long size, final String filepath, AzProgressListener handler) throws IOException {
        File file = new File(inputFile);
        if (!file.exists())
            throw new IOException("input file does not exists : " + inputFile);

        FileInputStream inputStream = new FileInputStream(file);

        writeToFile(inputStream, size, filepath, handler);
    }

    public static void writeToFile(final String inputFile, long size
            , final FileOutputStream outputStream, AzProgressListener handler) throws IOException {
        File file = new File(inputFile);
        if (!file.exists())
            throw new IOException("input file does not exists : " + inputFile);

        FileInputStream inputStream = new FileInputStream(file);

        writeToFile(inputStream, size, outputStream, handler);
    }

    public static byte[] getByteArr(final InputStream inputStream) throws IOException {
        byte[] buff;
        int len;
        final ByteArrayOutputStream byteOpStream = new ByteArrayOutputStream();
        buff = new byte[1024];
        while ((len = inputStream.read(buff, 0, 1024)) != -1)
            byteOpStream.write(buff, 0, len);
        buff = byteOpStream.toByteArray();
        return buff;
    }

    public static String encode(String message) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
            return Base64.encodeToString(md.digest(message.getBytes("UTF-8")), Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static String toString(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    public static boolean deleteFile(String path) {
        boolean suc = false;
        try {
            suc = new File(path).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return suc;
    }
}