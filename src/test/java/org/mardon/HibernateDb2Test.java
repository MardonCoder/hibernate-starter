package org.mardon;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.jpa.HibernateHints;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mardon.dao.CompanyRepository;
import org.mardon.dao.PaymentRepository;
import org.mardon.dao.UserRepository;
import org.mardon.dto.UserCreateDto;
import org.mardon.dto.UserReadDto;
import org.mardon.entity.*;
import org.mardon.filter.QPredicate;
import org.mardon.filter.UserFilter;
import org.mardon.interceptor.TransactionInterceptor;
import org.mardon.mapper.CompanyReadMapper;
import org.mardon.mapper.UserCreateMapper;
import org.mardon.mapper.UserReadMapper;
import org.mardon.service.UserService;
import util.HibernateTestUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.Assertions.tuple;
import static util.HibernateTestUtil.*;

class HibernateDb2Test {
    private static final SessionFactory sessionFactory = HibernateTestUtil.getSessionFactory();

    @BeforeEach
    void bdPrepare(){
        HibernateTestUtil.recreateDb();
    }

    @Test
    void testHql(){
        try (var session = sessionFactory.openSession()) {
            var dummyCompany = HibernateTestUtil.createRandomCompany();
            var dummyUser = HibernateTestUtil.createRandomUser(dummyCompany);

            session.beginTransaction();
            session.persist(dummyUser);

            var result = session.createQuery(
                    "select u from User u " +
                            "left join u.company c " +
                            "where u.personalInfo.firstname = :firstname and c.name = :companyName " +
                            "order by u.personalInfo.lastname desc", User.class)
                    .setParameter("firstname", dummyUser.getPersonalInfo().getFirstname())
                    .setParameter("companyName", dummyCompany.getName())
                    .list();

            var result2 = session.createNamedQuery("FindUser", User.class)
                    .setParameter("firstname", dummyUser.getPersonalInfo().getFirstname())
                    .setParameter("companyName", dummyCompany.getName())
//                    .setQueryFlushMode()
                    .setHint(AvailableHints.HINT_FETCH_SIZE, 50)
                    .setMaxResults(10)
//                    .setFirstResult(5)  // offset
                    .list();


            var countRows = session.createMutationQuery("update User u set u.role = 'ADMIN'").executeUpdate();
            session.flush();
            session.clear();


            var changedRoleUser = session.find(User.class, dummyUser.getId());
            session.getTransaction().commit();

            assertSoftly(softly -> {
                softly.assertThat(result).extracting(User::getUsername).containsExactly(dummyUser.getUsername());
                softly.assertThat(result2).extracting(User::getUsername).containsExactly(dummyUser.getUsername());
                softly.assertThat(changedRoleUser.getRole()).isEqualTo(Role.ADMIN);
            });
        }
    }

    @Test
    void criteriaFindAllTest(){
        try (var session = sessionFactory.openSession()) {
            var dummyCompany = HibernateTestUtil.createRandomCompany();
            var dummyUser = HibernateTestUtil.createRandomUser(dummyCompany);
            var dummyCompany2 = HibernateTestUtil.createRandomCompany();
            var dummyUser2 = HibernateTestUtil.createRandomUser(dummyCompany2);

            session.beginTransaction();
            session.persist(dummyUser);
            session.persist(dummyUser2);
            session.getTransaction().commit();
            session.clear();


            var cb = session.getCriteriaBuilder();
            var criteria = cb.createQuery(User.class);
            var user = criteria.from(User.class);
            criteria.select(user);
            var list = session.createQuery(criteria).list();

            assertThat(list)
                    .hasSize(2)
                    .extracting(User::getUsername)
                    .containsExactlyInAnyOrder(dummyUser.getUsername(), dummyUser2.getUsername());

        }
    }

    @Test
    void criteriaFindAllByFirstName(){
        try (var session = sessionFactory.openSession()) {
            var dummyCompany = HibernateTestUtil.createRandomCompany();
            var dummyUser = HibernateTestUtil.createRandomUser(dummyCompany);
            var dummyCompany2 = HibernateTestUtil.createRandomCompany();
            var dummyUser2 = HibernateTestUtil.createRandomUser(dummyCompany2);
            var dummyUser3 = User.builder().username("Noise").personalInfo(dummyUser.getPersonalInfo()).build();

            session.beginTransaction();
            session.persist(dummyUser);
            session.persist(dummyUser2);
            session.persist(dummyUser3);

            session.getTransaction().commit();
            var firstName = dummyUser.getPersonalInfo().getFirstname();
            session.clear();

            var cb = session.getCriteriaBuilder();
            var criteria = cb.createQuery(User.class);
            var user = criteria.from(User.class);

            criteria.select(user).where(
                    cb.equal(user.get(User_.personalInfo).get(PersonalInfo_.firstname), firstName)
            );

            var result = session.createQuery(criteria).list();

            assertThat(result)
                    .hasSize(2)
                    .extracting(u -> u.getPersonalInfo().getFirstname())
                    .containsOnly(firstName);
        }
    }

    @Test
    void criteriaLimitAndOrderTest(){
        try (var session = sessionFactory.openSession()) {
            var dummyUser = HibernateTestUtil.createRandomUser();
            var dummyUser2 = HibernateTestUtil.createRandomUser();
            var dummyUser3 = HibernateTestUtil.createRandomUser();
            var dummyUser4 = HibernateTestUtil.createRandomUser();


            session.beginTransaction();
            session.persist(dummyUser);
            session.persist(dummyUser2);
            session.persist(dummyUser3);
            session.persist(dummyUser4);
            session.getTransaction().commit();


            var cb = session.getCriteriaBuilder();
            var criteria = cb.createQuery(User.class);
            var user = criteria.from(User.class); // user table
            criteria.select(user);
            criteria.orderBy(cb.asc(user.get(User_.personalInfo).get(PersonalInfo_.birthDate)));

            var result = session.createQuery(criteria).setMaxResults(3).list();

            assertThat(result)
                    .hasSize(3)
                    .extracting(u -> u.getPersonalInfo().getBirthDate().birthdate())
                    .isSorted();
        }
    }

    @Test
    void criteriaFindAllByCompanyTest(){
        try (var session = sessionFactory.openSession()) {
            var goodCompany = HibernateTestUtil.createRandomCompany();
            var badCompany = HibernateTestUtil.createRandomCompany();
            var user1 = HibernateTestUtil.createRandomUser(goodCompany);
            var user2 = HibernateTestUtil.createRandomUser(goodCompany);
            var user3 = HibernateTestUtil.createRandomUser(badCompany);
            var user4 = HibernateTestUtil.createRandomUser(badCompany);
            var companyName = goodCompany.getName();

            session.beginTransaction();
            session.persist(user1);
            session.persist(user2);
            session.persist(user3);
            session.persist(user4);
            session.getTransaction().commit();
            session.clear();

            var cb = session.getCriteriaBuilder();
            var criteria = cb.createQuery(User.class);
            var company = criteria.from(Company.class);
            var employees = company.join(Company_.employees);

            criteria.select(employees).where(
                    cb.equal(company.get(Company_.name), companyName)
            );

            var result = session.createQuery(criteria).list();

            assertThat(result)
                    .hasSize(2)
                    .extracting(u -> u.getCompany().getName())
                    .containsOnly(companyName);
        }
    }

    @Test
    void queryDslFindAllTest(){
        try (var session = sessionFactory.openSession()) {
            var dummyCompany = HibernateTestUtil.createRandomCompany();
            var dummyUser = HibernateTestUtil.createRandomUser(dummyCompany);
            var dummyCompany2 = HibernateTestUtil.createRandomCompany();
            var dummyUser2 = HibernateTestUtil.createRandomUser(dummyCompany2);

            session.beginTransaction();
            session.persist(dummyUser);
            session.persist(dummyUser2);
            session.getTransaction().commit();
            session.clear();

            var result = new JPAQuery<User>(session)
                    .select(QUser.user)
                    .from(QUser.user)
                    .fetch();

            assertThat(result)
                    .hasSize(2)
                    .extracting(User::getUsername)
                    .containsExactlyInAnyOrder(dummyUser.getUsername(), dummyUser2.getUsername());
        }
    }

    @Test
    void queryDslFindByFirstNameTest(){
        try (var session = sessionFactory.openSession()) {
            var dummyCompany = HibernateTestUtil.createRandomCompany();
            var dummyUser = HibernateTestUtil.createRandomUser(dummyCompany);
            var dummyCompany2 = HibernateTestUtil.createRandomCompany();
            var dummyUser2 = HibernateTestUtil.createRandomUser(dummyCompany2);
            var dummyUser3 = User.builder().username("Noise").personalInfo(dummyUser.getPersonalInfo()).build();
            var firstName = dummyUser.getPersonalInfo().getFirstname();

            session.beginTransaction();
            session.persist(dummyUser);
            session.persist(dummyUser2);
            session.persist(dummyUser3);
            session.getTransaction().commit();
            session.clear();


            var result = new JPAQuery<User>(session)
                    .select(QUser.user)
                    .from(QUser.user)
                    .where(QUser.user.personalInfo.firstname.eq(firstName))
                    .fetch();

            assertThat(result)
                    .hasSize(2)
                    .extracting(u -> u.getPersonalInfo().getFirstname())
                    .containsOnly(firstName);
        }
    }

    @Test
    void findLimitAndOrderTest(){
        try (var session = sessionFactory.openSession()) {
            var user1 = HibernateTestUtil.createRandomUser();
            user1.getPersonalInfo().setFirstname("Alice");

            var user2 = HibernateTestUtil.createRandomUser();
            user2.getPersonalInfo().setFirstname("Bob");

            var user3 = HibernateTestUtil.createRandomUser();
            user3.getPersonalInfo().setFirstname("Charlie");

            var user4 = HibernateTestUtil.createRandomUser();
            user4.getPersonalInfo().setFirstname("David");


            session.beginTransaction();
            session.persist(user1);
            session.persist(user2);
            session.persist(user3);
            session.persist(user4);
            session.getTransaction().commit();
            session.clear();

            var result = new JPAQuery<User>(session)
                    .select(QUser.user)
                    .from(QUser.user)
                    .orderBy(QUser.user.personalInfo.firstname.asc())
                    .limit(2)
                    .fetch();

            assertThat(result)
                    .hasSize(2)
                    .extracting(u -> u.getPersonalInfo().getFirstname())
                    .containsExactly("Alice", "Bob");
        }
    }

    @Test
    void findAllByFilterTest(){
        var u1 = HibernateTestUtil.createRandomUser();
        var u2 = User.builder().username("NOISE").personalInfo(u1.getPersonalInfo()).build();
        var u3 = HibernateTestUtil.createRandomUser();
        var firstName = u1.getPersonalInfo().getFirstname();
        var lastName = u2.getPersonalInfo().getLastname();

        var filter = UserFilter.builder().firstname(firstName).lastname(lastName).build();
//        List<Predicate> predicates = new ArrayList<>();
//        if (filter.getFirstname() != null)
//            predicates.add(QUser.user.personalInfo.firstname.eq(filter.getFirstname()));
//        if (filter.getLastname() != null)
//            predicates.add(QUser.user.personalInfo.lastname.eq(filter.getLastname()));

        var predicate = QPredicate.builder()
                .add(filter.getFirstname(), QUser.user.personalInfo.firstname::eq)
                .add(filter.getLastname(), QUser.user.personalInfo.lastname::eq)
                .buildAnd();

        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(u1);
            session.persist(u2);
            session.persist(u3);
            session.getTransaction().commit();
            session.clear();

            var result = new JPAQuery<User>(session)
                    .select(QUser.user)
                    .from(QUser.user)
//                    .where(predicates.toArray(Predicate[]::new))
                    .where(predicate)
                    .fetch();

            assertThat(result)
                    .hasSize(2)
                    .extracting(
                            u -> u.getPersonalInfo().getFirstname(),
                            u -> u.getPersonalInfo().getLastname()
                    )
                    .containsOnly(tuple(firstName, lastName));
        }
    }

    @Test
    void np1Test(){
        var dummyUser = HibernateTestUtil.createRandomUser(HibernateTestUtil.createRandomCompany());
        var dummyUser2 = HibernateTestUtil.createRandomUser(HibernateTestUtil.createRandomCompany());
        try (var session = sessionFactory.openSession()) {
            var payment1 = createRandomPayment(dummyUser);
            var payment2 = createRandomPayment(dummyUser);

            var payment3 = createRandomPayment(dummyUser);
            var payment4 = createRandomPayment(dummyUser);

            session.beginTransaction();
            session.persist(dummyUser);
            session.persist(dummyUser2);
            session.getTransaction().commit();
            session.clear();

            // hql np1 solution
            // без fetch не будут селектнуты поля payments
//            var users = session.createQuery(
//                    "select u from User u join fetch u.payments join fetch u.company", User.class
//            ).list();
//            users.forEach(user -> System.out.println(user.getPayments().size()));

//            var findUser = session.find(User.class, dummyUser.getId());
//            System.out.println(findUser.getCompany().getName());
//            System.out.println(findUser.getPayments().size());

            // в criteria используем fetch сразу после join
            // в queryDSL используем fetchJoin сразу после join

            // всё что делает fetch это добавление колонок в результирующий набор
        }

        // fetchProfile
//        try (var session = sessionFactory.openSession()) {
//            // не работает для hql, querydsl, criteriaApi
//            session.enableFetchProfile("withCompanyAndPayments"); // также есть disable
//            session.beginTransaction();
//            var userFind = session.find(User.class, dummyUser.getId());
//            System.out.println(userFind.getCompany().getName());
//            System.out.println(userFind.getPayments().size());
//
//        }

        // Entity graph
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            RootGraph<?> graph = session.getEntityGraph("WithCompanyAndPayments");

            Map<String, Object> properties = Map.of(
                    GraphSemantic.LOAD.getJakartaHintName(), graph
            );
            var user = session.find(User.class, dummyUser.getId(), properties);

            System.out.println(user.getCompany().getName());
            System.out.println(user.getPayments().size());
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

            var users = session.createQuery(
                    "select u from User u", User.class
            ).setHint(GraphSemantic.LOAD.getJakartaHintName(), graph)
                    .list();
            users.forEach(it -> System.out.println(it.getPayments().size()));
            users.forEach(it -> System.out.println(it.getCompany().getName()));
        }

        try (var session = sessionFactory.openSession()) {
            var userGraph = session.createEntityGraph(User.class);
            userGraph.addAttributeNodes("company", "payments");
            var paymentSubGraph = userGraph.addSubgraph("payments", Payment.class);
            paymentSubGraph.addAttributeNodes("amount");

            Map<String, Object> properties = Map.of(
                    GraphSemantic.LOAD.getJakartaHintName(), userGraph
            );
            var user = session.find(User.class, dummyUser.getId(), properties);

        }
    }

    @Test
//    @Transactional
    void optimisticTest(){
        try (var session = sessionFactory.openSession()) {
            // узнать уровень изоляции через connection
//            session.doWork(connection -> System.out.println(connection.getTransactionIsolation()));
            session.beginTransaction();
            Payment payment = createRandomPayment();
            session.persist(payment);
            session.getTransaction().commit();

            session.beginTransaction();
            // необязательно lockmode если выставлена аннотация @OptimisticLoocking в сущности
//            var foundPayment = session.find(Payment.class, 1L, LockModeType.OPTIMISTIC);

            // инкремент в любом случае
            var foundPayment = session.find(Payment.class, 1L, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

            foundPayment.setAmount(foundPayment.getAmount()+10);

            session.getTransaction().commit();

        }
    }

    @Test
    void pessimisticTest(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            Payment payment = createRandomPayment();
            session.persist(payment);
            session.getTransaction().commit();

            session.beginTransaction();

// использует for share (блокировка строки для update, delete, select for update, select for no key update)
//            session.find(Payment.class, 1L, LockModeType.PESSIMISTIC_READ);
//            payment.setAmount(payment.getAmount()+10);

            // использует for update (более строгий, но тоже не трогает select)
            session.find(Payment.class, 1L, LockModeType.PESSIMISTIC_WRITE);
            payment.setAmount(payment.getAmount()+10);

            // изменяет версию в любом случае (также for update)
//            session.find(Payment.class, 1L, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
//            payment.setAmount(payment.getAmount()+10);
            session.beginTransaction().commit();

            session.beginTransaction();

            session.createQuery("select p from Payment p", Payment.class)
                    .setLockMode(LockModeType.NONE) // тоже можно здесь выставлять
//                    .setTimeout()
                    .setHint("jakarta.persistence.lock.timeout", 5) // таймаут на лок именно, а не запрос
                    .list();

            session.getTransaction().commit();
        }
    }

    @Test
    void readOnlyTest(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            Payment payment = createRandomPayment();
            session.persist(payment);
            session.getTransaction().commit();


//            session.setDefaultReadOnly(true);
            session.beginTransaction();
            session.setReadOnly(payment, true);

            session.createNativeMutationQuery("SET TRANSACTION READ ONLY ").executeUpdate();

            session.find(Payment.class, 1L);
            session.createQuery("select p from Payment p", Payment.class)
                    .setReadOnly(true)
                    .setHint(HibernateHints.HINT_READ_ONLY, true);

            session.getTransaction().commit();
        }
    }

    @Test
    void callbacksTest(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            var payment = createRandomPayment();
            session.persist(payment);

            session.flush();
            session.clear();

            var fPayment = session.find(Payment.class, payment.getId());
            Instant timeAfterPersist = Instant.now();

            fPayment.setAmount(fPayment.getAmount()+10);
            session.flush();

            assertSoftly(s->{
                s.assertThat(fPayment.getCreatedAt()).isNotNull().isBeforeOrEqualTo(timeAfterPersist);
                s.assertThat(fPayment.getUpdatedAt()).isNotNull().isAfter(fPayment.getCreatedAt());
            });

            session.getTransaction().rollback();
        }
    }

    @Test
    void eventListenersTest(){
        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            var payment = createRandomPayment();

            session.persist(payment);
            session.flush();

            String paymentId = payment.getId().toString();

            session.remove(payment);
            session.flush();

            var audits = session.createQuery("SELECT a from Audit a", Audit.class).list();

            try {
                assertSoftly(s -> {
                    s.assertThat(audits).hasSize(2);
                    s.assertThat(audits).extracting(Audit::getEntityId).containsOnly(paymentId);
                    s.assertThat(audits).extracting(Audit::getEntityName).containsOnly(Payment.class.getName());
                    s.assertThat(audits).extracting(Audit::getOperation).containsExactly(Audit.Operation.INSERT, Audit.Operation.DELETE);
                });
            } finally {
                session.getTransaction().rollback();
            }
        }
    }

    @Test
    void enversTest(){
        var company1 = createRandomCompany();
        var company2 = createRandomCompany();
        var user1 = createRandomUser(company1);
        var user2 = createRandomUser(company1);
        var user3 = createRandomUser(company2);
        var user4 = createRandomUser(company2);
        var user5 = createRandomUser(company2);
        var payment = createRandomPayment(user1);


        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(user1);
            session.persist(user2);
            session.persist(user3);
            session.persist(user4);
            session.persist(user5);
            session.persist(payment);

            session.getTransaction().commit();
        }

        Long paymentId = payment.getId(); // Получаем сгенерированный ID
        Payment oldPayment;

        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            var auditReader = AuditReaderFactory.get(session);

            // Получаем список всех ревизий для конкретного платежа
            List<Number> revisions = auditReader.getRevisions(Payment.class, paymentId);

            // Берем последнюю (или единственную, так как объект только создан) ревизию
            Number latestRevision = revisions.getLast();

            // Ищем объект по реальному ID и реальной ревизии
            oldPayment = auditReader.find(Payment.class, paymentId, latestRevision);

            session.getTransaction().commit();
        }

        assertSoftly(s -> {
            s.assertThat(oldPayment).isNotNull(); // Дополнительная страховка от NPE
            s.assertThat(payment.getId()).isEqualTo(oldPayment.getId());
            s.assertThat(payment.getAmount()).isEqualTo(oldPayment.getAmount());
        });
    }

    @Test
    void backupAuditTest(){
        var company = createRandomCompany();
        var oldCompanyName = company.getName();

        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(company);
            session.getTransaction().commit();

            session.beginTransaction();
            company.setName("NewName");
            session.getTransaction().commit();


            session.beginTransaction();

            var auditReader = AuditReaderFactory.get(session);
            var revisions = auditReader.getRevisions(Company.class, company.getId());

            var firstCompanyRevision = revisions.getFirst();
            var companyFirstRevision = auditReader.find(Company.class, company.getId(), firstCompanyRevision);

            company.setName(companyFirstRevision.getName());

            session.getTransaction().commit();
            session.clear();

            session.beginTransaction();
            var newCompany = session.find(Company.class, company.getId());
            var newRevisions = auditReader.getRevisions(Company.class, company.getId());
            session.getTransaction().commit(); // Закрываем последнюю транзакцию

            assertSoftly(s -> {
                s.assertThat(companyFirstRevision).isNotNull();
                s.assertThat(newCompany.getName()).isEqualTo(oldCompanyName);
                // Проверка, что откат записан в историю (создание, изменение, откат = 3 ревизии)
                s.assertThat(newRevisions).hasSize(3);
            });
        }
    }

    @Test
    void cache2lTest() throws InterruptedException {
        var company = createRandomCompany();
        var user = createRandomUser(company);
        try (var session = sessionFactory.openSession()) {

            session.beginTransaction();
            session.persist(user);
            session.persist(createRandomUser(company));
            session.persist(createRandomUser(company));
            session.getTransaction().commit();

            session.beginTransaction();
            var user1 = session.find(User.class, 1L);
            user1.getPersonalInfo().getFirstname();
            company = session.find(Company.class, 1);
            company.getName();
//            company.getEmployees().size();

            session.createQuery("select p from Payment p where p.receiver.id = :userId", Payment.class)
                    .setParameter("userId", user.getId())
                    .setCacheable(true)
//                    .setCacheRegion("")
                    .getResultList();
            // либо можно через хинты задать кэшэбл

            session.getTransaction().commit();

        }

//        Thread.sleep(10000);

        try (var session = sessionFactory.openSession()) {
            session.beginTransaction();
            var user1 = session.find(User.class, 1L);
            user1.getPersonalInfo().getFirstname();
            company = session.find(Company.class, 1);
            company.getName();
//            company.getEmployees().size();
            session.createQuery("select p from Payment p where p.receiver.id = :userId", Payment.class)
                    .setParameter("userId", user.getId())
                    .setCacheable(true)
                    .getResultList();
            session.getTransaction().commit();
        }
    }

    @Test
    void testRepository(){
        var payment = createRandomPayment();
        Optional<Payment> foundPayment;

        var session = (Session) Proxy.newProxyInstance(sessionFactory.getClass().getClassLoader(), new Class[]{Session.class},
                ((proxy, method, args) -> method.invoke(sessionFactory.getCurrentSession(), args)));

        session.beginTransaction();

        var paymentRepository = new PaymentRepository(session);
        paymentRepository.save(payment);
        session.flush();
        session.clear();

        foundPayment = paymentRepository.findById(1L);

        session.getTransaction().commit();

        assertThat(foundPayment)
                .isPresent()
                .get()
                .extracting(Payment::getId)
                .isEqualTo(payment.getId());
    }

    @Test
    @Transactional
    void testUserService() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var company = createRandomCompany();
        var user = createRandomUser(company);
        var session = createSessionProxy();
//        session.beginTransaction();

        var companyRepository = new CompanyRepository(session);

        var companyReadMapper = new CompanyReadMapper();
        var userReadMapper = new UserReadMapper(companyReadMapper);
        var userCreateMapper = new UserCreateMapper(companyRepository);

        var userRepository = new UserRepository(session);

//        var userService = new UserService(userRepository, userReadMapper, userCreateMapper);
        var transactionInterceptor = new TransactionInterceptor(sessionFactory);

        var userService = new ByteBuddy()
                .subclass(UserService.class)
                .method(ElementMatchers.any())
                .intercept(MethodDelegation.to(transactionInterceptor))
                .make()
                .load(UserService.class.getClassLoader())
                .getLoaded()
                .getDeclaredConstructor(UserRepository.class, UserReadMapper.class, UserCreateMapper.class)
                .newInstance(userRepository, userReadMapper, userCreateMapper);

        session.beginTransaction();
        companyRepository.save(company);
        session.getTransaction().commit();

        var userDto = new UserCreateDto(user.getPersonalInfo(), user.getUsername(), user.getInfo(), user.getRole(), user.getCompany().getId());
        var userId = userService.create(userDto);

        var foundUser = userService.findById(1L);

//        session.getTransaction().commit();

        assertThat(foundUser).isNotNull();
        assertThat(foundUser.get().id()).isEqualTo(userId);
    }

    @AfterAll
    static void closeSessionFactory(){
        sessionFactory.close();
    }
}
