# PSI-MI XML Maker User Guide

## Introduction

### What is the PSI-MI XML Maker?
The PSI-MI XML Maker is a tool designed to create XML files based on the [PSI-MI 3.0 format](https://rawgit.com/HUPO-PSI/miXML/master/3.0/doc/MIF300.html). These XML files can later be used in the editor for further processing.

### How to download?
- **MacOs** : download the .dmg file
- **Windows** : download the .zip file
- **Linux** : download the .tar file

### Supported File Formats
The PSI-MI XML Maker supports the following file formats:
- **CSV** (Comma-Separated Values)
- **TSV** (Tab-Separated Values)
- **XLSX** (Excel format from 2007 onwards)
- **XLS** (Excel format before 2007)

## File Processing

### 1. File Input
There are three ways to import a file into the PSI-MI XML Maker:
- Click the **File** button in the menu bar and select a file using the file browser.
- Click the **Fetch file** button in the **1. Fetch file** section to open the file browser.
- Drag and drop the desired file directly into the application window.

> **Note:** A **Publication ID** must be provided before an XML file can be created.

### 2. Formatting the Raw File
If the imported file does not conform to the Interactome template, it must be formatted.
- The user can select columns for **Bait** and **Prey**.
- If interactions are binary, the user can specify this by ticking the corresponding box.
- Additional interaction-related information can be selected.
- Once formatted, a new file named `previousFile_xmlFormatted.fileType` is generated.

### 3. Updating Uniprot IDs
Updating Uniprot IDs is optional but recommended.
- If working with an **XLSX** or **XLS** file, select the correct sheet.
- The **Participant ID** column must be specified.
- The **Organism** and **Database** columns are optional but can refine Uniprot mapping.
- Clicking **Update the Uniprot IDs** will start the process.
- If multiple Uniprot IDs are found, the user can choose one or manually enter the ID and database.
- Additional columns will be added to the file with the updated ID, organism, and database.

### 4. Creating Participants
Before creating interactions, participants must be defined.
- For **XLSX** or **XLS** files, select the correct sheet.
- Map the participant elements to the correct file columns.
- The application suggests columns based on similarity but requires user verification.
- A preview function is available to check values.

### 5. Creating the PSI-MI 3.0 XML File
- The **Publication Date** must be selected.
- Choose the **Number of interactions per XML file** (default: **1000**).
- If interactions exceed this limit, multiple files will be created.
- The **file name** and **save location** can be customised.
    - Default: `inputFileName_[n].xml` inside a directory named after the input file.
- Click **Browse...** to change the save location.
- Click **Create XML File** to generate and save the XML file(s).

---

### Contact & Support
For questions or troubleshooting, please refer to the official documentation or contact the development team.

