package curvemorph;

import java.util.ArrayList;

import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

public class CurveAssembly
{
	final CurveMorph cm;
	
	final CMDialog dial;
	
	int nFirstP; 
	
	int nLastP;
	
	ArrayList<Integer> frameRefs;
	
	ArrayList<Roi> curveRefs;
	
	double dMaxLength = -1;
	
	int nFinalSegmentsN = 0;
	
	final Overlay image_overlay;
	
	public CurveAssembly(final CurveMorph cm, final CMDialog dial)
	{
		this.cm = cm;
		this.dial = dial;
		image_overlay = new Overlay();
		init();
	}
	
	public void runAssembly()
	{
		
		for(int nRoi = 0; nRoi < curveRefs.size() - 1; nRoi++)
		{
			//add first ROI in the segment
			addRoi( curveRefs.get( nRoi ) , frameRefs.get( nRoi ), curveRefs.get( nRoi ).getStrokeWidth()  );
			
			int nIniSegmFrame = frameRefs.get( nRoi );
			int nLastSegmFrame = frameRefs.get( nRoi + 1 );
			double dSegmRange = nLastSegmFrame - nIniSegmFrame;
			double t;
			double fStrokeIni =  curveRefs.get( nRoi ).getStrokeWidth();
			double fStrokeLast =  curveRefs.get( nRoi + 1 ).getStrokeWidth();
			boolean bUseCentroid = false;
			if(dial.nMorphType == 0)
				bUseCentroid = true;
			final CurveLerp startI = CurveLerp.fromROI(curveRefs.get( nRoi ));
			final CurveLerp endI = CurveLerp.fromROI(curveRefs.get( nRoi + 1 ));
			final int [] nOrient = getOrientation(startI, endI);
			PolylineState start = new PolylineState(new CurveLerp( startI.resampleDouble( nFinalSegmentsN, nOrient[0] )));
			PolylineState end   = new PolylineState(new CurveLerp(   endI.resampleDouble( nFinalSegmentsN, nOrient[1] )));
			for(int nFrame = nIniSegmFrame + 1; nFrame < nLastSegmFrame; nFrame ++)
			{
				t = (nFrame - nIniSegmFrame) / dSegmRange;
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
				final float [][] xyMorphResampled = morphLerp.resampleFloat( nSegm );
				final PolygonRoi resROI = new PolygonRoi(xyMorphResampled[0], xyMorphResampled[1], Roi.POLYLINE);
				addRoi(resROI, nFrame, CurveMorpher.lerp( fStrokeIni, fStrokeLast, t ));
				//addRoi(resROI, nFrame, 0);
			}
		}
		
		//add last ROI
		final Roi lastRoi = curveRefs.get( curveRefs.size() -1 );
		addRoi(lastRoi, frameRefs.get( frameRefs.size() - 1 ), lastRoi.getStrokeWidth());
		
		
		if(dial.bAddToOverlay)
		{
			cm.imp.setOverlay( image_overlay );
			cm.imp.updateAndRepaintWindow();
			cm.imp.show();
		}
//		morphRefs = new ArrayList<>();
//		
//		//resample everything
//		for(int i = 0; i < curveRefs.size(); i++)
//		{
//			final CurveLerp temp = new CurveLerp(iniCurves.get( i ).resampleDouble( nFinalSegmentsN ));
//			morphRefs.add( new PolylineState(temp) );
//		}
	}
	
	void addRoi(final Roi roi, final int nFrame, final double dStrokeWidth)
	{
		roi.setStrokeWidth( dStrokeWidth );
		if(cm.bROIsAlongZ)
		{
			roi.setPosition( 1, nFrame, 1 );
		}
		else
		{
			roi.setPosition( 1, 1, nFrame );
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
		double dDistance = Double.MAX_VALUE;
		double dCurrDistance;
		int nStartLast = start.origXY[0].length-1;
		int nEndLast   = end.origXY[0].length-1;

		//begin-begin
		dCurrDistance = CurveLerp.distance( start.origXY[0][0], end.origXY[0][0], 
											start.origXY[1][0], end.origXY[1][0]);
		if(dCurrDistance < dDistance)
		{
			dDistance = dCurrDistance;
			nOrientation[0] = 0;
			nOrientation[0] = 0;

		}
		dCurrDistance = CurveLerp.distance( start.origXY[0][0], end.origXY[0][nEndLast], 
											start.origXY[1][0], end.origXY[1][nEndLast]);
		//begin - end
		if(dCurrDistance < dDistance)
		{
			dDistance = dCurrDistance;
			nOrientation[0] = 0;
			nOrientation[1] = 1;
		}
		
		dCurrDistance = CurveLerp.distance( start.origXY[0][nStartLast], end.origXY[0][0], 
											start.origXY[1][nStartLast], end.origXY[1][0]);
		//end - begin
		if(dCurrDistance < dDistance)
		{
			dDistance = dCurrDistance;
			nOrientation[0] = 1;
			nOrientation[1] = 0;
		}
		
		dCurrDistance = CurveLerp.distance( start.origXY[0][nStartLast], end.origXY[0][nEndLast], 
											start.origXY[1][nStartLast], end.origXY[1][nEndLast]);
		//end - begin
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
		frameRefs = new ArrayList<>();
		curveRefs = new ArrayList<>();
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
			curveRefs = new ArrayList<>();
			nFirstP = 1;

			if(cm.bROIsAlongZ)
			{
				nLastP = cm.dims[3];
			}
			else
			{
				nLastP = cm.dims[4];
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
			dMaxLength  = Math.max( dMaxLength, cLerp.dOrigLength );
		}
		
		nFinalSegmentsN = (int)Math.ceil( dMaxLength );

	}
	

}
