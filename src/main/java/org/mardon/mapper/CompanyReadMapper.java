package org.mardon.mapper;

import org.mardon.dto.CompanyReadDto;
import org.mardon.entity.Company;

public class CompanyReadMapper implements Mapper<Company, CompanyReadDto> {
    @Override
    public CompanyReadDto mapFrom(Company object) {
        return new CompanyReadDto(object.getId(), object.getName(), object.getLocales());
    }
}
