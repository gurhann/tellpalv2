package com.tellpal.v2.asset.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.StandardOpenOption;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.ServiceAccountCredentials;

/** Fail-closed local validation; dry run never calls credential or private-file methods. */
record ProbeConfiguration(boolean execute, String project, String bucket, Path credential, Path receipt, Path repository) {
    static ProbeConfiguration parse(String[] args) {
        var options = new HashMap<String, String>();
        boolean execute = false;
        for (int index = 0; index < args.length; index++) {
            if (args[index].equals("--execute-live") && !execute) { execute = true; continue; }
            if (!Set.of("--project", "--bucket", "--credential", "--receipt", "--repository").contains(args[index])
                    || index + 1 >= args.length || options.putIfAbsent(args[index], args[++index]) != null)
                throw new IllegalArgumentException("CONFIGURATION");
        }
        String project = options.get("--project");
        String bucket = options.get("--bucket");
        if (project == null || !project.matches("[a-z][a-z0-9-]{4,28}[a-z0-9]")) throw new IllegalArgumentException("PROJECT");
        if (bucket == null || !bucket.matches("[a-z0-9][a-z0-9._-]{1,220}[a-z0-9]")) throw new IllegalArgumentException("BUCKET");
        return new ProbeConfiguration(execute, project, bucket, absolute(options.get("--credential")),
                absolute(options.get("--receipt")), absolute(options.get("--repository")));
    }

    private static Path absolute(String text) {
        if (text == null || !Path.of(text).isAbsolute()) throw new IllegalArgumentException("ABSOLUTE_PATH_REQUIRED");
        return Path.of(text).normalize();
    }

    ServiceAccountCredentials credentials() throws IOException {
        privateFile(credential);
        if (Files.size(credential) > 65_536) throw new IOException("CREDENTIAL_SIZE");
        byte[] bytes;
        try (var channel = Files.newByteChannel(credential, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
                var input = Channels.newInputStream(channel)) {
            bytes = input.readNBytes(65_537);
        }
        if (bytes.length > 65_536) throw new IOException("CREDENTIAL_SIZE");
        var mapper = new ObjectMapper().enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        var json = mapper.readTree(bytes);
        if (!json.path("type").asText().equals("service_account") || !json.path("project_id").asText().equals(project)
                || !json.path("client_email").asText().matches("[a-zA-Z0-9._-]+@" + java.util.regex.Pattern.quote(project) + "\\.iam\\.gserviceaccount\\.com")
                || !json.path("token_uri").asText().equals("https://oauth2.googleapis.com/token")
                || !json.path("universe_domain").asText("googleapis.com").equals("googleapis.com"))
            throw new IOException("CREDENTIAL_BINDING");
        return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(bytes));
    }

    void privateFile(Path path) throws IOException {
        privateLocation(path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)
                || !Files.getOwner(path, LinkOption.NOFOLLOW_LINKS).equals(Files.getOwner(Path.of(System.getProperty("user.home")))))
            throw new IOException("PRIVATE_FILE");
        privatePermissions(path, false);
    }

    void privateLocation(Path path) throws IOException {
        Path root = repository.toRealPath();
        if (!Files.isDirectory(root.resolve(".git")) || !Files.isRegularFile(root.resolve("AGENTS.md")))
            throw new IOException("REPOSITORY_ROOT");
        Path parent = path.getParent().toRealPath();
        if (!parent.equals(path.getParent()) || parent.startsWith(root) || path.startsWith(root)) throw new IOException("PRIVATE_LOCATION");
        privatePermissions(parent, true);
        if (!Files.getOwner(parent).equals(Files.getOwner(Path.of(System.getProperty("user.home"))))) throw new IOException("PRIVATE_OWNER");
    }

    static void privatePermissions(Path path, boolean directory) throws IOException {
        var permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
        var allowed = directory ? Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
                : Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        if (!allowed.containsAll(permissions) || !permissions.contains(PosixFilePermission.OWNER_READ)
                || (directory && !permissions.contains(PosixFilePermission.OWNER_EXECUTE))) throw new IOException("PRIVATE_PERMISSIONS");
    }
}
