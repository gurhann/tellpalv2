package com.tellpal.v2.asset.reading.p5g;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/** Yerel macOS child RSS'sini ayrıcalıklı araç çalıştırmadan salt okur. */
final class P5gRss {
    private static final int STRUCT_BYTES = 96;
    private static final long RESIDENT_OFFSET = 64;
    private static MethodHandle handle;

    private P5gRss() { }

    private static synchronized MethodHandle reader() throws IOException {
        if (handle != null) return handle;
        try {
            var library = SymbolLookup.libraryLookup("/usr/lib/libproc.dylib", Arena.global());
            handle = Linker.nativeLinker().downcallHandle(library.find("proc_pid_rusage").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            return handle;
        } catch (RuntimeException | LinkageError error) { throw new IOException("RSS_MONITOR_UNAVAILABLE", error); }
    }

    static long read(long pid) throws IOException {
        // SDK sys/resource.h: RUSAGE_INFO_V0 yapısında RSS alanı 64. byte'tadır.
        try (var arena = Arena.ofConfined()) {
            var memory = arena.allocate(STRUCT_BYTES, 8);
            int result = (int) reader().invokeExact(Math.toIntExact(pid), 0, memory);
            long bytes = memory.get(ValueLayout.JAVA_LONG, RESIDENT_OFFSET);
            if (result == 0 && bytes > 0) return bytes;
            if (!ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) return 0;
            throw new IOException("RSS_MONITOR_UNAVAILABLE");
        } catch (IOException error) { throw error; }
        catch (Throwable error) { throw new IOException("RSS_MONITOR_UNAVAILABLE", error); }
    }
}
