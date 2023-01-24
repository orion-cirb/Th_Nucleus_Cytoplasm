import ThNucleusCytoplasm_Tools.NullPrintStream;
import ThNucleusCytoplasm_Tools.Tools;
import ThNucleusCytoplasm_Tools.Cell;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import org.apache.commons.io.FilenameUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
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
 * Detect DAPI nuclei and NeuN cells
 * Distinguish NeuN cells between TH-positive and TH-negative cells
 * Find intensity of different nuclei and cytoplasms in ORF1p channel
 */
public class Th_Nucleus_Cytoplasm implements PlugIn {
    
    Tools tools = new Tools();
    private boolean canceled = false;
    private String imageDir = "";
    public  String outDirResults = "";
    public String fileExt;
    public BufferedWriter globalResults;
    public BufferedWriter cellsResults;
    
    public void run(String arg) {
        try {
            if (canceled) {
                IJ.showMessage("Plugin canceled");
                return;
            }
            
            if (! tools.checkInstalledModules()) {
                return;
            }      
                    
            imageDir = IJ.getDirectory("Choose directory containing image files...");
            if (imageDir == null) {
                return;
            }
            
            // Find images with specific extension
            fileExt = tools.findImageType(new File(imageDir));           
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
            
            // Write headers in results files
            String header = "Image name\tCell ID\tNeuN-positive\tCell volume (µm3)\tCell mean ORF1p intensity\tNucleus volume (µm3)\tNucleus mean ORF1p intensity"
                    + "\tCytoplasm volume (µm3)\tCytoplasm mean ORF1p intensity\n";
            FileWriter fwCells = new FileWriter(outDirResults + "detailedResults.xls", false);
            cellsResults = new BufferedWriter(fwCells);
            cellsResults.write(header);
            cellsResults.flush();
            header = "Image name\tVolume (µm3)\tNb cells\tNb TH-positive cells\tCells mean volume (µm3)\tCells mean ORF1p intensity\tNuclei mean volume (µm3)"
                    + "\tNuclei mean ORF1p intensity\tCytoplasms mean volume (µm3)\tCytoplasm mean ORF1p intensity"
                    + "\tNb TH-negative nuclei\tTH-negative nuclei mean ORF1p intensity\n";
            FileWriter fwGlobal = new FileWriter(outDirResults + "globalResults.xls", false);
            globalResults = new BufferedWriter(fwGlobal);
            globalResults.write(header);
            globalResults.flush();
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));

            // Find image calibration
            tools.findImageCalib(meta);
            
            // Find channels names
            ArrayList<String> channels = tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Channels dialog
            ArrayList<String> channelsOrdered = tools.dialog(channels);
            if (channelsOrdered == null || tools.canceled) {
                IJ.showMessage("Plugin cancelled");
                return;
            }
            
            for (String f : imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                tools.print("--- ANALYZING IMAGE " + rootName + " ------");
                reader.setId(f);
                
                ImporterOptions options = new ImporterOptions();   
                options.setId(f);
                options.setSplitChannels(true);
                options.setQuiet(true);
                options.setCrop(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                PrintStream console = System.out;
                       
                //  Open DAPI channel
                tools.print("- Analyzing DAPI channel -");
                System.setOut(new NullPrintStream());
                ImagePlus imgDAPI = BF.openImagePlus(options)[channels.indexOf(channelsOrdered.get(0))];
                System.setOut(console);
                // Find DAPI nuclei
                Objects3DIntPopulation dapiPop = new Objects3DIntPopulation();
                dapiPop = tools.cellposeDetection(imgDAPI, tools.cellposeNucleusModel, tools.cellposeNucleusDiam, 0.5, true, tools.minNucleusVol, tools.maxNucleusVol);
                System.out.println(dapiPop.getNbObjects() + " DAPI nuclei found");
                
                
                 // Open Th channel
                tools.print("- Analyzing TH channel -");
                System.setOut(new NullPrintStream());
                ImagePlus imgTh = BF.openImagePlus(options)[channels.indexOf(channelsOrdered.get(1))];
                System.setOut(console);
                // Find Th cells
                Objects3DIntPopulation thPop = new Objects3DIntPopulation();
                thPop = tools.cellposeDetection(imgTh, "cyto2", tools.cellposeCellDiam, 0.5, false, tools.minCellVol, tools.maxCellVol);
                System.out.println(thPop.getNbObjects() + " TH cells found");
                
                // Colocalization between DAPI nuclei and Th cells
                tools.print("- Performing colocalization between DAPI nuclei and Th cells -");
                ArrayList<Cell> colocDapiThPop = tools.colocalization(thPop, dapiPop);
                System.out.println(colocDapiThPop.size() + " TH cells colocalized with DAPI nuclei");
               
                // Open NeuN channel
                tools.print("- Analyzing NeuN channel -");
                System.setOut(new NullPrintStream());
                ImagePlus imgNeuN = BF.openImagePlus(options)[channels.indexOf(channelsOrdered.get(3))];
                System.setOut(console);
                // Find NeuN cells
                Objects3DIntPopulation neunPop = new Objects3DIntPopulation();
                neunPop = tools.cellposeDetection(imgNeuN, "cyto2", tools.cellposeCellDiam, 0.5, true, tools.minCellVol, tools.maxCellVol);
                System.out.println(neunPop.getNbObjects() + " NeuN cells found");
                
                // Colocalization between TH and NeuN cells
                tools.print("- Determine which TH cells are NeuN-positives -");
                int nbNeuNPositiveCells = tools.NeuNPositivity(colocDapiThPop, neunPop);
                System.out.println(nbNeuNPositiveCells + " TH cells are NeuN-positives");
                                        
                //  Open ORF1p channel
                tools.print("- Measuring intensities in ORF1p channel -");
                System.setOut(new NullPrintStream());
                ImagePlus imgORF1p = BF.openImagePlus(options)[channels.indexOf(channelsOrdered.get(2))];   
                System.setOut(console);
                tools.fillCellPopParameters(colocDapiThPop, imgORF1p);
                               
                // Save image objects
                tools.print("- Saving results -");
                tools.drawNuclei(dapiPop, imgDAPI, rootName, outDirResults);
                tools.drawResults(colocDapiThPop, imgTh, rootName, outDirResults);
            
                // Write detailed results
                for(Cell cell: colocDapiThPop) {
                    cellsResults.write(rootName+"\t"+cell.cell.getLabel()+"\t"+cell.NeuNPositive+"\t"+cell.parameters.get("cellVol")+"\t"+cell.parameters.get("cellInt")+
                            "\t"+cell.parameters.get("nucleusVol")+"\t"+cell.parameters.get("nucleusInt")+
                            "\t"+cell.parameters.get("cytoplasmVol")+"\t"+cell.parameters.get("cytoplasmInt")+"\n");
                    cellsResults.flush();
                }
                
                // Write global results
                double[] nucleiParams = tools.getNucleiParams(dapiPop, imgORF1p);
                double imgVol = imgTh.getWidth() * imgTh.getHeight() * imgTh.getNSlices() * tools.pixelVol;
                globalResults.write(rootName+"\t"+imgVol+"\t"+colocDapiThPop.size()+"\t"+nbNeuNPositiveCells+"\t"+tools.findPopMeanParam(colocDapiThPop, "Vol", "cell")+"\t"+
                        tools.findPopMeanParam(colocDapiThPop, "Int", "cell")+"\t"+tools.findPopMeanParam(colocDapiThPop, "Vol", "nucleus")+"\t"+
                        tools.findPopMeanParam(colocDapiThPop, "Int", "nucleus")+"\t"+tools.findPopMeanParam(colocDapiThPop, "Vol", "cytoplasm")+"\t"+
                        tools.findPopMeanParam(colocDapiThPop, "Int", "cytoplasm")+"\t"+((int) nucleiParams[0])+"\t"+nucleiParams[1]+"\n");
                globalResults.flush();
                
                tools.flush_close(imgDAPI);
                tools.flush_close(imgNeuN);
                tools.flush_close(imgTh);
                tools.flush_close(imgORF1p);
            }
            
            cellsResults.close();
            globalResults.close();
            
        } catch (IOException | DependencyException | ServiceException | FormatException ex) {
            Logger.getLogger(Th_Nucleus_Cytoplasm.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        tools.print("--- All done! ---");
    }
}
