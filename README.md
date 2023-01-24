# Th_Nucleus_Cytoplasm

* **Developed for:** Olivia
* **Team:** Prochiantz
* **Date:** Janvier 2023
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
* Keep Th cells colocalizing with a nucleus only
* Mark Th cells with NeuN cells and tag them as NeuN+ or NeuN-
* Measure ORF1p intensity in the nucleus and the cytoplasm of each Th cell
* Measure ORF1p intensity in Th-negative nuclei


### Dependencies

* **3DImageSuite** Fiji plugin
* **Cellpose** conda environment + *cyto* and *cyto2* models

### Version history

Version 1 released on January 24, 2023.
