/*
 * Classname: CandOAsPerWZ
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Candidate OAs that are within specified distance interval of a single
 * specified WZ. (Candidates are determined by OA-WZ distances only;
 * if their flow count is zero this is not taken into consideration as
 * census record swapping may have occurred reducing an OA commute count to 0)
 * 
 * @author Richard Thomas
 * @version 1.1, August 2014
 */
public class CandOAsPerWZ {
		
	/**
	 * Candidate OAs origins for a single WZ destination
	 * (determined by measuring OA-WZ Population Weighted Centroid distances)
	 */
	private ArrayList<Integer> candOAsList;

//-----------------------------------------------------------------------------

	/**
	 * Calculate candidate OAs that are within specified distance
	 * interval of selected WZ, but rejecting all OAs with zero flow counts.
	 * (Although census record swapping might reduce an OA commute count to 0,
	 * it is considered better to still exclude such OAs from possible routing
	 * as preventing such routing will on the whole make the analysis clearer
	 * and more representative)
	 * 
	 * @param wzFlowList		List of total flows to each Workplace Zone (WZ)
	 * @param oaFlowList		List of total flows from each Output Area (OA)
	 * @param indexWZ			Destination Workplace Zone
	 * @param distanceSelect	Required distance interval (e.g. 0: 0-2km)
	 * @param minDistance		Minimum distance OA and WZ PWCs must be apart
	 * @param maxDistance		Minimum distance OA and WZ PWCs must be apart
	 */
	public CandOAsPerWZ(ZoneFlowList oaFlowList, ZoneFlowList wzFlowList,
			int indexWZ, int minDistance, int maxDistance) {
		
		// Initialise list of candidate OAs
		candOAsList = new ArrayList<Integer>();
		
		// Get WZ centroid
		int wzX = wzFlowList.flows.get(indexWZ).eastingPWC;
		int wzY = wzFlowList.flows.get(indexWZ).northingPWC;
		
		// Loop through OAs
		for (int indexOA = 0; indexOA < oaFlowList.flows.size(); indexOA++) {
			
			// Get OA Centroid
			int oaX = oaFlowList.flows.get(indexOA).eastingPWC;
			int oaY = oaFlowList.flows.get(indexOA).northingPWC;
		
			// Get OA-WZ distance^2 (in metres^2)
			int distSquared = (oaX - wzX) * (oaX - wzX)
					        + (oaY - wzY) * (oaY - wzY);
		
			// If within range and non-zero OA count, add to OA list
			if (distSquared >= minDistance * minDistance &&
					distSquared <= maxDistance * maxDistance &&
					oaFlowList.getCount(indexOA) > 0) {
				candOAsList.add(indexOA);
			}
		}
		
		// Debug: print count of OAs within range of each WZ
		//System.out.println("CandOAsPerWZ WZ " + indexWZ + ", OA Count = " + candOAsList.size());

	}

//-----------------------------------------------------------------------------

	/**
	 * Return newly shuffled list of candidate OAs for selected WZ.
	 * Allows possibility for each worker in a WZ to have a differently
	 * ordered list of the same OAs.. at the cost of MUCH more storage space;
	 * Bristol UA has 484 WZs, but 61281 commuters to those (a factor of 127x).
	 * 
	 * @param randGen	Pseudo-random number generator
	 * @return			Candidate OA index list (in random order)
	 */
	public int[] getNewlyShuffledCandOAsPerWZ(Random randGen) {
		
		// (Pseudo-randomly shuffle order of Candidate OAs 
		Collections.shuffle(candOAsList, randGen);

		// Convert OA list for this WZ from ArrayList<Integer> to int[]
		// (Seemingly clunky method, but apparently no better way to do in
		//  standard Java. ArrayUtils.toPrimitive() from Apache Commons Lang
		//  library would be a suitable alternative.)
		int[] shuffledCandOAsForWZ = new int[candOAsList.size()];
		for (int i = 0; i < candOAsList.size(); i++) {
			shuffledCandOAsForWZ[i] = candOAsList.get(i);
		}
		//System.out.println("shuffledCandOAsForWZ = " + shuffledCandOAsForWZ.length);
		return shuffledCandOAsForWZ;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Return current list of candidate OAs for selected WZ.
	 * 
	 * @return		Candidate OA index list (in order)
	 */
	public int[] getCandOAsPerWZ() {
		
		// Convert OA list for this WZ from ArrayList<Integer> to int[]
		// (Seemingly clunky method, but apparently no better way to do in
		//  standard Java. ArrayUtils.toPrimitive() from Apache Commons Lang
		//  library would be a suitable alternative.)
		int[] CandOAsForWZ = new int[candOAsList.size()];
		for (int i = 0; i < candOAsList.size(); i++) {
			CandOAsForWZ[i] = candOAsList.get(i);
		}
		return CandOAsForWZ;
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Get number of OAs within range of WZ this list was created for
	 * 
	 * @return	total count of OAs
	 */
	public int getCount() {
		return candOAsList.size();
	}
}
