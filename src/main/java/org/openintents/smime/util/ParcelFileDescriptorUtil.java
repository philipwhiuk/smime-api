/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *               2013 Florian Schmaus <flo@geekplace.eu>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.smime.util;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openintents.smime.util.SMimeApi.SMimeDataSource;

/**
 * Partially based on <a href="http://stackoverflow.com/questions/18212152/">Stackoverflow: Transfer InputStream to another Service (across process boundaries)</a>
 **/
public class ParcelFileDescriptorUtil {

    public interface IThreadListener {
        void onThreadFinished(final Thread thread);
    }

    public static ParcelFileDescriptor pipeFrom(InputStream inputStream)
            throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];


        // start the transfer thread
        TransferThread t = new TransferThread(inputStream, new ParcelFileDescriptor.AutoCloseOutputStream(writeSide));
        t.start();

        return readSide;
    }

    public static TransferThread pipeTo(OutputStream outputStream, ParcelFileDescriptor output)
            throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        // start the transfer thread
        TransferThread t = new TransferThread(new ParcelFileDescriptor.AutoCloseInputStream(output), outputStream);
        t.start();

        return t;
    }

    static class TransferThread extends Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buf = new byte[4096];
            int len;

            try {
                while ((len = mIn.read(buf)) > 0) {
                    mOut.write(buf, 0, len);
                }
                mOut.flush(); // just to be safe
            } catch (IOException e) {
            } finally {
                try {
                    mIn.close();
                } catch (IOException e) {
                }
                try {
                    mOut.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static ParcelFileDescriptor asyncPipeFromDataSource(SMimeDataSource dataSource) throws IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor readSide = pipe[0];
        ParcelFileDescriptor writeSide = pipe[1];

        new DataSourceTransferThread(dataSource, new ParcelFileDescriptor.AutoCloseOutputStream(writeSide)).start();

        return readSide;
    }

    static class DataSourceTransferThread extends Thread {
        final SMimeDataSource dataSource;
        final OutputStream outputStream;

        DataSourceTransferThread(SMimeDataSource dataSource, OutputStream outputStream) {
            super("IPC Transfer Thread");
            this.dataSource = dataSource;
            this.outputStream = outputStream;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                dataSource.writeTo(outputStream);
            } catch (IOException e) {
                Log.e(SMimeApi.TAG, "IOException when writing to out", e);
            } finally {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
