package javax.baja.status;

import javax.baja.sys.BValue;

public class BStatusValue extends BValue {
    private BStatus status = BStatus.ok;

    public BStatus getStatus() {
        return status;
    }

    public void setStatus(BStatus status) {
        this.status = status;
    }
}
