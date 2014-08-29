/*
 * Classname: Worker
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

import java.util.Random;

/**
 * Single "synthetic" commuter characteristics
 * 
 * @author Richard Thomas
 * @version 1.1, August 2014
 */
public class Worker {
	
	/**
	 *  Workplace Zone destination for this worker
	 */
	private int wz;
	
	/**
	 * List of candidate OAs within distance range in the order in which they
	 * will be tried out on subsequent population iterations.
	 * (Note this may be a pointer to a list shared by all workers in same WZ)
	 */
	private int candOASelectSequence[];
	
	/**
	 * OA Candidate pointer (starts at random count & gets incremented to iterate)
	 */
	private int indexCandOA;
	
	/**
	 * Currently selected OA. Only changed if newly indexed candidates are better
	 */
	private int currentOA;

//-----------------------------------------------------------------------------
	
	/**
	 * Trivial accessor function
	 * 
	 * @return	Currently selected OA
	 */
	public int getCurrentOA() {
		return currentOA;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Trivial accessor function
	 * 
	 * @param newCurrentOA	New value for currently selected OA
	 */
	public void setCurrentOA(int newCurrentOA) {
		currentOA = newCurrentOA;
	}

//-----------------------------------------------------------------------------
		
		/**
		 * Trivial accessor function
		 * 
		 * @return	Workplace Zone destination for this worker
		 */
		public int getWZ() {
			return wz;
		}
		
//-----------------------------------------------------------------------------

	/**
	 * Set initial flow for worker from random OA to specified WZ
	 * 
	 * @param indexWZ	Workplace Zone index
	 * @param candOAs	list of possible OAs for commuting to this WZ
	 * @param randGen	Pseudo-random number generator
	 * @return			initial OA assigned
	 */
	public int initFlow(int indexWZ, int[] candOAs, Random randGen) {
		
		// Get number of candidate OAs for this WZ
		int countOAs = candOAs.length;
		
		// Set Workplace Zone for this commuter
		wz = indexWZ;
		
		// Get sequence of candidate OAs specific to this worker
		// (This may be a pointer to a list shared by all workers in same WZ)
		candOASelectSequence = candOAs;
		
		// Randomly select one of the candidate OAs
		indexCandOA = randGen.nextInt(countOAs);
		currentOA = candOASelectSequence[indexCandOA];
		return currentOA;
	}

//-----------------------------------------------------------------------------

	/**
	 * Get next candidate OA from this workers search sequence
	 * (but don't change the worker current OA at this time)
	 * 
	 * @return Next Candidate OA in sequence
	 */
	public int getNextCandOA() {	

		// Step to next candidate index (wrap on total candidates)
		indexCandOA = (indexCandOA + 1) % candOASelectSequence.length;

		return candOASelectSequence[indexCandOA];
	}
}
