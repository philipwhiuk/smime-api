/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import org.openintents.smime.ISMimeService;
import org.openintents.smime.SmimeError;
import org.openintents.smime.R;

public class SMimeCertificatePreference extends Preference {
    private long mCertificateId;
    private String mSmimeProvider;
    private SMimeServiceConnection mServiceConnection;
    private String mDefaultUserId;

    public static final int REQUEST_CODE_KEY_PREFERENCE = 9999;

    private static final int NO_CERTIFICATE = 0;

    public SMimeCertificatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        return (mCertificateId == NO_CERTIFICATE) ? getContext().getString(R.string.smime_no_certificate_selected)
                : getContext().getString(R.string.smime_certificate_selected);
    }

    private void updateEnabled() {
        if (TextUtils.isEmpty(mSmimeProvider)) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }

    public void setSmimeProvider(String packageName) {
        mSmimeProvider = packageName;
        updateEnabled();
    }

    public void setDefaultUserId(String userId) {
        mDefaultUserId = userId;
    }

    @Override
    protected void onClick() {
        // bind to service
        mServiceConnection = new SMimeServiceConnection(
                getContext().getApplicationContext(),
                mSmimeProvider,
                new SMimeServiceConnection.OnBound() {
                    @Override
                    public void onBound(ISMimeService service) {

                        getSignCertificateId(new Intent());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(SMimeApi.TAG, "exception on binding!", e);
                    }
                }
        );
        mServiceConnection.bindToService();
    }

    private void getSignCertificateId(Intent data) {
        data.setAction(SMimeApi.ACTION_GET_SIGN_CERTIFICATE_ID);
        data.putExtra(SMimeApi.EXTRA_USER_ID, mDefaultUserId);

        SMimeApi api = new SMimeApi(getContext(), mServiceConnection.getService());
        api.executeApiAsync(data, null, null, new MyCallback(REQUEST_CODE_KEY_PREFERENCE));
    }

    //TODO: Rename Me
    private class MyCallback implements SMimeApi.ISMimeCallback {
        int requestCode;

        private MyCallback(int requestCode) {
            this.requestCode = requestCode;
        }

        @Override
        public void onReturn(Intent result) {
            switch (result.getIntExtra(SMimeApi.RESULT_CODE, SMimeApi.RESULT_CODE_ERROR)) {
                case SMimeApi.RESULT_CODE_SUCCESS: {

                    long keyId = result.getLongExtra(SMimeApi.EXTRA_SIGN_CERTIFICATE_ID, NO_CERTIFICATE);
                    save(keyId);

                    break;
                }
                case SMimeApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {

                    PendingIntent pi = result.getParcelableExtra(SMimeApi.RESULT_INTENT);
                    try {
                        Activity act = (Activity) getContext();
                        act.startIntentSenderFromChild(
                                act, pi.getIntentSender(),
                                requestCode, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(SMimeApi.TAG, "SendIntentException", e);
                    }
                    break;
                }
                case SMimeApi.RESULT_CODE_ERROR: {
                    SmimeError error = result.getParcelableExtra(SMimeApi.RESULT_ERROR);
                    if (error !=null)
                        Log.e(SMimeApi.TAG, "RESULT_CODE_ERROR: " + error.getMessage());
                    else
                        Log.e(SMimeApi.TAG, "App did not provide cause of error");
                    break;
                }
            }
        }
    }

    private void save(long newValue) {
        // Give the client a chance to ignore this change if they deem it
        // invalid
        if (!callChangeListener(newValue)) {
            // They don't want the value to be set
            return;
        }

        setAndPersist(newValue);
    }

    /**
     * Public API
     */
    public void setValue(long keyId) {
        setAndPersist(keyId);
    }

    /**
     * Public API
     */
    public long getValue() {
        return mCertificateId;
    }

    private void setAndPersist(long newValue) {
        mCertificateId = newValue;

        // Save to persistent storage (this method will make sure this
        // preference should be persistent, along with other useful checks)
        persistLong(mCertificateId);

        // Data has changed, notify so UI can be refreshed!
        notifyChanged();

        // also update summary
        setSummary(getSummary());
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // This preference type's value type is Long, so we read the default
        // value from the attributes as an Integer.
        return (long) a.getInteger(index, NO_CERTIFICATE);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            // Restore state
            mCertificateId = getPersistedLong(mCertificateId);
        } else {
            // Set state
            long value = (Long) defaultValue;
            setAndPersist(value);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.certificateId = mCertificateId;
        myState.smimeProvider = mSmimeProvider;
        myState.defaultUserId = mDefaultUserId;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mCertificateId = myState.certificateId;
        mSmimeProvider = myState.smimeProvider;
        mDefaultUserId = myState.defaultUserId;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p/>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        long certificateId;
        String smimeProvider;
        String defaultUserId;

        public SavedState(Parcel source) {
            super(source);

            certificateId = source.readInt();
            smimeProvider = source.readString();
            defaultUserId = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeLong(certificateId);
            dest.writeString(smimeProvider);
            dest.writeString(defaultUserId);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_KEY_PREFERENCE && resultCode == Activity.RESULT_OK) {
            getSignCertificateId(data);
            return true;
        } else {
            return false;
        }
    }

}