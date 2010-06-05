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
package com.servoy.eclipse.ui;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;

import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Utils;

public class DesignPerspective implements IPerspectiveFactory
{

	protected static final String[] actionIds = { "org.eclipse.debug.ui.debugActionSet", "org.eclipse.dltk.debug.ui.ScriptDebugActionSet", "org.eclipse.debug.ui.launchActionSet", "org.eclipse.ui.externaltools.ExternalToolsSet" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	@SuppressWarnings("restriction")
	public void createInitialLayout(IPageLayout layout)
	{
		String editorArea = layout.getEditorArea();
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.21f, editorArea);
		left.addView(SolutionExplorerView.PART_ID);
//		left.addView(IPageLayout.ID_RES_NAV);//move to sychronize perspective only

		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
		right.addView(IPageLayout.ID_OUTLINE);
		right.addView(IPageLayout.ID_PROP_SHEET);

		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.8f, editorArea);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);//move to debug perspective only
		bottom.addView(IPageLayout.ID_BOOKMARKS);
		bottom.addView(SearchUI.SEARCH_RESULT_VIEW_ID);
		bottom.addView(NewSearchUI.SEARCH_VIEW_ID);
		bottom.addView(IPageLayout.ID_TASK_LIST);

		/* remove the launch tool bar (run / debug) from the main tool bar and also the related + some similar actions */
		ApplicationWindow window = (ApplicationWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ICoolBarManager coolbarManager = window.getCoolBarManager2();
		for (String id : actionIds)
		{
			coolbarManager.remove(id);
		}
		ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
		IActionSetDescriptor[] actionSets = reg.getActionSets();
		for (IActionSetDescriptor element : actionSets)
		{
			for (String actionSetId : actionIds)
			{
				if (Utils.stringSafeEquals(element.getId(), actionSetId))
				{
					IExtension ext = element.getConfigurationElement().getDeclaringExtension();
					reg.removeExtension(ext, new Object[] { element });
				}
			}
		}

		setContentsOfShowViewMenu(layout);
	}

	protected void setContentsOfShowViewMenu(IPageLayout layout)
	{
		layout.addShowViewShortcut(SolutionExplorerView.PART_ID);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
	}
}
