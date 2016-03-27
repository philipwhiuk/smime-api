package org.openintents.smime;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SMimeApi {
    public static final String TAG = "SMIME API";
    public static final String SERVICE_INTENT = "org.openintents.smime.ISMimeService";
    public static final int API_VERSION = 1;
    public static final String ACTION_CHECK_PERMISSION = "org.openintents.smime.action.CHECK_PERMISSION";
    public static final String ACTION_CLEARTEXT_SIGN = "org.openintents.smime.action.CLEARTEXT_SIGN";
    public static final String ACTION_SIGN = "org.openintents.smime.action.SIGN";
    public static final String ACTION_ENCRYPT = "org.openintents.smime.action.ENCRYPT";
    public static final String ACTION_VERIFY = "org.openintents.smime.action.VERIFY";
    public static final String ACTION_ENCRYPT_AND_SIGN = "org.openintents.smime.action.ENCRYPT_AND_SIGN";
    public static final String ACTION_DECRYPT_VERIFY = "org.openintents.smime.action.DECRYPT_VERIFY";
    public static final String ACTION_DECRYPT_METADATA = "org.openintents.smime.action.DECRYPT_METADATA";
    public static final String ACTION_GET_SIGN_CERTIFICATE_ID = "org.openintents.smime.action.GET_SIGN_CERTIFICATE_ID";
    public static final String ACTION_GET_CERTIFICATE_IDS = "org.openintents.smime.action.GET_CERTIFICATE_IDS";
    public static final String ACTION_GET_CERTIFICATE = "org.openintents.smime.action.GET_CERTIFICATE";
    public static final String EXTRA_INPUT = "org.openintents.smime.extra.EXTRA_INPUT";
    public static final String EXTRA_OUTPUT = "org.openintents.smime.extra.EXTRA_OUTPUT";
    public static final String EXTRA_IDENTITY = "org.openintents.smime.extra.EXTRA_IDENTITY";
    public static final String EXTRA_OTHERPARTY = "org.openintents.smime.extra.EXTRA_OTHERPARTY";
    public static final String EXTRA_API_VERSION = "org.openintents.smime.extra.API_VERSION";
    public static final String HAS_PRIVATE_KEY = "org.openintents.smime.action.HAS_PRIVATE_KEY";
    public static final String HAS_PUBLIC_KEY = "org.openintents.smime.action.HAS_PUBLIC_KEY";

    public static final String RESULT_CODE = "result_code";
    public static final int RESULT_CODE_ERROR = 0;
    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    public static final String RESULT_ERROR = "error";
    public static final String RESULT_INTENT = "intent";

    public static final String EXTRA_RESULT_ERROR = "org.openintents.smime.extra.ERROR";
    public static final String EXTRA_RESULT_CODE = "org.openintents.smime.extra.RESULT_CODE";
    public static final String EXTRA_CERTIFICATE_ID = "certificate_id";

    public static final String RESULT_TYPE = "org.openintents.smime.extra.RESULT_TYPE";
    public static final int RESULT_TYPE_UNENCRYPTED_UNSIGNED = 0;
    public static final int RESULT_TYPE_ENCRYPTED = 1;
    public static final int RESULT_TYPE_SIGNED = 2;

    public static final String RESULT_SIGNATURE = "org.openintents.smime.extra.RESULT_SIGNATURE";
    public static final int RESULT_SIGNATURE_UNSIGNED = 0;
    public static final int RESULT_SIGNATURE_SIGNED = 1;
    public static final int RESULT_SIGNATURE_SIGNED_UNCOFIRMED = 2;
    public static final int RESULT_SIGNATURE_INVALID_EXPIRED = 3;

    public static Intent hasPrivateKey(final String address) {
        Intent intent = new Intent(HAS_PRIVATE_KEY);
        intent.putExtra(EXTRA_IDENTITY, Collections.singletonList(address).toArray(new String[1]));
        return intent;
    }

    public static Intent hasPublicKey(final String address) {
        Intent intent = new Intent(HAS_PUBLIC_KEY);
        intent.putExtra(EXTRA_IDENTITY, Collections.singletonList(address).toArray(new String[1]));
        return intent;
    }

    public static Intent verifyMessage(final String senderAddress) {
        Intent intent = new Intent(ACTION_VERIFY);
        intent.putExtra(EXTRA_OTHERPARTY, Collections.singletonList(senderAddress).toArray(new String[1]));

        return intent;
    }

    public static final Intent decryptAndVerifyMessage(final String senderAddress,
                                                       final String recipientAddress) {
        Intent intent = new Intent(ACTION_DECRYPT_VERIFY);
        intent.putExtra(EXTRA_IDENTITY, recipientAddress);
        intent.putExtra(EXTRA_OTHERPARTY, Collections.singletonList(senderAddress).toArray(new String[1]));

        return intent;
    }

    public static final Intent signMessage(String senderAddress) {
        Intent intent = new Intent(ACTION_SIGN);
        intent.putExtra(EXTRA_IDENTITY, senderAddress);

        return intent;
    }

    public static final Intent encryptMessage(List<String> recipientAddress) {
        Intent intent = new Intent(ACTION_ENCRYPT);
        intent.putExtra(EXTRA_OTHERPARTY, recipientAddress.toArray(new String[recipientAddress.size()]));

        return intent;
    }

    public static final Intent signAndEncryptMessage(String senderAddress, List<String> recipientAddress) {
        Intent intent = new Intent(ACTION_ENCRYPT_AND_SIGN);
        intent.putExtra(EXTRA_IDENTITY, senderAddress);
        intent.putExtra(EXTRA_OTHERPARTY, recipientAddress.toArray(new String[recipientAddress.size()]));

        return intent;
    }

    ISMimeService mService;
    Context mContext;
    final AtomicInteger mPipeIdGen = new AtomicInteger();

    public SMimeApi(Context context, ISMimeService service) {
        this.mContext = context;
        this.mService = service;
    }

    public interface ISMimeCallback {
        void onReturn(final Intent result);
    }

    private class SMimeAsyncTask extends AsyncTask<Void, Integer, Intent> {
        Intent data;
        InputStream is;
        OutputStream os;
        ISMimeCallback callback;

        private SMimeAsyncTask(Intent data, InputStream is, OutputStream os, ISMimeCallback callback) {
            this.data = data;
            this.is = is;
            this.os = os;
            this.callback = callback;
        }

        @Override
        protected Intent doInBackground(Void... unused) {
            return executeApi(data, is, os);
        }

        protected void onPostExecute(Intent result) {
            callback.onReturn(result);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void executeApiAsync(Intent data, InputStream is, OutputStream os, ISMimeCallback callback) {
        SMimeAsyncTask task = new SMimeAsyncTask(data, is, os, callback);

        // don't serialize async tasks!
        // http://commonsware.com/blog/2012/04/20/asynctask-threading-regression-confirmed.html
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        } else {
            task.execute((Void[]) null);
        }
    }


    public Intent executeApi(Intent data, InputStream is, OutputStream os) {
        ParcelFileDescriptor input = null;
        try {
            if (is != null) {
                input = ParcelFileDescriptorUtil.pipeFrom(is);
            }

            return executeApi(data, input, os);
        } catch (Exception e) {
            Log.e(SMimeApi.TAG, "Exception in executeApi call", e);
            Intent result = new Intent();
            result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(RESULT_ERROR,
                    new SMimeError(SMimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(SMimeApi.TAG, "IOException when closing ParcelFileDescriptor!", e);
                }
            }
        }
    }

    /**
     * InputStream and OutputStreams are always closed after operating on them!
     *
     * @param data
     * @param input
     * @param os
     * @return
     */
    public Intent executeApi(Intent data, ParcelFileDescriptor input, OutputStream os) {
        ParcelFileDescriptor output = null;

        try {
            // always send version from client
            data.putExtra(EXTRA_API_VERSION, SMimeApi.API_VERSION);

            Intent result;

            Thread pumpThread =null;
            int outputPipeId = 0;

            if (os != null) {
                outputPipeId = mPipeIdGen.incrementAndGet();
                output = mService.createOutputPipe(outputPipeId);
                pumpThread = ParcelFileDescriptorUtil.pipeTo(os, output);
            }

            // blocks until result is ready
            result = mService.execute(data, input, outputPipeId);

            // set class loader to current context to allow unparcelling
            // of SMimeError and SMimeSignatureResult
            // http://stackoverflow.com/a/3806769
            //result.setExtrasClassLoader(mContext.getClassLoader());
            Log.d(SMimeApi.TAG, "service result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(SMimeApi.TAG, "Exception in executeApi call", e);
            Intent result = new Intent();
            result.putExtra(EXTRA_RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(EXTRA_RESULT_ERROR,
                    new SMimeError(SMimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        } finally {
            // close() is required to halt the TransferThread
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(SMimeApi.TAG, "IOException when closing ParcelFileDescriptor!", e);
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(SMimeApi.TAG, "IOException when closing ParcelFileDescriptor!", e);
                }
            }
        }
    }


}
