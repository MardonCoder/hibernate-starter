package org.mardon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.mardon.entity.PersonalInfo;
import org.mardon.entity.Role;
import org.mardon.validation.UpdateCheck;

public record UserCreateDto(@Valid // validation better on this layer
                            PersonalInfo personalInfo,
                            @NotNull
                            String username,
                            String info,
                            @NotNull(groups = UpdateCheck.class)
                            Role role,
                            Integer companyId) {
}
