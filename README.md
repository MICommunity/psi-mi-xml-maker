# PSI-MI XML Maker User Guide

## Introduction

### What is the PSI-MI XML Maker?
The PSI-MI XML Maker is a tool designed to create XML files based on the [PSI-MI 3.0 format](https://rawgit.com/HUPO-PSI/miXML/master/3.0/doc/MIF300.html).
These XML files can later be used in the editor for further processing.

### How to download?
- **MacOS** : download the .dmg file
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

### 2. Formatting the input File
If the imported file is not constituted of one participant per row, it must be formatted.

#### 2.1 Selecting the columns to process
If the file format is **XLSX** or **XLS**, the user has to select the sheet to access. In other cases it is not necessary.
Then, the dropdown menus will fill with the headers of the file.
The user can now select the corresponding columns for the **Bait ID** and the **Prey ID**.
If it is present in the file it can also select the name columns.

#### 2.2 Selecting general information
The general information panel contains the participant identification method, the interaction detection method,
the host organism and the Interaction figure legend.
The user can select the corresponding options and they will be applied to **all** participants.

> **Note:** The options available for the dropdown menus are directly fetched on OLS.

#### 2.3 Selecting information concerning the baits
The same way as for the general information, the user can select information about the baits thanks to the dropdown menus.
The bait organism can be typed in by the user.
The user can select multiple experimental preparations. To do so, it has to increase the number in the spinner.
Then, multiple dropdown menu will appear.
The user can create features by clicking on the corresponding button.
This action will open a window where the user can create multiple features by increasing the number on the top of it.
The user can input manually the start and end location of the feature. It can also create multiple cross-references for it
(note that the feature cross-reference has to be typed in).
Once the feature(s) have been created, they will be applied to all the baits.

#### 2.4 Selecting information concerning the preys
The preys information selection is working the same way as the baits information selection.

The following options can also be typed in:
- the organism **Tax id**

#### 2.5 Formatting the file
Finally, the user can format the file by clicking the button **Format file**.
If the user ticks the box **Create binary interactions** each row of the input file will be considered an interaction.
If it is not ticked, the interaction number will remain the same as long as the bait do not change.

In the end, a new file called "inputFile_XMLFormatted.inputFileFormat" is created and will be used to work on.

### 3. Updating Uniprot IDs
If the file format is **XLSX** or **XLS**, the user has to select the sheet to access. In other cases it is not necessary.
Then, the dropdown menus will fill with the headers of the file.

Once done, the user has to select the corresponding columns in the file:

- the **Participant ID** column must be specified.
- the **Organism** and **Database** columns are optional but can refine Uniprot mapping.
- Clicking **Update the Uniprot IDs** will start the process.
- If multiple reviewed Uniprot IDs or no results are found, the user can choose one or manually enter the ID and database.
- Additional columns will be added to the file with the updated ID, organism, and database.

### 4. Creating Participants
Before creating interactions, participants must be defined.
- For **XLSX** or **XLS** files, select the correct sheet.
- The user can select the number of features per participant present in the file, by default this number is set to 1;
  - Once increased, the corresponding number of feature cells will be added in the selection table.
- Map the participant elements to the correct file columns.
- The application suggests columns based on similarity but requires user verification.
- The user can check the rightfulness of the data input thanks to the preview lines which are displayed from the 3rd line of the table.

### 5. Creating the PSI-MI 3.0 XML File
- The **Publication Date** must be selected.
- Choose the **Number of interactions per XML file** (default: **1000**).
- If interactions exceed this limit, multiple files will be created.
- The **file name** and **save location** can be customised.
    - Default: `inputFileName_[n].xml` inside a directory named after the input file.
- Click **Browse...** to change the save location.
- Click **Create XML File** to generate and save the XML file(s).

---

