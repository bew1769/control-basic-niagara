package javax.baja.alarm;

import javax.baja.sys.BBoolean;
import javax.baja.sys.BInterface;

/** Compile-only stand-in. The real interface lives in alarm-rt.jar. */
public interface BIAlarmSource extends BInterface {
    BBoolean ackAlarm(BAlarmRecord record);
}
