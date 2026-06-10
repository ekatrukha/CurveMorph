<picture>
	<source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/logo_black.gif">
	<img align="right" style="padding:10px" alt="BVB logo" src="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/logo_white.gif">
</picture>

[Fiji](https://fiji.sc/) plugin to morph (or interpolate) 2D curve/line ROIs (lines, polylines, freelines)    
of arbitrary width and build kymographs along them.   
It works with virtual stacks, multi-color images, timelapses, etc.   

Full documentation is available at [wiki](https://github.com/ekatrukha/CurveMorph/wiki).   
In short, see:   
* [Installation](https://github.com/ekatrukha/CurveMorph/wiki/Installation)
* [Getting started](https://github.com/ekatrukha/CurveMorph/wiki/Getting-started)
* [How to cite](https://github.com/ekatrukha/CurveMorph/wiki/How-to-cite)

### General idea  

The plugin can build a kymograph along the bending/undulating   
curves/lines/filaments (think microtubules, cilia, neurites, etc).   

For example, here is a growing and [bending microtubule](https://pubmed.ncbi.nlm.nih.gov/24462000/)   
with kinesins running along it.   

<picture>
	<source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/example.gif">
	<img alt="example movie" src="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/example_white.gif">
</picture>   
You can provide a reference ROIs at different time points of the movie,   
shown here (6 ROIs for 100 frames).   
<picture>
	<source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/example_result_ROIsinput.gif">
	<img alt="example movie with reference ROIs" src="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/example_result_ROIsinput_white.gif">
</picture> 

The plugin will morph (or interpolate) ROI shapes at all intermediate frames:   
<picture>
	<source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/example_result_ROIsinterp.gif">
	<img alt="example movie with interpolated ROIs" src="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/example_result_ROIsinterp_white.gif">
</picture>    

And create a kymograph along them:   
<picture>
	<source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/kymo.png">
	<img alt="kymograph" src="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/kymo_white.png">
</picture>    

Optionally, it will output a straightened "kymograph stack":   
<picture>
	<source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/KymoStack_example.gif">
	<img alt="kymograph stack output" src="https://raw.githubusercontent.com/ekatrukha/CurveMorph/main/logo/example/KymoStack_example_white.gif">
</picture>    

### Similar plugins

* [Dynamic kymograph](https://github.com/rudyzhou/Dynamic_Kymograph), uses only PolyLine ROIs and XY interpolation + fixed number of ROI nodes.
* [Trackmate kymograph](https://imagej.net/plugins/trackmate/extensions/trackmate-kymograph), kymograph along the straight line defined by two tracks.


----------

Developed in [Cell Biology group](http://cellbiology.science.uu.nl) of Utrecht University.  
<a href="mailto:katpyxa@gmail.com">E-mail</a> for any questions or tag <a href="https://forum.image.sc/u/ekatrukha/summary">@ekatrukha</a> at <a href="https://forum.image.sc/">image.sc</a> forum.   
