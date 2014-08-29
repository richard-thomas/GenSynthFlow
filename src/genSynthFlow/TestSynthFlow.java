/*
 * Classname: TestSynthFlow
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.Random;

/**
 * Test statistical variation of generated Synthetic Flow Data.
 * (Stripped down version of GenSynthFlow which creates multiple populations
 *  to see how they vary)
 * 
 * @author Richard Thomas
 * @version 1.0, July 2014
 */
public class TestSynthFlow {

	/*
	 * Number of times to generate a synthetic population
	 */
	private static final int TOTAL_RUNS = 20;
	
	/**
	 * Seed for pseudo-random number generator for  building population
	 * Either fix it (for repeatable population) or use system time in ms
	 */
	private static final long RAND_SEED = 2;
//	private static final long RAND_SEED = System.currentTimeMillis();
	
	/**
	 * Selected distance interval (e.g. 0 = 0_2km)
	 */
	static final int SEL_DIST_INTERVAL = 1;

	/**
	 * Text to refer to each distance interval
	 */
	static final String[] DISTANCE_INTERVAL_TEXT = {
		"0_2km",	// 0: 0-2 km
		"2_5km",	// 1: 2-5 km
		"5_10km",	// 2: 5-10 km
		"10_20km",	// 3: 10-20 km
		"20_30km",	// 4: 20-30 km
		"30_40km",	// 5: 30-40 km
		"40_60km",	// 6: 40-60 km
		"over_60km"	// 7: 60+ km
	};
	
	/**
	 * (Input) CSV file of Workplace Zone commuting totals (by distance)
	 */
	static final String WZ_TOTALS_CSV_FILENAME =
			"WZ_2011_PWC_XY_WP702_4UA.csv";
//			"WZ_2011_PWC_XY_WP702_Bris_UA.csv";
	
	/**
	 * (Input) CSV file of Output Area commuting totals (by distance)
	 */
	static final String OA_TOTALS_CSV_FILENAME =
			"OA_2011_PWC_XY_QS702_4UA.csv";
//			"OA_2011_PWC_XY_QS702_Bris_UA.csv";
	
	/**
	 * (Output) Filename stub to write details of OAs (with their zone codes)
	 * and their flow counts for the synthesized population vs the census
	 * derived population for mapping quality of results.
	 * (Set to null if diagnostics not required)
	 */
	/**
	 * Field index along CSV line for ONS WZ geography code
	 */
	static final int WZ_CODE_CSV_FIELD = 1;
	
	/**
	 * Field index along CSV line for ONS WZ geography code
	 */
	static final int OA_CODE_CSV_FIELD = 2;
	
	/**
	 * Range allowed between PWCs for each distance interval
	 */
	static final int[][] PWC_DISTANCE_LIMITS = {
		{0, 	2500},	// 0: 0-2 km
		{1500, 	5500},	// 1: 2-5 km
		{4500, 10500},	// 2: 5-10 km
		{9500, 20500},	// 3: 10-20 km
		{19500,30500},	// 4: 20-30 km
		{29500,40500},	// 5: 30-40 km
		{39500,60500},	// 6: 40-60 km
		{59500,999500}	// 7: 60+ km
	};
	
	/**
	 * Pseudo-random number generator for building population
	 * (seed can be fixed for repeatable populations)
	 */
	static Random randGenSynthPop = new Random(RAND_SEED);
	
//-----------------------------------------------------------------------------

	/**
	 * (Ordered) lists of candidate OAs in distance range for each WZ
	 */
	private static CandOAsPerWZ[] candOAsForAllWZs;
	
	/*
	 * WZ centroids & totals extracted from 2011 census-derived CSV files
	 */
	private static ZoneFlowList wzFlowList;
	
	/*
	 * OA centroids & totals extracted from 2011 census-derived CSV files
	 */
	private static ZoneFlowList oaFlowList;

//-----------------------------------------------------------------------------

	/**
	 * Main program entry point and sequencing of major operations
	 * 
	 * @param args	(ignored for now)
	 * @throws FileNotFoundException	Unable to open specified input CSV files
	 * @throws IOException				Error reading specified input CSV files
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
		// Synthetic population of workers
		SynthPop synthPop;
	
		// Read OA and WZ centroids & totals from 2011 census-derived CSV files
		System.out.println("Reading Census CSV files...");
		wzFlowList = new ZoneFlowList(WZ_TOTALS_CSV_FILENAME, WZ_CODE_CSV_FIELD);
		oaFlowList = new ZoneFlowList(OA_TOTALS_CSV_FILENAME, OA_CODE_CSV_FIELD);
		int countWZ = wzFlowList.flows.size();
		int countOA = oaFlowList.flows.size();
		
		// Determine distance range to allow between OA and WZ
		int minDistance = PWC_DISTANCE_LIMITS[SEL_DIST_INTERVAL][0];
		int maxDistance = PWC_DISTANCE_LIMITS[SEL_DIST_INTERVAL][1];

		// Loop through WZs copying across selected distance counts,
		// then generating candidate OAs for each WZ
		System.out.println("Finding candidate OAs within range of each WZ...");
		System.out.println("Nominal range: " +
				DISTANCE_INTERVAL_TEXT[SEL_DIST_INTERVAL]);
		System.out.printf("Searched range: %.1f to %.1f km%n",
				minDistance / 1000.0, maxDistance / 1000.0);
		
		// For each OA and WZ flow list, use the counts for the selected
		// distance interval
		int totalWZFlows = wzFlowList.setDistance(SEL_DIST_INTERVAL);
		int totalOAFlows = oaFlowList.setDistance(SEL_DIST_INTERVAL);
		
		// Generate (ordered) lists of candidate OAs in range for each WZ
		candOAsForAllWZs = new CandOAsPerWZ[countWZ];
		for (int indexWZ = 0; indexWZ < countWZ; indexWZ++) {
			candOAsForAllWZs[indexWZ] = new CandOAsPerWZ(oaFlowList, wzFlowList,
					indexWZ, minDistance, maxDistance);
		}
		
		// Loop through each WZ and each candidate OA possible from that WZ
		
		// Count total possible flows in range (even if their count might end
		// up as zero) by summing count of all candidate OAs for each WZ
		int maxFlows = 0;
		for (int indexWZ = 0; indexWZ < countWZ; indexWZ++) {
			maxFlows += candOAsForAllWZs[indexWZ].getCount();
		}

		// Create fixed array to store all possible flow counts for each run
		short[][] flowCounts = new short[TOTAL_RUNS][maxFlows];
		
		// Create several synthetic populations and gather statistics about
		// variations in final flow data. Do not further adjust random seed
		// to allow it to free-run for maximum entropy.
		for (int run = 0; run < TOTAL_RUNS; run++) {
			
			// Generate initial synthetic population, assigning final values for
			// destination WZs, but only initial values for OA origins.
			System.out.printf("%nGenerating initial synthetic population %d...%n",
					run + 1);
			synthPop = new SynthPop(wzFlowList, oaFlowList, candOAsForAllWZs,
					randGenSynthPop);

			// Diagnostic output
			System.out.println();
			System.out.println("Number of WZs: " + countWZ);
			System.out.println("Number of OAs: " + countOA);
			System.out.println("Total flows from census: WZ = " + totalWZFlows +
					", OA = " + totalOAFlows +
					", OA-WZ = " + (totalOAFlows - totalWZFlows));

			// Iterate population to get closer to OA targets
			System.out.printf("%nIterate population to get closer to OA targets...%n");
			synthPop.iterSynthFlowDist(oaFlowList);

			// Generate synthetic flow matrix based on just totals
			System.out.printf("%nGenerating synthetic flows matrix...%n");
			int[][] flowODMatrix = synthPop.popToFlowMatrix(countOA, countWZ);
			
			// Store final flow counts for every possible flow (1.5 million)
			
			// Loop through each WZ and each candidate OA possible from that WZ
			int flowIndex = 0;
			for (int indexWZ = 0; indexWZ < countWZ; indexWZ++) {

				// Do only if OAs and WZs in range (even if count is zero)
				// by getting only candidate OAs for this WZ
				int[] candOAs = candOAsForAllWZs[indexWZ].getCandOAsPerWZ();

				for (int indexCandOA = 0; indexCandOA < candOAs.length;
						indexCandOA++) {

					// Get Candidate OA
					int indexOA = candOAs[indexCandOA];

					// Get flow descriptors
					flowCounts[run][flowIndex] =
							(short)flowODMatrix[indexOA][indexWZ];
					flowIndex++;
				}
			}
			
			// Shouldn't need to keep details of what each flow is as should
			// be the same every time, but do a sanity check!
			assert (flowIndex == maxFlows) :
				"flowIndex = " + flowIndex + ", but maxFlows = " + maxFlows;
			
			// Hint to VM now is a good time to do garbage collection
			// (not guaranteed to do anything)
			System.out.println("Attempting garbage collection...");
			synthPop = null;
			flowODMatrix = null;
			System.gc();
		}
		
		System.out.println("Max flows = " + maxFlows);
		
		// TODO: temporary; don't need to write out file
		// Write out flow counts (and their mean) to a file, but only if mean
		// is non-zero (i.e. some flow on at least one simulation
		Formatter logFile = null;
		logFile = new Formatter("flows.csv");
//		double sumPropVar = 0; // Sum of proportional variation from mean
		int totalInSum = 0;
		double sumScaledStdDev = 0;
		for (int flowID = 0; flowID < maxFlows; flowID++) {
			int sumFlow = 0;
			for (int run = 0; run < TOTAL_RUNS; run++) {
				sumFlow += flowCounts[run][flowID];
			}
			
			if (sumFlow > 0) {
				double meanFlow = ((double)sumFlow) / TOTAL_RUNS;
				double MagDiff;				// Magnitude of difference from mean
				double sumMagDiffs = 0.0;	// Sum Magnitude of diff from mean
				double sumMagDiffsSqr = 0.0;// Sum Magnitude of diff^2 from mean
				for (int run = 0; run < TOTAL_RUNS; run++) {
					int flowCount = flowCounts[run][flowID];
//					sumPropVar += (flowCount - meanFlow) / meanFlow;
					MagDiff = Math.abs(flowCount - meanFlow);
					sumMagDiffs += MagDiff;
					sumMagDiffsSqr += MagDiff * MagDiff;
				}
				double stdDev = Math.sqrt(sumMagDiffsSqr / TOTAL_RUNS -
						(sumMagDiffs * sumMagDiffs) / (TOTAL_RUNS * TOTAL_RUNS));
				logFile.format("%d,%d,%d,%.6f,%.6f%n", flowCounts[0][flowID],
						flowCounts[1][flowID], flowCounts[2][flowID],
						meanFlow, stdDev);
				sumScaledStdDev += stdDev / meanFlow;
				totalInSum++;
			}
		}
		logFile.close();
		System.out.println("sumScaledStdDev = " + sumScaledStdDev);
		System.out.println("totalInSum = " + totalInSum);
		System.out.println("sumScaledStdDev/totalInSum = " +
				(sumScaledStdDev / totalInSum));

		System.out.printf("%nTestSynthFlow: All Done!%n");
	}

}
