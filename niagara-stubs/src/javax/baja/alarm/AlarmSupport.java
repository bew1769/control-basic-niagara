package javax.baja.alarm;

import javax.baja.sys.BFacets;
import javax.baja.sys.Context;

/** Compile-only stand-in. The real class lives in alarm-rt.jar. */
public class AlarmSupport {
    public AlarmSupport(BIAlarmSource source, BAlarmSourceInfo info) {
    }

    public BAlarmRecord newFaultAlarm(BFacets facets) throws Exception {
        return null;
    }

    public void toNormal(BFacets facets, Context cx) throws Exception {
    }

    public boolean ackAlarm(BAlarmRecord record) throws Exception {
        return false;
    }

    public BAlarmRecord getLastFault() {
        return null;
    }
}
