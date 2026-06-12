package curvemorph;

import java.util.ArrayList;

import ij.IJ;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

public class CurveAssembly
{
	final CurveMorph_ cm;
	
	final CMDialog dial;
	
	public static final int EndsMap_Closest = 0, EndsMap_First = 1, EndsMap_End = 2; 
	
	public int nFirstP; 
	
	public int nLastP;
	
	/** ordered list of frame numbers **/
	final ArrayList<Integer> frameRefs = new ArrayList<>();

	/** ordered list of ROIS **/
	final ArrayList<Roi> curveRefs = new ArrayList<>();

	/** ordered list of interpolated ROIs **/
	final public ArrayList<float [][]> resampledRefs = new ArrayList<>();
	
	/** ordered list of interpolated ROIs' widths **/
	final public ArrayList<Integer> resampledWidths = new ArrayList<>();
	
	public double dMaxLength = -1;
	
	public int nMaxWidth = -1;
	
	int nFinalSegmentsN = 0;
	
	final Overlay image_overlay;
	
	public CurveAssembly(final CurveMorph_ cm, final CMDialog dial)
	{
		this.cm = cm;
		this.dial = dial;
		image_overlay = new Overlay();
		//makes an ordered list of frames (frameRefs) and ROIs (curveRefs) 
		init();
	}
	
	public void runAssembly()
	{
		//display progress
		IJ.showStatus( "CurveMorph: morphing ROIs..." );
		final int nTotFrames = frameRefs.get( frameRefs.size()-1 ) - frameRefs.get( 0 ) + 1;
		int nCount = 0;
		IJ.showProgress( nCount, nTotFrames);
		
		for(int nRoi = 0; nRoi < curveRefs.size() - 1; nRoi++)
		{
			
			int nIniSegmFrame = frameRefs.get( nRoi );
			int nLastSegmFrame = frameRefs.get( nRoi + 1 );
			
			if(nRoi == curveRefs.size() - 2)
			{				
				nLastSegmFrame = frameRefs.get( nRoi + 1 );
			}
			
			double dSegmRange = nLastSegmFrame - nIniSegmFrame;
			double t;
			double fStrokeIni =  curveRefs.get( nRoi ).getStrokeWidth();
			double fStrokeLast =  curveRefs.get( nRoi + 1 ).getStrokeWidth();
			boolean bUseCentroid = false;
			if(dial.nAlgorithm == 0)
				bUseCentroid = dial.bUseCentriod;
			final CurveLerp startI = CurveLerp.fromROI(curveRefs.get( nRoi ));
			final CurveLerp endI = CurveLerp.fromROI(curveRefs.get( nRoi + 1 ));
			
			final int [] nOrient = getOrientation(startI, endI);

			PolylineState start = new PolylineState(new CurveLerp( startI.resampleDouble( nFinalSegmentsN, nOrient[0] )));
			PolylineState end   = new PolylineState(new CurveLerp(   endI.resampleDouble( nFinalSegmentsN, nOrient[1] )));
			for(int nFrame = nIniSegmFrame; nFrame < nLastSegmFrame; nFrame ++)
			{
				t = (nFrame - nIniSegmFrame) / dSegmRange;

				final float [][] xyMorphResampled = getIntermediate( start, end, t, bUseCentroid );								
				final PolygonRoi resROI = new PolygonRoi(xyMorphResampled[0], xyMorphResampled[1], Roi.POLYLINE);
				final double dROIWidth = CurveMorpher.lerp( fStrokeIni, fStrokeLast, t ); 
				addRoi(resROI, nFrame, dROIWidth);
				nCount++;
				if(dial.bMakeKymograph)
				{
					//current ROI
					resampledRefs.add( xyMorphResampled );
					int nKymoWidth = Math.max( 1, (int)Math.round( dROIWidth ) );
					nMaxWidth = Math.max( nMaxWidth, nKymoWidth );
					resampledWidths.add( nKymoWidth );
				}
				//the last point 
				if((nRoi == (curveRefs.size() - 2)) && (nFrame == nLastSegmFrame - 1))
				{
					t = 1.0;
					nCount++;
					final float [][] xyMorphResampledLast = getIntermediate( start, end, t, bUseCentroid );								
					final PolygonRoi resROILast = new PolygonRoi(xyMorphResampledLast[0], xyMorphResampledLast[1], Roi.POLYLINE);
					addRoi(resROILast, nLastSegmFrame,  fStrokeLast);
					if(dial.bMakeKymograph)
					{
						//current ROI
						resampledRefs.add(xyMorphResampledLast);
						int nKymoWidth = Math.max( 1, (int)Math.round( fStrokeLast ) );
						nMaxWidth = Math.max( nMaxWidth, nKymoWidth );
						resampledWidths.add( nKymoWidth );

					}
				}
				IJ.showProgress( nCount, nTotFrames);
			}
		}		
		IJ.showStatus( "CurveMorph: morphing ROIs done." );
		IJ.showProgress( 2, 2);
		if(dial.bAddToOverlay)
		{
			cm.imp.setOverlay( image_overlay );
			cm.imp.updateAndRepaintWindow();
			cm.imp.show();
		}
	}
	
	float [][] getIntermediate(final PolylineState start, final PolylineState end, double t, boolean bUseCentroid)
	{
		final double[][] xyMorph;
		if(dial.nAlgorithm == 0)
		{
			xyMorph = CurveMorpher.getMorphState(start, end, t, bUseCentroid);
		}
		else
		{
			xyMorph = CurveMorpher.getInterpolatedState( start, end, t );
		}
		final CurveLerp morphLerp = new CurveLerp(xyMorph);
		final int nSegm = (int) Math.ceil(morphLerp.dOrigLength);
		return morphLerp.resampleFloat( nSegm );
	}
	
	
	void addRoi(final Roi roi, final int nFrame, final double dStrokeWidth)
	{
		roi.setStrokeWidth( dStrokeWidth );
		roi.setStrokeColor( cm.strokeColor );
		if(cm.bHasHyperstackPos)
		{
			if(cm.bROIsAlongZ)
			{
				roi.setPosition( 1, nFrame, 1 );
			}
			else
			{
				roi.setPosition( 1, 1, nFrame );
			}
		}
		else
		{
			roi.setPosition( nFrame );
		}
		
		if(dial.bAddToOverlay)
		{
			image_overlay.add( roi );
		}
		
		if(dial.bAddToManager)
		{
			roi.setName( "interp" + Integer.toString( nFrame ) );
			cm.rm.add( roi, 0 );
		}
	}
	
	/** function returns closest ends of two curves 
	 * 00 - both first ends
	 * 01 - first start, second end,
	 * 10 - first end, second start,
	 * 11 - first end, second end **/
	int [] getOrientation(final CurveLerp start, final CurveLerp end)
	{
		int [] nOrientation = new int [2];
		
		if(dial.nEndsMap == EndsMap_First)
		{
			return nOrientation;
		}
		
		if(dial.nEndsMap == EndsMap_End)
		{
			nOrientation[0]  =  1;
			nOrientation[1]  =  1;
			return nOrientation;
		}
		double dDistance = Double.MAX_VALUE;
		double dCurrDistance;
		int nStartLast = start.origXY[0].length - 1;
		int nEndLast   =   end.origXY[0].length - 1;

		//begin-begin
		dCurrDistance = CurveLerp.distance( start.origXY[0][0], end.origXY[0][0], 
											start.origXY[1][0], end.origXY[1][0]);
		if(dCurrDistance < dDistance)
		{
			dDistance = dCurrDistance;
			nOrientation[0] = 0;
			nOrientation[0] = 0;

		}
		
		//begin - end
		dCurrDistance = CurveLerp.distance( start.origXY[0][0], end.origXY[0][nEndLast], 
											start.origXY[1][0], end.origXY[1][nEndLast]);	
		if(dCurrDistance < dDistance)
		{
			dDistance = dCurrDistance;
			nOrientation[0] = 0;
			nOrientation[1] = 1;
		}
		
		//end - begin		
		dCurrDistance = CurveLerp.distance( start.origXY[0][nStartLast], end.origXY[0][0], 
											start.origXY[1][nStartLast], end.origXY[1][0]);
		if(dCurrDistance < dDistance)
		{
			dDistance = dCurrDistance;
			nOrientation[0] = 1;
			nOrientation[1] = 0;
		}
		
		//end - begin
		dCurrDistance = CurveLerp.distance( start.origXY[0][nStartLast], end.origXY[0][nEndLast], 
											start.origXY[1][nStartLast], end.origXY[1][nEndLast]);
		if(dCurrDistance < dDistance)
		{
			dDistance = dCurrDistance;
			nOrientation[0] = 1;
			nOrientation[1] = 1;
		}
		
		return nOrientation;
	}
	
	
	/** make an ordered list of frames and ROIs **/
	public void init()
	{

		boolean bAddLastROI = false;
		
		int nLastROIFrame = cm.refFrames[cm.refFrames.length - 1];
		//let's assembly final timepoints
		//range from ROIS
		if(dial.nRange == 1)
		{
			nFirstP = cm.refFrames[0];
			nLastP = nLastROIFrame;
		}
		//range from image
		else
		{
			nFirstP = 1;

			if(cm.bHasHyperstackPos)
			{
				if(cm.bROIsAlongZ)
				{
					nLastP = cm.dims[3];
				}
				else
				{
					nLastP = cm.dims[4];
				}
			}
			else
			{
				nLastP = cm.nStackSize;
			}
			if(nLastP != nLastROIFrame)
				bAddLastROI = true;
			if(nFirstP != cm.refFrames[0])
			{
				frameRefs.add( 1 );
				curveRefs.add( cm.curveROIs.get( 0 ) );
			}
		}
		for(int i = 0; i < cm.curveROIs.size(); i++)
		{
			curveRefs.add( cm.curveROIs.get( i ) );
			frameRefs.add( cm.refFrames[i] );
		}
		if(bAddLastROI)
		{
			frameRefs.add( nLastP );
			curveRefs.add( cm.curveROIs.get(cm.curveROIs.size() - 1) );
		}
		ArrayList<CurveLerp> iniCurves = new ArrayList<>();
			
		for(int i = 0; i < curveRefs.size(); i++)
		{
			final Roi roi = curveRefs.get( i );
			final FloatPolygon poly = roi.getFloatPolygon();
			
			double [][] xy = new double[2][poly.npoints];
			for(int k = 0; k < poly.npoints; k++)
			{
				xy[0][k] = poly.xpoints[k];
				xy[1][k] = poly.ypoints[k];
			}
			final CurveLerp cLerp = new CurveLerp(xy);
			iniCurves.add( cLerp );
			dMaxLength  = Math.max( dMaxLength, cLerp.getLength() );
		}
		
		nFinalSegmentsN = (int)Math.ceil( dMaxLength );

	}
	

}
