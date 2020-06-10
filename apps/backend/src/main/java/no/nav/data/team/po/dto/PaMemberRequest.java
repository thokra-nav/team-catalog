package no.nav.data.team.po.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import no.nav.data.team.common.validator.Validated;
import no.nav.data.team.common.validator.Validator;
import no.nav.data.team.team.domain.TeamRole;

import java.util.List;

import static no.nav.data.team.common.utils.StreamUtils.nullToEmptyList;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.StringUtils.upperCase;

@Data
@Builder
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class PaMemberRequest implements Validated {

    private String navIdent;
    private List<TeamRole> roles;
    private String description;

    @Override
    public void format() {
        setNavIdent(upperCase(navIdent));
        setRoles(nullToEmptyList(roles));
        setDescription(trimToNull(description));
    }

    @Override
    public void validateFieldValues(Validator<?> validator) {
        validator.checkPatternRequired(PaMemberRequest.Fields.navIdent, navIdent, Validator.NAV_IDENT_PATTERN);
        validator.checkBlank(PaMemberRequest.Fields.roles, roles.isEmpty() ? null : roles.get(0).name());
    }
}