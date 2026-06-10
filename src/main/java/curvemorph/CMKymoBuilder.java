package curvemorph;

import java.util.ArrayList;
import java.util.Arrays;

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
	final int nFirstP; 
	final int nLastP;
	final int nSpan;
	boolean bROIsAlongT;
	final CMDialog cd;	
	T type;
	final String sTitle;
	final ImagePlus imp;
	
	@SuppressWarnings( "unchecked" )
	public CMKymoBuilder (final CurveMorph_ cm, final CurveAssembly curveAssembly)
	{
		this.cd = cm.cmDialog;
		imp = cm.imp;
		sTitle = cm.imp.getTitle();
		nKymoMaxLength = (int) Math.ceil( curveAssembly.dMaxLength );
		nKymoMaxWidth = curveAssembly.nMaxWidth;
		nFirstP = curveAssembly.nFirstP;
		nLastP = curveAssembly.nLastP;
		nSpan = nLastP - nFirstP + 1;
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
	
	@SuppressWarnings( "null" )
	public ImagePlus getKymograph()
	{
		//make an output
		ImgFactory<T> factory = new ArrayImgFactory<>(type);
		long [] dimsIn = RAI.dimensionsAsLongArray();
		long [] dimsStack = new long [5];
		long [] dimsKymo = new long [5];
		//kymo dimensions
		dimsKymo[0] = nKymoMaxLength;
		dimsKymo[1] = nSpan;
		//dimsKymo[1] = dimsIn[4];
		dimsKymo[4] = 1;
		for(int d = 2; d < 4; d++)
		{
			dimsKymo[d] = dimsIn[d];
		}
		
		//kymo stack dimensions
		dimsStack[0] = nKymoMaxLength;
		dimsStack[1] = nKymoMaxWidth;
		for(int d = 2; d < 4; d++)
		{
			dimsStack[d] = dimsIn[d];
		}
		dimsStack[4] = nSpan;
		//make interpolation factory
		final InterpolatorFactory<T, RandomAccessible< T >> interpFactory = new ClampingNLinearInterpolatorFactory<>();

		//kymo output 
		final Img< T > kymoImg = factory.create(dimsKymo);
		//kymo stack output 
		
		final Img< T > currStraightAll ;
		if(cd.bShowKymoStack)
			currStraightAll = factory.create(dimsStack);
		else
			currStraightAll = factory.create( 1 );
		
		IJ.showStatus( "CurveMorph: building kymograph..." );
		IJ.showProgress( 0, 0 );
		//kymo dimension (always last)
		int nPos = 0;
		for(long dK = nFirstP - 1; dK < nLastP; dK++ )
		{		
			final int nLength = length.get( nPos );
			final int nWidth = width.get( nPos );
			final ReMap map = coordsSample.get( nPos );
			IJ.showProgress(nPos, nSpan );
			
			final int [] newXY = new int[2];
			final int [] kymoXY = new int[2];
			kymoXY[1] = nPos;

			for(long dZT = 0; dZT < dimsIn[3]; dZT++ )
			{
				for(long dC = 0; dC < dimsIn[2]; dC++ )
				{
					final RandomAccessibleInterval< T > raiXY = getMultiHyperslice( RAI, (int)dK, (int)dZT, (int)dC );
					
					RealRandomAccessible< T > interpolate = Views.interpolate( Views.extendZero( raiXY ), interpFactory);
					RealRandomAccess<T> ra = interpolate.realRandomAccess();

					final RandomAccessibleInterval< T > kymoRai = getMultiHyperslice( kymoImg, 0, (int)dZT, (int)dC );
					RandomAccess< T > raKymo = kymoRai.randomAccess();
					int nShiftX = 0;
					int nShiftY = 0;
					
					if(cd.nKymoAlign == 0)
					{
						nShiftX = (int) Math.floor( 0.5 * (nKymoMaxLength - nLength));
						nShiftY = (int) Math.floor( 0.5 * (nKymoMaxWidth - nWidth));
					}
					if(cd.nKymoAlign == 2)
					{
						nShiftX = nKymoMaxLength - nLength;
						nShiftY = nKymoMaxWidth - nWidth;
					}
					
					RandomAccessibleInterval< T > currStraight = null;
					RandomAccess< T > raStr = null;

					if(cd.bShowKymoStack)
					{
						currStraight = getMultiHyperslice( currStraightAll, nPos, (int)dZT, (int)dC );
						raStr = currStraight.randomAccess();						
					}
					//taking the array of straightened
					float [][] strArray = new float [nLength][nWidth];
					for(int x = 0; x < nLength; x++)
					{
						for(int y = 0; y < nWidth; y++)
						{
							final int ind = x + y * nLength;
							ra.setPosition( map.mapCoords[ind] );
							strArray[x][y] = ra.get().getPowerFloat();
							if(cd.bShowKymoStack)
							{
								newXY[0] = x + nShiftX;
								newXY[1] = y + nShiftY;
								raStr.setPosition( newXY );
								raStr.get().set( ra.get() );
							}
						}
					}
					//let's calculate the line
					float [] line = processWideLine(strArray);
					for(int x = 0; x < nLength; x++)
					{
						kymoXY[0] = x + nShiftX;	
						raKymo.setPosition( kymoXY );
						raKymo.get().setReal( line[x] );
					}
				}
			}
			nPos++;
		}
		IJ.showStatus( "CurveMorph: building kymograph done." );
		if(cd.bShowKymoStack)
		{
			ImagePlus impKymoStack;
			String sStackTitle = "KymoStack " + sTitle;
			if(bROIsAlongT)
				impKymoStack = ImageJFunctions.wrap( currStraightAll, sStackTitle );	
			else
				impKymoStack = ImageJFunctions.wrap( Views.permute(currStraightAll, 3, 4), sStackTitle ); 			
			impKymoStack.setTitle( "KymoStack " + sTitle );
			impKymoStack.setCalibration( imp.getCalibration());
			if(imp.isComposite())
				impKymoStack = TransferLUTs.transfer( imp, impKymoStack );
			impKymoStack.show();

		}
		ImagePlus impKymo;
		final String sKymoTitle = cd.getPefix() + sTitle;
		if(bROIsAlongT)
		{
			impKymo = ImageJFunctions.wrap( kymoImg, sKymoTitle );
		}
		else
		{
			impKymo = ImageJFunctions.wrap( Views.permute( kymoImg, 3, 4 ), sKymoTitle);
		}
		
		if(imp.isComposite())
		{
			impKymo = TransferLUTs.transfer( imp, impKymo );
		}
		impKymo.show();
		
		return impKymo;
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
						map.mapCoords[ind][d] = center[d] + normale[d]*(y - nShift);
					}
				}
			}
			coords.add( map );			
		}
		return coords;		
	}
	
	float [] processWideLine(final float [][] lineprof2D)
	{
		final int nLength = lineprof2D.length;
		final int nWidth = lineprof2D[0].length;
		final float[] values_out = new float[nLength];
		float fMin;
		float fMax;
		float fMedian = 0.0f;
		float fMean;
		float fCount;
		float curVal;
		for(int k = 0; k < nLength; k++)
		{
			fCount = 0;
			fMin = Float.MAX_VALUE;
			fMax = (-1) * Float.MAX_VALUE;
			fMean = 0;
			for(int j = 0; j < nWidth; j++)
			{
				curVal = lineprof2D[ k ][ j ];			 
				if( Float.isNaN( curVal ))
				{
					continue;
				}
				fMean += curVal;
				fCount ++;
				fMax = Math.max (curVal, fMax);
				fMin = Math.min (curVal, fMin);
			}
			fMean /= fCount;
			if(cd.nSubtractType == CMDialog.SUBTRACT_MEDIAN)
			{
				final float [] singleLine = new float[nWidth];
				for(int j = 0; j < nWidth; j++)
				{
					singleLine[j] = lineprof2D[k][j];
				}
				Arrays.sort( singleLine );
				if (nWidth % 2 == 0)
					fMedian = (singleLine[nWidth/2] + singleLine[nWidth/2 - 1])*0.5f;
				else
					fMedian = singleLine[nWidth/2];
			}

			//VALUE
			if(cd.nKymoType == CMDialog.VALUE_AVG)
				curVal = fMean;
			else
				curVal = fMax;

			//SUBTRACT
			switch (cd.nSubtractType)
			{
			case CMDialog.SUBTRACT_MIN:
				curVal -= fMin;
				break;
			case CMDialog.SUBTRACT_AVG:
				curVal -= fMean;
				break;
			case CMDialog.SUBTRACT_MEDIAN:
				curVal -= fMedian;
				break;
			default:
				break;
			}
			//finally
			values_out[ k ] = curVal;

		}

		return values_out;
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
	
	RandomAccessibleInterval< T > getMultiHyperslice(RandomAccessibleInterval<T> rai, int... slicePos)
	{
		if(slicePos.length == 0 )
			return rai;
		final int dims = rai.numDimensions();
		RandomAccessibleInterval< T >  out = rai;
		for(int i = 0; i < slicePos.length; i++)
		{
			out = Views.hyperSlice( out, dims - 1 - i, slicePos[i] );
		}
		return out;
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
