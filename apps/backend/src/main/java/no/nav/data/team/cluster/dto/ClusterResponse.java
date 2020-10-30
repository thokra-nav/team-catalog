package no.nav.data.team.cluster.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.data.common.rest.ChangeStampResponse;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "name", "description", "tags", "changeStamp"})
public class ClusterResponse {

    private UUID id;
    private String name;
    private String description;
    private List<String> tags;

    private ChangeStampResponse changeStamp;

}