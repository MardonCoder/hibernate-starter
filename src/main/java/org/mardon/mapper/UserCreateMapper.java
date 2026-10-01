package org.mardon.mapper;

import lombok.RequiredArgsConstructor;
import org.mardon.dao.CompanyRepository;
import org.mardon.dto.UserCreateDto;
import org.mardon.entity.User;

import java.util.Map;

@RequiredArgsConstructor
public class UserCreateMapper implements Mapper<UserCreateDto, User> {

    private final CompanyRepository companyRepository;

    @Override
    public User mapFrom(UserCreateDto object) {
        return User.builder()
                .personalInfo(object.personalInfo())
                .username(object.username())
                .info(object.info())
                .role(object.role())
                .company(companyRepository.findById(object.companyId()).orElseThrow(IllegalArgumentException::new))
                .build();
    }
}
