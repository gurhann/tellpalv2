package com.tellpal.v2.user.web.mobile;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Doğrulanmış body'yi nonblocking yazar; payload ve permit gerçek cleanup'a kadar tutulur. */
@Component
@org.springframework.context.annotation.Profile({"local","test"})
public class ReadingImageComparisonResponseWriter {
    private final Semaphore capacity=new Semaphore(1,true);
    private final java.util.concurrent.ScheduledExecutorService timer=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("reading-response-deadline").factory());
    @jakarta.annotation.PreDestroy public void close(){timer.shutdownNow();}
    private final long deadlineMillis;
    private volatile long heldBytes;
    public ReadingImageComparisonResponseWriter(@Value("${tellpal.reading-comparison.deadline-millis:0}") long deadlineMillis) {
        this.deadlineMillis=deadlineMillis;
    }
    public void requireSupported(HttpServletRequest request) {
        if(!request.isAsyncSupported() || !(request.getAttribute(ReadingImageComparisonTomcatConfiguration.ABORT) instanceof Runnable))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bounded Tomcat response yeteneği gerekli");
    }
    public Lease acquire() {
        if(deadlineMillis<=0 || deadlineMillis>Integer.MAX_VALUE) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Açık response süresi gerekli");
        if(!capacity.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Teslimat kapasitesi dolu");
        return new Lease();
    }
    public int activeTransfers(){return 1-capacity.availablePermits();}
    public long heldPayloadBytes(){return heldBytes;}
    public final class Lease implements AutoCloseable {
        private final long started=System.nanoTime();
        private final AtomicBoolean closed=new AtomicBoolean();
        long remainingMillis(){return deadlineMillis-java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);}
        @Override public void close(){if(closed.compareAndSet(false,true)){heldBytes=0;capacity.release();}}
    }
    public void write(HttpServletRequest request,HttpServletResponse response,byte[] bytes,Lease lease) throws IOException {
        long remaining=lease.remainingMillis();
        if(remaining<=0) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Response süresi doldu");
        response.setContentType("image/png");response.setContentLength(bytes.length);
        response.setHeader("Cache-Control","private,no-store");response.setHeader("Connection","close");
        Object capability=request.getAttribute(ReadingImageComparisonTomcatConfiguration.ABORT);
        if(!(capability instanceof Runnable abort))throw new IllegalStateException("BOUNDED_RESPONSE_CONTAINER_REQUIRED");
        var async=request.startAsync();
        var writer=new Transfer(async,bytes,lease,abort);heldBytes=bytes.length;
        boolean listening=false;
        try {
            async.addListener(writer);listening=true;async.setTimeout(remaining);
            writer.deadline=timer.schedule(writer::abort,remaining,java.util.concurrent.TimeUnit.MILLISECONDS);
            writer.output=response.getOutputStream();writer.output.setWriteListener(writer);
        } catch(IOException|RuntimeException setupFailure) {
            writer.requestCompletion();
            if(!listening)writer.release();
        }
    }
    private static final class Transfer implements WriteListener,AsyncListener {
        private final AsyncContext async;
        private ServletOutputStream output;
        private final Lease lease;
        private byte[] bytes;
        private int offset;
        private boolean completionRequested,released;
        private final Runnable abortConnection;
        private java.util.concurrent.ScheduledFuture<?> deadline;
        Transfer(AsyncContext async,byte[] bytes,Lease lease,Runnable abort){this.async=async;this.bytes=bytes;this.lease=lease;this.abortConnection=abort;}
        @Override public synchronized void onWritePossible() {
            if(bytes==null)return;
            try {
                while(output.isReady() && bytes!=null) {
                    if(lease.remainingMillis()<=0){abort();return;}
                    int count=Math.min(8192,bytes.length-offset);
                    output.write(bytes,offset,count);offset+=count;
                    if(offset==bytes.length){requestCompletion();return;}
                }
            } catch(IOException|RuntimeException failure){requestCompletion();}
        }
        private synchronized void requestCompletion() {
            bytes=null;
            if(completionRequested)return;
            completionRequested=true;
            try{async.complete();}catch(IllegalStateException alreadyCompleted){release();}
        }
        private synchronized void abort(){
            if(released)return;
            bytes=null;abortConnection.run();requestCompletion();
        }
        private synchronized void release(){if(released)return;released=true;bytes=null;if(deadline!=null)deadline.cancel(false);lease.close();}
        @Override public void onError(Throwable failure){requestCompletion();release();}
        @Override public void onComplete(AsyncEvent event){release();}
        @Override public void onTimeout(AsyncEvent event){abort();}
        @Override public void onError(AsyncEvent event){requestCompletion();release();}
        @Override public void onStartAsync(AsyncEvent event){ }
    }
}
