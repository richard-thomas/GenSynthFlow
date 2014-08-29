/*
 * Classname: AreaWorkingCounts
 * 
 * Copyright (c) 2014 Richard Thomas and University of Leeds
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Artistic License 2.0 as published by the
 * Open Source Initiative (http://opensource.org/licenses/Artistic-2.0)
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package genSynthFlow;

/**
 * Working counts of flows from each area (OA). These counts are initially
 * built up worker-by-worker when the synthetic population is created, then
 * updated as the population flows are iterated.
 * 
 * @author Richard Thomas
 * @version 1.1, August 2014
 */
public class AreaWorkingCounts {
	
	/**
	 * Current number of journeys originating in each area
	 */
	private int[] areaCount;
	
	/**
	 * Census-derived flow list associated with this area type
	 */
	private ZoneFlowList areaFlowList;
	
	/**
	 * smallest (typically most negative) proportional mismatch 
	 */
	private double minMismatch;
	
	/**
	 * largest (typically most positive) proportional mismatch
	 */
	private double maxMismatch;
	
	/**
	 * arithmetic mean of absolute differences from target counts 
	 */
	private double meanDifference;
	
//-----------------------------------------------------------------------------

	/**
	 * Accessor function 
	 * @return	count for specified area
	 */
	public int getCount(int indexArea) {
		return areaCount[indexArea];
	}

	/**
	 * Accessor function 
	 * @return	smallest (typically most negative) proportional mismatch 
	 */
	public double getMinMismatch() {
		return minMismatch;
	}

	/**
	 * Accessor function 
	 * @return	largest (typically most positive) proportional mismatch 
	 */
	public double getMaxMismatch() {
		return maxMismatch;
	}

	/**
	 * Accessor function 
	 * @return	arithmetic mean of absolute differences from target counts 
	 */
	public double getMeanDifference() {
		return meanDifference;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Create array of current counts of workers assigned to each area
	 * (initialised to all zeroes)
	 * 
	 * @param flowList	List of total flows from each area
	 */
	public AreaWorkingCounts(ZoneFlowList flowList) {
		
		areaCount = new int[flowList.flows.size()];

		// Take note of flow list for use by other class methods
		areaFlowList = flowList;		
	}

//-----------------------------------------------------------------------------
	
	/**
	 * Measure simple (signed) difference between an area's current flow count
	 * and the census-derived target flow count.
	 *  
	 * @param indexArea		Area Index  (to associated FlowList)
	 * @return				Simple difference
	 */
	public int getDifference(int indexArea) {
		return areaCount[indexArea] - areaFlowList.getCount(indexArea);
	}


//-----------------------------------------------------------------------------

	/**
	 * 	Get a measure of the difference between an area's current flow count
	 *  and the census-derived target flow count. This is scaled to the size
	 *  of the target count.
	 *  
	 * @param indexArea		Area Index  (to associated FlowList)
	 * @param tweak			Adjustment to current OA count
	 * @return				Difference scaled by target size
	 */
	public double getMismatch(int indexArea, int tweak) {
		
		// Get target census-derived OA count 
		int targetCount = areaFlowList.getCount(indexArea);
		
		// Get difference with current OA count (+ tweak)
		int diffTweaked = areaCount[indexArea] - targetCount + tweak;
		
		// Avoid division by zero
		if (targetCount != 0) {
			return ((double) diffTweaked) / targetCount;
		}
		else if (diffTweaked == 0) {
			return 0.0;
		}
		
		// This should never occur so make it distinctive if it does!
		else {
			return 9999.0;
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Increment count of workers commuting from specified area
	 * 
	 * @param index		Area index (to associated FlowList)
	 */
	public void incCount(int index) {
		areaCount[index]++;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Decrement count of workers commuting from specified area
	 * 
	 * @param index		Area index (to associated FlowList)
	 */
	public void decCount(int index) {
		areaCount[index]--;
	}

//-----------------------------------------------------------------------------

	/**
	 * Recalculate differences and proportional mismatches of all area counts
	 * against target area counts. Calculate various statistics on these.
	 * 
	 * @return 	magnitude of worst proportional mismatch
	 */
	public double checkCounts() {

		double mismatch;
		int difference;
		int sumDifferences = 0;

		// Set defaults so they will quickly be over-written
		minMismatch = 1000.0;
		maxMismatch = -1000.0;

		// Generate stats for mismatches between current and target area counts
		for (int indexArea = 0; indexArea < areaCount.length; indexArea++) {
			mismatch = getMismatch(indexArea, 0);
			difference = getDifference(indexArea);
			sumDifferences += Math.abs(difference);

			// Update min/max area proportional mismatches
			if (mismatch > maxMismatch) {
				maxMismatch = mismatch;
			}
			else if (mismatch < minMismatch) {
				minMismatch = mismatch;
			}
		}

		// Update mean of count differences across all areas
		meanDifference = ((double) sumDifferences) / areaCount.length;

		// Return magnitude of worst proportional mismatch
		return Math.max(Math.abs(minMismatch), Math.abs(maxMismatch));
	}

}
