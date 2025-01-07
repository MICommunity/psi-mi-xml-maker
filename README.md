# The PSI-MI XML-maker

The PSI-MI XML-maker is a tool designed to create XML files based on the 
[PSI-MI 3.0 format](https://rawgit.com/HUPO-PSI/miXML/master/3.0/doc/MIF300.html).
Those files can later be input in the editor for further processing.

## Requirements
The PSI-MI XML-maker runs under [JDK11](https://www.oracle.com/uk/java/technologies/javase/jdk11-archive-downloads.html).

## How to launch for the first time?

1. In your console, run the command `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home` to be set up on JDK11.
2. To be sure that you are on this version you can check with the command `java --version`.
3. Then go to the *PSI-MI-XML-Maker* directory by using the command `cd path_to_directory/PSI-MI-XML-Maker` and run `mvn clean install`.
4. After the build is complete, you may be prompted to grant access to the Finder. Once you grant access, you can save the PSI-MI XML-maker as an app.

Once the setup is complete, you can launch the PSI-MI XML-maker and start generating XML files based on the PSI-MI 3.0 format.

## How to use?

### What kind of file the PSI-XML maker support?

- CSV (Comma Separated values)
- TSV (Tab Separated values)
- XLSX (Excel format (from 2007))
- XLS (Excel format (before 2007))

## File processing

### 1. File input 
    They are 3 ways to import a file in the PSI-MI XML maker:
      - Pressing the File button in the menu bar will lead to an input button which open the file browser.
      - Pressing the "Fetch file" button in the "1. Fetch file section" will open the file browser.
      - Dragging the wanted file directly into the window.
      Once the file is loaded, enter the publication ID. The XML file will not be created without it.

### 2. Update the Uniprot Ids
    The second section is not mandatory but recommended. 
    In the case of an XLSX or XLS file, the sheet on which the XML maker is working has to be selected. 
    Then, the same way as other formats, the columns will be displayed in the three selectors. 
    The user has to select for each selector the corresponding column in the file. 
    Once done, a click on the "Update the Uniprot ids" button will launch the process.

### 3. Create the participants
    Before creating the interactions, the user has to create the participants. 
    The same way as the Uniprot update, the user has to select the sheet for the XLSX or XLSX file. 
    Then the user has to associate the participant's elements to the columns in the file. 
    Once done, a click on the "Create participant" button will launch the process. 
    The participants will be created and associated depending on their interactions.

### 4. Create the PSI-MI 3.0 XML file
    Finally, the user has to select the date of the publication. 
    Then, the saving location has to be saved. 
    Clicking on the "Browse..." button will open the saving file browser. 
    Once those requirements filed, clicking on the "Create XML file" button will write and save the XML file.