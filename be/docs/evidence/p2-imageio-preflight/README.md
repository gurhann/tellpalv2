# Java 25 encoder preflight

Date: 2026-10-16. This is a synthetic codec/runtime preflight, not P2 engine acceptance, a production profile or a human quality verdict.

`ImageIoPreflight.java` generates a deterministic 257×193 opaque RGB fixture. It encodes PNG and JPEG, writes new files, reopens them through fresh ImageIO readers and checks actual format, byte count, SHA-256, dimensions and decode. PNG additionally checks all 49,601 RGB pixels. Missing files and half-length files fail independent reread verification. The probe derives MIME from the reader format; it does not claim an external storage service supplied that MIME. JPEG q85 is a runtime control only.

## Host

`raw-results.txt`: Homebrew OpenJDK 25.0.2, macOS/aarch64.

From the repository root, use a fresh directory:

```sh
probe_dir=$(mktemp -d /private/tmp/tellpal-p2-preflight.XXXXXX)
java be/docs/evidence/p2-imageio-preflight/ImageIoPreflight.java "$probe_dir"
```

The primary agent independently reproduced the same output hashes and outcomes after the investigation agent's original run.

## Deployment-base runtime, locally

`temurin25-container-results.txt`: Eclipse Adoptium 25.0.4.1+1-LTS, Linux/aarch64. Official `eclipse-temurin:25-jre` image, as used by the backend Dockerfile, resolved during this run to:

```text
sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64
```

The preflight used a local Colima VM (existing 4 CPU/4 GiB configuration), Docker 29.5.2 and a container limited to one CPU, 192 MiB memory/swap, 64 PIDs, 32 MiB `/tmp`, 96 MiB Java heap and 16 MiB direct memory. Networking was disabled and the root filesystem read-only. These are synthetic test limits, not approved production budgets or a peak native/RSS measurement. Classes were compiled with host `javac --release 25`; bytecode then ran in the actual JRE container.

```sh
mkdir -p be/target/p2-container-preflight/classes
probe_output=$(mktemp -d "$PWD/be/target/p2-container-preflight/output.XXXXXX")
javac --release 25 -d be/target/p2-container-preflight/classes \
  be/docs/evidence/p2-imageio-preflight/ImageIoPreflight.java
docker --context colima run --rm --network none --read-only \
  --memory 192m --memory-swap 192m --cpus 1 --pids-limit 64 \
  --tmpfs /tmp:rw,size=32m \
  -v "$PWD/be/target/p2-container-preflight/classes:/probe:ro" \
  -v "$probe_output:/output:rw" \
  eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64 \
  java -Xmx96m -XX:MaxDirectMemorySize=16m -Djava.awt.headless=true \
  -cp /probe ImageIoPreflight /output
```

The first attempt mounted host `/private/tmp`, which was not shared into this existing Colima VM. The JVM could not locate the class; the failure is retained in `temurin25-container-mount-failure.txt`. Repeating with workspace-backed mounts passed. No failed attempt is represented as encoder success.

## Results and limits

Both host and JRE container produced:

| Format | Bytes | SHA-256 |
| --- | ---: | --- |
| PNG | 149157 | `ea6033481d8418140d31723627177e652da8b25e02533c642c3793ac3c9a09da` |
| JPEG | 23857 | `1153c14ab2c07422ddc555cbc2a3dfbc9fa33186b5c9f7009ade9f1370be57a1` |

Both PNG/JPEG providers actually encoded and decoded; their class and provider versions appear in raw results. Identical outputs for this fixture do not prove universal cross-runtime determinism or codec quality. No full application image, GCS network operation, production rollout, mobile quality comparison or P2 worker acceptance is inferred from this preflight. The P2 engine needs its own end-to-end, resource and PostgreSQL evidence.


## Separate packaged P2 child proof

`PackagedReadingProbe.java` runs the actual `ReadingImageProcess` from the executable Spring Boot JAR. It creates independent red/green/blue/white source quadrants, waits for real child exit, writes the result with CREATE_NEW, then uses a fresh decoder to check 40×30 PNG geometry and four fixed expected color sentinels. Source bytes and persisted SHA-256 are also checked. Dimensions/hash alone would accept a blank encoder regression; the color assertions would reject it.

After `bash ./mvnw verify` from `be/`, reproduce from the repository root:

```sh
mkdir -p be/target/p2-packaged-probe/classes
packaged_output=$(mktemp -d "$PWD/be/target/p2-packaged-probe/output.XXXXXX")
javac --release 25 -cp be/target/classes -d be/target/p2-packaged-probe/classes \
  be/docs/evidence/p2-imageio-preflight/PackagedReadingProbe.java
docker --context colima run --rm --network none --read-only \
  --memory 768m --memory-swap 768m --cpus 1 --pids-limit 128 \
  --tmpfs /tmp:rw,size=64m \
  -v "$PWD/be/target/tellpal-v2-backend-0.0.1-SNAPSHOT.jar:/app/app.jar:ro" \
  -v "$PWD/be/target/p2-packaged-probe/classes:/probe:ro" \
  -v "$packaged_output:/output:rw" \
  eclipse-temurin:25-jre@sha256:fd4b1c02c64eef378212e23656fc0b0caa41a357876fa0b60e51ceda94a3db64 \
  java -Xmx256m -XX:MaxDirectMemorySize=32m -Djava.awt.headless=true \
  -Dloader.main=PackagedReadingProbe -Dloader.path=/probe -cp /app/app.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher /output
```

The synthetic profile declares a 32 MiB parent reservation, child heap/direct limits of 96/16 MiB, process ceiling 256 MiB, 4 MiB temporary space and explicit growth admission. These are local probe values only. The [engine verification report](../p2-reading-preparation/verification.md) links the final output and exact tested JAR hash. This proof is conversion/persistence inside the deployment-base JRE, not a deployed job/publication flow or live Firebase proof.
