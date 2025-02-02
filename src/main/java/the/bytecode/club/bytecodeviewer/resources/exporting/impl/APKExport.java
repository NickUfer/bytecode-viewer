package the.bytecode.club.bytecodeviewer.resources.exporting.impl;

import the.bytecode.club.bytecodeviewer.BytecodeViewer;
import the.bytecode.club.bytecodeviewer.Configuration;
import the.bytecode.club.bytecodeviewer.gui.components.FileChooser;
import the.bytecode.club.bytecodeviewer.gui.components.MultipleChoiceDialogue;
import the.bytecode.club.bytecodeviewer.resources.exporting.Exporter;
import the.bytecode.club.bytecodeviewer.util.APKTool;
import the.bytecode.club.bytecodeviewer.util.FileContainer;
import the.bytecode.club.bytecodeviewer.util.JarUtils;
import the.bytecode.club.bytecodeviewer.util.MiscUtils;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static the.bytecode.club.bytecodeviewer.Constants.fs;
import static the.bytecode.club.bytecodeviewer.Constants.tempDirectory;

/**
 * @author Konloch
 * @since 6/27/2021
 */
public class APKExport implements Exporter
{
	@Override
	public void promptForExport()
	{
		if (BytecodeViewer.getLoadedClasses().isEmpty())
		{
			BytecodeViewer.showMessage("First open a class, jar, zip, apk or dex file.");
			return;
		}
		
		List<FileContainer> containers = BytecodeViewer.getFiles();
		List<FileContainer> validContainers = new ArrayList<>();
		List<String> validContainersNames = new ArrayList<>();
		FileContainer container;
		
		for (FileContainer fileContainer : containers)
		{
			if (fileContainer.APKToolContents != null && fileContainer.APKToolContents.exists())
			{
				validContainersNames.add(fileContainer.name);
				validContainers.add(fileContainer);
			}
		}
		
		if (!validContainers.isEmpty())
		{
			container = validContainers.get(0);
			
			//if theres only one file in the container don't bother asking
			if (validContainers.size() >= 2)
			{
				MultipleChoiceDialogue dialogue = new MultipleChoiceDialogue("Bytecode Viewer - Select APK",
						"Which file would you like to export as an APK?",
						validContainersNames.toArray(new String[0]));
				
				container = containers.get(dialogue.promptChoice());
			}
		} else {
			BytecodeViewer.showMessage("You can only export as APK from a valid APK file. Make sure Settings>Decode Resources is ticked on." +
					"\n\nTip: Try exporting as DEX, it doesn't rely on decoded APK resources");
			return;
		}
		
		final FileContainer finalContainer = container;
		
		Thread exportThread = new Thread(() ->
		{
			if (BytecodeViewer.viewer.compileOnSave.isSelected() && !BytecodeViewer.compile(false))
				return;
			
			JFileChooser fc = new FileChooser(new File(Configuration.lastDirectory),
					"Select APK Export",
					"Android APK",
					"apk");
			
			int returnVal = fc.showSaveDialog(BytecodeViewer.viewer);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				Configuration.lastDirectory = fc.getSelectedFile().getAbsolutePath();
				final File file = fc.getSelectedFile();
				String output = file.getAbsolutePath();
				
				//auto appened .apk
				if (!output.endsWith(".apk"))
					output = output + ".apk";
				
				final File file2 = new File(output);
				if (file2.exists())
				{
					MultipleChoiceDialogue dialogue = new MultipleChoiceDialogue("Bytecode Viewer - Overwrite File",
							"Are you sure you wish to overwrite this existing file?",
							new String[]{"Yes", "No"});
					
					if (dialogue.promptChoice() == 0) {
						file.delete();
					} else {
						return;
					}
				}
				
				Thread saveThread = new Thread(() ->
				{
					BytecodeViewer.viewer.updateBusyStatus(true);
					final String input = tempDirectory + fs + MiscUtils.getRandomizedName() + ".jar";
					JarUtils.saveAsJar(BytecodeViewer.getLoadedClasses(), input);
					
					Thread buildAPKThread = new Thread(() ->
					{
						APKTool.buildAPK(new File(input), file2, finalContainer);
						BytecodeViewer.viewer.updateBusyStatus(false);
					});
					buildAPKThread.start();
				});
				saveThread.start();
			}
		});
		exportThread.start();
	}
}
