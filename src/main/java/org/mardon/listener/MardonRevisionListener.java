package org.mardon.listener;

import org.hibernate.envers.RevisionListener;
import org.mardon.entity.Revision;

public class MardonRevisionListener implements RevisionListener {
    @Override
    public void newRevision(Object revisionEntity) {
        ((Revision) revisionEntity).setUsername("mardon");
    }
}
