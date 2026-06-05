package curvemorph;

import java.util.ArrayList;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.VirtualStackAdapter;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import ij.IJ;
import ij.ImagePlus;

public class CMKymoBuilder < T extends RealType< T > & NativeType< T > >
{
	final int nKymoMaxLength;
	final int nKymoMaxWidth;
	
	final RandomAccessibleInterval<T> RAI;
	
	final ArrayList<ReMap> coordsSample;
	
	final public ArrayList<Integer> width;
	final public ArrayList<Integer> length;
	boolean bROIsAlongT;
	final CMDialog cd;	
	T type;
	final String sTitle;
	
	@SuppressWarnings( "unchecked" )
	public CMKymoBuilder (final CurveMorph cm, final CurveAssembly curveAssembly)
	{
		this.cd = cm.cmDialog;
		sTitle = cm.imp.getTitle();
		nKymoMaxLength = (int) Math.ceil( curveAssembly.dMaxLength );
		nKymoMaxWidth = curveAssembly.nMaxWidth;
		bROIsAlongT = cm.bROIsAlongT;
		if(!cm.bHasHyperstackPos)
		{
			if(cm.imp.getNFrames()>1)
			{
				bROIsAlongT = true;
			}
		}
		//wrap img to RAI
		if(bROIsAlongT)
		{
			RAI = ( RandomAccessibleInterval< T > ) Views.permute( wrapImagePlusToRAIXYCTZ(cm.imp), 3, 4);			
		}
		else
		{
			RAI = ( RandomAccessibleInterval< T > ) wrapImagePlusToRAIXYCTZ(cm.imp);
		}
		type = RAI.getType();
		length = new ArrayList<>();
		width = curveAssembly.resampledWidths;
		coordsSample = prepareSamplingCoordinates(curveAssembly.resampledRefs);

	}
	
	public ImagePlus getKymograph()
	{
		//make an output
		ImgFactory<T> factory = new ArrayImgFactory<>(type);
		long [] dimsIn = RAI.dimensionsAsLongArray();
		long [] dimsStack = new long [5];
		for(int d = 2; d < 5; d++)
		{
			dimsStack[d] = dimsIn[d];
		}
		dimsStack[0] = nKymoMaxLength;
		dimsStack[1] = nKymoMaxWidth;

		//make interpolation factory
		final InterpolatorFactory<T, RandomAccessible< T >> interpFactory = new ClampingNLinearInterpolatorFactory<>();
	
		final Img< T > currStraightAll = factory.create(dimsStack);
		
		IJ.showStatus( "CurveMorph: building kymograph..." );
		IJ.showProgress( 0, 0 );
		//kymo dimension (always last)
		for(long dK = 0; dK < dimsIn[4]; dK++ )
		{
			final IntervalView< T > rai1 = Views.hyperSlice( RAI, 4, dK );
			final int nLength = length.get( (int) dK );
			final int nWidth = width.get( (int)dK );
			final ReMap map = coordsSample.get( (int)dK );
			IJ.showProgress((int) dK, (int)(dimsIn[4] - 1) );
			
			final IntervalView< T > currStraight1 = Views.hyperSlice( currStraightAll, 4, dK );
			final int [] newXY = new int[2];
			for(long dZT = 0; dZT < dimsIn[3]; dZT++ )
			{
				final IntervalView< T > rai2 = Views.hyperSlice( rai1, 3, dZT );
				final IntervalView< T > currStraight2 = Views.hyperSlice( currStraight1, 3, dZT );
				for(long dC = 0; dC < dimsIn[2]; dC++ )
				{
					final IntervalView< T > raiXY = Views.hyperSlice( rai2, 2, dC );
					final IntervalView< T > currStraight = Views.hyperSlice( currStraight2, 2, dC );
					
					//IntervalView< T > raiOutXY = Views.hyperSlice( raiOut1, 3, dC );
					RealRandomAccessible< T > interpolate = Views.interpolate( Views.extendZero( raiXY ), interpFactory);
					RealRandomAccess<T> ra = interpolate.realRandomAccess();
					RandomAccess< T > raStr = currStraight.randomAccess();
					int nShiftX = 0;
					int nShiftY = 0;
					if(cd.nKymoAlign == 0)
					{
						nShiftX = (int) Math.floor( 0.5*(nKymoMaxLength - nLength));
						nShiftY = (int) Math.floor( 0.5*(nKymoMaxWidth - nWidth));
					}
					if(cd.nKymoAlign == 2)
					{
						nShiftX = nKymoMaxLength - nLength;
						nShiftY = nKymoMaxWidth - nWidth;
					}
					for(int x = 0; x < nLength; x++)
					{
						for(int y = 0; y < nWidth; y++)
						{
							final int ind = x + y * nLength;
							newXY[0] = x + nShiftX;
							newXY[1] = y + nShiftY;
							raStr.setPosition( newXY );
							ra.setPosition( map.oldCoords[ind] );
							raStr.get().set( ra.get() );
						}
					}
				
				}
			}
		}
		IJ.showStatus( "CurveMorph: building kymograph done." );
		if(cd.bShowKymoStack)
		{
			ImagePlus impKymoStack = ImageJFunctions.show( currStraightAll );
			impKymoStack.setTitle( "KymoStack " + sTitle );
		}
		return null;
	}
	
	ArrayList<ReMap> prepareSamplingCoordinates(final ArrayList<float[][]> input)
	{
		final ArrayList<ReMap> coords = new ArrayList<>();
		for(int nRoi = 0; nRoi < input.size(); nRoi++)
		{
			final float[][] currFloat = input.get( nRoi );
			final int N = currFloat[0].length;
			//smooth with 3 points average keeping first last points
			double [][] smooth = smoothThreePoints(currFloat);
			
			double [] cumLength = CurveLerp.calculateCumLength( smooth );
			final int nLength = (int)Math.floor( cumLength[ N - 1 ]);
			length.add( nLength );
			final int nWidth =  width.get( nRoi );
			final ReMap map = new ReMap(nLength, nWidth);
			double [] resampleLength = new double[nLength];
			for(int i = 0; i < nLength; i++)
			{
				resampleLength[i] = i;
			}
			
			//perform cubic spline interpolation with a step of 1 px			
			double [][] resampledSmooth = new double[2][];			
			double [][] resampledSlope = new double[2][];

			for(int d = 0; d < 2; d ++)
			{
				final CubicSpline spline = new CubicSpline(cumLength,smooth[d]);
				resampledSmooth[d] = spline.evalSpline( resampleLength );
				resampledSlope[d] = spline.evalSlope( resampleLength );
			}
			for(int x = 0; x < nLength; x++)
			{
				//get vector of the normale
				final double [] normale = new double[2];
				normale[0] = (-1.0)*resampledSlope[1][x]; // -y component of tangent
				normale[1] = resampledSlope[0][x]; // // x component of tangent
				final double[] center = new double[2];
				center[0] = resampledSmooth[0][x];
				center[1] = resampledSmooth[1][x];

				double nShift = (nWidth - 1) * 0.5;
				for(int y = 0; y < nWidth; y++)
				{
					final int ind = x + y * nLength;
					for(int d = 0; d < 2; d++ )
					{
						map.oldCoords[ind][d] = center[d] + normale[d]*(y - nShift);
					}
				}
			}
			coords.add( map );			
		}
		return coords;		
	}
	
	public double[][] smoothThreePoints (final float[][] in)
	{
		final int N = in[0].length;
		double [][] smooth = new double[2][N];
		//keep first/last point the same
		for(int d = 0; d < 2; d++)
		{
			smooth[d][0] = in[d][0] + 0.5;
			smooth[d][N-1] = in[d][N-1] + 0.5;

		}
		//smooth middle
		for(int d = 0; d < 2; d++)
		{
			for(int l = 1; l < N - 1; l++)
			{
				smooth[d][l] = ((in[d][l-1] + in[d][l] + in[d][l+1])/3.0) + 0.5;//0.5 for the center
			}
		}
		return smooth;
	}
	
	public static RandomAccessibleInterval<?> wrapImagePlusToRAIXYCTZ(final ImagePlus imp)
	{
		
		final Img< ? > raiIn = VirtualStackAdapter.wrap( imp );
		RandomAccessibleInterval<?> outRAI = raiIn;
		final int nDims = raiIn.numDimensions();
		
		for(int i = 0; i < 5 - nDims; i ++)
		{
			outRAI = Views.addDimension(outRAI, 0, 0);			
		}
		String sDims = getImageJAxesOrder(imp);
		if(sDims.indexOf( 'C' ) != 4)
		{
			outRAI = Views.permute( outRAI, sDims.indexOf( 'C' ) , 4);
			StringBuilder sb = new StringBuilder(sDims);

			sb.setCharAt(sDims.indexOf( 'C' ), sDims.charAt( 4 ));
			sb.setCharAt(4, 'C');
			sDims = sb.toString();
		}
		
		if(sDims.charAt(3) == 'Z')
		{
			outRAI = Views.permute( outRAI, 3 , 2);			
		}
		
		return Views.permute( outRAI, 2, 4 );
		
		//return outRAI;
	}
	
	public static String getImageJAxesOrder( final ImagePlus ip )
	{
		String sDims = "XY";
		boolean bC = false;
		boolean bT = false;
		boolean bZ = false;
		if ( ip.getNChannels() > 1 )
		{
			sDims = sDims + "C";
			bC = true;
		}

		if ( ip.getNSlices() > 1 )
		{
			sDims = sDims + "Z";
			bZ = true;
		}

		if ( ip.getNFrames() > 1 )
		{
			sDims = sDims + "T";
			bT = true;
		}
		if(!bC)
		{
			sDims = sDims + "C";
		}
		if(!bZ)
		{
			sDims = sDims + "Z";
		}
		if(!bT)
		{
			sDims = sDims + "T";
		}

		return sDims;
	}
//	public static void main(String[] args) throws Exception 
//	{
//		//verify shift
//		int nWidth = 10;
//		double nShift = (nWidth - 1)*0.5;
//		for(int y = 0; y < nWidth; y++)
//		{
//			System.out.println(y - nShift);
//		}
//	}
	
}
