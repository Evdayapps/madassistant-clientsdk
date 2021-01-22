package com.evdayapps.madassistant.common.transmission;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model for data that is transmitted from the client to the repository
 */
public class TransmissionModel implements Parcelable {

    public static final Creator<TransmissionModel> CREATOR = new Creator<TransmissionModel>() {
        @Override
        public TransmissionModel createFromParcel(Parcel source) {
            return new TransmissionModel(source);
        }

        @Override
        public TransmissionModel[] newArray(int size) {
            return new TransmissionModel[size];
        }
    };
    public long sessionId;
    public String transmissionId;
    public long timestamp;
    public int numTotalSegments;
    public int currentSegmentIndex;
    public int type;
    public boolean encrypted;
    public byte[] payload;

    public TransmissionModel(
        long sessionId,
        String transmissionId,
        long timestamp,
        boolean encrypted,
        int numTotalSegments,
        int currentSegmentIndex,
        int type,
        byte[] payload
    ) {
        this.sessionId = sessionId;
        this.transmissionId = transmissionId;
        this.timestamp = timestamp;
        this.encrypted = encrypted;
        this.numTotalSegments = numTotalSegments;
        this.currentSegmentIndex = currentSegmentIndex;
        this.type = type;
        this.payload = payload;
    }

    protected TransmissionModel(Parcel in) {
        this.sessionId = in.readLong();
        this.transmissionId = in.readString();
        this.timestamp = in.readLong();
        this.encrypted = in.readByte() != 0;
        this.numTotalSegments = in.readInt();
        this.currentSegmentIndex = in.readInt();
        this.type = in.readInt();
        this.payload = in.createByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.sessionId);
        dest.writeString(this.transmissionId);
        dest.writeLong(this.timestamp);
        dest.writeByte(this.encrypted ? (byte) 1 : (byte) 0);
        dest.writeInt(this.numTotalSegments);
        dest.writeInt(this.currentSegmentIndex);
        dest.writeInt(this.type);
        dest.writeByteArray(this.payload);
    }
}
