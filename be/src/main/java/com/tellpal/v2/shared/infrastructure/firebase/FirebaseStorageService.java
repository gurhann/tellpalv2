package com.tellpal.v2.shared.infrastructure.firebase;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseStorageService.class);

    private final String storageBucket;

    public FirebaseStorageService(
            @Value("${app.firebase.storage-bucket:tellpal-dev.appspot.com}") String storageBucket) {
        this.storageBucket = storageBucket;
    }

    /**
     * Uploads data to Firebase Storage at the given object path.
     *
     * @param objectPath  the destination path within the bucket (e.g. "content/story/key/tr/cover.jpg")
     * @param data        raw bytes to upload
     * @param contentType MIME type (e.g. "image/jpeg")
     * @return the object path that was uploaded
     */
    public String upload(String objectPath, byte[] data, String contentType) {
        try {
            StorageClient storageClient = getStorageClient();
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(storageBucket, objectPath))
                    .setContentType(contentType)
                    .build();
            storageClient.bucket().getStorage().create(blobInfo, data);
            log.debug("Uploaded object to Firebase Storage: {}", objectPath);
            return objectPath;
        } catch (FirebaseStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FirebaseStorageException(
                    "Failed to upload object to Firebase Storage: " + objectPath, e);
        }
    }

    /**
     * Returns a signed download URL for the given object path (valid for 1 hour).
     *
     * @param objectPath the path within the bucket
     * @return a signed URL string
     */
    public String getDownloadUrl(String objectPath) {
        try {
            StorageClient storageClient = getStorageClient();
            var blob = storageClient.bucket().get(objectPath);
            if (blob == null) {
                throw new FirebaseStorageException(
                        "Object not found in Firebase Storage: " + objectPath);
            }
            return blob.signUrl(1, TimeUnit.HOURS).toString();
        } catch (FirebaseStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FirebaseStorageException(
                    "Failed to get download URL for object: " + objectPath, e);
        }
    }

    /**
     * Deletes the object at the given path from Firebase Storage.
     *
     * @param objectPath the path within the bucket
     */
    public void delete(String objectPath) {
        try {
            StorageClient storageClient = getStorageClient();
            var blob = storageClient.bucket().get(objectPath);
            if (blob != null) {
                blob.delete();
                log.debug("Deleted object from Firebase Storage: {}", objectPath);
            } else {
                log.warn("Attempted to delete non-existent object: {}", objectPath);
            }
        } catch (FirebaseStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FirebaseStorageException(
                    "Failed to delete object from Firebase Storage: " + objectPath, e);
        }
    }

    private StorageClient getStorageClient() {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new FirebaseStorageException(
                    "Firebase is not initialized. "
                    + "Set FIREBASE_CREDENTIALS_PATH to enable Firebase Storage.");
        }
        return StorageClient.getInstance();
    }
}
