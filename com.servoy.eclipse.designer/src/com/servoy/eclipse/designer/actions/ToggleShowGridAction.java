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
package com.servoy.eclipse.designer.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.SnapToGrid;

import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;


/**
 * Action for show-grid toggle in form designer toolbar.
 * 
 * @author rgansevles
 */

public class ToggleShowGridAction extends ViewerTogglePropertyAction
{
	/**
	 * Constructor
	 * 
	 * @param diagramViewer the GraphicalViewer whose grid enablement and visibility properties are to be toggled
	 */
	public ToggleShowGridAction(GraphicalViewer diagramViewer)
	{
		super(diagramViewer, DesignerActionFactory.TOGGLE_SHOW_GRID.getId(), DesignerActionFactory.TOGGLE_SHOW_GRID_TEXT,
			DesignerActionFactory.TOGGLE_SHOW_GRID_TOOLTIP, DesignerActionFactory.TOGGLE_SHOW_GRID_IMAGE, SnapToGrid.PROPERTY_GRID_VISIBLE);
	}
}
