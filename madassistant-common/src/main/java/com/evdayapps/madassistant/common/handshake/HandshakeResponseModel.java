package com.evdayapps.madassistant.common.handshake;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data model for the handshake response
 * This is the model that is sent back whenever a client requests a connection to the
 * repository application
 */
public class HandshakeResponseModel implements Parcelable {

    public static final Creator<HandshakeResponseModel> CREATOR = new Creator<HandshakeResponseModel>() {
        @Override
        public HandshakeResponseModel createFromParcel(Parcel in) {
            return new HandshakeResponseModel(in);
        }

        @Override
        public HandshakeResponseModel[] newArray(int size) {
            return new HandshakeResponseModel[size];
        }
    };
    /**
     * Was the handshake successful
     * Usually true
     * False if:
     * - the application doesn't support the sdk version of the client
     */
    public boolean successful;
    /**
     * Human readable reason for the failure if {@link #successful} is false
     */
    public String errorMessage;
    /**
     * An encrypted string that is shared by the developer with the user
     * The user enters this into the app from the settings screen for an app
     * This string contains the permissions that are applicable to the current device
     */
    public String authToken;

    public HandshakeResponseModel() {

    }

    protected HandshakeResponseModel(Parcel in) {
        successful = in.readByte() != 0;
        errorMessage = in.readString();
        authToken = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (successful ? 1 : 0));
        dest.writeString(errorMessage);
        dest.writeString(authToken);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
