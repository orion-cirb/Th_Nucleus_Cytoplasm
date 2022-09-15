package Tools;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.RGBStackMerge;
import ij.plugin.HyperStackConverter;
import ij.process.ImageProcessor;
import io.scif.DependencyException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom.Point3D;
import mcib3d.geom.Point3DInt;
import mcib3d.geom.Voxel3D;
import mcib3d.geom2.BoundingBox;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.VoxelInt;
import mcib3d.geom2.measurements.MeasureCentroid;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationColocalisation;
import mcib3d.image3d.ImageHandler;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import Cellpose.*;

/**
 *
 * @author hm
 */
public class Tools {

    public boolean canceled = false;
    private final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    
    public ArrayList<String> channelsName;
    public Calibration cal;
    
    public double minNucleusVol = 300;
    public double maxNucleusVol = 1500;
    public double minCellVol = 300;
    public double maxCellVol = 1500;
    
    /*private Object syncObject = new Object();
    private final double stardistPercentileBottom = 0.2;
    private final double stardistPercentileTop = 99.8;
    private final double stardistProbThreshNuc = 0.75;
    private final double stardistOverlayThreshNuc = 0.25;
    private final double stardistProbThreshDot = 0.9;
    private final double stardistOverlayThreshDot = 0.25;
    private File modelsPath = new File(IJ.getDirectory("imagej")+File.separator+"models");
    private String stardistOutput = "Label Image"; 
    public String stardistCellModel = "";
    public String stardistDotModel = "";
    
    private CLIJ2 clij2 = CLIJ2.getInstance();*/
    
    
//    
//     /**
//     * check  installed modules
//     * @return 
//     */
//    public boolean checkInstalledModules() {
//        // check install
//        ClassLoader loader = IJ.getClassLoader();
//        try {
//            loader.loadClass("net.haesleinhuepf.clij2.CLIJ2");
//        } catch (ClassNotFoundException e) {
//            IJ.log("CLIJ not installed, please install from update site");
//            return false;
//        }
//        try {
//            loader.loadClass("mcib3d.geom.Object3D");
//        } catch (ClassNotFoundException e) {
//            IJ.log("3D ImageJ Suite not installed, please install from update site");
//            return false;
//        }
//        return true;
//    }
//    
//     /*
//    Find starDist models in Fiji models folder
//    */
//    public String[] findStardistModels() {
//        FilenameFilter filter = (dir, name) -> name.endsWith(".zip");
//        File[] modelList = modelsPath.listFiles(filter);
//        String[] models = new String[modelList.length];
//        for (int i = 0; i < modelList.length; i++) {
//            models[i] = modelList[i].getName();
//        }
//        Arrays.sort(models);
//        return(models);
//    }   
//    
//    /* Median filter 
//     * Using CLIJ2
//     * @param ClearCLBuffer
//     * @param sizeXY
//     * @param sizeZ
//     */ 
//    public ClearCLBuffer median_filter(ClearCLBuffer  imgCL, double sizeXY, double sizeZ) {
//        ClearCLBuffer imgCLMed = clij2.create(imgCL);
//        clij2.mean3DBox(imgCL, imgCLMed, sizeXY, sizeXY, sizeZ);
//        clij2.release(imgCL);
//        return(imgCLMed);
//    }
//    
//    /**
//     * Reset labels of the objects
//     */
//    public void resetLabels(Objects3DIntPopulation pop){
//        float label = 0;
//        for(Object3DInt obj: pop.getObjects3DInt()) {
//            obj.setLabel(label);
//            label++;
//        }
//    }
//
//    /**
//     * Remove object with one Z
//     */
//    public Objects3DIntPopulation zFilterPop (Objects3DIntPopulation pop) {
//        Objects3DIntPopulation popZ = new Objects3DIntPopulation();
//        for (Object3DInt obj : pop.getObjects3DInt()) {
//            int zmin = obj.getBoundingBox().zmin;
//            int zmax = obj.getBoundingBox().zmax;
//            if (zmax != zmin)
//                popZ.addObject(obj);
//        }
//        resetLabels(popZ);
//        return popZ;
//    }
//    
//     /**
//     * return objects population in an ClearBuffer image
//     * @param imgCL
//     * @return pop objects population
//     */
//
//    public Objects3DIntPopulation getPopFromClearBuffer(ClearCLBuffer imgBin, double min, double max) {
//        ClearCLBuffer labelsCL = clij2.create(imgBin);
//        clij2.connectedComponentsLabelingBox(imgBin, labelsCL);
//        ClearCLBuffer labelsSizeFilter = clij2.create(imgBin);
//        // filter size
//        clij2.excludeLabelsOutsideSizeRange(labelsCL, labelsSizeFilter, min/(cal.pixelWidth*cal.pixelWidth*cal.pixelDepth),
//                max/(cal.pixelWidth*cal.pixelWidth*cal.pixelDepth));
//        clij2.release(labelsCL);
//        ImagePlus img = clij2.pull(labelsSizeFilter);
//        clij2.release(labelsSizeFilter);
//        ImageHandler imh = ImageHandler.wrap(img);
//        flush_close(img);
//        Objects3DIntPopulation pop = new Objects3DIntPopulation(imh);
//        pop.setVoxelSizeXY(cal.pixelWidth);
//        pop.setVoxelSizeZ(cal.pixelDepth);
//        imh.closeImagePlus();
//        return(pop);
//    }  
//    
//    
//    
//    /**
//    Fill cells parameters
//    */
//    public void pvCellsParameters (Objects3DIntPopulation cellsPop, ArrayList<Cells_PV> pvCells, ImagePlus imgPv, ImagePlus imgGFP) {
//        for (Object3DInt obj : cellsPop.getObjects3DInt()) {
//            double pvVol = new MeasureVolume(obj).getVolumeUnit();
//            double pvInt = new MeasureIntensity(obj, ImageHandler.wrap(imgPv)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
//            double pvGFPInt = new MeasureIntensity(obj, ImageHandler.wrap(imgGFP)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
//            Cells_PV pv = new Cells_PV(pvVol, pvInt, pvGFPInt, false, 0, 0, 0, 0, 0, 0, 0, 0);
//            pvCells.add(pv);
//        }
//        
//    } 
    
/**
     * Find images in folder
     * @param imagesFolder
     * @param imageExt
     * @return 
     */
    public ArrayList<String> findImages(String imagesFolder, String imageExtension) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExtension))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }
    
//     /**
//     * Find image type
//     */
//    public String findImageType(File imagesFolder) {
//        String ext = "";
//        String[] files = imagesFolder.list();
//        for (String name : files) {
//            String fileExt = FilenameUtils.getExtension(name);
//            switch (fileExt) {
//               case "nd" :
//                   ext = fileExt;
//                   break;
//                case "czi" :
//                   ext = fileExt;
//                   break;
//                case "lif"  :
//                    ext = fileExt;
//                    break;
//                case "isc2" :
//                    ext = fileExt;
//                    break;
//            }
//        }
//        return(ext);
//    }
    
     /**
     * Find image calibration
     * @param meta
     * @return 
     */
    public Calibration findImageCalib(IMetadata meta) {
        cal = new Calibration();  
        // Read image calibration
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelHeight = cal.pixelWidth;
        if (meta.getPixelsPhysicalSizeZ(0) != null)
            cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
        else
            cal.pixelDepth = 1;
        cal.setUnit("microns");
        return(cal);
    }
    
    
     /**
     * Find channels name
     * @param imageName
     * @return 
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public ArrayList<String> findChannels(String imageName, IMetadata meta, ImageProcessorReader reader, ArrayList<String> channelsName) throws DependencyException, ServiceException, FormatException, IOException {
        this.channelsName = channelsName;
        int nbChannels = reader.getSizeC();
        ArrayList<String> channels = new ArrayList<String>();
        String imageExtension =  FilenameUtils.getExtension(imageName);
        switch (imageExtension) {
            case "nd" :
                for (int n = 0; n < nbChannels; n++) {
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelName(0, n).toString());
                }
                break;
            case "lif" :
                for (int n = 0; n < nbChannels; n++) {
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelName(0, n).toString());
                }
                break;
            case "czi" :
                for (int n = 0; n < nbChannels; n++) {
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelFluor(0, n).toString());
                }
                break;
            case "ics" :
                for (int n = 0; n < nbChannels; n++) {
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelExcitationWavelength(0, n).value().toString());
                }
                break;    
            default :
                for (int n = 0; n < nbChannels; n++)
                    channels.add(Integer.toString(n));
        }
        return(channels);         
    }

    
    public ArrayList<String> dialog(ArrayList<String> channels) {
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 100, 0);
        gd.addImage(icon);

        gd.addMessage("Channels", Font.getFont("Monospace"), Color.blue);
        int index = 0;
        for (String name: channelsName) {
            gd.addChoice(name + ": ", channels.toArray(new String[0]), channels.get(index));
            index++;
        }

        gd.addMessage("Nuclei detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("min nucleus volume: ", minNucleusVol);
        gd.addNumericField("max nucleus volume: ", maxNucleusVol);

        gd.addMessage("Cells detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("min cell volume: ", minCellVol);
        gd.addNumericField("max cell volume: ", maxCellVol);

        gd.addMessage("Image calibration and size", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Pixel size: ", cal.pixelWidth);
        gd.showDialog();

        if (gd.wasCanceled())
            canceled = true;

        ArrayList<String> channelsOrdered = new ArrayList<String>();
        for (int n = 0; n < channels.size(); n++) 
            channelsOrdered.add(gd.getNextChoice());

        minNucleusVol = gd.getNextNumber();
        maxNucleusVol = gd.getNextNumber();
        minCellVol = gd.getNextNumber();
        maxCellVol = gd.getNextNumber();
        cal.pixelWidth = cal.pixelHeight = gd.getNextNumber();
        
        return(channelsOrdered);
    }
     
    // Flush and close images
    public void flush_close(ImagePlus img) {
        img.flush();
        img.close();
    }
  
    
    /*
    * Look for all 3D nuclei in a Z-stack: 
    * - apply CellPose slice by slice 
    * - use clij to get nuclei in 3D
    */
   public Objects3DIntPopulation cellposeNucleiPop(ImagePlus imgNuclei, String cellposeModel, int channel, int diameter, String cellposeEnvDir) throws IOException{
       
       CellposeTaskSettings settings = new CellposeTaskSettings(cellposeModel, channel, diameter, cellposeEnvDir);
       settings.setStitchThreshold(0.25);
       settings.useGpu(true);
       
       CellposeSegmentImgPlusAdvanced cellpose = new CellposeSegmentImgPlusAdvanced(settings, imgNuclei);
       ImagePlus nuclei = cellpose.run();
       nuclei.show();
       
       return null;
       /*File starDistModelFile = new File(stardistModel);
       StarDist2D star = new StarDist2D(syncObject, starDistModelFile);
       star.loadInput(imgM);
       star.setParams(stardistPercentileBottom, stardistPercentileTop, stardistProbThreshNuc, stardistOverlayThreshNuc, stardistOutput);
       star.run();
       flush_close(imgM);
       // label in 3D
       ImagePlus nuclei = (resized) ? star.getLabelImagePlus().resize(width, height, 1, "none") : star.getLabelImagePlus();
       imgCL = clij2.push(nuclei);
       Objects3DIntPopulation nPop = getPopFromClearBuffer(imgCL, minVol, maxVol); 
       clij2.release(imgCL);
       Objects3DIntPopulation popZ = zFilterPop(nPop);  
       flush_close(nuclei);
       return(popZ);*/
   } 

//     /**
//     * Find volume of objects  
//     * @param dotsPop
//     * @return vol
//     */
//    
//    public double findPopVolume (Objects3DIntPopulation dotsPop) {
//        IJ.showStatus("Findind object's volume");
//        List<Double[]> results = dotsPop.getMeasurementsList(new MeasureVolume().getNamesMeasurement());
//        double sum = results.stream().map(arr -> arr[1]).reduce(0.0, Double::sum);
//        return(sum);
//    }
//    
//    /**
//     * Find intensity of objects  
//     * @param dotsPop
//     * @return intensity
//     */
//    
//    public double findPopIntensity (Objects3DIntPopulation dotsPop, ImagePlus img) {
//        IJ.showStatus("Findind object's intensity");
//        ImageHandler imh = ImageHandler.wrap(img);
//        double sumInt = 0;
//        for(Object3DInt obj : dotsPop.getObjects3DInt()) {
//            MeasureIntensity intMes = new MeasureIntensity(obj, imh);
//            sumInt +=  intMes.getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
//        }
//        return(sumInt);
//    }
//    
//    /*
//    Check if label object exist in pop
//    */
//    private boolean checkObjLabel(Objects3DIntPopulation pop , Object3DInt obj) {
//        boolean check = false;
//        for (Object3DInt objPop : pop.getObjects3DInt()) {
//            if (objPop.getLabel() == obj.getLabel()) {
//                check = true;
//                break;
//            }
//        }
//        return(check);
//    }
//    
//    /**
//     * Find coloc cells
//     * pop1 PV cells
//     * pop2 PNN cells
//     * @return PV cells in PNN cells
//     */
//    public Objects3DIntPopulation findColocCells (Objects3DIntPopulation pop1, Objects3DIntPopulation pop2, ArrayList<Cells_PV> pvCells, ImagePlus imgPNN) {
//        Objects3DIntPopulation colocPop = new Objects3DIntPopulation();
//        IJ.showStatus("Finding colocalized cells population ...");
//        for(Object3DInt obj2: pop2.getObjects3DInt()) {
//            Point3D PtObj2 =  new MeasureCentroid(obj2).getCentroidAsPoint();
//            for(Object3DInt obj1 : pop1.getObjects3DInt()) {
//                Point3D PtObj1 =  new MeasureCentroid(obj1).getCentroidAsPoint();
//                double dist = PtObj1.distance(PtObj2, cal.pixelWidth, cal.pixelDepth);
//                if ((dist <= minDist) ) {
//                    if (!checkObjLabel(colocPop, obj1)) {
//                        colocPop.addObject(obj1);
//                        Cells_PV pvCell = pvCells.get((int)obj1.getLabel());
//                        double pnnInt = new MeasureIntensity(obj2, ImageHandler.wrap(imgPNN)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
//                        double pnnVol = new MeasureVolume(obj2).getValueMeasurement(MeasureVolume.VOLUME_UNIT);
//                        pvCell.setcellPV_PNN(true);
//                        pvCell.setcellPNNInt(pnnInt);
//                        pvCell.setcellPNNVol(pnnVol);
//                        pvCells.set((int)obj1.getLabel(), pvCell);
//                    }
//                    break;
//                }
//            }
//        }
//        colocPop.setVoxelSizeXY(cal.pixelWidth);
//        colocPop.setVoxelSizeZ(cal.pixelDepth);
//        return(colocPop);    
//    }
//    
//    /**
//     * Find dots in cells
//     * @param cellsPop pv cells
//     * @param dotsPop dots
//     * @param pvCells
//     * @param img
//     * @param channel GFP or DAPI
//     * @return 
//     */
//    public Objects3DIntPopulation findDotsinCells (Objects3DIntPopulation cellsPop, Objects3DIntPopulation dotsPop, ArrayList<Cells_PV> pvCells, ImagePlus img, String channel) {
//        Objects3DIntPopulation colocPop = new Objects3DIntPopulation();
//        IJ.showStatus("Finding dots in cells population ...");
//        for (Object3DInt cellObj : cellsPop.getObjects3DInt()) {
//            int cellIndex = (int)cellObj.getLabel();
//            IJ.showStatus("Finding "+channel+" foci in PV cell "+cellIndex+"/"+cellsPop.getNbObjects());
//            Cells_PV pvCell = pvCells.get(cellIndex);
//            int dots = 0;
//            double dotsVol = 0;
//            double dotsInt = 0;
//            for (Object3DInt dotObj : dotsPop.getObjects3DInt()) {
//                Point3D pt = new MeasureCentroid(dotObj).getCentroidAsPoint();
//                if (cellObj.contains(new VoxelInt(pt.getRoundX(), pt.getRoundY(), pt.getRoundZ(), 0))) {
//                    dots++;
//                    dotsVol += new MeasureVolume(dotObj).getVolumeUnit();
//                    dotsInt += new MeasureIntensity(dotObj, ImageHandler.wrap(img)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
//                    colocPop.addObject(dotObj);
//                }
//            }
//            switch (channel) {
//                case "GFP":
//                    pvCell.setnbFoci(dots);
//                    pvCell.setfociVol(dotsVol);
//                    pvCell.setfociInt(dotsInt);
//                    break;
//                case "DAPI":
//                    pvCell.setnbDapiFoci(dots);
//                    pvCell.setfociDapiVol(dotsVol);
//                    pvCell.setfociDapiInt(dotsInt);
//                    break;
//                default :
//            }
//            pvCells.set(cellIndex, pvCell);
//        }
//        colocPop.setVoxelSizeXY(cal.pixelWidth);
//        colocPop.setVoxelSizeZ(cal.pixelDepth);
//        return(colocPop);    
//    }
//    
//    /**
//     * Find dots in cells
//     * @param pop1 pv cells
//     * @param pop2 dots
//     * @param pvCells
//     * @param img
//     * @param channel GFP or DAPI
//     * @return 
//     */
//    public Objects3DIntPopulation colocDotsCells (Objects3DIntPopulation cellsPop, Objects3DIntPopulation dotsPop, ArrayList<Cells_PV> pvCells, ImagePlus img, String channel) {
//        Objects3DIntPopulation colocPop = new Objects3DIntPopulation();
//        IJ.showStatus("Finding dots in cells population ...");
//        MeasurePopulationColocalisation coloc = new MeasurePopulationColocalisation(cellsPop, dotsPop);
//        ResultsTable table = coloc.getResultsTableOnlyColoc();
//        table.show("coloc");
//        for (Object3DInt cellObj : cellsPop.getObjects3DInt()) {
//            int cellIndex = (int)cellObj.getLabel();
//            IJ.showStatus("Finding "+channel+" foci in PV cell "+cellIndex+"/"+cellsPop.getNbObjects());
//            Cells_PV pvCell = pvCells.get(cellIndex);
//            int dots = 0;
//            double dotsVol = 0;
//            double dotsInt = 0;
//            for (Object3DInt dotObj : dotsPop.getObjects3DInt()) {
//                double colocVal = coloc.getValueObjectsPair(cellObj, dotObj);
//                if (colocVal > 0) {
//                    dots++;
//                    dotsVol += new MeasureVolume(dotObj).getVolumeUnit();
//                    dotsInt += new MeasureIntensity(dotObj, ImageHandler.wrap(img)).getValueMeasurement(MeasureIntensity.INTENSITY_SUM);
//                    colocPop.addObject(dotObj);
//                }
//            }
//            switch (channel) {
//                case "GFP":
//                    pvCell.setnbFoci(dots);
//                    pvCell.setfociVol(dotsVol);
//                    pvCell.setfociInt(dotsInt);
//                    break;
//                case "DAPI":
//                    pvCell.setnbDapiFoci(dots);
//                    pvCell.setfociDapiVol(dotsVol);
//                    pvCell.setfociDapiInt(dotsInt);
//                    break;
//                default :
//            }
//            pvCells.set(cellIndex, pvCell);
//        }
//        colocPop.setVoxelSizeXY(cal.pixelWidth);
//        colocPop.setVoxelSizeZ(cal.pixelDepth);
//        return(colocPop);    
//    }
//    
//     /**
//     * Label object
//     * @param popObj
//     * @param img 
//     */
//    public void labelsObject (Object3DInt obj, ImageHandler imh) {
//        int fontSize = Math.round(8f/(float)imh.getCalibration().pixelWidth);
//        Font tagFont = new Font("SansSerif", Font.PLAIN, fontSize);
//        float label = obj.getLabel();
//        BoundingBox box = obj.getBoundingBox();
//        int z = box.zmin;
//        int x = box.xmin - 2;
//        int y = box.ymin - 2;
//        imh.getImagePlus().setSlice(z+1);
//        ImageProcessor ip = imh.getImagePlus().getProcessor();
//        ip.setFont(tagFont);
//        ip.setColor(255);
//        ip.drawString(Integer.toString((int)label), x, y);
//        imh.getImagePlus().updateAndDraw();
//    }
//   
//    /**
//     * Save dots Population in image
//     * @param pop1 PV cells
//     * @param pop2 PNN cells
//     * @param pop3 fociGFP
//     * @param pop4 fociDAPI
//     * @param pop5 PV/PNN
//     * @param imageName
//     * @param img 
//     * @param outDir 
//     */
//    public void saveImgObjects(Objects3DIntPopulation pop1, Objects3DIntPopulation pop2, Objects3DIntPopulation pop3, Objects3DIntPopulation pop4, 
//            Objects3DIntPopulation pop5, String imageName, ImagePlus img, String outDir) {
//        //create image objects population
//        
//
//        //PV cells green
//        ImageHandler imgObj1 = ImageHandler.wrap(img).createSameDimensions();
//        if (pop1.getNbObjects() > 0)
//            for (Object3DInt obj : pop1.getObjects3DInt())
//                obj.drawObject(imgObj1, 255);
//        
//        //PNN cells red
//        ImageHandler imgObj2 = imgObj1.createSameDimensions();
//        if (pop2.getNbObjects() > 0)
//            for (Object3DInt obj : pop2.getObjects3DInt())
//                obj.drawObject(imgObj2, 255);
//        
//        // Foci GFP cyan
//        ImageHandler imgObj3 = imgObj1.createSameDimensions();
//        if (pop3.getNbObjects() > 0)
//            for (Object3DInt obj : pop3.getObjects3DInt())
//                obj.drawObject(imgObj3, 255);        
//        
//        // Foci DAPI blue
//        ImageHandler imgObj4 = imgObj1.createSameDimensions();
//        if (pop4.getNbObjects() > 0) 
//            for (Object3DInt obj : pop4.getObjects3DInt())
//                obj.drawObject(imgObj4, 255);
//        
//        // PV/PNN yellow
//        ImageHandler imgObj5 = imgObj1.createSameDimensions();
//        if (pop5.getNbObjects() > 0) {
//            for (Object3DInt obj : pop5.getObjects3DInt()) {
//                    labelsObject(obj, imgObj5);
//                    obj.drawObject(imgObj5, 255);
//            }
//        }
//   
//        // save image for objects population
//        ImagePlus[] imgColors = {imgObj2.getImagePlus(), imgObj1.getImagePlus(), imgObj4.getImagePlus(), null, imgObj3.getImagePlus(),null,imgObj5.getImagePlus() };
//        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
//        imgObjects.setCalibration(img.getCalibration());
//        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
//        ImgObjectsFile.saveAsTiff(outDir + imageName + "_Objects.tif"); 
//        imgObj1.closeImagePlus();
//        imgObj2.closeImagePlus();
//        imgObj3.closeImagePlus();
//        imgObj4.closeImagePlus();
//        imgObj5.closeImagePlus();
//        flush_close(imgObjects);
//    }
//    

//    
///**
//     * 
//     * @param xmlFile
//     * @return
//     * @throws ParserConfigurationException
//     * @throws SAXException
//     * @throws IOException 
//     */
//    public ArrayList<Point3DInt> readXML(String xmlFile) throws ParserConfigurationException, SAXException, IOException {
//        ArrayList<Point3DInt> ptList = new ArrayList<>();
//        int x = 0, y = 0 ,z = 0;
//        File fXmlFile = new File(xmlFile);
//        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//	Document doc = dBuilder.parse(fXmlFile);
//        doc.getDocumentElement().normalize();
//        NodeList nList = doc.getElementsByTagName("Marker");
//        for (int n = 0; n < nList.getLength(); n++) {
//            Node nNode = nList.item(n);
//            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
//                Element eElement = (Element) nNode;
//                x = Integer.parseInt(eElement.getElementsByTagName("MarkerX").item(0).getTextContent());
//                y = Integer.parseInt(eElement.getElementsByTagName("MarkerY").item(0).getTextContent());
//                z = Integer.parseInt(eElement.getElementsByTagName("MarkerZ").item(0).getTextContent())-zSlicesToIgnore;
//            }
//            Point3DInt pt = new Point3DInt(x, y, z);
//            ptList.add(pt);
//        }
//        return(ptList);
//    }
    
}
