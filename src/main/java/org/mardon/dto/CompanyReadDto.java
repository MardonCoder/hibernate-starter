package org.mardon.dto;

import org.mardon.entity.LocaleInfo;

import java.util.List;
import java.util.Map;

public record CompanyReadDto(Integer id, String name, List<LocaleInfo> locales) {
}
