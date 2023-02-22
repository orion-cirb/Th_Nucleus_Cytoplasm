package ThNucleusCytoplasm_Tools;

import ThNucleusCytoplasm_Tools.Cellpose.CellposeTaskSettings;
import ThNucleusCytoplasm_Tools.Cellpose.CellposeSegmentImgPlusAdvanced;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import org.apache.commons.io.FilenameUtils;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import javax.swing.ImageIcon;
import loci.common.services.ServiceException;
import loci.common.services.DependencyException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Object3DComputation;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.Objects3DIntPopulationComputation;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurementsPopulation.MeasurePopulationColocalisation;
import mcib3d.geom2.measurementsPopulation.PairObjects3DInt;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.lang.BooleanUtils;



/*
 * @author ORION_CIRB
 */
public class Tools {

    private final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    
    public ArrayList<String> channelsName = new ArrayList<String>(Arrays.asList("DAPI", "TH", "ORF1p", "NeuN"));;
    public Calibration cal;
    public double pixelVol;
    
    public String cellposeEnvDir = IJ.isWindows()? System.getProperty("user.home")+File.separator+"miniconda3"+File.separator+"envs"+File.separator+"CellPose" : "/opt/miniconda3/envs/cellpose";
    public String cellposeNucleusModel = "cyto";
    public int cellposeNucleusDiam = 80;
    public double minNucleusVol = 50;
    public double maxNucleusVol = 2000;
    public String cellposeCellModel = "cyto2";
    public int cellposeCellDiam = 110;
    public double minCellVol = 200;
    public double maxCellVol = 6000;
    
    
    /*
     * Display a message in the ImageJ console and status bar
     */
    public void print(String log) {
        System.out.println(log);
        IJ.showStatus(log);
    }

    
    /*
     * Check that needed modules are installed
     */
    public boolean checkInstalledModules() {
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("mcib3d.geom.Object3D");
        } catch (ClassNotFoundException e) {
            IJ.log("3D ImageJ Suite not installed, please install from update site");
            return false;
        }
        return true;
    }
    
    
    /*
     * Find type of images in folder
     */
    public String findImageType(File imagesFolder) {
        String ext = "";
        String[] files = imagesFolder.list();
        for (String name : files) {
            String fileExt = FilenameUtils.getExtension(name);
            switch (fileExt) {
               case "nd" :
                   ext = fileExt;
                   break;
                case "czi" :
                   ext = fileExt;
                   break;
                case "lif"  :
                    ext = fileExt;
                    break;
                case "ics" :
                    ext = fileExt;
                    break;
                case "ics2" :
                    ext = fileExt;
                    break;
                case "lsm" :
                    ext = fileExt;
                    break;
                case "tif" :
                    ext = fileExt;
                    break;
                case "tiff" :
                    ext = fileExt;
                    break;
            }
        }
        return(ext);
    }
    
    
    /*
     * Find images in folder
     */
    public ArrayList<String> findImages(String imagesFolder, String imageExtension) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No image found in " + imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            // Find images with extension
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExtension) && !f.startsWith("."))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }
    
    
    /*
     * Find image calibration
     */
    public void findImageCalib(IMetadata meta) {
        cal = new Calibration();  
        // Read image calibration
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelHeight = cal.pixelWidth;
        if (meta.getPixelsPhysicalSizeZ(0) != null)
            cal.pixelDepth = meta.getPixelsPhysicalSizeZ(0).value().doubleValue();
        else
            cal.pixelDepth = 1;
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth + ", Z calibration = " + cal.pixelDepth);
    }
    
   
    /**
     * Find channels name
     * @throws loci.common.services.DependencyException
     * @throws loci.common.services.ServiceException
     * @throws loci.formats.FormatException
     * @throws java.io.IOException
     */
    public ArrayList<String> findChannels(String imageName, IMetadata meta, ImageProcessorReader reader) throws DependencyException, ServiceException, FormatException, IOException {
        int nbChannels = reader.getSizeC();
        ArrayList<String> channels = new ArrayList<String>();
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                for (int n = 0; n < nbChannels; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelName(0, n));
                }
                break;
            case "nd2" :
                for (int n = 0; n < nbChannels; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelName(0, n));
                }
                break;
            case "lif" :
                for (int n = 0; n < nbChannels; n++) 
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelName(0, n));
                break;
            case "czi" :
                for (int n = 0; n < nbChannels; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelFluor(0, n));
                break;
            case "ics" :
                for (int n = 0; n < nbChannels; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelExcitationWavelength(0, n).value().toString());
                break;
            case "ics2" :
                for (int n = 0; n < nbChannels; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels.add(Integer.toString(n));
                    else 
                        channels.add(meta.getChannelExcitationWavelength(0, n).value().toString());
                break;   
            default :
                for (int n = 0; n < nbChannels; n++)
                    channels.add(Integer.toString(n));
        }
        return(channels);         
    }
    

    /*
     * Generate dialog box
     */
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
        
        gd.addMessage("CellPose", Font.getFont("Monospace"), Color.blue);
        gd.addDirectoryField​("Env directory", cellposeEnvDir);
        
        gd.addMessage("Nuclei detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min volume (µm3): ", minNucleusVol);
        gd.addNumericField("Max volume (µm3): ", maxNucleusVol);

        gd.addMessage("Cells detection", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min volume (µm3): ", minCellVol);
        gd.addNumericField("Max volume (µm3): ", maxCellVol);

        gd.addMessage("Image calibration and size", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Pixel size: ", cal.pixelWidth);
        gd.addNumericField("Pixel depth: ", cal.pixelDepth);
        gd.showDialog();

        ArrayList<String> channelsOrdered = new ArrayList<String>();
        for (int n = 0; n < channels.size(); n++) 
            channelsOrdered.add(gd.getNextChoice());
        
        cellposeEnvDir = gd.getNextString();
        minNucleusVol = gd.getNextNumber();
        maxNucleusVol = gd.getNextNumber();
        minCellVol = gd.getNextNumber();
        maxCellVol = gd.getNextNumber();
        cal.pixelWidth = cal.pixelHeight = gd.getNextNumber();
        cal.pixelDepth = gd.getNextNumber();
        pixelVol = cal.pixelWidth * cal.pixelHeight * cal.pixelDepth;
        
        if (gd.wasCanceled())
            channelsOrdered = null;
                
        return(channelsOrdered);
    }
     
       
    /*
     * Look for all 3D cells in a Z-stack: 
     * - apply CellPose in 2D slice by slice 
     * - let CellPose reconstruct cells in 3D using the stitch threshold parameters
     */
   public Objects3DIntPopulation cellposeDetection(ImagePlus img, String cellposeModel, int diameter, double stitchThreshold, double volMin, double volMax) throws IOException{
       // Define CellPose settings
       double resizeFactor = 0.5;
       CellposeTaskSettings settings = new CellposeTaskSettings(cellposeModel, 1, (int)(diameter*resizeFactor), cellposeEnvDir);
       settings.setStitchThreshold(stitchThreshold);
       settings.useGpu(true);
       
       // Run CellPose
       ImagePlus imgResized = img.resize((int)(img.getWidth()*resizeFactor), (int)(img.getHeight()*resizeFactor), "none");
       CellposeSegmentImgPlusAdvanced cellpose = new CellposeSegmentImgPlusAdvanced(settings, imgResized);
       ImagePlus imgOut = cellpose.run();
       imgOut = imgOut.resize(img.getWidth(), img.getHeight(), "none");
       imgOut.setCalibration(cal);
       
       // Get cells as a population of objects
       Objects3DIntPopulation pop = new Objects3DIntPopulation(ImageHandler.wrap(imgOut));
       System.out.println(pop.getNbObjects() + " Cellpose detections");
       
       // Filter cells by size
       pop = zFilterPop(pop);
       System.out.println(pop.getNbObjects() + " detections remaining after z-filtering");
       pop = new Objects3DIntPopulationComputation​(pop).getFilterSize​(volMin/pixelVol, volMax/pixelVol); 
       System.out.println(pop.getNbObjects() + " detections remaining after size filtering");
       pop.resetLabels();
       
       flush_close(imgResized);
       flush_close(imgOut);
       return(pop);
   } 
   
   
    /*
     * Remove objects present in only one z slice from population 
     */
    public Objects3DIntPopulation zFilterPop (Objects3DIntPopulation pop) {
        Objects3DIntPopulation popZ = new Objects3DIntPopulation();
        for (Object3DInt obj : pop.getObjects3DInt()) {
            int zmin = obj.getBoundingBox().zmin;
            int zmax = obj.getBoundingBox().zmax;
            if (zmax != zmin)
                popZ.addObject(obj);
        }
        return popZ;
    }
    
    
    /*
     * Find cells colocalizing with a nucleus
     */
    public ArrayList<Cell> colocalization(Objects3DIntPopulation thPop, Objects3DIntPopulation neunPop, Objects3DIntPopulation nucleiPop) {
        ArrayList<Cell> colocPop = new ArrayList<Cell>();
        
        if (nucleiPop.getNbObjects() > 0) {
            if (neunPop.getNbObjects() > 0) {
                MeasurePopulationColocalisation colocNeuN = new MeasurePopulationColocalisation(neunPop, nucleiPop);
                for (Object3DInt neun: neunPop.getObjects3DInt()) {
                    List<PairObjects3DInt> listNeuN = colocNeuN.getPairsObject1(neun.getLabel(), true);
                    if (!listNeuN.isEmpty()) {
                        PairObjects3DInt pairNeuN = listNeuN.get(listNeuN.size()-1);
                        Object3DInt nucleus = pairNeuN.getObject3D2();
                        double colocVol = pairNeuN.getPairValue();
                        if (colocVol > 0.5*nucleus.size()) {
                            nucleus.setIdObject(neun.getLabel());
                        }
                    }
                }
            }
                
            if (thPop.getNbObjects() > 0) {
                MeasurePopulationColocalisation colocTh = new MeasurePopulationColocalisation(thPop, nucleiPop);
                for (Object3DInt th: thPop.getObjects3DInt()) {
                    List<PairObjects3DInt> listTh = colocTh.getPairsObject1(th.getLabel(), true);
                    if (!listTh.isEmpty()) {
                        PairObjects3DInt pairTh = listTh.get(listTh.size()-1);
                        Object3DInt nucleus = pairTh.getObject3D2();
                        double colocVol = pairTh.getPairValue();
                        if (colocVol > 0.5*nucleus.size()) {
                            nucleus.setCompareValue​(th.getLabel());
                        }
                    }
                }
            }    
                  
            for (Object3DInt nucleus: nucleiPop.getObjects3DInt()) {
                Object3DInt cell = null;
                Object3DInt cyto = null;
                boolean thPos = false, neunPos = false;
                
                float neunLabel = nucleus.getIdObject();
                if(neunLabel != 0) {
                    cell = neunPop.getObjectByLabel(neunLabel);
                    neunPos = true;
                }
                
                float thLabel = (float) nucleus.getCompareValue();
                if(thLabel != 0) {
                    cell = thPop.getObjectByLabel(thLabel);
                    thPos = true;
                }
                
                if (cell != null) {
                    Object3DComputation objComputation = new Object3DComputation(cell);
                    cyto = objComputation.getObjectSubtracted(nucleus);
                }
                colocPop.add(new Cell(cell, nucleus, cyto, thPos, neunPos));
            }
        }
            
        resetLabels(colocPop);
        return(colocPop);
        
        /*ArrayList<Cell> colocPop = new ArrayList<Cell>();       
        if (nucleiPop.getNbObjects() > 0) {
            MeasurePopulationColocalisation colocTh = new MeasurePopulationColocalisation(nucleiPop, thPop);
            MeasurePopulationColocalisation colocNeuN = new MeasurePopulationColocalisation(nucleiPop, neunPop);
            
            for (Object3DInt nucleus: nucleiPop.getObjects3DInt()) {
                Object3DInt cell = null;
                Object3DInt cyto = null;
                boolean thPos = false, neunPos = false;
                
                for (Object3DInt neun: neunPop.getObjects3DInt()) {
                    double colocVal = colocNeuN.getValueObjectsPair(nucleus, neun);
                    if (colocVal > 0.6*nucleus.size()) {
                        cell = neun;
                        neunPos = true;
                        neunPop.removeObject(neun);
                        break;
                    }
                }
                for (Object3DInt th: thPop.getObjects3DInt()) {
                    double colocVal = colocTh.getValueObjectsPair(nucleus, th);
                    if (colocVal > 0.6*nucleus.size()) {
                        cell = th;
                        thPos = true;
                        thPop.removeObject(th);
                        break;
                    }
                }
                
                if (cell != null) {
                    Object3DComputation objComputation = new Object3DComputation(cell);
                    cyto = objComputation.getObjectSubtracted(nucleus);
                }
                colocPop.add(new Cell(cell, nucleus, cyto, thPos, neunPos));
            }
        }
        resetLabels(colocPop);
        return(colocPop);*/
    }
    
    
    /*
     * Reset labels of cells in population
     */
    public void resetLabels(ArrayList<Cell> cellPop) {
        float label = 1;
        for (Cell cell: cellPop) {
            cell.nucleus.setLabel(label);
            if (cell.cell != null) 
                cell.cell.setLabel(label);
            if (cell.cytoplasm != null)
                cell.cytoplasm.setLabel(label);
            label++;
        }
    }
    
    
    /*
     * Set volume and intensity parameters of all cells in population
     */
    public void fillCellPopParameters(ArrayList<Cell> cellPop, ImagePlus img) {
        ImageHandler imh = ImageHandler.wrap(img);
        for (Cell cell: cellPop) {
            cell.fillVolumes(pixelVol);
            cell.fillIntensities(imh);
        }
    }
    
    
    /**
     * 
     */
    public int[] countCells(ArrayList<Cell> colocPop) {
        int nbThNeun = 0, nbTh = 0, nbNeun = 0, nbNone = 0;
        for (Cell cell: colocPop) {
            nbThNeun += BooleanUtils.toInteger(cell.THPositive && cell.NeuNPositive);
            nbTh += BooleanUtils.toInteger(cell.THPositive && !cell.NeuNPositive);
            nbNeun += BooleanUtils.toInteger(!cell.THPositive && cell.NeuNPositive);
            nbNone+= BooleanUtils.toInteger(!cell.THPositive && !cell.NeuNPositive);
        }
        return(new int[]{nbThNeun, nbTh, nbNeun, nbNone});
    }
    
         
    /*
     * Save population of cells in image
     */
    public void drawResults(ArrayList<Cell> pop, ImagePlus img, String imgName, String outDir) {
        ImageHandler imgObj1 = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgObj2 = imgObj1.createSameDimensions();
        ImageHandler imgObj3 = imgObj1.createSameDimensions();
        ImageHandler imgObj4 = imgObj1.createSameDimensions();
        ImageHandler imgObj5 = imgObj1.createSameDimensions();
        ImageHandler imgObj6 = imgObj1.createSameDimensions();
        
        if (pop.size() > 0) {
            for (Cell cell: pop) {
                cell.nucleus.drawObject(imgObj1);
                cell.nucleus.drawObject(imgObj2, 255);
                
                if (cell.THPositive) {
                    cell.cytoplasm.drawObject(imgObj3);
                    cell.cytoplasm.drawObject(imgObj4, 255);
                }
                if (cell.NeuNPositive) {
                    cell.cytoplasm.drawObject(imgObj5);
                    cell.cytoplasm.drawObject(imgObj6, 255);
                }
                    
            }
        }
       
        ImagePlus[] imgColors1 = {imgObj6.getImagePlus(), imgObj4.getImagePlus(), imgObj2.getImagePlus(), img};
        ImagePlus imgObjects1 = new RGBStackMerge().mergeHyperstacks(imgColors1, false);
        imgObjects1.setCalibration(cal);
        FileSaver ImgObjectsFile1 = new FileSaver(imgObjects1);
        ImgObjectsFile1.saveAsTiff(outDir + imgName + "_cells.tif"); 
        imgObj2.closeImagePlus();
        imgObj4.closeImagePlus();
        imgObj6.closeImagePlus();
        flush_close(imgObjects1);
        
        ImagePlus[] imgColors2 = {imgObj5.getImagePlus(), imgObj3.getImagePlus(), imgObj1.getImagePlus()};
        ImagePlus imgObjects2 = new RGBStackMerge().mergeHyperstacks(imgColors2, false);
        imgObjects2.setCalibration(cal);
        FileSaver ImgObjectsFile2 = new FileSaver(imgObjects2);
        ImgObjectsFile2.saveAsTiff(outDir + imgName + "_labels.tif"); 
        imgObj1.closeImagePlus();
        imgObj3.closeImagePlus();
        imgObj5.closeImagePlus();
        flush_close(imgObjects2);
    }
    

    /*
     * Flush and close an image
     */
    public void flush_close(ImagePlus img) {
        img.flush();
        img.close();
    }     
}
