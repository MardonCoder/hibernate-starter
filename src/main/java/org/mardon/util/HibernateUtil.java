package org.mardon.util;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.Configuration;
import org.mardon.converter.BirthdayConverter;

@UtilityClass
public class HibernateUtil {

    static {
        var configuration = buildConfiguration();
        configuration.configure();

        sessionFactory = configuration.buildSessionFactory();
    }
    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory(){
        if (sessionFactory.isClosed()) {
            Configuration configuration = new Configuration();
            configuration.setPhysicalNamingStrategy(new PhysicalNamingStrategySnakeCaseImpl());
            configuration.addAttributeConverter(BirthdayConverter.class); // !!КОНВЕРТЕР НЕ ЗАБУДЬ!!
            configuration.configure();

            sessionFactory = configuration.buildSessionFactory();
            return sessionFactory;
        }
        return sessionFactory;
    }

    private final String sql = """
                    DROP TABLE IF EXISTS profile;
                    DROP SEQUENCE IF EXISTS profile_id_seq;
                    DROP TABLE IF EXISTS users;
                    DROP SEQUENCE IF EXISTS users_id_seq;
                    DROP TABLE IF EXISTS company_locale;
                    DROP TABLE IF EXISTS company;
                    DROP SEQUENCE IF EXISTS company_id_seq;
                    
                    CREATE TABLE company
                    (
                        id INT PRIMARY KEY ,
                        name VARCHAR(64) NOT NULL UNIQUE
                    );
                    CREATE SEQUENCE company_id_seq INCREMENT 1 START 1;
                    
                    CREATE TABLE company_locale(
                        company_id INT NOT NULL REFERENCES company(id),
                        lang CHAR(2),
                        description VARCHAR(128) NOT NULL,
                        PRIMARY KEY (company_id, lang)
                    );
                    
                    CREATE TABLE users
                    (
                        id BIGINT PRIMARY KEY,
                        username VARCHAR(128) UNIQUE NOT NULL ,
                        firstname VARCHAR(128),
                        lastname VARCHAR(128),
                        birth_date DATE,
                        role VARCHAR(128),
                        info JSONB,
                        company_id INT REFERENCES company(id)
                    );
                    CREATE SEQUENCE users_id_seq INCREMENT 1 START 1;
                    
                    CREATE TABLE profile(
                        id BIGINT PRIMARY KEY,
                        user_id BIGINT NOT NULL UNIQUE REFERENCES users,
                        street VARCHAR(64),
                        language CHAR(2)
                    );
                    CREATE SEQUENCE profile_id_seq INCREMENT 1 START 1;
                    """;

    public void clearDb() {
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeMutationQuery(sql).executeUpdate();
            session.getTransaction().commit();
        }
    }

    public Configuration buildConfiguration(){
        Configuration configuration = new Configuration();
        configuration.setPhysicalNamingStrategy(new PhysicalNamingStrategySnakeCaseImpl());
        configuration.addAttributeConverter(BirthdayConverter.class); // !!КОНВЕРТЕР НЕ ЗАБУДЬ!!
        return configuration;
    }
}
