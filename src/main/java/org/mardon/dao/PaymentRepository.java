package org.mardon.dao;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.mardon.entity.Payment;


public class PaymentRepository extends RepositoryBase<Long, Payment> {

    public PaymentRepository(EntityManager entityManager){
        super(Payment.class, entityManager);
    }
}
