package com.arbonaut.sampling.design.cluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * Rotated Square-shaped plot cluster.
 */
public class RotatedSquareShapedCluster implements PlotCluster {
    
    private double plotDistance;
    private int numPlots = 4;  
    
    /**
     * RotatedSquareShapedCluster constructor.
     * Note: As for now, Rotated Square-shaped plot clusters have a fixed size of 4 sub-Plots.
     *
     * @param plotDistance  Distance between sub-plots    
     */              
    public RotatedSquareShapedCluster(double plotDistance)
    {
        this.plotDistance = plotDistance;
    }
    
    public CoordinateSequence create(Coordinate centre)
    {
        Coordinate[] coords = new Coordinate[this.numPlots];
        
		// calculate distance between sub-plots and centre point (applying Pythagorean theorem)
		double distToCentre = plotDistance / Math.sqrt(2.0);
        
		// centre is in the middle of the square
        double x = centre.x; 
		double y =centre.y;
    	
		int n = 0; // index for coords[]
		
		// create cluster Plots clockwise starting from the northern Plot 
		// 1. Plot
		coords[n] = new Coordinate( x, (y + distToCentre) ); n++;// only Y coord affected as we move north
		// 2. Plot
		coords[n] = new Coordinate( (x + distToCentre ), y ); n++;// only X coord affected as we move east
		// 3. Plot
		coords[n] = new Coordinate( x, (y - distToCentre) ); n++;// only Y coord affected as we move south
		// 4. Plot
		coords[n] = new Coordinate( (x - distToCentre ), y ); // only Y coord affected as we move west
		
		return new CoordinateArraySequence(coords);
    }
}
