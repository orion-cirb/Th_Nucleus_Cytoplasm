import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import org.apache.commons.io.FilenameUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Objects3DIntPopulation;



/*
 * Detect DAPI nuclei and Th cells
 * Find intensity in 561 channel
 */
public class Th_Nucleus_Cytoplasm implements PlugIn {
    
    Utils utils = new Utils();
    private boolean canceled = false;
    private String imageDir = "";
    public  String outDirResults = "";
    public String fileExt;
    public ArrayList<String> channelsName = new ArrayList<String>(Arrays.asList("DAPI nuclei", "Th cells", "561"));
    public BufferedWriter globalResults;
    public BufferedWriter cellsResults;
    
    public void run(String arg) {
        try {
            if (canceled) {
                IJ.showMessage("Plugin canceled");
                return;
            }
            
            if (! utils.checkInstalledModules()) {
                return;
            }      
                    
            imageDir = IJ.getDirectory("Choose directory containing image files...");
            if (imageDir == null) {
                return;
            }
            
            // Find images with specific extension
            fileExt = utils.findImageType(new File(imageDir));           
            ArrayList<String> imageFiles = utils.findImages(imageDir, fileExt);            
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
            
            // Write headers in results files
            String header = "Image name\tCell ID\tCell volume (µm3)\tCell 561 intensity\tNucleus volume (µm3)\tNucleus 561 intensity"
                    + "\tCytoplasm volume (µm3)\tCytoplasm 561 intensity\n";
            FileWriter fwCells = new FileWriter(outDirResults + "detailedResults.xls", false);
            cellsResults = new BufferedWriter(fwCells);
            cellsResults.write(header);
            cellsResults.flush();
            header = "Image name\tVolume (µm3)\tNb cells\tCells mean volume (µm3)\tCells mean 561 intensity\tNuclei mean volume (µm3)\tNuclei mean 561 intensity\tCytoplasms mean volume (µm3)\tCytoplasm mean 561 intensity\n";
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
            utils.cal = utils.findImageCalib(meta);
            
            // Find channels names
            ArrayList<String> channels = utils.findChannels(imageFiles.get(0), meta, reader, channelsName);

            // Channels dialog
            ArrayList<String> channelsOrdered = utils.dialog(channels);
            if (channelsOrdered == null || utils.canceled) {
                IJ.showMessage("Plugin cancelled");
                return;
            }
            
            for (String f : imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                utils.print("--- ANALYZING IMAGE " + rootName + " ------");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();   
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setCrop(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                       
                //  Open nuclei channel
                utils.print("- Analyzing " + channelsName.get(0) + " channel " + channelsOrdered.get(0) + " -");
                int channelNuclei = channels.indexOf(channelsOrdered.get(0));
                ImagePlus imgNuclei = BF.openImagePlus(options)[channelNuclei];                
                // Find nuclei
                Objects3DIntPopulation nucleiPop = new Objects3DIntPopulation();
                nucleiPop = utils.cellposeDetection(imgNuclei, "cyto", 1, 80, 0.5, false, utils.minNucleusVol, utils.maxNucleusVol);
                System.out.println(nucleiPop.getNbObjects() + " " + channelsName.get(0) + " found");
                utils.drawPop(nucleiPop, imgNuclei.duplicate(), rootName+"_allNuclei", outDirResults);
                utils.flush_close(imgNuclei);
                
                // Open Th cells channel
                utils.print("- Analyzing " + channelsName.get(1) + " channel " + channelsOrdered.get(1) + " -");
                int channelTh = channels.indexOf(channelsOrdered.get(1));
                ImagePlus imgTh = BF.openImagePlus(options)[channelTh];
                // Find TH cells
                Objects3DIntPopulation thPop = new Objects3DIntPopulation();
                thPop = utils.cellposeDetection(imgTh, "cyto2", 1, 100, 0.5, true, utils.minCellVol, utils.maxCellVol);
                System.out.println(thPop.getNbObjects() + " " + channelsName.get(1) + " found");
                utils.drawPop(thPop, imgTh.duplicate(), rootName+"_allCells", outDirResults);
                        
                // Colocalization
                utils.print("- Performing colocalization between " + channelsName.get(0) + " and " + channelsName.get(1) + " -");
                ArrayList<Cell> colocPop = utils.colocalization(thPop, nucleiPop);
                System.out.println(colocPop.size() + " " + channelsName.get(1) + " colocalized with " + channelsName.get(0));
                utils.resetLabels(colocPop);
                
                //  Open 561 channel
                utils.print("- Measuring intensities in " + channelsName.get(2) + " channel " + channelsOrdered.get(2) + " -");
                int channel561 = channels.indexOf(channelsOrdered.get(2));
                ImagePlus img561 = BF.openImagePlus(options)[channel561];    
                utils.fillCellPopParameters(colocPop, img561);
                utils.flush_close(img561);
                               
                // Save image objects
                utils.print("- Saving results -");
                utils.drawResults(colocPop, imgTh, rootName, outDirResults);
            
                // Write detailed results
                for(Cell cell: colocPop) {
                    cellsResults.write(rootName+"\t"+cell.cell.getLabel()+"\t"+cell.parameters.get("cellVol")+"\t"+cell.parameters.get("cellInt")+
                            "\t"+cell.parameters.get("nucleusVol")+"\t"+cell.parameters.get("nucleusInt")+
                            "\t"+cell.parameters.get("cytoplasmVol")+"\t"+cell.parameters.get("cytoplasmInt")+"\n");
                    cellsResults.flush();
                }
                
                // Write global results
                double imgVol = imgTh.getWidth() * imgTh.getHeight() * imgTh.getNSlices() * utils.pixelVol;
                globalResults.write(rootName+"\t"+imgVol+"\t"+colocPop.size()+"\t"+utils.findPopMeanParam(colocPop, "Vol", "cell")+"\t"+
                        utils.findPopMeanParam(colocPop, "Int", "cell")+"\t"+utils.findPopMeanParam(colocPop, "Vol", "nucleus")+"\t"+
                        utils.findPopMeanParam(colocPop, "Int", "nucleus")+"\t"+utils.findPopMeanParam(colocPop, "Vol", "cytoplasm")+"\t"+
                        utils.findPopMeanParam(colocPop, "Int", "cytoplasm")+"\n");
                globalResults.flush();
                utils.flush_close(imgTh);
            }
            
            cellsResults.close();
            globalResults.close();
            
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(Th_Nucleus_Cytoplasm.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        utils.print("--- All done! ---");
    }
}
