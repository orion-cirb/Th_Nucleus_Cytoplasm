import Tools.Tools;
import com.sun.jna.StringArray;
import ij.IJ;
import ij.ImagePlus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.common.services.ServiceFactory;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.util.ImageProcessorReader;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import loci.plugins.in.ImporterOptions;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.geom2.Objects3DIntPopulation;
import org.apache.commons.io.FilenameUtils;
import org.bridj.ann.Array;



/*
 * Detect DAPI nuclei and Th cells
 * Find intensity in 561 channel
 */
public class Th_Nucleus_Cytoplasm implements PlugIn {
    
    Tools tools = new Tools();
    private boolean canceled = false;
    private String imageDir = "";
    public  String outDirResults = "";
    public String fileExt = "nd";
    public ArrayList<String> channelsName = new ArrayList<String>(Arrays.asList("DAPI nuclei", "Th cells", "561"));
    public BufferedWriter globalResults;
    public BufferedWriter cellsResults;
    
    public void run(String arg) {
        try {
            if (canceled) {
                IJ.showMessage("Plugin canceled");
                return;
            }
            imageDir = IJ.getDirectory("Choose directory containing image files...");
            if (imageDir == null) {
                return;
            }
            
            // Find images with specific extension
            ArrayList<String> imageFiles = tools.findImages(imageDir, fileExt);            
            if (imageFiles == null) {
                IJ.showMessage("Error", "No images found with " + fileExt + " extension");
                return;
            }
                        
            // Create output folder
            outDirResults = imageDir + File.separator + "Results" + File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            
            // Write headers
            String header = "Image name\tNucleus ID\tNucleus volume (µm3)\tNucleus sphericity\tNucleus intensity\tNucleus corrected intensity\t"
                    + "Cytoplasm volume (µm3)\tCytoplasm intensity\tCytoplasm corrected intensity\n";
            FileWriter fwCells = new FileWriter(outDirResults + "cellsResults.xls", false);
            cellsResults = new BufferedWriter(fwCells);
            cellsResults.write(header);
            cellsResults.flush();
            header = "Image name\tNb nuclei\tBackground mean intensity\tBackground intensity std\n";
            FileWriter fwGlobal = new FileWriter(outDirResults + "globalResults.xls", false);
            globalResults = new BufferedWriter(fwGlobal);
            globalResults.write(header);
            globalResults.flush();
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory;
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));

            // Find image calibration
            tools.cal = tools.findImageCalib(meta);
            
            // Find channels names
            ArrayList<String> channels = tools.findChannels(imageFiles.get(0), meta, reader, channelsName);

            // Channels dialog
            ArrayList<String> channelsOrdered = tools.dialog(channels);
            if (channelsOrdered == null || tools.canceled) {
                IJ.showStatus("Plugin cancelled");
                return;
            }
            
            for (String f : imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();   
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setCrop(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                       
                // Find nucleus
                System.out.println("Opening " + channelsName.get(0) + " channel " + channelsOrdered.get(0) + "...");
                int channel = channels.indexOf(channelsOrdered.get(0));
                ImagePlus imgNuclei = BF.openImagePlus(options)[channel];
                
                //Compute section volume in µm^3
                double pixVol = tools.cal.pixelWidth * tools.cal.pixelHeight * tools.cal.pixelDepth;
                double sectionVol = imgNuclei.getWidth() * imgNuclei.getHeight() * imgNuclei.getNSlices() * pixVol;
                
                // Find nucleus
                Objects3DIntPopulation nucleiPop = new Objects3DIntPopulation();
                nucleiPop = tools.cellposeNucleiPop(imgNuclei, "cyto", 1, 80, "/opt/miniconda3/envs/cellpose");
                //System.out.println(nucleiPop.getNbObjects() + " nuclei founds");
                
                /*// Find inner/outer ring nucleus
                // outer
                System.out.println("Finding outer ring ....");
                Objects3DPopulation outerRingPop = tools.createDonutPop(nucPop, imgZCropNuc, tools.outerNucDil, true);
                // inner
                System.out.println("Finding inner ring ....");
                Objects3DPopulation innerRingPop = tools.createDonutPop(nucPop, imgZCropNuc, tools.innerNucDil, false);
                // inner nucleus
                System.out.println("Finding inner nucleus ....");
                Objects3DPopulation innerNucPop = tools.getInnerNucleus(nucPop, imgZCropNuc, tools.innerNucDil, false);
                
                tools.closeImages(imgZCropNuc);
                   
                // open OFRP1 Channel
                System.out.println("Opening OFR1P channel " + channels.get(1)+ " ...");
                channel = channels.indexOf(chs.get(1));
                ImagePlus imgOFR1P = BF.openImagePlus(options)[channel];
                
                // Take same stack as nucleus
                ImagePlus imgZCropOFR1P = new Duplicator().run(imgOFR1P, tools.zMax, imgOFR1P.getNSlices());
                tools.closeImages(imgOFR1P);
                
                // Find background
                double[] bgOFR1P = tools.find_background(imgZCropOFR1P);
                
                // Find cell cytoplasm
                Objects3DPopulation cellPop = tools.findCells(imgZCropOFR1P, nucPop);
                
                // mask OFR1P with nucleus object
                ImagePlus imgOFR1P_NucleusMask = tools.maskImage(imgZCropOFR1P, nucPop, outDirResults, rootName+"_nucleusMasked.tif");
                
                 // read all intensity in OFR1P nucleus masked channel
                double sumNucMaskedIntensity = tools.readSumIntensity(imgOFR1P_NucleusMask, 0, null);
                
                // mask OFR1P with nucleus and cell object
                ImagePlus imgOFR1P_CellMask = tools.maskImage(imgOFR1P_NucleusMask, cellPop, outDirResults, rootName+"_CellsMasked.tif");
                
                tools.closeImages(imgOFR1P_NucleusMask);
                // read all intensity in OFR1P nucleus cells masked channel
                double sumCellMaskedIntensity = tools.readSumIntensity(imgOFR1P_CellMask, bgOFR1P[0]+bgOFR1P[1], outDirResults+rootName+"_CellProcesses.tif");
                tools.closeImages(imgOFR1P_CellMask);
                
                // Dots detections
                // All dots population
                Objects3DPopulation allDotsPop = new Objects3DPopulation();
                if (tools.dotsDetect)
                    allDotsPop = tools.find_dots(imgZCropOFR1P, 2, 1, "Triangle");

                // Save image objects
                tools.saveImageObjects(nucPop, outerRingPop, null, imgZCropOFR1P, outDirResults+rootName+"_OuterRingObjects.tif", 40);
                tools.saveImageObjects(innerRingPop, innerNucPop, null, imgZCropOFR1P, outDirResults+rootName+"_innerRingObjects.tif", 40);
                if (tools.dotsDetect)
                    tools.saveImageObjects(nucPop, allDotsPop, cellPop, imgZCropOFR1P, outDirResults+rootName+"_dotsObjects.tif", 40);
                
                // tags nucleus with parameters
                ArrayList<Nucleus> nucleus = tools.tagsNuclei(imgZCropOFR1P, nucPop, innerNucPop, innerRingPop, outerRingPop, cellPop, allDotsPop);
                             
                // Write results
                for (Nucleus nuc : nucleus) {
                    nucleus_Analyze.write(rootName+"\t"+nuc.getIndex()+"\t"+nuc.getNucVol()+"\t"+nuc.getNucCir()+"\t"+nuc.getNucInt()+"\t"+(nuc.getNucInt() - bgOFR1P[0] * (nuc.getNucVol()/volPix)) +"\t"+nuc.getNucDots()+"\t"+nuc.getNucDotsVol()+"\t"+nuc.getNucDotsInt()+
                            "\t"+nuc.getInnerNucVol()+"\t"+nuc.getInnerNucInt()+"\t"+(nuc.getInnerNucInt() - bgOFR1P[0] * (nuc.getInnerNucVol()/volPix))+"\t"+nuc.getInnerNucDots()+"\t"+nuc.getInnerNucDotsVol()+"\t"+nuc.getInnerNucDotsInt()+
                            "\t"+nuc.getInnerRingVol()+"\t"+nuc.getInnerRingInt()+"\t"+(nuc.getInnerRingInt() - bgOFR1P[0] * (nuc.getInnerRingVol()/volPix))+"\t"+nuc.getInnerRingDots()+"\t"+nuc.getInnerRingDotsVol()+"\t"+nuc.getInnerRingDotsInt()+
                            "\t"+nuc.getOuterRingVol()+"\t"+nuc.getOuterRingInt()+"\t"+(nuc.getOuterRingInt() - bgOFR1P[0] * (nuc.getOuterRingVol()/volPix))+"\t"+nuc.getOuterRingDots()+"\t"+nuc.getOuterRingDotsVol()+"\t"+nuc.getOuterRingDotsInt()+
                            "\t"+nuc.getCytoVol()+"\t"+nuc.getCytoInt()+"\t"+(nuc.getCytoInt() - bgOFR1P[0] * (nuc.getCytoVol()/volPix))+"\t"+nuc.getCytoDots()+"\t"+nuc.getCytoDotsVol()+"\t"+nuc.getCytoDotsInt()+"\n");
                    nucleus_Analyze.flush();
                }
                // Global measurements
                
                nucleusGlobal_Analyze.write(rootName+"\t"+nucPop.getNbObjects()+"\t"+sectionVol+"\t"+bgOFR1P[0]+"\t"+bgOFR1P[1]+"\t"+sumNucMaskedIntensity+"\t"+sumCellMaskedIntensity+"\n");
                nucleusGlobal_Analyze.flush();               
                tools.closeImages(imgZCropOFR1P);*/
                break;
            }
            
            cellsResults.close();
            globalResults.close();
            
        } catch (IOException | DependencyException | io.scif.DependencyException | ServiceException | FormatException ex) { //  |  ParserConfigurationException | SAXException ex
            //Logger.getLogger(Th_Nucleus_Cytoplasm.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        
        IJ.showStatus("Process done!");
    }
}
