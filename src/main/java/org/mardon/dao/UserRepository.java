package org.mardon.dao;

import jakarta.persistence.EntityManager;
import org.mardon.entity.User;

public class UserRepository extends RepositoryBase<Long, User> {
    public UserRepository(EntityManager entityManager){
        super(User.class, entityManager);
    }
}
