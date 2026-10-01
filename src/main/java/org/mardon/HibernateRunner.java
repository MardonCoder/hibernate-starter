package org.mardon;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.mardon.entity.*;
import org.mardon.util.HibernateUtil;

import java.time.LocalDate;

@Slf4j
public class HibernateRunner {

//    public static final Logger log = LoggerFactory.getLogger(HibernateRunner.class);

    public static void main(String[] args) {
        Company company = Company.builder()
                .name("Google")
                .build();
        User user = User.builder()
                .username("petr2@gmail.com")
                .personalInfo(PersonalInfo.builder()
                        .firstname("Petr")
                        .lastname("Petrov")
                        .birthDate(new Birthday(LocalDate.of(2001, 1, 1)))
                        .build())
                .role(Role.USER)
                .info("""
                            {
                            "allgood": true
                            }
                            """)
                .company(company)
                .build();
        try (SessionFactory sessionFactory = HibernateUtil.getSessionFactory()) {
            try (Session session = sessionFactory.openSession()) {
                var transaction = session.beginTransaction();

//                session.persist(company);
                session.persist(user); // пример persist

                transaction.commit();
            }
            try (Session session = sessionFactory.openSession()) {
                var transaction = session.beginTransaction();
                user = session.merge(user);
                user.getCompany().setName("Apple");
//                session.merge(user); // пример merge
                session.refresh(user); // пример refresh
                log.info("ИМЯ КОМПАНИИ ОТКАТИЛОСЬ НА GOOGLE: {}", user.getCompany().getName());
                transaction.commit();
            }
        }
    }
}
