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
    
    public Cell(Object3DInt cell, Object3DInt nucleus, Object3DInt cytoplasm, boolean THPositive, boolean NeuNPositive) {
        this.cell = cell;
        this.nucleus = nucleus;
        this.cytoplasm = cytoplasm;
        this.NeuNPositive = NeuNPositive;
        this.THPositive = THPositive;
        this.parameters = new HashMap<>();
    }
    
    public void fillVolumes(double pixelVol) {
        if (cell != null)
            parameters.put("cellVol", cell.size() * pixelVol);
        else
            parameters.put("cellVol", Double.NaN);
        parameters.put("nucleusVol", nucleus.size() * pixelVol);
        if (cytoplasm != null)
            parameters.put("cytoplasmVol", cytoplasm.size() * pixelVol);
        else
            parameters.put("cytoplasmVol", Double.NaN);
    }
    
    public void fillIntensities(ImageHandler imh) {
        if (cell != null)
            parameters.put("cellInt", new MeasureIntensity(cell, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG));
        else
            parameters.put("cellInt", Double.NaN);
        parameters.put("nucleusInt", new MeasureIntensity(nucleus, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG));
        if (cytoplasm != null)
            parameters.put("cytoplasmInt", new MeasureIntensity(cytoplasm, imh).getValueMeasurement(MeasureIntensity.INTENSITY_AVG));
        else
            parameters.put("cytoplasmInt", Double.NaN);
    }
    
}
