// MADAssistantRepositoryAIDL.aidl
package com.evdayapps.madassistant.common;

// Declare any non-default types here with import statements
import com.evdayapps.madassistant.common.handshake.HandshakeResponseModel;
import com.evdayapps.madassistant.common.transmission.TransmissionModel;

interface MADAssistantRepositoryAIDL {
    /**
     * Connect to this service
     *
     * @return A string (shared by the developer to the user) that is a string representation of
     * the user's rights, encrypted with the appropriate passphrase and key. This is processed by
     * the library in the client app and deemed worthy or not
     */
    HandshakeResponseModel performHandshake(int sdkVersion);

    long startSession();

    void endSession();

    void log(in TransmissionModel data);
}
