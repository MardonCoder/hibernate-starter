package org.mardon.filter;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserFilter {
    String firstname;
    String lastname;
}
