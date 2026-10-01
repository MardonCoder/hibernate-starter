package org.mardon.dao;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.mardon.entity.Company;

public class CompanyRepository extends RepositoryBase<Integer, Company> {
    public CompanyRepository(EntityManager entityManager){
        super(Company.class, entityManager);
    }
}
