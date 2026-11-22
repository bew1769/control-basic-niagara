package javax.baja.status;

import javax.baja.sys.BInterface;

/** Compile-only stand-in. The real interface lives in baja.jar. */
public interface BIStatus extends BInterface {
    BStatus getStatus();
}
