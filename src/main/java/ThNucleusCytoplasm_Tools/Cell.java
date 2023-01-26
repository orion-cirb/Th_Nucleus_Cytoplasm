package ThNucleusCytoplasm_Tools;

import java.util.HashMap;
import mcib3d.image3d.ImageHandler;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.measurements.MeasureIntensity;

/**
 *
 * @author hm
 */
public class Cell {
    public Object3DInt cell;
    public Object3DInt nucleus;
    public Object3DInt cytoplasm;
    public boolean NeuNPositive;
    public boolean THPositive;
    public HashMap<String, Double> parameters;
    
    public Cell(Object3DInt cell, Object3DInt nucleus, Object3DInt cytoplasm) {
        this.cell = cell;
        this.nucleus = nucleus;
        this.cytoplasm = cytoplasm;
        this.NeuNPositive = false;
        this.THPositive = false;
        this.parameters = new HashMap<>();
    }
    
    public void fillVolumes(double pixelVol) {
        parameters.put("cellVol", cell.size() * pixelVol);
        parameters.put("nucleusVol", nucleus.size() * pixelVol);
        parameters.put("cytoplasmVol", cytoplasm.size() * pixelVol);
    }
    
    public void fillIntensities(ImageHandler imh) {
        parameters.put("cellInt", new MeasureIntensity(cell, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG));
        parameters.put("nucleusInt", new MeasureIntensity(nucleus, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG));
        parameters.put("cytoplasmInt", new MeasureIntensity(cytoplasm, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG));
    }
    
}
