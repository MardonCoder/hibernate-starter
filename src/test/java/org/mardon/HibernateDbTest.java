package org.mardon;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.mardon.entity.*;
import org.mardon.util.HibernateUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class HibernateDbTest {

    private static final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    private static Company company;
    private static User userWithCompany;
    private static User userWithoutCompany;

    @BeforeEach
    void clearDbAndInitializeFields(){
        HibernateUtil.clearDb();
        company = Company.builder().name("Google").build();
        userWithCompany = User.builder()
                .username("Dummy@gmail.com")
                .role(Role.USER)
                .personalInfo(PersonalInfo.builder()
                        .firstname("Dummy")
                        .lastname("Dummer")
                        .birthDate(new Birthday(LocalDate.of(2000,1,1)))
                        .build())
                .info("""
                        {
                        "is_test": true,
                        "number": 1,
                        "all": "good"
                        }
                        """)
                .company(company)
                .build();
        userWithoutCompany = User.builder()
                .username("Dummy@gmail.com")
                .role(Role.USER)
                .personalInfo(PersonalInfo.builder()
                        .firstname("Dummy")
                        .lastname("Dummer")
                        .birthDate(new Birthday(LocalDate.of(2000,1,1)))
                        .build())
                .info("""
                        {
                        "is_test": true,
                        "number": 1,
                        "all": "good"
                        }
                        """)
                .build();
    }

    @Test
    void addingCompanyAndUserTest(){
        User fromDbUser;
        Company fromUserCompany;
        Company fromDbCompany;
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(company);
            session.persist(userWithCompany);
            session.getTransaction().commit();
        }

        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            fromDbUser = session.find(User.class, userWithCompany.getId());
            fromUserCompany = fromDbUser.getCompany();
            fromDbCompany = session.find(Company.class, company.getId());
            session.getTransaction().commit();
        }

        assertSoftly(softly ->{
            softly.assertThat(company.getId()).isEqualTo(fromDbCompany.getId());
            softly.assertThat(company.getId()).isEqualTo(fromUserCompany.getId());
            softly.assertThat(userWithCompany.getId()).isEqualTo(fromDbUser.getId());
        });
    }

    @Test
    void cascadePersistTest(){
        // cascade persist
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(userWithCompany);
            session.getTransaction().commit();
        }

        Company fromDbCompany;
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            fromDbCompany = session.find(Company.class, company.getId());
        }
        assertSoftly(softly ->{
            softly.assertThat(company.getId()).isEqualTo(fromDbCompany.getId());
            softly.assertThat(company.getName()).isEqualTo(fromDbCompany.getName());
        });
    }

    @Test
    void testProfile(){
        var user = User.builder().username("Vanya").build();
        Profile profile = null;
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(user);
            profile = Profile.builder().language("EN").street("Pushkin").user(user).build();
            profile.setUser(user);
            session.persist(profile);
            session.getTransaction().commit();
        }

        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            var userFromDb = session.find(User.class, user.getId());
            var profileFromDb = session.find(Profile.class, profile.getId());
            session.getTransaction().commit();

//            assertThat(userFromDb.getProfile().getId()).isEqualTo(profileFromDb.getId());
        }
    }

    @Test
    void testLocaleInfo(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();

            session.persist(company );

            var company1 = session.find(Company.class, 1);
            company1.getLocales().add(LocaleInfo.of("ru", "Описание на русском"));
            company1.getLocales().add(LocaleInfo.of("en", "English description"));

            session.getTransaction().commit();
        }
    }

    @Test
    void testOrdering(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();

            var user1 = User.builder().username("Abror").personalInfo(PersonalInfo.builder().lastname("Abrorov").build()).company(company).build();
            var user2 = User.builder().username("Babajon").company(company).build();
            var user3 = User.builder().username("Damir").company(company).build();

            session.persist(user3);
            session.persist(user1);
            session.persist(user2);

            // 1. Принудительно отправляем INSERT-ы в базу данных
            session.flush();
            // 2. Очищаем L1 кэш сессии. Без этого session.get() вернет объект из памяти с пустой коллекцией
            session.clear();

            // 3. Загружаем компанию из БД. При обращении к employees Hibernate сгенерирует SELECT с ORDER BY
            Company loadedCompany = session.find(Company.class, company.getId());

            Set<User> users = loadedCompany.getEmployees();

            assertSoftly(softly -> {
                softly.assertThat(users.contains(user1)).isTrue();
                softly.assertThat(users.contains(user2)).isTrue();
                softly.assertThat(users.contains(user3)).isTrue();
            });

            session.getTransaction().commit();
        }
    }

    @Test
    void testMapping(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();

            session.persist(userWithCompany);

            session.flush();
            session.clear();

            var loadedCompany = session.find(Company.class, userWithCompany.getCompany().getId());
            assertThat(loadedCompany.getEmployees().contains(userWithCompany));


            session.getTransaction().commit();
        }
    }

    @AfterAll
    static void closeSessionFactoryAndClearDb(){
        HibernateUtil.clearDb();
        sessionFactory.close();
    }
}