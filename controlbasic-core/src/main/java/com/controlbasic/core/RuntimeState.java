package com.controlbasic.core;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Persistent scan state: variables, timers, PID, and the print buffer. */
public final class RuntimeState {
    public TimeSource time = TimeSource.SYSTEM;
    public double scanIntervalSeconds = 1;
    public PointImage points;

    final Map<String, Double> vars = new HashMap<String, Double>();
    final Map<String, double[]> arrays = new HashMap<String, double[]>();
    final Map<Integer, boolean[]> edges = new HashMap<Integer, boolean[]>();
    final Map<String, TimerMark> timers = new HashMap<String, TimerMark>();
    final Map<String, Boolean> levelPrev = new HashMap<String, Boolean>();
    final Map<Integer, Long> intervals = new HashMap<Integer, Long>();
    final Map<Integer, PidMark> pids = new HashMap<Integer, PidMark>();
    final Map<Integer, double[][]> tables = new HashMap<Integer, double[][]>();
    final Random random = new Random();
    final StringBuilder console = new StringBuilder();

    long scans;
    boolean halted;
    int onErrorIp = -1;
    boolean inHandler;
    int steps;

    public void reset() {
        vars.clear();
        arrays.clear();
        edges.clear();
        timers.clear();
        levelPrev.clear();
        intervals.clear();
        pids.clear();
        console.setLength(0);
        scans = 0;
        halted = false;
        onErrorIp = -1;
        inHandler = false;
        steps = 0;
    }

    public long scanCount() {
        return scans;
    }

    public boolean isHalted() {
        return halted;
    }

    public String console() {
        return console.toString();
    }

    public double getVar(String name) {
        Double v = vars.get(Functions.canon(name));
        return v == null ? 0 : v.doubleValue();
    }

    public void setTable(int index, double[] xs, double[] ys) {
        if (xs == null || ys == null || xs.length != ys.length || xs.length == 0) {
            tables.remove(Integer.valueOf(index));
            return;
        }
        tables.put(Integer.valueOf(index), new double[][] {xs.clone(), ys.clone()});
    }

    void appendConsole(String text) {
        console.append(text);
        if (console.length() > 4000) {
            console.delete(0, console.length() - 4000);
        }
    }

    static final class TimerMark {
        boolean on;
        long since;

        TimerMark(boolean on, long since) {
            this.on = on;
            this.since = since;
        }
    }

    static final class PidMark {
        double integral;
        double prevError;
        boolean hasPrevError;
        long lastMillis;
        boolean started;
    }

    static final class ForFrame {
        final String name;
        final double end;
        final double step;
        final int afterIp;
        final int bodyIp;

        ForFrame(String name, double end, double step, int afterIp, int bodyIp) {
            this.name = name;
            this.end = end;
            this.step = step;
            this.afterIp = afterIp;
            this.bodyIp = bodyIp;
        }
    }
}
