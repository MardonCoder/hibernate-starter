package org.mardon.listener;

import org.hibernate.event.spi.*;
import org.mardon.entity.Audit;

public class AuditTableListener implements PreDeleteEventListener, PreInsertEventListener {
    @Override
    public boolean onPreDelete(PreDeleteEvent preDeleteEvent) {
        auditEntity(preDeleteEvent, Audit.Operation.DELETE);
        return false;
    }

    @Override
    public boolean onPreInsert(PreInsertEvent preInsertEvent) {
        auditEntity(preInsertEvent, Audit.Operation.INSERT);
        return false;
    }

    private void auditEntity(AbstractPreDatabaseOperationEvent event, Audit.Operation operation){
        if (event.getEntity().getClass() != Audit.class){
            var audit = Audit.builder()
                    .entityId(event.getId() != null ? event.getId().toString() : null)
                    .entityName(event.getEntity().getClass().getName())
                    .entityContent(event.getEntity().toString())
                    .operation(operation)
                    .build();
            var factory = event.getSession().getFactory();
            try (var statelessSession = factory.openStatelessSession()) {
                statelessSession.insert(audit);
            }
        }
    }
}
