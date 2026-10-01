package org.mardon.dto;

import org.mardon.entity.Company;
import org.mardon.entity.PersonalInfo;
import org.mardon.entity.Role;

public record UserReadDto(
        Long id,
        PersonalInfo personalInfo,
        String username,
        String info,
        Role role,
        CompanyReadDto company) {

}
