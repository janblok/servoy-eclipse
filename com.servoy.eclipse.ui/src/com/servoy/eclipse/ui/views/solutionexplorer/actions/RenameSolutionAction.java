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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.team.core.RepositoryProvider;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.IdentDocumentValidator;

public class RenameSolutionAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public RenameSolutionAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Rename solution");
		setToolTipText("Rename solution");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			state = false;
			if (((SimpleUserNode)sel.getFirstElement()).getRealObject() instanceof ServoyProject)
			{
				ServoyProject servoyProject = (ServoyProject)((SimpleUserNode)sel.getFirstElement()).getRealObject();
				if (!ServoyModelManager.getServoyModelManager().getServoyModel().isModuleActive(servoyProject.getProject().getName()))
				{
					state = true;
				}
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof ServoyProject)
		{
			ServoyProject servoyProject = (ServoyProject)node.getRealObject();
			if (RepositoryProvider.getProvider(servoyProject.getProject()) != null)
			{
				MessageDialog.openInformation(viewer.getViewSite().getShell(), "Cannot rename solution",
					"Cannot rename a solution that has team provider, must remove share first.");
				return;
			}
			Solution editingSolution = servoyProject.getEditingSolution();

			InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Rename solution", "Rename solution", "", new IInputValidator()
			{
				public String isValid(String newText)
				{
					boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
					return valid ? null : (newText.length() == 0 ? "" : "Invalid solution name");
				}
			});
			int res = nameDialog.open();
			if (res == Window.OK)
			{
				String name = nameDialog.getValue();
				ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name);
				if (project == null)
				{
					try
					{
						servoyProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
						editingSolution.updateName(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), name);
						IProjectDescription description = servoyProject.getProject().getDescription();
						description.setName(name);
						description.setLocation(null);
						servoyProject.getProject().move(description, false, null);
						EclipseRepository repository = (EclipseRepository)ServoyModel.getDeveloperRepository();
						String protectionPassword = ApplicationServerSingleton.get().calculateProtectionPassword(editingSolution.getSolutionMetaData(), null);
						editingSolution.getSolutionMetaData().setProtectionPassword(protectionPassword);
						repository.updateNodesInWorkspace(new IPersist[] { editingSolution }, true);
						servoyProject.getSolution().getSolutionMetaData().setProtectionPassword(protectionPassword);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
						MessageDialog.openError(viewer.getViewSite().getShell(), "Rename failed", e.getMessage());
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
						MessageDialog.openError(viewer.getViewSite().getShell(), "Rename failed", "Could not move the project to new directory");
					}
				}
				else
				{
					MessageDialog.openError(viewer.getViewSite().getShell(), "Rename failed", "A project with name '" + name + "' already exists.");
				}
			}

		}
	}
}
