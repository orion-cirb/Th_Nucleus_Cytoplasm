# Th_Nucleus_Cytoplasm

* **Developed for:** Olivia
* **Team:** Prochiantz
* **Date:** February 2023
* **Software:** Fiji


### Images description

3D images taken with a x20 objective

4 channels:
  1. *CSU_405:* DAPI nuclei
  2. *CSU_488:* Th cells 
  3. *CSU_561:* ORF1p
  4. *CSU_642:* NeuN
  

### Plugin description

* Detect DAPI nuclei with Cellpose
* Detect Th cells with Cellpose
* Detect NeuN cells with Cellpose
* Colocalize nuclei with Th and NeuN cells and tag each nucleus as being NeuN+/NeuN- and Th+/Th-
* Measure ORF1p intensity of nuclei, cytoplasms and cells

### Dependencies

* **3DImageSuite** Fiji plugin
* **Cellpose** conda environment + *cyto* and *cyto2* models

### Version history

Version 2 released on February 3, 2023.
