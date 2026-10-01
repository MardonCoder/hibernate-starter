package util;

import lombok.experimental.UtilityClass;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.mardon.entity.*;
import org.mardon.listener.AuditTableListener;
import org.mardon.util.HibernateUtil;
import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class HibernateTestUtil {

    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.6");
    private SessionFactory sessionFactory;

    static {
        postgres.start();
        sessionFactory = initializeSessionFactory();
    }

    public SessionFactory getSessionFactory(){
        if (sessionFactory.isClosed())
            sessionFactory = initializeSessionFactory();
        return sessionFactory;
    }

    private SessionFactory initializeSessionFactory(){
        var configuration = HibernateUtil.buildConfiguration();
        configuration.setProperty("hibernate.connection.url", postgres.getJdbcUrl());
        configuration.setProperty("hibernate.connection.username", postgres.getUsername());
        configuration.setProperty("hibernate.connection.password", postgres.getPassword());
        configuration.configure();

        var sf =  configuration.buildSessionFactory();

        var sessionFactoryImpl = sf.unwrap(SessionFactoryImpl.class);
        var listenerRegistry = sessionFactoryImpl.getServiceRegistry().getService(EventListenerRegistry.class);
        var auditTableListener = new AuditTableListener();
        listenerRegistry.appendListeners(EventType.PRE_INSERT, auditTableListener);
        listenerRegistry.appendListeners(EventType.PRE_DELETE, auditTableListener);

        return sf;
    }

    private final String sql = """
                    DROP TABLE IF EXISTS company_aud;
                    DROP TABLE IF EXISTS payments_aud;
                    DROP TABLE IF EXISTS revinfo;
                    DROP SEQUENCE IF EXISTS revinfo_seq;
                    
                    DROP TABLE IF EXISTS profile;
                    DROP SEQUENCE IF EXISTS profile_id_seq;
                    DROP TABLE IF EXISTS payments;
                    DROP SEQUENCE IF EXISTS payments_id_seq;
                    DROP TABLE IF EXISTS users;
                    DROP SEQUENCE IF EXISTS users_id_seq;
                    DROP TABLE IF EXISTS company_locale;
                    DROP TABLE IF EXISTS company;
                    DROP SEQUENCE IF EXISTS company_id_seq;
                    DROP TABLE IF EXISTS audit;
                    DROP SEQUENCE IF EXISTS audit_id_seq;
                    
                    
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
                        company_id INT REFERENCES company(id),
                        type VARCHAR(31),
                        project_name VARCHAR(128),
                        language VARCHAR(31)
                    );
                    CREATE SEQUENCE users_id_seq INCREMENT 1 START 1;
                    
                    CREATE TABLE profile(
                        id BIGINT PRIMARY KEY,
                        user_id BIGINT NOT NULL UNIQUE REFERENCES users,
                        street VARCHAR(64),
                        language CHAR(2)
                    );
                    CREATE SEQUENCE profile_id_seq INCREMENT 1 START 1;
                    
                    CREATE TABLE payments(
                        id BIGINT PRIMARY KEY ,
                        amount INT NOT NULL ,
                        receiver_id BIGINT REFERENCES users,
                        version BIGINT,
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP,
                        created_by VARCHAR(50),
                        updated_by VARCHAR(50)
                    );
                    CREATE SEQUENCE payments_id_seq INCREMENT 1 START 1;
                    
                    CREATE SEQUENCE IF NOT EXISTS audit_id_seq INCREMENT 1 START 1;
                    
                    CREATE TABLE audit (
                        id BIGINT NOT NULL,
                        entity_id VARCHAR(255),
                        entity_name VARCHAR(255),
                        entity_content VARCHAR(255),
                        operation VARCHAR(255),
                        CONSTRAINT pk_audit PRIMARY KEY (id)
                    );
                    
                    
                    -- Секвенция и глобальная таблица ревизий Envers
                    CREATE SEQUENCE IF NOT EXISTS revinfo_seq INCREMENT BY 50 START WITH 50;
                    
                    CREATE TABLE IF NOT EXISTS revinfo (
                        rev INTEGER NOT NULL,
                        revtstmp BIGINT,
                        username VARCHAR(255),
                        PRIMARY KEY (rev)
                    );
                    
                    -- Таблица аудита для сущности Company
                    CREATE TABLE company_aud (
                        id INTEGER NOT NULL,
                        rev INTEGER NOT NULL,
                        revtype SMALLINT,
                        name VARCHAR(255),
                        PRIMARY KEY (id, rev),
                        CONSTRAINT fk_company_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
                    );
                    
                    -- Таблица аудита для сущности Payment
                    CREATE TABLE payments_aud (
                        id BIGINT NOT NULL,
                        rev INTEGER NOT NULL,
                        revtype SMALLINT,
                        amount INTEGER,
                        version BIGINT,
                        receiver_id BIGINT,
                    
                        -- Поля, унаследованные от AuditableEntity
                        created_at TIMESTAMPTZ,
                        created_by VARCHAR(255),
                        updated_at TIMESTAMPTZ,
                        updated_by VARCHAR(255),
                    
                        PRIMARY KEY (id, rev),
                        CONSTRAINT fk_payments_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
                    );
                    
                    """;

    public void recreateDb(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeMutationQuery(sql).executeUpdate();
            session.getTransaction().commit();
        }
    }

    public static String genNoise(int length){
        return new Random().ints(48, 123) // от '0' (48) до 'z' (122)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)) // фильтрация спецсимволов между цифрами и буквами
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static User createRandomUser(){
        Random r = ThreadLocalRandom.current();
        var user = User.builder()
                .role(Role.USER)
                .username(genNoise(10))
                .personalInfo(PersonalInfo.builder()
                        .birthDate(new Birthday(LocalDate.of(r.nextInt(1970, 2006), r.nextInt(1, 12), r.nextInt(1, 28))))
                        .firstname(genNoise(7))
                        .lastname(genNoise(7))
                        .build())
                .build();

        Profile profile = Profile.builder().language(genNoise(2)).street(genNoise(10)).build();
        user.setProfile(profile);

        return user;
    }

    public static User createRandomUser(Company c){
        var u = createRandomUser();
        u.setCompany(c);
        return u;
    }

    public static Payment createRandomPayment(){
        Random r = ThreadLocalRandom.current();
        return Payment.builder().amount(r.nextInt(10, 20_000)).build();
    }

    public static Payment createRandomPayment(User user){
        var payment = createRandomPayment();
        user.addPayment(payment);
        return payment;
    }

    public static Company createRandomCompany(){
        return Company.builder().name(genNoise(7)).build();
    }

    public String getInfo(){
        return """
                url: %s
                username: %s
                dbname: %s
                password: %s
                """.formatted(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getDatabaseName(), postgres.getPassword());
    }

    public String getLink(){
        return "psql -h localhost -p %s -U %s -d %s".formatted(
                postgres.getJdbcUrl().substring(28, 33), // jdbc:postgresql://localhost:64492/test?loggerLevel=OFF
                postgres.getUsername(),
                postgres.getDatabaseName()
        );
    }

    public Session createSessionProxy(){
        return (Session) Proxy.newProxyInstance(sessionFactory.getClass().getClassLoader(), new Class[]{Session.class},
                ((proxy, method, args) -> method.invoke(sessionFactory.getCurrentSession(), args)));
    }
}
