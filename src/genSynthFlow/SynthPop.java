/*
 * Classname: SynthPop
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.Random;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Creates a synthetic population of workers travelling from OAs to WZs.
 * Iterates OA origins to minimize counts errors with census-derived flows.
 * 
 * @author Richard Thomas
 * @version 1.1, August 2014
  */
public class SynthPop {

	/**
	 * Shuffled candidate OA sequence for every worker?
	 * (Otherwise done only once for all workers in each WZ)
	 * Note: uses MUCH more memory due to having a candidate OA list for all
	 * workers instead of just one per WZ (For just Bristol UA, WZs = 484,
	 * population = 61281 for 2-5km - executable size = 55MB vs 289MB)
	 */
	private static final boolean
		RESHUFFLE_OA_CAND_SEQUENCE_FOR_EVERY_WORKER = true;
	
	/**
	 * Use the proportion of each OA count mismatch to the total count from
	 * the census-derived OA count total when deciding whether to switch a
	 * worker from being re-assigned from one OA to another. If false,
	 * then iteration decision will be simply made on whether it reduces the
	 * sum of magnitudes of the differences between current and target OA
	 * counts (a method which is generally much slower to converge and can
	 * result in proportional large errors in OA counts).
	 */
	private static final boolean ITERATE_USING_PROPORTIONAL_MISMATCH = true;
	
	/**
	 * Filename to write diagnostic table of all OA differences from target
	 * values at each active iteration (one where OA origins switches occur).
	 * (Set to null if diagnostics not required)
	 */
	private static final String ITERATION_TABLE_LOG_FILENAME =
			"diag_iterations_table.csv";
//			null;

	/**
	 * Filename to write diagnostic log of each change in assigned origin OA
	 * for every worker. (Set to null if diagnostics not required)
	 */
	private static final String ITERATION_CHANGES_LOG_FILENAME =
//			"diag_iteration_changes.log" ;
			null;

//-----------------------------------------------------------------------------

	/**
	 * The entire synthetic population
	 */
	private Worker[] workers;

	/**
	 * Current number of journeys originating in each OA
	 * (initialised to all zeroes)
	 */
	private AreaWorkingCounts oaWorkingCount;
	
	/**
	 * CSV format log file for iterations of synthesised and target OA flows
	 */
	private CSVWriter writerIter = null;

//-----------------------------------------------------------------------------

	/**
	 * Generate synthetic population, assigning final values for destination
	 * WZs, but only initial values for OA origins (from list of OAs within
	 * range of each WZ).
	 * 
	 * @param wzFlowList	List of total flows to each Workplace Zone (WZ)
	 * @param oaFlowList	List of total flows from each Output Area (OA)
	 * @param candOAsPerWZ	OAs within range of each WZ
	 * @param randGenSynthPop	Systematically seeded random number generator
	 */
	public SynthPop(ZoneFlowList wzFlowList, ZoneFlowList oaFlowList,
			CandOAsPerWZ[] candOAsPerWZ, Random randGenSynthPop) {

		int population = 0;		// Working population
		int skippedWZs = 0;		// WZs skipped due to no OAs in range
		ArrayList<Worker> workerList = new ArrayList<Worker>();

		
		int countWZ = wzFlowList.flows.size();

		// Create array of current counts of workers assigned to each OA
		// (initialised to all zeroes)
		oaWorkingCount = new AreaWorkingCounts(oaFlowList);

		// Decide on size of synthetic population by summing WZ flow counts,
		// but only for WZs with any OAs in range
		for (int indexWZ = 0; indexWZ < countWZ; indexWZ++) {

			// Shuffled sequence of candidate OAs for this WZ
			int[] candOASelectSequence;
			
			// Get shuffled sequence once for all workers in WZ
			if (!RESHUFFLE_OA_CAND_SEQUENCE_FOR_EVERY_WORKER) {
				candOASelectSequence = candOAsPerWZ[indexWZ].
						getNewlyShuffledCandOAsPerWZ(randGenSynthPop);
			}

			// Get number of possible OAs for commuting to this WZ 
			int countCandOAs = candOAsPerWZ[indexWZ].getCount();

			// As long as there are some OAs within range of this WZ, create
			// workers for all those travelling to current WZ
			// (as reported in census data)
			if (countCandOAs > 0) {
				population += wzFlowList.getCount(indexWZ);
				
				for (int i = 0; i < wzFlowList.getCount(indexWZ); i++) {
					
					// Re-shuffle candidate OA sequence for every workers
					if (RESHUFFLE_OA_CAND_SEQUENCE_FOR_EVERY_WORKER) {
						candOASelectSequence = candOAsPerWZ[indexWZ].
								getNewlyShuffledCandOAsPerWZ(randGenSynthPop);
					}
			
					// Create new commuter and assign initial OA-WZ flow
					Worker newWorker = new Worker();
					workerList.add(newWorker);
					int assignedOA = newWorker.initFlow(indexWZ,
							candOASelectSequence, randGenSynthPop);

					// Increment commuter count for (randomly) assigned OA
					oaWorkingCount.incCount(assignedOA);
				}
			}
			else {
				System.out.printf(
						"Warning: skipping WZ %d (%s): No OAs within range%n",
						indexWZ, wzFlowList.flows.get(indexWZ).zoneCode);
				skippedWZs++;
			}
		}
		
		System.out.printf(
				"WZs skipped due to no OAs within range: %d/%d (%.1f%%)%n",
				skippedWZs, countWZ, 100.0 * skippedWZs / countWZ);
		System.out.println("Synthesized working population size: " + population);

		// (Pseudo)-randomly shuffle order of workers so that they are not
		// bunched together by WZ
		Collections.shuffle(workerList, randGenSynthPop);
		
		// Convert ArrayList "workerList" to simple array "workers"
		workers = new Worker[population];
		workerList.toArray(workers);
	}

//-----------------------------------------------------------------------------

	/**
	 * Iterate synthetic flow data to better match OA flows
	 * 
	 * @param oaFlowList	List of total flows from each OA
	 */
	public void iterSynthFlowDist(ZoneFlowList oaFlowList) {

		int iterations = 0;
		int stableIterations = 0;	// Count of iterations with no gains
		

		// Stop searching when we have looped through all possible OAs and
		// seen no changes
		int maxStableIterations = oaFlowList.flows.size();
		
		// Create log file for iterations of synthesised and target OA flows
		if (ITERATION_TABLE_LOG_FILENAME != null) {
			openIterationsLog(oaFlowList);
		}
		
		// Create log file for all iteration changes of assigned OA origins
		Formatter logIterationChanges = null;
		if (ITERATION_CHANGES_LOG_FILENAME != null) {
			try {
				logIterationChanges =
						new Formatter(ITERATION_CHANGES_LOG_FILENAME);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(
						"Error opening changes logfile: ploughing on anyway..");
			}
		}

		// Loop until no OA swaps occur for the total number of OAs
		while(stableIterations < maxStableIterations) {		
//		while(stableIterations < maxStableIterations && iterations < 400) {		
			
			// Take note if any worker's OAs get switched on this iteration
			boolean oaSwitchOccurred = false;
			
			// For diagnostics, track distance as it gets reduced
			double sumDistanceSqr = 0;
			
			// Iterate through entire population to try to improve errors
			for (int indexWorkers = 0; indexWorkers < workers.length;
					indexWorkers++) {
				int currentOA = workers[indexWorkers].getCurrentOA();
				int wz = workers[indexWorkers].getWZ();
				boolean switchOA = false;

				// Get next candidate OA in this worker's sequence
				int newCandOA = workers[indexWorkers].getNextCandOA();

				// Get current OA-WZ distance^2
				int currentDistanceSqr = GenSynthFlow.getDistanceSqr(
						currentOA, wz);
				sumDistanceSqr += currentDistanceSqr;

				// Get new OA-WZ distance^2
				int newDistanceSqr = GenSynthFlow.getDistanceSqr(
						newCandOA, wz);

				// Use the proportion of each OA count mismatch to the total
				// count from the census-derived OA count total when deciding
				// whether to switch a worker from being re-assigned from one
				// OA to another.
				if (ITERATE_USING_PROPORTIONAL_MISMATCH) {
					
					// Get proportional mismatch in current OA journey count
					// from target
					double currentOAMismatch =
							oaWorkingCount.getMismatch(currentOA, 0);

					// Get proportional mismatch (after updating) in current OA
					// journey count from target (how does it change if 1
					// worker was moved away from this OA?)
					double currentOAUpdatedMismatch =
							oaWorkingCount.getMismatch(currentOA, -1);

					// Get proportional mismatches from target for new
					// candidate OA
					double newOAMismatch =
							oaWorkingCount.getMismatch(newCandOA, 0);

					// Get proportional mismatches (after updating) from target
					// for new candidate OA (how does it change if 1 worker
					// moved to this OA?)
					double newOAUpdatedMismatch =
							oaWorkingCount.getMismatch(newCandOA, +1);

					// Get magnitude of the worser of the proportional
					// mismatches before and after
					double worstCurrentMismatch = Math.max(
							Math.abs(currentOAMismatch),
							Math.abs(newOAMismatch));
					double worstUpdatedMismatch = Math.max(
							Math.abs(currentOAUpdatedMismatch),
							Math.abs(newOAUpdatedMismatch));
					
					// Create a decaying mismatch tolerance to allow initial freedom
					// to swap flows around based primarily on shortening distances,
					// then focusing on reducing mismatch in counts
					double mismatch_tolerance = 1 + 1.0 / iterations / iterations;
					//double mismatch_tolerance = 1 + 1.0 / iterations;

					// Switch to new candidate OA if less proportional mismatch
					// or if within-tolerance proportional mismatch but it shortens
					// length of flow
					switchOA = ((worstUpdatedMismatch < worstCurrentMismatch ||
							(worstUpdatedMismatch <
									worstCurrentMismatch * mismatch_tolerance &&
							 newDistanceSqr < currentDistanceSqr)) &&
							currentOA != newCandOA);
				}
				
				// ELSE iteration decision will be simply made on whether it
				// reduces the sum of magnitudes of the differences between
				// current and target OA counts (a method which is generally
				// much slower to converge and can result in proportional
				// large errors in OA counts).
				else { 	// !ITERATE_USING_PROPORTIONAL_MISMATCH

					// Get Difference in current OA journey count from target
					double currentOADifference =
							oaWorkingCount.getDifference(currentOA);

					// Get Difference (after updating) in current OA journey
					// count from target (how does it change if 1 worker was
					// moved away from this OA?)
					double currentOAUpdatedDifference =
							oaWorkingCount.getDifference(currentOA) - 1;

					// Get Differences from target for new candidate OA
					double newOADifference =
							oaWorkingCount.getDifference(newCandOA);

					// Get Differences (after updating) from target for new
					// candidate OA (how does it change if 1 worker moved to
					// this OA?)
					double newOAUpdatedDifference =
							oaWorkingCount.getDifference(newCandOA) + 1;

					// Get sum of the 2 difference magnitudes before and after
					double sumCurrentDifference =
							Math.abs(currentOADifference) +
							Math.abs(newOADifference);
					double sumUpdatedDifference =
							Math.abs(currentOAUpdatedDifference) +
							Math.abs(newOAUpdatedDifference);
					
					// Switch to new candidate OA if less mean difference
					switchOA = ((sumUpdatedDifference < sumCurrentDifference &&
							newDistanceSqr < currentDistanceSqr) &&
							currentOA != newCandOA);
				}
				
				// If it will reduce extent of mismatches, swap to new candidate OA
				if (switchOA) {
					oaWorkingCount.decCount(currentOA);
					workers[indexWorkers].setCurrentOA(newCandOA);
					oaWorkingCount.incCount(newCandOA);
					oaSwitchOccurred = true;
					
					// Write to logfile details of every time a worker origin
					// OA is reassigned
					if (logIterationChanges != null) {
						logIterationChanges.format("- Worker %d (WZ %d), OA: %d -> %d%n",
								indexWorkers, workers[indexWorkers].getWZ(),
								currentOA, newCandOA);
					}
				}	
			}

			if (oaSwitchOccurred == true) {
				stableIterations = 0;
				
				// See how close current OA counts are to target OA counts
				// Print to screen and to logfile (if it exists)
				double mismatch = oaWorkingCount.checkCounts();
				double meanDist = Math.sqrt(sumDistanceSqr / workers.length);
				System.out.printf(
						"Step %d, mean distance = %.0f, worst proportional mismatch = %.4f (mean diff = %.4f)%n",
						iterations, meanDist, mismatch,
						oaWorkingCount.getMeanDifference());
				if (logIterationChanges != null) {
					logIterationChanges.format(
							"Step %d, mean distance = %.0f, worst prop mismatch = %.4f (mean diff = %.4f)%n",
							iterations, meanDist, mismatch,
							oaWorkingCount.getMeanDifference());
				}
				
				// Write difference of each current OA count from target OA counts
				if (ITERATION_TABLE_LOG_FILENAME != null) {
					updateIterationsLog(mismatch, oaFlowList);
				}
			}
			else {
				stableIterations++;
				
				// Display diagnostics as this bit could be very slow!
				if (stableIterations % (maxStableIterations / 10) == 0) {
					System.out.printf(
							"Population stable? (%d%%)%n",
							10 * Math.round(10.0 * stableIterations /
									maxStableIterations));
				}
			}
			iterations++;
		}

		// Close Iterations table CSV file
		if (ITERATION_TABLE_LOG_FILENAME != null) {
			closeIterationsLog(oaFlowList);
		}
		
		// Close Iteration changes log CSV file
		if (logIterationChanges != null) {
			logIterationChanges.close();
		}
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Convert final synthetic population to a flow data matrix
	 * 
	 * @param countOA	Total flows from each Output Area (OA)
	 * @param countWZ	Total flows to each Workplace Zone (WZ)
	 * @return			Synthesized Origin(OA)-Destination(WZ) flow matrix
	 */
	public int[][] popToFlowMatrix(int countOA, int countWZ) {

		// Create cell for each OA (origin) to each WZ (destination)
		int[][] synthFlowMatrix = new int[countOA][countWZ];

		// Add each worker's journey into the flow matrix
		for (int indexW = 0; indexW < workers.length; indexW++) {
			synthFlowMatrix[workers[indexW].getCurrentOA()]
						   [workers[indexW].getWZ()]++;
		}

		return synthFlowMatrix;
	}
	
//-----------------------------------------------------------------------------

	/**
	 *  Create log file for iterations of synthesised and target OA flows
	 *
	 * @param oaFlowList	List of total flows from each OA
	 */
	private void openIterationsLog(ZoneFlowList oaFlowList) {

		int countOA = oaFlowList.flows.size();
		String fileNameIter = ITERATION_TABLE_LOG_FILENAME;
		System.out.println("Writing OA assignment iterations to file:");
		System.out.println("  " + fileNameIter);

		try {
			writerIter = new CSVWriter(new FileWriter(fileNameIter), ',',
					CSVWriter.NO_QUOTE_CHARACTER);
		} catch (IOException e) {

			// Print warning, but stumble on anyway as nearly finished
			e.printStackTrace();
		}

		// Write field names (header) lines
		String[] fieldsOA = new String[countOA+2];
		String[] fieldsTarget = new String[countOA+2];
		fieldsOA[0] = "OA Index:";
		fieldsOA[1] = "";
		fieldsTarget[0] = "OA Target Count:";
		fieldsTarget[1] = "";
		for (int i = 0; i < countOA; i++) {
			fieldsOA[i+2] = String.valueOf(i);
			fieldsTarget[i+2] = String.valueOf(oaFlowList.getCount(i));
		}
		writerIter.writeNext(fieldsOA);
		writerIter.writeNext(fieldsTarget);
		
		// Write statistics on initial population to file
		double mismatch = oaWorkingCount.checkCounts();
		updateIterationsLog(mismatch, oaFlowList);
	}

//-----------------------------------------------------------------------------

	/**
	 *  Write difference of each current OA count from target OA counts
	 *
	 * @param oaFlowList	List of total flows from each OA
	 * @param mismatch		OA count proportional mismatch (convergence measure)
	 */
	private void updateIterationsLog(double mismatch, ZoneFlowList oaFlowList) {

		int countOA = oaFlowList.flows.size();
		String[] fieldsOA = new String[countOA+2];

		// Field 0: Mean of OA Count Differences proportioned to census OA counts
		fieldsOA[0] = Double.toString(mismatch);
		
		// Field 1: Mean of OA Count Differences
		fieldsOA[1] = Double.toString(oaWorkingCount.getMeanDifference());
		
		// Fields[2-]: OA count differences for each OA
		for (int i = 0; i < countOA; i++) {
			int difference = oaWorkingCount.getDifference(i);
			fieldsOA[i+2] = String.valueOf(difference);
		}
		
		// Write line of fields to CSV file
		writerIter.writeNext(fieldsOA);
	}

//-----------------------------------------------------------------------------

	/**
	 * Close Iterations log CSV file
	 * 
	 * @param oaFlowList	List of total flows from each OA
	 */
	private void closeIterationsLog(ZoneFlowList oaFlowList) {

		double oaMismatch;
		
		int countOA = oaFlowList.flows.size();
		String[] fieldsOA = new String[countOA+2];
		fieldsOA[0] = "Scaled mismatches:";
		fieldsOA[1] = "";
		for (int oa = 0; oa < countOA; oa++) {
			oaMismatch = oaWorkingCount.getMismatch(oa, 0);
			fieldsOA[oa+2] = String.format("%.3f", oaMismatch);
		}
		writerIter.writeNext(fieldsOA);
		
		// Print summary of individual OA mismatch range
		String[] summaryMsg = new String[1];
		summaryMsg[0] = String.format(
				"Individual OA mismatches range from %f to %f%n",
				oaWorkingCount.getMinMismatch(),
				oaWorkingCount.getMaxMismatch());
		writerIter.writeNext(summaryMsg);

		// Close Iterations log CSV file
		try {
			writerIter.close();
		} catch (IOException e) {

			// Print warning, but stumble on anyway
			e.printStackTrace();
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Write OA code errors table to CSV file for mapping quality of results
	 * 
	 * @param filenameCsv	Name of file to write to (should end in ".csv")
	 * @param oaFlowList	List of total flows from each OA
	 */
	public void writeZoneCodeFlowErrorsCsv(String filenameCsv,
			ZoneFlowList oaFlowList) {

		// Create log file for all iteration changes of assigned OA origins
		Formatter logFile = null;
		try {
			logFile = new Formatter(filenameCsv);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println(
					"Error opening flow errors logfile: ploughing on anyway..");
		}
		logFile.format(
				"OA_Index,OA_Code,n_Target,n_Actual,Difference,Mismatch%n");

		int countOA = oaFlowList.flows.size();
		for (int oa = 0; oa < countOA; oa++) {
			
			// Get actual and target counts
			int actualCount = oaWorkingCount.getCount(oa);
			int targetCount = oaFlowList.getCount(oa);
			
			// Get difference between actual and target count
			int difference = actualCount - targetCount;
			
			// Get mismatch to size of target count
			double mismatch = oaWorkingCount.getMismatch(oa, 0);

			logFile.format("%d,%s,%d,%d,%d,%.6f%n", oa,
					oaFlowList.flows.get(oa).zoneCode, targetCount,
					actualCount, difference, mismatch);
		}

		// Close log CSV file
		logFile.close();
	}

}
