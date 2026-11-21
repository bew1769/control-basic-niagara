package javax.baja.workbench.view;

import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.ui.BWidget;

public class BWbView extends BWidget {
    private BWidget content;

    public void setContent(BWidget content) {
        this.content = content;
    }

    public BWidget getContent() {
        return content;
    }

    protected void doLoadValue(BObject value, Context cx) throws Exception {
    }
}
