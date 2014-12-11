

package org.sufficientlysecure.rootcommands.util;

import java.io.IOException;

public class BrokenBusyboxException extends IOException {
    private static final long serialVersionUID = 8337358201589488409L;

    public BrokenBusyboxException() {
        super();
    }

    public BrokenBusyboxException(String detailMessage) {
        super(detailMessage);
    }

}
