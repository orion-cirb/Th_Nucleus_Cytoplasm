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
import mcib3d.image3d.ImageHandler;



/*
 * @author ORION_CIRB
 */
public class Tools {

    public boolean canceled = false;
    private final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
    
    public ArrayList<String> channelsName = new ArrayList<String>(Arrays.asList("DAPI", "TH", "ORF1p", "NeuN"));;
    public Calibration cal;
    public double pixelVol;
    
    public String cellposeEnvDir;
    public String cellposeNucleusModel = "cyto";
    public int cellposeNucleusDiam = 80;
    public double minNucleusVol = 50;
    public double maxNucleusVol = 2000;
    public String cellposeCellModel = "cyto2";
    public int cellposeCellDiam = 100;
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
        String tempEnv = "/opt/miniconda3/envs/cellpose";
        if (IJ.isWindows()) {
            tempEnv = System.getProperty("user.home")+File.separator+"miniconda3"+File.separator+"envs"+File.separator+"CellPose";
        }
        gd.addDirectoryField​("Env directory", tempEnv);
        
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

        if (gd.wasCanceled())
            canceled = true;

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
        
        return(channelsOrdered);
    }
     
       
    /*
     * Look for all 3D cells in a Z-stack: 
     * - apply CellPose in 2D slice by slice 
     * - let CellPose reconstruct cells in 3D using the stitch threshold parameters
     */
   public Objects3DIntPopulation cellposeDetection(ImagePlus img, String cellposeModel, int diameter, double stitchThreshold, boolean zFilter, double volMin, double volMax) throws IOException{
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
       ImageHandler imgH = ImageHandler.wrap(imgOut);
       Objects3DIntPopulation pop = new Objects3DIntPopulation(imgH);
       int nbCellsBeforeFiltering = pop.getNbObjects();
       System.out.println(nbCellsBeforeFiltering + " CellPose detections");
       
       // Filter cells by size
       if (zFilter) {
           pop = zFilterPop(pop);
           System.out.println(pop.getNbObjects() + " detections remaining after z-filtering (" + (nbCellsBeforeFiltering-pop.getNbObjects()) + " filtered out)");
       }
       Objects3DIntPopulationComputation popComputation = new Objects3DIntPopulationComputation​(pop);
       Objects3DIntPopulation popFilter = popComputation.getFilterSize​(volMin/pixelVol, volMax/pixelVol);
       popFilter.resetLabels();
       System.out.println(popFilter.getNbObjects() + " detections remaining after size filtering (" + (pop.getNbObjects()-popFilter.getNbObjects()) + " filtered out)");
       
       flush_close(imgOut);
       imgH.closeImagePlus();
       return(popFilter);
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
    public ArrayList<Cell> colocalization(Objects3DIntPopulation cellsPop, Objects3DIntPopulation nucleiPop) {
        ArrayList<Cell> colocPop = new ArrayList<Cell>();
        if (cellsPop.getNbObjects() > 0 && nucleiPop.getNbObjects() > 0) {
            MeasurePopulationColocalisation coloc = new MeasurePopulationColocalisation(nucleiPop, cellsPop);
            for (Object3DInt cell: cellsPop.getObjects3DInt()) {
                for (Object3DInt nucleus: nucleiPop.getObjects3DInt()) {
                    double colocVal = coloc.getValueObjectsPair(nucleus, cell);
                    if (colocVal > 0.25*nucleus.size()) {
                        Object3DComputation objComputation = new Object3DComputation​(cell);
                        Object3DInt cytoplasm = objComputation.getObjectSubtracted(nucleus);
                        nucleus.setComment("TH positive");
                        colocPop.add(new Cell(cell, nucleus, cytoplasm));
                        break;
                    }
                }
            }
        }
        resetLabels(colocPop);
        return(colocPop);
    }
    
    
    /*
     * Determine if cells are NeuN-positives
     */
    public int NeuNPositivity(ArrayList<Cell> cells, Objects3DIntPopulation pop, String type) {
        Objects3DIntPopulation cellsPop = new Objects3DIntPopulation();
        for(Cell cell: cells) 
            cellsPop.addObject(cell.cell);
        
        int nbNeuNCells = 0;
        if (cellsPop.getNbObjects() > 0 && pop.getNbObjects() > 0) {
            MeasurePopulationColocalisation coloc = new MeasurePopulationColocalisation(cellsPop, pop);
            for (Cell cell: cells) {
                for (Object3DInt obj: pop.getObjects3DInt()) {
                    double colocVal = coloc.getValueObjectsPair(cell.cell, obj);
                    if (colocVal > 0.25*obj.size()) {
                        if (type.equals("th"))
                            cell.NeuNPositive = true;
                        else
                            cell.THPositive = true;
                        nbNeuNCells++;
                        break;
                    }
                }
            }
        }
        return(nbNeuNCells);
    }
    
    /*
     * Reset labels of cells in population
     */
    public void resetLabels(ArrayList<Cell> cellPop) {
        float label = 1;
        for (Cell cell: cellPop) {
            cell.cell.setLabel(label);
            cell.nucleus.setLabel(label);
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
    
    
    /*
     * Find mean volume or intensity of a population of objects
     */
    public double findPopMeanParam(ArrayList<Cell> pop, String parameter, String object) {
        double sum = 0;
        for(Cell cell: pop) {
            sum += cell.parameters.get(object+parameter);
        }
        return(sum / pop.size());
    }
          

    public double[] getNucleiParams(Objects3DIntPopulation pop, ImagePlus img) {
        double nb = 0;
        double sum = 0;
        ImageHandler imh = ImageHandler.wrap(img);
        for(Object3DInt obj: pop.getObjects3DInt()) {
            if (obj.getComment() != ("TH positive")) {
                nb++;
                sum += new MeasureIntensity(obj, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG);
            }
        }
        return new double[]{nb, sum / nb};
    }
    
    
    public void drawNuclei(Objects3DIntPopulation pop, ImagePlus img, String imgName, String outDir) {
        ImageHandler imgObj1 = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgObj2 = imgObj1.createSameDimensions();
        if (pop.getNbObjects() > 0) {
            for (Object3DInt obj: pop.getObjects3DInt()) {
                if (obj.getComment() == "NeuN positive") {
                    obj.drawObject(imgObj1, 255);
                } else {
                    obj.drawObject(imgObj2, 255);
                }
            } 
        }
       
        ImagePlus[] imgColors = {null, null, imgObj1.getImagePlus(), img, imgObj2.getImagePlus()};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(img.getCalibration());
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDir + imgName + "_nuclei.tif"); 
        imgObj1.closeImagePlus();
        imgObj2.closeImagePlus();
        flush_close(imgObjects);
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
                cell.nucleus.drawObject(imgObj4, 255);
                
                if(cell.NeuNPositive) {
                    cell.cytoplasm.drawObject(imgObj2);
                    cell.cytoplasm.drawObject(imgObj5, 255);
                } else {
                    cell.cytoplasm.drawObject(imgObj3);
                    cell.cytoplasm.drawObject(imgObj6, 255);
                }
            } 
        }
       
        ImagePlus[] imgColors1 = {imgObj3.getImagePlus(), imgObj2.getImagePlus(), imgObj1.getImagePlus()};
        ImagePlus imgObjects1 = new RGBStackMerge().mergeHyperstacks(imgColors1, false);
        imgObjects1.setCalibration(img.getCalibration());
        FileSaver ImgObjectsFile1 = new FileSaver(imgObjects1);
        ImgObjectsFile1.saveAsTiff(outDir + imgName + "_labels.tif"); 
        imgObj1.closeImagePlus();
        imgObj2.closeImagePlus();
        flush_close(imgObjects1);
        
        ImagePlus[] imgColors2 = {imgObj6.getImagePlus(), imgObj5.getImagePlus(), imgObj4.getImagePlus(), img};
        ImagePlus imgObjects2 = new RGBStackMerge().mergeHyperstacks(imgColors2, false);
        imgObjects2.setCalibration(img.getCalibration());
        FileSaver ImgObjectsFile2 = new FileSaver(imgObjects2);
        ImgObjectsFile2.saveAsTiff(outDir + imgName + "_cells.tif"); 
        imgObj3.closeImagePlus();
        imgObj4.closeImagePlus();
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
