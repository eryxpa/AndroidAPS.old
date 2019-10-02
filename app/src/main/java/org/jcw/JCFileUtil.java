package org.jcw;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JCFileUtil {
    public static byte[] readFromFile(String sFile) throws IOException {
        FileInputStream fich = null;

        try {
            fich = new FileInputStream(sFile);

            byte[] file = readInputStream(fich, fich.available());
            fich.close();
            return file;
        } catch (final FileNotFoundException e) {
            throw e;
        } catch (final IOException e) {
            throw e;
        } finally {
            close(fich);
        }
    }

    public static byte[] readInputStream(InputStream in, int size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (size > 0) {
                copy(in, out, size);
            } else {
                copy(in, out, 4096);
            }
            byte[] bReturn = out.toByteArray();
            return bReturn;
        } catch (IOException e) {
            throw e;
        } finally {
            out.close();
        }
    }

    public static void copy(InputStream in, OutputStream out, int size) throws IOException {
        byte[] buf = new byte[size > 0 ? size : 4096];
        while (true) {
            int len = in.read(buf);
            if (len < 0) {
                break;
            }
            out.write(buf, 0, len);
        }
    }

    public static void close(Closeable in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // se ignora el error
            }
        }
    }
}
