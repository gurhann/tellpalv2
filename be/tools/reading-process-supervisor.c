/* Her işi ayrı oturumda tutar; guardian çıkmadan bütün sahipli süreçler durur. */
#define _GNU_SOURCE
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <signal.h>
#include <unistd.h>
#include <poll.h>
#include <fcntl.h>
#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef __linux__
#include <sys/prctl.h>
#include <dirent.h>
#else
#include <sys/sysctl.h>
#endif
#ifndef SOURCE_SHA256
#error SOURCE_SHA256 gerekli
#endif
static volatile sig_atomic_t stop_requested;
static void stop(int sig) { (void)sig; stop_requested = 1; }
/* Zombie artık çalışamaz/yazamaz; Linux guardian onu ayrıca waitpid ile toplar. */
static int members(pid_t group, FILE *output) {
    int live = 0;
#ifdef __linux__
    DIR *directory = opendir("/proc");
    if (!directory) return -1;
    struct dirent *entry;
    while ((entry = readdir(directory))) {
        char *end; long pid = strtol(entry->d_name, &end, 10);
        if (*end || pid <= 0) continue;
        char path[128], line[8192], state; long parent, pgid;
        snprintf(path, sizeof path, "/proc/%ld/stat", pid);
        FILE *status = fopen(path, "r");
        if (!status) { if (errno != ENOENT && errno != ESRCH) { closedir(directory); return -1; } continue; }
        char *read = fgets(line, sizeof line, status); fclose(status);
        if (!read) continue;
        char *name_end = strrchr(line, ')');
        if (!name_end || sscanf(name_end + 1, " %c %ld %ld", &state, &parent, &pgid) != 3) { closedir(directory); return -1; }
        if (pgid == group && state != 'Z' && state != 'X') { live++; if (output) fprintf(output, "%ld\n", pid); }
    }
    closedir(directory);
#else
    int query[] = {CTL_KERN, KERN_PROC, KERN_PROC_PGRP, group}; size_t size = 0;
    if (sysctl(query, 4, NULL, &size, NULL, 0)) return -1;
    struct kinfo_proc *processes = malloc(size + 64 * sizeof *processes);
    if (!processes) return -1;
    size += 64 * sizeof *processes;
    if (sysctl(query, 4, processes, &size, NULL, 0)) { free(processes); return -1; }
    for (size_t i = 0; i < size / sizeof *processes; i++) {
        if (processes[i].kp_proc.p_stat != SZOMB) {
            live++; if (output) fprintf(output, "%d\n", processes[i].kp_proc.p_pid);
        }
    }
    free(processes);
#endif
    return live;
}
int main(int argc, char **argv) {
    if (argc == 2 && !strcmp(argv[1], "--version")) { puts("tellpal-reading-supervisor-v1 " SOURCE_SHA256 " compiler=" __VERSION__); return 0; }
    if (argc < 3) return 126;
#ifdef __linux__
    if (prctl(PR_SET_CHILD_SUBREAPER, 1)) return 126;
#endif
    signal(SIGTERM, stop); signal(SIGINT, stop); signal(SIGHUP, stop);
    int ready[2]; if (pipe(ready)) return 126;
    pid_t worker = fork();
    if (worker < 0) return 126;
    if (!worker) {
        close(ready[0]);
        if (setsid() != getpid()) _exit(126);
        if (write(ready[1], "R", 1) != 1) _exit(126);
        close(ready[1]);
        /* Parent lifeline yalnız guardian'a aittir; native araçlara taşınmaz. */
        close(STDIN_FILENO); open("/dev/null", 0);
        signal(SIGTERM, SIG_DFL); signal(SIGINT, SIG_DFL); signal(SIGHUP, SIG_DFL);
        execvp(argv[2], argv + 2); _exit(127);
    }
    close(ready[1]); char established = 0;
    ssize_t n; do { n = read(ready[0], &established, 1); } while (n < 0 && errno == EINTR);
    close(ready[0]);
    if (n != 1 || established != 'R') { kill(worker, SIGKILL); waitpid(worker, NULL, 0); return 126; }
    /* Worker reap edilmeden PID'si tekrar kullanılamaz; sinyal yalnız bu PGID'ye gider. */
    int stopping = 0, unexpected = 0, status = 0, empty = 0;
    char temporary[4096]; snprintf(temporary, sizeof temporary, "%s.tmp", argv[1]);
    for (;;) {
        siginfo_t exited; memset(&exited, 0, sizeof exited);
        if (waitid(P_PID, worker, &exited, WEXITED | WNOHANG | WNOWAIT) && errno != EINTR) stop_requested = 1;
        if (exited.si_pid == worker) {
            status = exited.si_code == CLD_EXITED ? exited.si_status : 128 + exited.si_status;
            if (!stopping && members(worker, NULL) > 0) unexpected = 1;
            stopping = 1;
        }
        struct pollfd lifeline = {STDIN_FILENO, POLLIN | POLLHUP, 0};
        if (poll(&lifeline, 1, 0) > 0 || stop_requested) stopping = 1;
        if (stopping) {
            /* Tek taramaya dayanmaz: geç fork da aynı sahipli grupta kalır. */
            kill(-worker, SIGSTOP); kill(-worker, SIGKILL);
        }
        FILE *snapshot = fopen(temporary, "w");
        int live = members(worker, snapshot);
        int published = snapshot != NULL;
        if (snapshot && fclose(snapshot)) published = 0;
        if (!published || rename(temporary, argv[1]) || live < 0) { stopping = 1; unexpected = 1; }
        if (stopping && live == 0) { if (++empty == 2) break; } else empty = 0;
        struct timespec pause = {0, 10000000}; nanosleep(&pause, NULL);
    }
    /* Linux subreaper gerçek wait tamamlanana kadar bütün orphan çocukları toplar. */
    while (waitpid(-1, NULL, 0) > 0 || errno == EINTR) { }
    unlink(temporary);
    return unexpected ? 125 : status;
}
