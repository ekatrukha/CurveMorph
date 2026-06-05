package curvemorph;

import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

public class CurveLerp
{
	double [][] origXY;
	double [] origCumLength;
	
	int nOrigPoints = 0;
	
	double dOrigLength = 0;
	
	public CurveLerp(final double [][] initialXY)
	{
		nOrigPoints = initialXY[0].length;
		origXY = new double [2][nOrigPoints];
		
		for (int d = 0; d < 2; d++)
		{
			for(int i = 0; i < nOrigPoints; i++)
			{
				origXY[d][i] = initialXY[d][i];
			}
		}
		setupOrigLength();
	}
	
	public static CurveLerp fromROI(final Roi roi)
	{
		if(roi.getType() == Roi.LINE)
		{
			final double [][] xy = new double[2][2];
			xy[0][0] = ((Line)roi).x1d;
			xy[1][0] = ((Line)roi).y1d;
			xy[0][1] = ((Line)roi).x2d;
			xy[1][1] = ((Line)roi).y2d;
			return new CurveLerp(xy);
		}
		//polyline or freeline
		final FloatPolygon poly = ((PolygonRoi)roi).getFloatPolygon();
		final double [][] xy = new double[2][poly.npoints];

		for(int i = 0; i < poly.npoints; i++)
		{
			xy[0][i] = poly.xpoints[i];
			xy[1][i] = poly.ypoints[i];
		}
		return new CurveLerp(xy);			
	}
	public int getPointsN()
	{
		return nOrigPoints;
	}
	
	public double [][] getXY()
	{
		return origXY;
	}
	
	/** cumulative length **/
	public double [] getCumLength()
	{
		return origCumLength;
	}
	
	public double getLength()
	{
		return dOrigLength;
	}
	
	void setupOrigLength()
	{		
		origCumLength = calculateCumLength(origXY);
		dOrigLength = origCumLength[ nOrigPoints - 1 ];
		return;
	}
	
	public static double [] calculateCumLength(final double[][] in)
	{
		double dLength = 0.0;
		final int nPoints = in[0].length;
		final double[] cumLength = new double [nPoints];
		for(int i = 0; i < nPoints - 1; i++)
		{
			dLength += distance( in[0][i+1], in[0][i], in[1][i+1], in[1][i]);
			cumLength[ i + 1] = dLength;
		}
		return cumLength;
	}
	
	public static double calculateLength(final double[][] in)
	{
		final int nPoints = in[0].length;
		
		return calculateCumLength(in)[nPoints];
	}
	
	public double[][] resampleDouble(int nSegments)
	{
		double nSegmLenght = dOrigLength/nSegments;
		double [] dResampleLength = new double [nSegments + 1];
		double [][] resampledXY = new double[2][nSegments + 1];
		for (int i = 1; i < nSegments + 1; i++)
		{
			dResampleLength[i] = nSegmLenght * i;
		}
		for(int d = 0; d < 2; d++)
		{
			resampledXY[d] = LinearInterpolation.evalLinearInterp( origCumLength, origXY[d], dResampleLength );
		}
		return resampledXY;
	}
	
	public double[][] resampleDouble (int nSegments, int nOrientation)
	{
		double [][] resampledXY = resampleDouble(nSegments);
		if(nOrientation == 0)
		{
			return resampledXY;
		}

		int nLength = resampledXY[0].length;
		double [][] reverseXY = new double[2][nLength ];
		for(int i = 0; i < nLength; i++)
		{
			for(int d = 0; d < 2; d++)
			{
				reverseXY[d][i] = resampledXY[d][nLength - i -1];
			}

		}
		return reverseXY;

	}
	
	public float[][] resampleFloat(int nSegments)
	{

		double [][] resampledXY = resampleDouble (nSegments);
		
		float [][] fResampledXY = new float[2][nSegments + 1];

		for (int i = 0; i < nSegments + 1; i++)
		{
			for(int d = 0; d < 2; d++)
			{
				fResampledXY[d][i] = (float)resampledXY[d][i];
			}
		}
		return fResampledXY;
	}
	
	public static double distance (double x1, double x2, double y1, double y2)
	{
		return Math.sqrt( Math.pow( x1 - x2, 2 ) + Math.pow( y1 - y2, 2 ) );
	}

}
