/*
 * Classname: GenSynthFlow
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Generate Synthetic Flow Data between OAs and WZs based on tables
 * of total commuters from each OA and from each WZ, combined with
 * knowledge about distance of travel to work for those commuters.
 * 
 * @author Richard Thomas
 * @version 1.1, August 2014
 */
public class GenSynthFlow {

	/**
	 * Seed for pseudo-random number generator for  building population
	 * Either fix it (for repeatable population) or use system time in ms
	 */
	private static final long RAND_SEED =
			1;
//			7805692770L;
//			System.currentTimeMillis();
	
	/**
	 * Selected distance interval (e.g. 0 = 0_2km)
	 */
	static final int SEL_DIST_INTERVAL = 3;

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
			"MSOA_2011_PWC_XY_WP702_4UA.csv";
//			"WZ_2011_PWC_XY_WP702_4UA.csv";
//			"WZ_2011_PWC_XY_WP702_Bris_UA.csv";
	
	/**
	 * (Input) CSV file of Output Area commuting totals (by distance)
	 */
	static final String OA_TOTALS_CSV_FILENAME =
			"MSOA_2011_PWC_XY_QS702_4UA.csv";
//			"OA_2011_PWC_XY_QS702_4UA.csv";
//			"OA_2011_PWC_XY_QS702_Bris_UA.csv";
	
	/**
	 * (Output Diagnostic) CSV filename stub (no suffix!) of all flows within
	 * selected distance interval. This can be transformed to line plots
	 * using "XY to line" tool in ArcGIS.
	 * (Set to null if diagnostics not required)
	 */
	static final String FLOWS_IN_RANGE_CSV_FILESTUB =
//			"OA_WZ_2011_PWC_Flows_In_Range_4UA_";
//			"OA_WZ_2011_PWC_Flows_In_Range_Bris_UA_";
			null;
	
	/**
	 * (Output) CSV filename stub (no suffix!) of final estimated flows counts
	 * between each OA-WZ origin-destination pair. 2 Output files will be
	 * produced (for ArcGIS and QGIS) allowing transformation to
	 * line plots using "XY to line" ArcGIS tool or "points2one" QGIS plugin.
	 */
	static final String FLOW_MATRIX_CSV_FILESTUB =
			"MSOA_2011_PWC_Flow_Matrix_4UA_";
//			"OA_WZ_2011_PWC_Flow_Matrix_4UA_";
//			"OA_WZ_2011_PWC_Flow_Matrix_Bris_UA_";
	
	/**
	 * (Output) Filename stub to write details of OAs (with their zone codes)
	 * and their flow counts for the synthesized population vs the census
	 * derived population for mapping quality of results.
	 * (Set to null if diagnostics not required)
	 */
	private static final String OA_ZONECODE_FLOW_ERRORS_FILESTUB =
			"MSOA_Zone_Coded_Flow_Errors_4UA_" ;
//			"OA_Zone_Coded_Flow_Errors_4UA_" ;
//			"OA_Zone_Coded_Flow_Errors_Bris_UA_" ;
//			null;
	
	/**
	 * Field index along CSV line for ONS WZ geography code
	 */
	static final int WZ_CODE_CSV_FIELD = 0; 	// for MSOA table
//	static final int WZ_CODE_CSV_FIELD = 1; 	// for OA table
	
	/**
	 * Field index along CSV line for ONS OA geography code
	 */
	static final int OA_CODE_CSV_FIELD = 0;		// for MSOA table
//	static final int OA_CODE_CSV_FIELD = 2; 	// for OA table
	
	/**
	 * Range allowed between PWCs for each distance interval
	 * (allow 500m extra as may reach a nearby area even if not its centroid)
	 */
	static final int[][] PWC_DISTANCE_LIMITS = {
		{0, 	2000},	// 0: 0-2 km
		{2000, 	5000},	// 1: 2-5 km
		{5000, 10000},	// 2: 5-10 km
		{10000,20000},	// 3: 10-20 km
		{20000,30000},	// 4: 20-30 km
		{30000,40000},	// 5: 30-40 km
		{40000,60000},	// 6: 40-60 km
		{60000,999500}	// 7: 60+ km
	};
	
	// (Previously allowed 500m extra as may reach a nearby area even if not
	// its centroid - however this cause unwanted boosting of numbers of flows
	// in overlapping regions)
	/* static final int[][] PWC_DISTANCE_LIMITS = {
		{0, 	2500},	// 0: 0-2 km
		{1500, 	5500},	// 1: 2-5 km
		{4500, 10500},	// 2: 5-10 km
		{9500, 20500},	// 3: 10-20 km
		{19500,30500},	// 4: 20-30 km
		{29500,40500},	// 5: 30-40 km
		{39500,60500},	// 6: 40-60 km
		{59500,999500}	// 7: 60+ km
	};
	*/
	
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
		
		// Write to a CSV file all OA-WZ flow paths within selected distance
		// range. This can be transformed to line plots using "XY to line"
		// tool in ArcGIS.
		if (FLOWS_IN_RANGE_CSV_FILESTUB != null) {
			System.out.printf("%nDiagnostic dump of flows in range %s...%n",
					DISTANCE_INTERVAL_TEXT[SEL_DIST_INTERVAL]);
			String filename = FLOWS_IN_RANGE_CSV_FILESTUB +
					DISTANCE_INTERVAL_TEXT[SEL_DIST_INTERVAL] + ".csv";
			writeFlowsInRangeCsv(filename);
		}
		
		// Generate initial synthetic population, assigning final values for
		// destination WZs, but only initial values for OA origins.
		System.out.printf(
				"%nGenerating initial synthetic population (seed = %d)...%n",
				RAND_SEED);
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
		
		// Write flow matrix to CSV files for ArcGIS and QGIS
		String matrixFilenameStub = FLOW_MATRIX_CSV_FILESTUB +
				DISTANCE_INTERVAL_TEXT[SEL_DIST_INTERVAL];
		writeFlowMatrixPairsCsv(flowODMatrix, oaFlowList, wzFlowList,
				matrixFilenameStub);
		
		// Write OA code errors table to CSV file for mapping quality of results
		if (OA_ZONECODE_FLOW_ERRORS_FILESTUB != null) {

			String flowErrorsFilename = OA_ZONECODE_FLOW_ERRORS_FILESTUB +
					DISTANCE_INTERVAL_TEXT[SEL_DIST_INTERVAL] + ".csv";
			System.out.println(
					"Writing OA flow count errors to mappable file:");
			System.out.println("  " + flowErrorsFilename);
			synthPop.writeZoneCodeFlowErrorsCsv(flowErrorsFilename, oaFlowList);
		}

		System.out.printf("%nGenSynthFlow: All Done!%n");
	}

//-----------------------------------------------------------------------------

	/**
	 * Write to a CSV file all OA-WZ flow paths within selected distance range.
	 * This can be transformed to line plots in ArcGIS using "XY to line".
	 * 
	 * @param flowsInRangeCsv	Filename for CSV output
	 */
	private static void writeFlowsInRangeCsv(String flowsInRangeCsv) {

		System.out.println("Writing flows in range to file: " +
				flowsInRangeCsv);

		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new FileWriter(flowsInRangeCsv), ',',
					CSVWriter.NO_QUOTE_CHARACTER);
		} catch (IOException e) {
			
			// Not the end of the world so stumble on after a warning..
			e.printStackTrace();
		}

		// Write field names (header) line
		String[] fields = "FLOW_OA_WZ#OA11CD#OA_X#OA_Y#WZ11CD#WZ_X#WZ_Y".split("#");
		writer.writeNext(fields);

		// Loop through each WZ and each candidate OA possible from that WZ
		for (int indexWZ = 0; indexWZ < wzFlowList.flows.size(); indexWZ++) {
			int[] candOAs = candOAsForAllWZs[indexWZ].getCandOAsPerWZ();
			
			// Temporary diagnostics
			//System.out.printf("WZ %d: cand OAs = %d%n", indexWZ, candOAsForAllWZs[indexWZ].getCount());
			
			for (int indexCandOA = 0; indexCandOA < candOAs.length;
					indexCandOA++) {
				int candOA = candOAs[indexCandOA];
				fields[0] = oaFlowList.flows.get(candOA).zoneCode +
						wzFlowList.flows.get(indexWZ).zoneCode;
				fields[1] = oaFlowList.flows.get(candOA).zoneCode;
				fields[2] = String.valueOf(oaFlowList.flows.get(candOA).eastingPWC);
				fields[3] = String.valueOf(oaFlowList.flows.get(candOA).northingPWC);
				fields[4] = wzFlowList.flows.get(indexWZ).zoneCode;
				fields[5] = String.valueOf(wzFlowList.flows.get(indexWZ).eastingPWC);
				fields[6] = String.valueOf(wzFlowList.flows.get(indexWZ).northingPWC);

				// Write fields to a line in the CSV file
				writer.writeNext(fields);
			}
		}

		// Close CSV file
		try {
			writer.close();
		} catch (IOException e) {

			// Print warning, but stumble on anyway
			e.printStackTrace();
		}
	}

//-----------------------------------------------------------------------------
		
	/**
	 * Write Synthesized Origin-Destination Flow Matrix Pairs to a CSV file
	 * Each OA-WZ record (line in file) holds:
	 * FLOW_OA_WZ	- Flow ID (concatenation of OA_Code & WZ_Code)
	 * OA_Code 		- ONS "Output Area" (origin) label
	 * OA_PWC_X		- Easting of OA Population Weighted Centroid
	 * OA_PWC_Y		- Northing of OA Population Weighted Centroid
	 * WZ_Code		- ONS "Workplace Zone" (destination) label
	 * WZ_PWC_X		- Easting of WZ Population Weighted Centroid
	 * WZ_PWC_Y		- Northing of WZ Population Weighted Centroid
	 * Count		- Total commuters on this flow route
	 *
	 * @param filenameStub	Filename stub (omitting ".csv") for CSV output
	 */
	private static void writeFlowMatrixPairsCsv(int[][] flowMatrix,
			ZoneFlowList oaList, ZoneFlowList wzList,
			String filenameStub) {

		// Create file formatted to support QGIS "points2one" plugin
		String fileNameArcG = filenameStub + "_ArcGIS.csv";
		String fileNameQgis = filenameStub + "_QGIS.csv";
		
		System.out.println(
				"Writing ArcGIS 'XY to Line'-tool compliant OA-WZ flow pairs to file:");
		System.out.println("  " + fileNameArcG);
		System.out.println(
				"Writing QGIS 'points2one'-plugin compliant version to file:");
		System.out.println("  " + fileNameQgis);

		CSVWriter writerArcG = null;
		CSVWriter writerQgis = null;
		try {
			writerArcG = new CSVWriter(new FileWriter(fileNameArcG), ',',
					CSVWriter.NO_QUOTE_CHARACTER);
			writerQgis = new CSVWriter(new FileWriter(fileNameQgis), ',',
					CSVWriter.NO_QUOTE_CHARACTER);
		} catch (IOException e) {
			
			// Print warning, but stumble on anyway as nearly finished
			e.printStackTrace();
		}

		// Write field names (header) lines
		String[] fieldsArcG = { "FLOW_OA_WZ", "OA_Code", "OA_PWC_X",
				"OA_PWC_Y", "WZ_Code", "WZ_PWC_X", "WZ_PWC_Y", "Count" };
		writerArcG.writeNext(fieldsArcG);
		String[] fieldsQgis = { "FLOW_OA_WZ", "OA_Code", "WZ_Code",
				"Count", "Zone_Type", "Zone_PWC_X", "Zone_PWC_Y" };
		writerQgis.writeNext(fieldsQgis);

		
		// Loop through each WZ and each candidate OA possible from that WZ
		for (int indexOA = 0; indexOA < flowMatrix.length; indexOA++) {
			for (int indexWZ = 0; indexWZ < flowMatrix[0].length; indexWZ++) {
				int flowCount = flowMatrix[indexOA][indexWZ];
				
				// Only write OA-WZ pair if at least one commuter
				if (flowCount > 0) {
					String oaCode = oaList.flows.get(indexOA).zoneCode;
					String oaX = String.valueOf(oaList.flows.get(indexOA).eastingPWC);
					String oaY = String.valueOf(oaList.flows.get(indexOA).northingPWC);
					String wzCode = wzList.flows.get(indexWZ).zoneCode;
					String wzX = String.valueOf(wzList.flows.get(indexWZ).eastingPWC);
					String wzY = String.valueOf(wzList.flows.get(indexWZ).northingPWC);

					// Write fields to a line in the ArcGIS CSV file
					fieldsArcG[0] = oaCode + wzCode;
					fieldsArcG[1] = oaCode;
					fieldsArcG[2] = oaX;
					fieldsArcG[3] = oaY;
					fieldsArcG[4] = wzCode;
					fieldsArcG[5] = wzX;
					fieldsArcG[6] = wzY;
					fieldsArcG[7] = String.valueOf(flowCount);
					writerArcG.writeNext(fieldsArcG);
					
					// Write fields to 2 lines in the QGIS CSV file
					fieldsQgis[0] = oaCode + wzCode;
					fieldsQgis[1] = oaCode;
					fieldsQgis[2] = wzCode;
					fieldsQgis[3] = String.valueOf(flowCount);
					fieldsQgis[4] = "OA";
					fieldsQgis[5] = oaX;
					fieldsQgis[6] = oaY;
					writerQgis.writeNext(fieldsQgis);
					fieldsQgis[4] = "WZ";
					fieldsQgis[5] = wzX;
					fieldsQgis[6] = wzY;
					writerQgis.writeNext(fieldsQgis);
				}
			}
		}

		// Close CSV file
		try {
			writerArcG.close();
			writerQgis.close();
		} catch (IOException e) {

			// Print warning, but stumble on anyway
			e.printStackTrace();
		}
	}

//-----------------------------------------------------------------------------

	/**
	 * Get distance^2 between specified origin and destination
	 * 
	 * @param indexOA 	Origin index
	 * @param indexWZ	Destination index
	 * @return			Origin-Destination distance^2
	 */
	public static int getDistanceSqr(int indexOA, int indexWZ) {

		// Get WZ centroid
		int wzX = wzFlowList.flows.get(indexWZ).eastingPWC;
		int wzY = wzFlowList.flows.get(indexWZ).northingPWC;

		// Get OA Centroid
		int oaX = oaFlowList.flows.get(indexOA).eastingPWC;
		int oaY = oaFlowList.flows.get(indexOA).northingPWC;

		// Get OA-WZ distance^2 (in metres^2)
		int distSquared = (oaX - wzX) * (oaX - wzX)
				+ (oaY - wzY) * (oaY - wzY);

		return distSquared;
	}

}
