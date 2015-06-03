package com.arbonaut.sampling.util;


import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.opengis.coverage.processing.Operation;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.InternationalString;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * This class contains static methods to process raster data used in weighted sampling.
 * @author daniel
 *
 */
public class RasterProcessing {
	
	/**
	 * Queries raster pixel values at given Plot location.
	 * This method is overloaded so that it fits both raster files with integer as well as floating point values.
	 * Works with multiband rasters, too.
	 * {@link http://docs.geotools.org/latest/userguide/library/coverage/grid.html}
	 * @param coverage the input raster
	 * @param plot position at which the raster shall be queried
	 * @param dest specifies a raster with floating-point values (double type)
	 * @return array containing raster pixel values for all bands at the specified position. 
	 * @throws Exception
	 */
	public static double[] getValueAtPosition(GridCoverage2D coverage, Coordinate coord, CoordinateReferenceSystem coordCrs, double[] dest) throws Exception{
		// check if CRS from Plot and Raster are different and reproject if needed
		CoordinateReferenceSystem crsRaster = coverage.getCoordinateReferenceSystem();
		boolean needsReproject = !CRS.equalsIgnoreMetadata(coordCrs, crsRaster);
		MathTransform transform = CRS.findMathTransform(coordCrs, crsRaster, true);

		if(needsReproject){
			// create Point --> tranform Point --> extract coord form Point
			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
			Point point = geometryFactory.createPoint( coord );
			point = (Point)JTS.transform(point, transform);
			coord = point.getCoordinate();
		}
		
		DirectPosition position = new DirectPosition2D( crsRaster, coord.x, coord.y);
		
		double[] value =coverage.evaluate(position, (double[]  ) null);
		return value;
	}
	
	
	/**
	 * Clips a Geometry from an input GridCoverage2D.
	 * @param clipGeometry must have the same CRS as coverage. This is provided when 
	 * clipGeometries have been extracted using the getClipGeometries() method.
	 * @param coverage
	 * @return
	 */
	public static GridCoverage2D getClippedCoverage(Geometry clipGeometry, GridCoverage2D coverage){
		ArrayList<Geometry> clipGeometries = new ArrayList<Geometry>();
		clipGeometries.add(clipGeometry);
		return getClippedCoverage(clipGeometries, coverage);
	}
	
	
	/**
	 * Clips an input GridCoverage2D using ArrayList<Geometry> clipGeometries.
	 * @param clipGeometries must have the same CRS as coverage. This is provided when 
	 * clipGeometries have been extracted using the getClipGeometries() method.
	 * @param coverage
	 * @return
	 */
	public static GridCoverage2D getClippedCoverage(ArrayList<Geometry> clipGeometries, GridCoverage2D coverage){
		CoverageProcessor processor = new CoverageProcessor();
		Operation operation = processor.getOperation("CoverageCrop");
		ParameterValueGroup params = operation.getParameters();
		params.parameter("Source").setValue(coverage);
		GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
		Geometry[] a = clipGeometries.toArray(new Geometry[0]);
		GeometryCollection c = new GeometryCollection(a, factory);
		params.parameter("ROI").setValue(c);
		params.parameter("ForceMosaic").setValue(true); 
		System.setProperty("com.sun.media.jai.disableMediaLib", "true"); // gets rid of the annoying Exception in the following line
		GridCoverage2D clippedCoverage = (GridCoverage2D)processor.doOperation(params); // this line throws the following annoying Exception if JAI MediaLib is not disabled: Error: Could not find mediaLib accelerator wrapper classes. Continuing in pure Java mode.
		return clippedCoverage;
	}
	
	
	/**
	 * Convenience method for reading GeoTIFF raster files.
	 * @param rasterFile
	 * @return the GeoTIFF raster file as a GridCoverage2D object
	 * @throws Exception
	 */
	public static GridCoverage2D readGeoTiff(File rasterFile) throws Exception{
		// create Reader  
		GeoTiffReader reader = new GeoTiffReader(rasterFile);
		// read Raster data
		GridCoverage2D coverage = reader.read(null);
		return coverage;
	}

	
	/**
	 * Gets the NODATA value of the specified band of a GridCoverage2D object. For use with weight rasters: bandIndex should always be 0
	 * as weight rasters are supposed to only have 1 band. If there is no category named "No data", return value will be
	 * Double.NEGATIVE_INFINITY.
	 * @param coverage input raster 
	 * @param bandIndex the band to get the NODATA value from
	 * @return noDataValue
	 * @throws Exception
	 */
	public static double getNoDataValue(GridCoverage2D coverage, int bandIndex) throws Exception{
		double noDataValue = Double.NEGATIVE_INFINITY; // is that a wise thing to do here? --> primitive variables cannot be set to NULL, so i need a dummy value that at the same time is unlikely to be used as a valid raster data value

		GridSampleDimension band = coverage.getSampleDimension(bandIndex); 
		List<Category> categories = band.getCategories();
		Iterator<Category> iterator = categories.iterator();
		while(iterator.hasNext()){
			Category category = iterator.next();
			InternationalString name = category.getName();

			if(name.toString().equals("No data")){
				NumberRange<? extends Number> range = category.getRange();

				if(!range.getMinValue().equals(range.getMaxValue())){ //"No data" category must only contain one value
					throw new Exception("Category \"No data\" contains more than 1 value");
				}else{
					noDataValue = (Double)range.getMinValue(); //might as well use geMaxValue() as min must be equal to max
				}
				break;
			}
		}
		return noDataValue;
	}
	
	
	/**
	 * Gets the maximum value out of all not-NULL values in the specified band of the input GridCoverage2D object (NULL values will be ignored).
	 * If the operation somehow does not work correctly, the return value will be Double.NEGATIVE_INFINITY
	 * @param coverage
	 * @param band index of the band of the raster file that is to be used (should always be 0 in the case of weight rasters)
	 * @return maximum value
	 * @throws Exception 
	 */
	public static double getCoverageMaxValue(GridCoverage2D coverage, int band) throws Exception{
		// get renderedImage
		RenderedImage renderedImage = coverage.getRenderedImage();
		// get image params
		int numColumns = renderedImage.getWidth();
		int numRows = renderedImage.getHeight();
		int minX = renderedImage.getMinX();
		int minY = renderedImage.getMinY();
		double noDataValue = getNoDataValue(coverage, band);// raster noDataValue might differ from 0 and must therefore be specifically queried and ignored
		
		double maxValue = Double.NEGATIVE_INFINITY;
		double[] values = new double[1];
		double currentValue = 0; 
		
		// iterate over renderedImage using image params
		for(int r = minY; r < minY + numRows; r++){ // loop over rows
			for(int c = minX; c < minX + numColumns; c++){ // loo over columns
				coverage.evaluate(new GridCoordinates2D(c, r), values);
				currentValue = values[0];
				if(currentValue != noDataValue){ // ignore NULL values (might be different according to input file)
					if(currentValue > maxValue) maxValue = currentValue;
				}

			}
		}
		return maxValue;
	}	
	
	
	/**
	 * Calculates the sum of all not-NULL values in the specified band of the input GridCoverage2D object(NULL values will be ignored).
	 * @param coverage
	 * @param band index of the band of the raster file that is to be used (should always be 0 in the case of weight rasters)
	 * @return sum
	 * @throws Exception 
	 */
	public static double getCoverageSum(GridCoverage2D coverage, int band) throws Exception{
		// get renderedImage
		RenderedImage renderedImage = coverage.getRenderedImage();
		// get image params
		int numColumns = renderedImage.getWidth();
		int numRows = renderedImage.getHeight();
		int minX = renderedImage.getMinX();
		int minY = renderedImage.getMinY();
		double noDataValue = getNoDataValue(coverage, band); // raster noDataValue might differ from 0 and must therefore be specifically queried and ignored

		double sum = 0;
		double[] values = new double[1];
		double currentValue = 0; 
		
		// iterate over renderedImage using image params
		for(int r = minY; r < minY + numRows; r++){ // loop over rows
			for(int c = minX; c < minX + numColumns; c++){ // loo over columns
				coverage.evaluate(new GridCoordinates2D(c, r), values);
				currentValue = values[0];
				if(currentValue != noDataValue){ // ignore NULL values (might be different according to input file)
					sum = sum + currentValue;
				}
			}
		}
		return sum;
	}

	
	/**
	 * Applies a rejection testing operation to a normalizedPlotWeight value in order to 
	 * decide on whether to keep a sampled plot in the sample.
	 * The normalizedPlotWeight Parameter is compared to a uniformly distributed random number.
	 * If normalizedPlotWeight < random number, return value will be false, otherwise true.
	 * For the uniformly distributed random number, Math.random() is used; the method Javadoc says that 
	 * "values are chosen pseudorandomly with (approximately) uniform distribution"
	 * @param normalizedPlotWeight: must be in range 0-1
	 * @return
	 */
	public static boolean rejectionTesting(double normalizedPlotWeight){
		boolean keepPlot = true;
		// use uniformly distributed numbers
		//Math.random() documentation: values are chosen pseudorandomly with (approximately) uniform distribution 
		double randomNumber = Math.random();
		if (normalizedPlotWeight < randomNumber){
			keepPlot = false;
		}
		return keepPlot;
	}

}
