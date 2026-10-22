package com.ben.services.repository;

import com.ben.common_lib.enums.CoverageType;
import com.ben.services.model.Ancillary;
import com.ben.services.model.InsuranceCoverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceCoverageRepository extends JpaRepository<InsuranceCoverage, Long> {

    List<InsuranceCoverage> findByAncillary(Ancillary ancillary);

    List<InsuranceCoverage> findByAncillaryAndActiveTrue(Ancillary ancillary);

    List<InsuranceCoverage> findByCoverageType(CoverageType coverageType);

    List<InsuranceCoverage> findByAncillaryIdAndActiveTrue(Long ancillaryId);
}
