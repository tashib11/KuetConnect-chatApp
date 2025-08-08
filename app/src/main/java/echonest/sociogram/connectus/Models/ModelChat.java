package echonest.sociogram.connectus.Models;

import android.graphics.Bitmap;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ModelChat {
    String message, receiver, sender, timestamp, type, aesKey, messageId;
    boolean isSeen;

    // Additional fields for image and video uploading
    String localImageUri; // Temporary local URI during upload
    boolean isUploading; // Indicates if the message is being uploaded
    int uploadProgress; // Upload progress percentage
    String thumbnailUrl; // URI for video thumbnail during upload

    // Add this field for message status
    String messageStatus; // "Sending", "Sent", or "Seen"

    transient Bitmap decryptedBitmap;
    // Default constructor
    public ModelChat() {
    }

    // Constructor for general use
    public ModelChat(String message, String receiver, String sender, String timestamp, String type, boolean isSeen, String thumbnailUrl, String messageStatus, String aesKey) {
        this.message = message;
        this.receiver = receiver;
        this.sender = sender;
        this.timestamp = timestamp;
        this.type = type;
        this.isSeen = isSeen;
        this.thumbnailUrl = thumbnailUrl;
        this.localImageUri = null;
        this.isUploading = false;
        this.uploadProgress = 0;
        this.messageStatus = messageStatus;
        this.aesKey = aesKey; // Initialize aesKey
    }

    public Bitmap getDecryptedBitmap() {
        return decryptedBitmap;
    }

    public void setDecryptedBitmap(Bitmap bitmap) {
        this.decryptedBitmap = bitmap;
    }

    // Getters and setters
    public String getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(String messageStatus) {
        this.messageStatus = messageStatus;
    }


    // Getter and Setter for message
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Getter and Setter for receiver
    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    // Getter and Setter for sender
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    // Getter and Setter for timestamp
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // Getter and Setter for type
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Getter and Setter for isSeen
    public boolean isSeen() {
        return isSeen;
    }

    public void setSeen(boolean seen) {
        isSeen = seen;
    }

    // Getter and Setter for localImageUri
    public String getLocalImageUri() {
        return localImageUri;
    }

    public void setLocalImageUri(String localImageUri) {
        this.localImageUri = localImageUri;
    }

    // Getter and Setter for isUploading
    public boolean isUploading() {
        return isUploading;
    }

    public void setUploading(boolean uploading) {
        isUploading = uploading;
    }

    // Getter and Setter for uploadProgress
    public int getUploadProgress() {
        return uploadProgress;
    }

    public void setUploadProgress(int uploadProgress) {
        this.uploadProgress = uploadProgress;
    }

    // Getter and Setter for thumbnailUri
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }


    public String getFormattedTimestamp() {
        try {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(Long.parseLong(timestamp));
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.ENGLISH);
            return sdf.format(cal.getTime());
        } catch (NumberFormatException e) {
            return "";
        }
    }
    // Getter for aesKey
    public String getAesKey() {
        return aesKey;
    }

    // Setter for aesKey
    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
    // Add getter and setter
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
}
