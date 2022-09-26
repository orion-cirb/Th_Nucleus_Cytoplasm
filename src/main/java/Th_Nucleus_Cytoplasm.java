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
 * Detect DAPI nuclei and TH cells
 * Find intensity in ORF1p channel
 */
public class Th_Nucleus_Cytoplasm implements PlugIn {
    
    Utils utils = new Utils();
    private boolean canceled = false;
    private String imageDir = "";
    public  String outDirResults = "";
    public String fileExt;
    public ArrayList<String> channelsName = new ArrayList<String>(Arrays.asList("DAPI", "TH", "ORF1p"));
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
            String header = "Image name\tCell ID\tCell volume (µm3)\tCell ORF1p intensity\tNucleus volume (µm3)\tNucleus ORF1p intensity"
                    + "\tCytoplasm volume (µm3)\tCytoplasm ORF1p intensity\n";
            FileWriter fwCells = new FileWriter(outDirResults + "detailedResults.xls", false);
            cellsResults = new BufferedWriter(fwCells);
            cellsResults.write(header);
            cellsResults.flush();
            header = "Image name\tVolume (µm3)\tNb cells\tCells mean volume (µm3)\tCells mean ORF1p intensity\tNuclei mean volume (µm3)"
                    + "\tNuclei mean ORF1p intensity\tCytoplasms mean volume (µm3)\tCytoplasm mean ORF1p intensity"
                    + "\tNb TH-negative nuclei\tTH-negative nuclei mean ORF1p intensity\n";
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
                       
                //  Open DAPI channel
                utils.print("- Analyzing " + channelsName.get(0) + " channel -");
                int channelDAPI = channels.indexOf(channelsOrdered.get(0));
                ImagePlus imgDAPI = BF.openImagePlus(options)[channelDAPI];                
                // Find DAPI nuclei
                Objects3DIntPopulation dapiPop = new Objects3DIntPopulation();
                dapiPop = utils.cellposeDetection(imgDAPI, "cyto", 1, 80, 0.5, false, utils.minNucleusVol, utils.maxNucleusVol);
                System.out.println(dapiPop.getNbObjects() + " " + channelsName.get(0) + " nuclei found");
                
                // Open Th channel
                utils.print("- Analyzing " + channelsName.get(1) + " channel -");
                int channelTh = channels.indexOf(channelsOrdered.get(1));
                ImagePlus imgTh = BF.openImagePlus(options)[channelTh];
                // Find Th cells
                Objects3DIntPopulation thPop = new Objects3DIntPopulation();
                thPop = utils.cellposeDetection(imgTh, "cyto2", 1, 100, 0.5, true, utils.minCellVol, utils.maxCellVol);
                System.out.println(thPop.getNbObjects() + " " + channelsName.get(1) + " cells found");
                        
                // Colocalization
                utils.print("- Performing colocalization between " + channelsName.get(0) + " nuclei and " + channelsName.get(1) + " cells -");
                ArrayList<Cell> colocPop = utils.colocalization(thPop, dapiPop);
                System.out.println(colocPop.size() + " " + channelsName.get(1) + " cells colocalized with " + channelsName.get(0) + " nuclei");
                utils.resetLabels(colocPop);
                
                //  Open ORF1p channel
                utils.print("- Measuring intensities in " + channelsName.get(2) + " channel -");
                int channelORF1p = channels.indexOf(channelsOrdered.get(2));
                ImagePlus imgORF1p = BF.openImagePlus(options)[channelORF1p];    
                utils.fillCellPopParameters(colocPop, imgORF1p);
                               
                // Save image objects
                utils.print("- Saving results -");
                utils.drawNuclei(dapiPop, imgDAPI, rootName, outDirResults);
                utils.drawResults(colocPop, imgTh, rootName, outDirResults);
            
                // Write detailed results
                for(Cell cell: colocPop) {
                    cellsResults.write(rootName+"\t"+cell.cell.getLabel()+"\t"+cell.parameters.get("cellVol")+"\t"+cell.parameters.get("cellInt")+
                            "\t"+cell.parameters.get("nucleusVol")+"\t"+cell.parameters.get("nucleusInt")+
                            "\t"+cell.parameters.get("cytoplasmVol")+"\t"+cell.parameters.get("cytoplasmInt")+"\n");
                    cellsResults.flush();
                }
                
                // Write global results
                double[] nucleiParams = utils.getNucleiParams(dapiPop, imgORF1p);
                double imgVol = imgTh.getWidth() * imgTh.getHeight() * imgTh.getNSlices() * utils.pixelVol;
                globalResults.write(rootName+"\t"+imgVol+"\t"+colocPop.size()+"\t"+utils.findPopMeanParam(colocPop, "Vol", "cell")+"\t"+
                        utils.findPopMeanParam(colocPop, "Int", "cell")+"\t"+utils.findPopMeanParam(colocPop, "Vol", "nucleus")+"\t"+
                        utils.findPopMeanParam(colocPop, "Int", "nucleus")+"\t"+utils.findPopMeanParam(colocPop, "Vol", "cytoplasm")+"\t"+
                        utils.findPopMeanParam(colocPop, "Int", "cytoplasm")+"\t"+((int) nucleiParams[0])+"\t"+nucleiParams[1]+"\n");
                globalResults.flush();
                
                utils.flush_close(imgDAPI);
                utils.flush_close(imgTh);
                utils.flush_close(imgORF1p);
            }
            
            cellsResults.close();
            globalResults.close();
            
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(Th_Nucleus_Cytoplasm.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        utils.print("--- All done! ---");
    }
}
