package com.tellpal.v2.asset.application;

import com.tellpal.v2.shared.infrastructure.firebase.FirebaseStorageService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Packages optimized page assets into ZIP files and uploads them to Firebase Storage.
 *
 * <p>STORY content produces two ZIP packages:
 * <ul>
 *   <li>part1.zip — pages 1–3</li>
 *   <li>part2.zip — pages 4+</li>
 * </ul>
 *
 * <p>Non-STORY content (AUDIO_STORY, MEDITATION, LULLABY) produces a single content.zip.
 *
 * <p>Files inside each ZIP are named by page number: {@code 1.jpg}, {@code 1.mp3}, etc.
 */
@Service
public class ZipPackagingService {

    /**
     * Represents one page's assets (image + audio bytes).
     *
     * @param pageNumber  1-based page number
     * @param imageBytes  JPEG image bytes, may be null
     * @param audioBytes  MP3 audio bytes, may be null
     */
    public record PageAsset(int pageNumber, byte[] imageBytes, byte[] audioBytes) {}

    /**
     * Result of a single ZIP upload.
     *
     * @param packageName  logical name of the package (e.g. "part1.zip")
     * @param storagePath  full Firebase Storage object path
     * @param downloadUrl  signed download URL
     */
    public record ZipPackageResult(String packageName, String storagePath, String downloadUrl) {}

    private final FirebaseStorageService firebaseStorageService;

    public ZipPackagingService(FirebaseStorageService firebaseStorageService) {
        this.firebaseStorageService = firebaseStorageService;
    }

    /**
     * Packages STORY content into two ZIP files and uploads them.
     *
     * @param pages           all story pages (any order; split is done by pageNumber)
     * @param baseStoragePath base path in Firebase Storage (e.g. {@code /content/STORY/key/tr/packages})
     * @return list of uploaded package results (part1 always present; part2 only if pages 4+ exist)
     */
    public List<ZipPackageResult> packageStory(List<PageAsset> pages, String baseStoragePath) {
        List<PageAsset> part1Pages = pages.stream()
                .filter(p -> p.pageNumber() <= 3)
                .toList();

        List<PageAsset> part2Pages = pages.stream()
                .filter(p -> p.pageNumber() > 3)
                .toList();

        List<ZipPackageResult> results = new ArrayList<>();

        results.add(uploadZip(part1Pages, "part1.zip", baseStoragePath + "/part1.zip"));

        if (!part2Pages.isEmpty()) {
            results.add(uploadZip(part2Pages, "part2.zip", baseStoragePath + "/part2.zip"));
        }

        return results;
    }

    /**
     * Packages non-STORY content into a single ZIP file and uploads it.
     *
     * @param pages       all content pages
     * @param storagePath base path in Firebase Storage (e.g. {@code /content/MEDITATION/key/tr/packages})
     * @return the uploaded package result
     */
    public ZipPackageResult packageSingle(List<PageAsset> pages, String storagePath) {
        return uploadZip(pages, "content.zip", storagePath + "/content.zip");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ZipPackageResult uploadZip(List<PageAsset> pages, String packageName, String objectPath) {
        byte[] zipBytes = buildZip(pages);
        firebaseStorageService.upload(objectPath, zipBytes, "application/zip");
        String downloadUrl = firebaseStorageService.getDownloadUrl(objectPath);
        return new ZipPackageResult(packageName, objectPath, downloadUrl);
    }

    /**
     * Builds a ZIP archive from the given pages.
     *
     * <p>For each page the following entries are added (when the byte array is non-null):
     * <ul>
     *   <li>{@code {pageNumber}.jpg} — image bytes</li>
     *   <li>{@code {pageNumber}.mp3} — audio bytes</li>
     * </ul>
     *
     * @param pages pages to include
     * @return raw ZIP bytes
     */
    private byte[] buildZip(List<PageAsset> pages) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (PageAsset page : pages) {
                if (page.imageBytes() != null) {
                    addEntry(zos, page.pageNumber() + ".jpg", page.imageBytes());
                }
                if (page.audioBytes() != null) {
                    addEntry(zos, page.pageNumber() + ".mp3", page.audioBytes());
                }
            }
        } catch (IOException e) {
            throw new ZipPackagingException("Failed to build ZIP package", e);
        }
        return baos.toByteArray();
    }

    private void addEntry(ZipOutputStream zos, String entryName, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(data);
        zos.closeEntry();
    }
}
