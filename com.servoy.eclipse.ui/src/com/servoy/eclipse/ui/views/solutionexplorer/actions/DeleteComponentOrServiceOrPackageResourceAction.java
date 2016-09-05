/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;

/**
 * Deletes the selected components or services or packages of components or services.
 *
 * @author gganea
 */
public class DeleteComponentOrServiceOrPackageResourceAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection selection;
	private final Shell shell;
	private final UserNodeType nodeType;


	public DeleteComponentOrServiceOrPackageResourceAction(Shell shell, String text, UserNodeType nodeType)
	{
		this.shell = shell;
		this.nodeType = nodeType;
		setText(text);
		setToolTipText(text);
	}

	@Override
	public void run()
	{
		if (selection != null && MessageDialog.openConfirm(shell, getText(), "Are you sure you want to delete?"))
		{
			//first save the current selection so that he user can't change it while the job is running
			List<SimpleUserNode> saveTheSelection = new ArrayList<SimpleUserNode>();
			Iterator<SimpleUserNode> it = selection.iterator();
			while (it.hasNext())
				saveTheSelection.add(it.next());
			//start the delete job
			RunInWorkspaceJob deleteJob = new RunInWorkspaceJob(new DeleteComponentOrServiceResourcesWorkspaceJob(saveTheSelection));
			deleteJob.setName("Deleting component or service resources");
			deleteJob.setRule(ServoyModel.getWorkspace().getRoot());
			deleteJob.setUser(false);
			deleteJob.schedule();
		}
	}

	private class DeleteComponentOrServiceResourcesWorkspaceJob implements IWorkspaceRunnable
	{

		private final List<SimpleUserNode> savedSelection;

		public DeleteComponentOrServiceResourcesWorkspaceJob(List<SimpleUserNode> selection)
		{
			savedSelection = selection;
		}

		@Override
		public void run(IProgressMonitor monitor) throws CoreException
		{
			IProject resources = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject();
			Iterator<SimpleUserNode> it = savedSelection.iterator();
			while (it.hasNext())
			{
				SimpleUserNode next = it.next();
				Object realObject = next.getRealObject();
				IResource resource = null;
				if (realObject instanceof IResource)
				{
					resource = (IResource)realObject;

				}
				else if (realObject instanceof IPackageReader)
				{
					resource = SolutionExplorerTreeContentProvider.getResource((IPackageReader)realObject);
				}
				else if (next.getType() == UserNodeType.COMPONENT || next.getType() == UserNodeType.SERVICE || next.getType() == UserNodeType.LAYOUT)
				{
					resource = getComponentFolderToDelete(resources, next);
				}


				if (resource != null)
				{
					try
					{
						if (resource instanceof IFolder)
						{
							resources.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
							deleteFolder((IFolder)resource);
						}
						else
						{
							if (resource instanceof IProject)
							{
								IProject[] referencingProjects = ((IProject)resource).getReferencingProjects();
								for (IProject iProject : referencingProjects)
								{
									RemovePackageProjectReferenceAction.removeProjectReference(iProject, (IProject)resource);
								}
							}
							resource.delete(true, new NullProgressMonitor());
							resources.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}

		private void deleteFolder(IFolder folder) throws CoreException
		{
			for (IResource resource : folder.members())
			{
				if (resource instanceof IFolder)
				{
					deleteFolder((IFolder)resource);
				}
				else
				{
					try
					{
						resource.delete(true, new NullProgressMonitor());
					}
					catch (CoreException e)
					{
						// can go wrong once, try again with a refresh
						resource.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						resource.delete(true, new NullProgressMonitor());
					}
				}
			}
			folder.delete(true, new NullProgressMonitor());
		}

		private IResource getComponentFolderToDelete(IProject resources, SimpleUserNode next)
		{
			WebObjectSpecification spec = (WebObjectSpecification)next.getRealObject();
			IContainer[] dirResource;
			IResource resource = null;
			OutputStream out = null;
			InputStream in = null;
			try
			{
				dirResource = resources.getWorkspace().getRoot().findContainersForLocationURI(spec.getSpecURL().toURI());
				if (dirResource.length == 1 && dirResource[0].getParent().exists()) resource = dirResource[0].getParent();

				if (resource != null)
				{
					IResource parent = resource.getParent();

					IFile m = getFile(parent);
					m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
					Manifest manifest = new Manifest(m.getContents());
					manifest.getEntries().remove(resource.getName() + "/" + resource.getName() + ".spec");
					out = new ByteArrayOutputStream();
					manifest.write(out);
					in = new ByteArrayInputStream(out.toString().getBytes());
					m.setContents(in, IResource.FORCE, new NullProgressMonitor());
				}
			}
			catch (URISyntaxException e)
			{
				ServoyLog.logError(e);
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			finally
			{
				try
				{
					if (out != null) out.close();
					if (in != null) in.close();
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
			}
			return resource;
		}

		private IFile getFile(IResource resource)
		{
			String manifest = "META-INF/MANIFEST.MF";
			if (resource instanceof IFolder) return ((IFolder)resource).getFile(manifest);
			if (resource instanceof IProject) return ((IProject)resource).getFile(manifest);
			return null;

		}

	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		// allow multiple selection
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = true;
		Iterator<SimpleUserNode> it = sel.iterator();
		while (it.hasNext() && state)
		{
			SimpleUserNode node = it.next();
			state = (node.getRealType() == nodeType);
			if (node.getType() == UserNodeType.COMPONENT || node.getType() == UserNodeType.SERVICE || node.getType() == UserNodeType.LAYOUT)
			{
				state = node.getRealObject() instanceof WebObjectSpecification || node.parent.getRealObject() instanceof IFolder ||
					node.parent.getRealObject() instanceof IProject;
			}
		}
		if (state)
		{
			selection = sel;
		}
		setEnabled(state);
	}

}
