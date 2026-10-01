package org.mardon.dto;

import org.mardon.entity.PersonalInfo;
import org.mardon.entity.Role;

public record UserCreateDto(PersonalInfo personalInfo,
                            String username,
                            String info,
                            Role role,
                            Integer companyId) {
}
