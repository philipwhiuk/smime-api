package org.openintents.smime.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.openintents.smime.ISMimeService;
import org.openintents.smime.SmimeError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SMimeApi {
    public static final String TAG = "SMIME API";
    public static final String SERVICE_INTENT = "org.openintents.smime.ISMimeService";
    /**
     * see CHANGELOG.md
     */
    public static final int API_VERSION = 1;

    /**
     * General extras
     * --------------
     *
     * required extras:
     * int           EXTRA_API_VERSION           (always required)
     *
     * returned extras:
     * int           RESULT_CODE                 (RESULT_CODE_ERROR, RESULT_CODE_SUCCESS or RESULT_CODE_USER_INTERACTION_REQUIRED)
     * SmimeError  RESULT_ERROR                (if RESULT_CODE == RESULT_CODE_ERROR)
     * PendingIntent RESULT_INTENT               (if RESULT_CODE == RESULT_CODE_USER_INTERACTION_REQUIRED)
     */
    public static final String ACTION_CHECK_PERMISSION = "org.openintents.smime.action.CHECK_PERMISSION";

    //TODO: Update to match SMIME
    /**
     * Sign text or binary data resulting in a detached signature.
     * No OutputStream necessary for ACTION_DETACHED_SIGN
     * The detached signature is returned separately in RESULT_DETACHED_SIGNATURE.
     * <p/>
     * required extras:
     * long          EXTRA_SIGN_CERTIFICATE_ID           (certificate id of signing certificate)
     * <p/>
     * optional extras:
     * boolean       EXTRA_REQUEST_ASCII_ARMOR   (request ascii armor for detached signature)
     * char[]        EXTRA_PASSPHRASE            (certificate passphrase)
     * <p/>
     * returned extras:
     * byte[]        RESULT_DETACHED_SIGNATURE
     */
    public static final String ACTION_SIGN = "org.openintents.smime.action.SIGN";
    /**
     * Encrypt
     * <p/>
     * required extras:
     * String[]      EXTRA_USER_IDS              (=emails of recipients, if more than one certificate has a user_id, a PendingIntent is returned via RESULT_INTENT)
     * or
     * long[]        EXTRA_CERTIFICATE_IDS
     * <p/>
     * optional extras:
     * boolean       EXTRA_REQUEST_ASCII_ARMOR   (request ascii armor for output)
     * char[]        EXTRA_PASSPHRASE            (certificate passphrase)
     * String        EXTRA_ORIGINAL_FILENAME     (original filename to be encrypted as metadata)
     * boolean       EXTRA_ENABLE_COMPRESSION    (enable ZLIB compression, default ist true)
     */
    public static final String ACTION_ENCRYPT = "org.openintents.smime.action.ENCRYPT";
    public static final String ACTION_VERIFY = "org.openintents.smime.action.VERIFY";
    /**
     * Sign and encrypt
     * <p/>
     * required extras:
     * String[]      EXTRA_USER_IDS              (=emails of recipients, if more than one certificate has a user_id, a PendingIntent is returned via RESULT_INTENT)
     * or
     * long[]        EXTRA_CERTIFICATE_IDS
     * <p/>
     * optional extras:
     * long          EXTRA_SIGN_CERTIFICATE_ID           (certifcate id of signing certificate)
     * boolean       EXTRA_REQUEST_ASCII_ARMOR   (request ascii armor for output)
     * char[]        EXTRA_PASSPHRASE            (certificate passphrase)
     * String        EXTRA_ORIGINAL_FILENAME     (original filename to be encrypted as metadata)
     * boolean       EXTRA_ENABLE_COMPRESSION    (enable ZLIB compression, default ist true)
     */
    public static final String ACTION_SIGN_AND_ENCRYPT = "org.openintents.smime.action.SIGN_AND_ENCRYPT";
    /**
     * Decrypts and verifies given input stream. This methods handles encrypted-only, signed-and-encrypted,
     * and also signed-only input.
     * OutputStream is optional, e.g., for verifying detached signatures!
     * <p/>
     * If SmimeSignatureResult.getResult() == SmimeSignatureResult.RESULT_CERTIFICATE_MISSING
     * in addition a PendingIntent is returned via RESULT_INTENT to download missing certificates.
     * On all other status, in addition a PendingIntent is returned via RESULT_INTENT to open
     * the certificate view in the app providing the service (e.g. OpenSMIME).
     * <p/>
     * optional extras:
     * byte[]        EXTRA_DETACHED_SIGNATURE    (detached signature)
     * <p/>
     * returned extras:
     * SmimeSignatureResult   RESULT_SIGNATURE
     * SmimeDecryptionResult  RESULT_DECRYPTION
     * SmimeDecryptMetadata   RESULT_METADATA
     * String                   RESULT_CHARSET   (charset which was specified in the headers of ascii armored input, if any)
     */
    public static final String ACTION_DECRYPT_VERIFY = "org.openintents.smime.action.DECRYPT_VERIFY";
    /**
     * Decrypts the header of an encrypted file to retrieve metadata such as original filename.
     * <p/>
     * This does not decrypt the actual content of the file.
     * <p/>
     * returned extras:
     * SmimeDecryptMetadata   RESULT_METADATA
     * String                   RESULT_CHARSET   (charset which was specified in the headers of ascii armored input, if any)
     */
    public static final String ACTION_DECRYPT_METADATA = "org.openintents.smime.action.DECRYPT_METADATA";
    /**
     * Select certificate id for signing
     * <p/>
     * optional extras:
     * String      EXTRA_USER_ID
     * <p/>
     * returned extras:
     * long        EXTRA_SIGN_CERTIFICATE_ID
     */
    public static final String ACTION_GET_SIGN_CERTIFICATE_ID = "org.openintents.smime.action.GET_SIGN_CERTIFICATE_ID";
    /**
     * Get certificate ids based on given user ids (=emails)
     * <p/>
     * required extras:
     * String[]      EXTRA_USER_IDS
     * <p/>
     * returned extras:
     * long[]        RESULT_CERTIFICATE_IDS
     */
    public static final String ACTION_GET_CERTIFICATE_IDS = "org.openintents.smime.action.GET_CERTIFICATE_IDS";
    /**
     * This action returns RESULT_CODE_SUCCESS if the Smime Provider already has the certificate
     * corresponding to the given certificate id in its database.
     * <p/>
     * It returns RESULT_CODE_USER_INTERACTION_REQUIRED if the Provider does not have the certificate.
     * The PendingIntent from RESULT_INTENT can be used to retrieve those from a certificate server.
     * <p/>
     * If an Output stream has been defined the whole public certificate is returned.
     * required extras:
     * long        EXTRA_CERTIFICATE_ID
     * <p/>
     * optional extras:
     * String      EXTRA_REQUEST_ASCII_ARMOR (request that the returned certificate is encoded in ASCII Armor)
     *
     */
    public static final String ACTION_GET_CERTIFICATE = "org.openintents.smime.action.GET_CERTIFICATE";

    /* Intent extras */
    public static final String EXTRA_API_VERSION = "api_version";
    // ACTION_SIGN
    public static final String RESULT_DETACHED_SIGNATURE = "detached_signature";

    // ENCRYPT, SIGN_AND_ENCRYPT
    public static final String EXTRA_USER_IDS = "user_ids";
    public static final String EXTRA_CERTIFICATE_IDS = "certificate_ids";
    public static final String EXTRA_SIGN_CERTIFICATE_ID = "sign_certificate_id";
    // optional extras:
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_ORIGINAL_FILENAME = "original_filename";
    public static final String EXTRA_ENABLE_COMPRESSION = "enable_compression";
    public static final String EXTRA_ENCRYPT_OPPORTUNISTIC = "opportunistic";

    // GET_SIGN_CERTIFICATE_ID
    public static final String EXTRA_USER_ID = "user_id";

    // GET_CERTIFICATE
    public static final String EXTRA_CERTIFICATE_ID = "certificate_id";
    public static final String RESULT_CERTIFICATE_IDS = "key_ids";

    /* Service Intent returns */
    public static final String RESULT_CODE = "result_code";

    // get actual error object from RESULT_ERROR
    public static final int RESULT_CODE_ERROR = 0;
    // success!
    public static final int RESULT_CODE_SUCCESS = 1;
    // get PendingIntent from RESULT_INTENT, start PendingIntent with startIntentSenderForResult,
    // and execute service method again in onActivityResult
    public static final int RESULT_CODE_USER_INTERACTION_REQUIRED = 2;

    public static final String RESULT_ERROR = "error";
    public static final String RESULT_INTENT = "intent";

    // DECRYPT_VERIFY
    public static final String EXTRA_DETACHED_SIGNATURE = "detached_signature";
    public static final String RESULT_SIGNATURE = "signature";
    public static final String RESULT_DECRYPTION = "decryption";
    public static final String RESULT_METADATA = "metadata";
    // This will be the charset which was specified in the headers of ascii armored input, if any
    public static final String RESULT_CHARSET = "charset";

    // INTERNAL, should not be used
    public static final String EXTRA_CALL_UUID1 = "call_uuid1";
    public static final String EXTRA_CALL_UUID2 = "call_uuid2";


    //TODO: delete
    public static final String EXTRA_INPUT = "org.openintents.smime.extra.EXTRA_INPUT";
    public static final String EXTRA_OUTPUT = "org.openintents.smime.extra.EXTRA_OUTPUT";
    public static final String EXTRA_IDENTITY = "org.openintents.smime.extra.EXTRA_IDENTITY";
    public static final String EXTRA_OTHERPARTY = "org.openintents.smime.extra.EXTRA_OTHERPARTY";
    public static final String HAS_PRIVATE_KEY = "org.openintents.smime.action.HAS_PRIVATE_KEY";
    public static final String HAS_PUBLIC_KEY = "org.openintents.smime.action.HAS_PUBLIC_KEY";
    public static final String EXTRA_RESULT_ERROR = "org.openintents.smime.extra.ERROR";
    public static final String EXTRA_RESULT_CODE = "org.openintents.smime.extra.RESULT_CODE";

    public static final String RESULT_TYPE = "org.openintents.smime.extra.RESULT_TYPE";
    public static final int RESULT_TYPE_UNENCRYPTED_UNSIGNED = 0;
    public static final int RESULT_TYPE_ENCRYPTED = 1;
    public static final int RESULT_TYPE_SIGNED = 2;

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
        Intent intent = new Intent(ACTION_SIGN_AND_ENCRYPT);
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
     */
    public Intent executeApi(Intent data, SMimeDataSource dataSource, OutputStream os) {
        ParcelFileDescriptor input = null;
        try {
            if (dataSource != null) {
                input = ParcelFileDescriptorUtil.asyncPipeFromDataSource(dataSource);
            }

            return executeApi(data, input, os);
        } catch (Exception e) {
            Log.e(SMimeApi.TAG, "Exception in executeApi call", e);
            Intent result = new Intent();
            result.putExtra(RESULT_CODE, RESULT_CODE_ERROR);
            result.putExtra(RESULT_ERROR,
                    new SmimeError(SmimeError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
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

    public interface SMimeDataSource {
        void writeTo(OutputStream os) throws IOException;
    }

    public interface PermissionPingCallback {
        void onSmimePermissionCheckResult(Intent result);
    }

    public void checkPermissionPing(final PermissionPingCallback permissionPingCallback) {
        Intent intent = new Intent(SMimeApi.ACTION_CHECK_PERMISSION);
        executeApiAsync(intent, null, null, new ISMimeCallback() {
            @Override
            public void onReturn(Intent result) {
                permissionPingCallback.onSmimePermissionCheckResult(result);
            }
        });
    }
}
