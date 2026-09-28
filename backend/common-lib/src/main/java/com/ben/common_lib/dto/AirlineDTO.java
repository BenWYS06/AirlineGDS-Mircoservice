package com.ben.common_lib.dto;

import com.ben.common_lib.embeddable.Support;
import com.ben.common_lib.enums.AirlineStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class AirlineDTO {

    private Long id;

    private String iataCode;
    private String icaoCode;

    private String name;
    private String alias;
    private String country;

    private String logoUrl;
    private String website;

    private AirlineStatus status;
    private String alliance;

    private Support support;
    private String headquarters;

    private Instant createdAt;
    private Instant updatedAt;

    private Long baggagePolicyId;

    private Long userId;
    private Long updatedByUserId;
}
