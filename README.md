# Th_Nucleus_Cytoplasm

* **Developed for:** Olivia
* **Team:** Proschiantz
* **Date:** September 2022
* **Software:** Fiji


### Images description

3D images taken with a x20 objective

3 channels:
  1. *CSU_405:* DAPI nuclei
  2. *CSU_488:* Th cells 
  3. *CSU_561:* ORF1p

### Plugin description

* Detect DAPI nuclei with Cellpose
* Detect Th cells with Cellpose
* Keep Th cells colocalizing with a nucleus only
* Measure ORF1p intensity in the nucleus and the cytoplasm of each Th cell
* Measure ORF1p intensity in Th-negative nuclei


### Dependencies

* **3DImageSuite** Fiji plugin
* **Cellpose** conda environment + *cyto* and *cyto2* models

### Version history

Version 1 released on September 22, 2022.
