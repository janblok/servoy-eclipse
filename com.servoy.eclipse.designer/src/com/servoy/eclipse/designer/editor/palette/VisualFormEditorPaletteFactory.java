/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.designer.editor.palette;

import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.ToolEntry;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.ui.Activator;


/**
 * Utility class that can create a GEF Palette for the Visual Form Editor.
 * 
 * @author rgansevles
 * @since 6.0
 */
public class VisualFormEditorPaletteFactory
{

	private static PaletteContainer createElementsDrawer()
	{
		PaletteDrawer componentsDrawer = new PaletteDrawer("Elements");
		ToolEntry component;

		component = new ElementCreationToolEntry("Button", "Create a button", new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_BUTTON),
			Activator.loadImageDescriptorFromBundle("button.gif"), Activator.loadImageDescriptorFromBundle("button.gif"));
		componentsDrawer.add(component);

		component = new ElementCreationToolEntry("Label", "Create a label", new RequestTypeCreationFactory(VisualFormEditor.REQ_PLACE_LABEL),
			Activator.loadImageDescriptorFromBundle("text.gif"), Activator.loadImageDescriptorFromBundle("text.gif"));
		componentsDrawer.add(component);

		// TODO: Add more

		return componentsDrawer;
	}

	/**
	 * Creates the PaletteRoot and adds all palette elements. Use this factory
	 * method to create a new palette for your graphical editor.
	 * 
	 * @return a new PaletteRoot
	 */
	static public PaletteRoot createPalette()
	{
		PaletteRoot palette = new PaletteRoot();
//		palette.add(createToolsGroup(palette));
		palette.add(createElementsDrawer());
		return palette;
	}

}
