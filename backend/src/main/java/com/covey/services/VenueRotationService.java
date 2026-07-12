package com.covey.services;

import com.covey.models.VenueExclusion;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages venue rotation logic (12-week exclusion window).
 *
 * Ensures venues selected in the last 12 weeks are not re-selected for a given city.
 * Implements the 12-week lookback requirement for WBS 1.3.6.
 */
public class VenueRotationService {

  public VenueRotationService() {}

  /**
   * Build exclusion set of venue IDs used in the last 12 weeks for a city.
   *
   * @param exclusionRecords VenueExclusion records for the last 12 weeks
   * @return Set of venue IDs to exclude from selection
   */
  public Set<String> buildExclusionSet(List<VenueExclusion> exclusionRecords) {
    Set<String> excluded = new HashSet<>();

    if (exclusionRecords == null) {
      return excluded;
    }

    for (VenueExclusion record : exclusionRecords) {
      if (record.getVenueIds() != null) {
        excluded.addAll(record.getVenueIds());
      }
    }

    return excluded;
  }
}
