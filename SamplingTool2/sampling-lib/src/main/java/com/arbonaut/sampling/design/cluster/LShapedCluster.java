
package com.arbonaut.sampling.design.cluster;

import java.util.ArrayList;

import org.geotools.geometry.jts.JTSFactoryFinder;


import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * L-shaped plot cluster.
 * L-shaped cluster is a series of plots with an offset applied to the y 
 * coordinate. 
 */
public class LShapedCluster implements PlotCluster {
    
    private double plotDistance;
    private int numPlots;  
    
    /**
     * LShapedCluster constructor.
     *
     * @param plotDistance  Distance between sub-plots   
     * @param numPlots   Number of plots in the cluster     
     */              
    public LShapedCluster(double plotDistance, int numPlots)
    {
        this.plotDistance = plotDistance;
        this.numPlots = numPlots;
    }

    public CoordinateSequence create(Coordinate centre)
    {
    	Coordinate[] coords = new Coordinate[this.numPlots];

    	// Determine number of plots to be created along each axis of the L
    	int numPlotsVertical = 0;
    	int numPlotsHorizontal = 0;

    	// odd total number of Plots shaping the cluster
    	if(this.numPlots % 2 != 0){ 
    		// in case of an odd total Plot number, both axes are of the same length
    		numPlotsVertical = numPlotsHorizontal = (this.numPlots-1) / 2;
    	} 
		// even total number of Plots shaping the cluster
    	else{ 
			// in case of an even total Plot number, the vertical axis will be one Plot longer than the horizontal axis
			numPlotsVertical = this.numPlots / 2;
			numPlotsHorizontal = (this.numPlots / 2) -1;
		} 

		// extract seed point coords and use them to build the other Plots
		double x = centre.x;
    	double y = centre.y;

    	int n = 0; // index for coords[], as it has to be in continuous usage throughout 2 for loops
		
    	// vertical axis
		for(int i = 0; i <= numPlotsVertical; i++){
			coords[n] = new Coordinate(x, y);	
			y += this.plotDistance; // as we build along the vertical axis, only y coords are affected
			n++;
				
		}

		// reset y coord for horizontal axis 
		y = centre.y;

		// horizontal axis
		for(int i = 0; i < numPlotsHorizontal; i++){
			x += this.plotDistance; // as we build along the vertical axis, only x coords are affected
			coords[n] = new Coordinate(x, y);	
			n++;
		}
		return new CoordinateArraySequence(coords);
    }
}

///////////////////////////////////////////////////////////////////////////////
// copied code, to be shoveled around and modified
///////////////////////////////////////////////////////////////////////////////
/**
 * Grow L-shaped clusters from given seed points with a 
 * specified number of sub-plots per cluster 
 * and a specified distance separating the sub-plots (distance in Meters).
 * In case of an even total number of sub-plots shaping the cluster, the vertical axis 
 * will be one sub-plots longer than the horizontal axis (just like the real character "L").
 * @param clusterSeedPoints. Must be in UTM projection.
 * @param distBetweenSubPlots
 * @param numClusterSubPlots
 * @param orientation 1: regular "L" shape -1: upside down
 * @param stratum this param is needed in order to check whether all generated cluster plots are inside the stratum. Must be in UTM projection. 
 */
//public static ArrayList<Plot> create_L_clusters(ArrayList<Plot> clusterSeedPoints, int distBetweenSubPlots, int numClusterSubPlots, int orientation,  Stratum stratum ){
//	
//	// initialize output ArrayList
//	ArrayList<Plot> outputPlots = new ArrayList<Plot>();
//
//	// iterate over seed points
//	for(Plot seedPoint : clusterSeedPoints){
//
//		int clusterNr = 0;
//		int subPlotNr = 0;
//
//		if(numClusterSubPlots > 0){
//			// use seedPoint.plotNr as clusterNr and set new seedPoint.plotNr to 1
//			clusterNr = seedPoint.getPlotNr();
//			subPlotNr = 1;
//			seedPoint.setClusterNr(clusterNr);
//			seedPoint.setPlotNr(subPlotNr);
//
//			// add each seed point to output after changing its numbering
//			outputPlots.add(seedPoint);
//
//			// Determine number of plots to be created along each axis of the L
//			int numPlotsVertical = 0;
//			int numPlotsHorizontal = 0;
//
//			// odd total number of Plots shaping the cluster
//			if(numClusterSubPlots % 2 != 0){ 
//				// in case of an odd total Plot number, both axes are of the same length
//				numPlotsVertical = numPlotsHorizontal = (numClusterSubPlots-1) / 2;
//			} 
//
//			// even total number of Plots shaping the cluster
//			if(numClusterSubPlots % 2 == 0){ 
//				// in case of an even total Plot number, the vertical axis will be one Plot longer than the horizontal axis
//				numPlotsVertical = numClusterSubPlots / 2;
//				numPlotsHorizontal = (numClusterSubPlots / 2) -1;
//			} 
//
//
//			// extract seed point coords and use them to build the other Plots
//			double x = seedPoint.getPoint().getCoordinate().x;
//			double y = seedPoint.getPoint().getCoordinate().y;
//
//
//			// build Plots along vertical axis
//			for(int i = 0; i < numPlotsVertical; i++){
//
//				if(orientation == 1){ // regular "L"
//					y += distBetweenSubPlots; // as we build along the vertical axis, only y coords are affected
//				}
//				if(orientation == -1){ // upside down "L"
//					y -= distBetweenSubPlots;
//				}
//
//				// create Points using GeometryFactory
//				GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
//				Coordinate coord = new Coordinate( x, y );
//				Point point = geometryFactory.createPoint( coord );
//
//				// check if Point inside stratum, create Plot and add Plot to output ArrayList
//				if(point.within(stratum.getGeometry())){
//					// a plot contains -aside from the Point object as a property - the name of the stratum it is located in and CRS information
//					subPlotNr++;
//					Plot plot = new Plot(point, stratum.getName(), stratum.getCRS(), subPlotNr, clusterNr);
//					// check if there is a weight value and add it
//					double weight = seedPoint.getWeight();
//					if( weight != -1.0){
//						plot.setWeight(weight);
//					}
//					outputPlots.add(plot);
//				}
//			}
//
//
//			// reset y coord (which has been increased during vertical axis Plot construction)  so that we can start building plots along the horizontal axis
//			y = seedPoint.getPoint().getCoordinate().y;
//
//			// build plots along horizontal axis
//			for(int i = 0; i < numPlotsHorizontal; i++){
//				x += distBetweenSubPlots; // as we build along the vertical axis, only x coords are affected
//
//				// create Points using GeometryFactory
//				GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
//				Coordinate coord = new Coordinate( x, y );
//				Point point = geometryFactory.createPoint( coord );
//
//				// check if Point inside stratum, create Plot and add Plot to output ArrayList
//				if(point.within(stratum.getGeometry())){
//					// a plot contains -aside from the Point object as a property - the name of the stratum it is located in and CRS information
//					subPlotNr++;
//					Plot plot = new Plot(point, stratum.getName(), stratum.getCRS(), subPlotNr, clusterNr);
//					// check if there is a weight value and add it
//					double weight = seedPoint.getWeight();
//					if( weight != -1.0){
//						plot.setWeight(weight);
//					}
//					outputPlots.add(plot);
//				}
//			}
//		}
//	}
//	return outputPlots;
//}
