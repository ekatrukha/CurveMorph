package curvemorph;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

public class CurveMorph_ implements PlugIn 
{
	/** current version **/
	String sVersion = "0.0.2";
	
	/** current image, must be stack or timelapse **/
	ImagePlus imp;
	
	/** image dimensions **/
	int [] dims;
	
	/** image slices, in case not hyperstack **/
	int nStackSize;
	
	/** ROI manager instance **/
	RoiManager rm;

	/** if we work along Z **/
	boolean bROIsAlongZ = false;
	
	/** if we work along T **/
	boolean bROIsAlongT = false;
	
	/** if no HyperStack position was selected **/
	boolean bHasHyperstackPos = true;
	
	public ArrayList<Roi> curveROIs;
	
	public int [] refFrames;
	
	CMDialog cmDialog = new CMDialog();
	
	Color strokeColor;
	
	@Override
	public void run( String arg )
	{
		IJ.register( CurveMorph_.class );
		//check the inputs
		if(!verifyInitialInput())
			return;
		if(!checkAssembleROIs())
			return;
		if(!cmDialog.showDialog())
			return;
		final CurveAssembly curveAssembly = new CurveAssembly(this, cmDialog);
		curveAssembly.runAssembly();
		if(cmDialog.bMakeKymograph)
		{
			final CMKymoBuilder<?> kymoBuilder = new CMKymoBuilder<>(this, curveAssembly);
			kymoBuilder.getKymograph();
		}
	}
	
	/** function that checks for input image and ROIs presence **/
	boolean verifyInitialInput()
	{
		//check if we have ROIs
		rm = RoiManager.getInstance2();
		if (rm == null) 
		{
			IJ.error( "CurveMorph error", "No ROIs in ROI manager." );
			return false;
		}
		else if(rm.getCount() < 2)
		{
			IJ.error("CurveMorph error", "ROI manager must contain at least 2 ROIs." );
			return false;			
		}
		//check if we have an image
		imp = IJ.getImage();
		
		if (imp == null)
		{
			IJ.noImage();
			return false;
		}
		
		if( imp.getBitDepth() == 24)
		{
			IJ.error( "CurveMorph plugin does not support RGB images.\n"
					+ "But you can switch to 3 separate color channel processing via Image -> Color -> Make Composite.");
			return false;
		}
		
		// check if it is stack or timelapse
		dims = imp.getDimensions();
		if( dims[3] == 1 && dims[4] == 1)
		{
			IJ.error( "CurveMorph error", "The input image should be stack or timelapse." );
			return false;
		}
		return true;
	}
	
	/** function checks the input ROIs and sorts them **/
	boolean checkAssembleROIs()
	{
		ArrayList<Roi> candidateROIs = new ArrayList<>();
		//get ROIs
		final Roi[] allRois = rm.getRoisAsArray();
		int nLineRois = 0;
		
		for (final Roi roi:allRois)
		{
			final int nType = roi.getType();
			//see that this is a line or curve
			if(nType >= Roi.LINE && nType <= Roi.FREELINE)
			{
				candidateROIs.add( roi );
				nLineRois++;
			}
			
			if(!roi.hasHyperStackPosition())
			{
				bHasHyperstackPos = false;
				nStackSize = imp.getStackSize();
				if(roi.getPosition() > nStackSize )
				{
					IJ.error( "CurveMorph error", "ROI " + roi.getName() + " position is larger than total number of slices." );
					return false;
				}
			}
			else
			{
				if(roi.getZPosition() > 1)
				{
					bROIsAlongZ = true;
					if(roi.getZPosition() > dims[3])
					{
						IJ.error( "CurveMorph error", "ROI " + roi.getName() + " Z-slice position is larger than total number of slices." );
						return false;
					}
	
				}
				if(roi.getTPosition() > 1)
				{
					bROIsAlongT = true;
					if(roi.getTPosition() > dims[4])
					{
						IJ.error( "CurveMorph error", "ROI " + roi.getName() + " timepoint position is larger than total number of frames." );
						return false;
					}
				}
			}

		}
		
		if(nLineRois < 2)
		{
			IJ.error( "CurveMorph error", "The plugin requires at least two line/curve ROIs in ROI manager." );
			return false;
		}
		
		if(bHasHyperstackPos)
		{
			if(bROIsAlongZ && bROIsAlongT)
			{
				IJ.error( "CurveMorph error", "Provided ROIs can only change either in Z or T position, not both." );
				return false;
			}
			if(!bROIsAlongZ && !bROIsAlongT)
			{
				IJ.error( "CurveMorph error", "You need to provide multiple ROIs at either different slices or different frames only." );	
				return false;
			}
		}
		
		//check if all ROIs have unique position
		Set<Integer> uniquePositions = new HashSet<>();
		int [][] indexedROIs = new int [nLineRois][2];
		int nCount = 0;
		
		for(final Roi roi:candidateROIs)
		{
			//roi index
			indexedROIs[nCount][0] = nCount;
			if(bHasHyperstackPos)
			{
				if(bROIsAlongT)
				{
					uniquePositions.add( roi.getTPosition());
					indexedROIs[nCount][1] = roi.getTPosition();
				}
				else
				{
					uniquePositions.add( roi.getZPosition());
					indexedROIs[nCount][1] = roi.getZPosition();
				}
			}
			else
			{
				uniquePositions.add( roi.getPosition());
				indexedROIs[nCount][1] = roi.getPosition();				
			}
			nCount++;
		}
		
		//duplicate ROIs at the same slice/frame
		if(uniquePositions.size() != nLineRois )
		{
			IJ.error( "CurveMorph error", "Each provided ROI must be at a different slice/frame." );
			return false;
		}
		
		//sort ROIs along the corresponding axis
		Arrays.sort(indexedROIs, (a, b) -> Integer.compare(a[1], b[1]));
		
		refFrames = new int [nLineRois];
		curveROIs = new ArrayList<>();
		for(int i = 0; i < nLineRois; i++)
		{
			curveROIs.add( candidateROIs.get( indexedROIs[i][0] ) );
			refFrames[ i ] = indexedROIs[i][1];
		}
		strokeColor = allRois[0].getStrokeColor();
		return true;
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) throws Exception 
	{
		new ImageJ();
	
		//ImagePlus image = IJ.openImage("/home/eugene/Desktop/people/Varsha/20250131_bendingMT/Average_20240510.tif");
		//ImagePlus image = IJ.openImage("/home/eugene/Desktop/people/Varsha/20260506_more_examples/20260319_trimmed.tif");
		//ImagePlus image  = IJ.openVirtual( "/home/eugene/Desktop/projects/CurveMorph/example_neurons/250711_ci1_div3_SD.tif" );
		
		ImagePlus image = IJ.openImage("/home/eugene/Desktop/projects/CurveMorph/example/example.tif");
		//ImagePlus image = IJ.openImage("/home/eugene/Desktop/people/Christophe/Untitled.tif");
		//ImagePlus image = IJ.openImage("/home/eugene/Desktop/people/Christophe/ends/Untitled.tif");
		//ImagePlus image = IJ.openImage("/home/eugene/Desktop/people/Christophe/ExM_MT.tif");
		image.show();
		RoiManager rMan = RoiManager.getInstance2();
		if (rMan == null) {
			rMan = new RoiManager(); // creates a new one if needed
		}

		//rMan.open( "/home/eugene/Desktop/people/Varsha/20260506_more_examples/20260319_trimmed.zip" );
		rMan.open( "/home/eugene/Desktop/projects/CurveMorph/example/example_RoiSet.zip" );
		//rMan.open( "/home/eugene/Desktop/people/Christophe/RoiSet3.zip" );
		//rMan.open( "/home/eugene/Desktop/people/Christophe/RoiSet_MT.zip" );
		// run the plugin
		IJ.runPlugIn(CurveMorph_.class.getName(), "");

		//manualTest();

	}



}
