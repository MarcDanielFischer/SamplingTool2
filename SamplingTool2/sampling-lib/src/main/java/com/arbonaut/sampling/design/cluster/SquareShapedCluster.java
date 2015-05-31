package com.arbonaut.sampling.design.cluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * Square-shaped plot cluster.
 */
public class SquareShapedCluster implements PlotCluster {
    
    private double plotDistance;
    private int numPlots;  
    
    /**
     * SquareShapedCluster constructor.
     *
     * @param plotDistance  Distance between sub-plots    
     * @param numPlots   Number of plots in the cluster. Note: any given input number 
     * will result in output clusters that consist of a number of sub-plots that is divisible by 4.    
     */              
    public SquareShapedCluster(double plotDistance, int numPlots)
    {
        this.plotDistance = plotDistance;
        this.numPlots = numPlots;
    }
    
    public CoordinateSequence create(Coordinate centre)
    {
        Coordinate[] coords = new Coordinate[this.numPlots];
        
		// centre is in the middle of the square
		// first sub-plot lies in upper left corner
        double offset = plotDistance * ((double)numPlots / 8);
        double x = centre.x - offset; 
		double y =centre.y + offset;
    	
		int n = 0; // index for coords[], as it has to be in continuous usage throughout several for-loops
		
		// create first sub-plot
		coords[n] = new Coordinate( x, y );
		n++;
		
		// rest of sub-plots on upper side 
		for(int i = 0; i < (numPlots / 4); i++){
			x = x + plotDistance;
			coords[n] = new Coordinate( x, y );
			n++;
		}

		// right side
		for(int i = 0; i < (numPlots / 4); i++){
			y = y - plotDistance;
			coords[n] = new Coordinate( x, y ); 
			n++;
		}

		// lower side
		for(int i = 0; i < (numPlots / 4); i++){
			x = x - plotDistance;
			coords[n] = new Coordinate( x, y ); 
			n++;
		}

		// left side
		for(int i = 0; i < (numPlots / 4) -1; i++){
			y = y + plotDistance;
			coords[n] = new Coordinate( x, y ); 
			n++;
		}
		
		return new CoordinateArraySequence(coords);
    }
}



