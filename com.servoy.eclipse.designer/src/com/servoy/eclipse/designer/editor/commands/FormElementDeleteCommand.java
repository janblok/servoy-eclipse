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
package com.servoy.eclipse.designer.editor.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.wizards.ICheckBoxView;
import com.servoy.eclipse.ui.wizards.SelectAllButtonsBar;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFlattenedPersistWrapper;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;


/**
 * A command to remove elements from a form. The command can be undone or redone.
 */
public class FormElementDeleteCommand extends Command
{
	/** Objects to remove. */
	private IPersist[] children;

	/** Object to remove from. */
	private ISupportChilds[] parents;

	private final Set<Form> subforms = new HashSet<Form>();

	/**
	 * Create a command that will remove the element from its parent.
	 *
	 * @param form the Form containing the child
	 * @param child the element to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public FormElementDeleteCommand(IPersist child)
	{
		this(new IPersist[] { child });
	}

	/**
	 * Create a command that will remove the element from its parent.
	 *
	 * @param form the Form containing the child
	 * @param children the elements to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public FormElementDeleteCommand(IPersist[] children)
	{
		if (children == null)
		{
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < children.length; i++)
		{
			IPersist child = children[i];
			if (child == null || child.getParent() == null)
			{
				throw new IllegalArgumentException();
			}
			else if (child instanceof IFlattenedPersistWrapper)
			{
				children[i] = ((IFlattenedPersistWrapper)child).getWrappedPersist();
			}
		}
		this.children = children;
	}

	private class ConfirmDeleteDialog extends Dialog implements ICheckStateListener, ICheckBoxView
	{
		private final List<IPersist> overridingPersists;
		private CheckboxTableViewer checkboxTableViewer;
		private HashMap<String, List<IPersist>> options;
		Set<IPersist> selected;
		private final String description;
		private SelectAllButtonsBar selectAllButtons;
		public Set<String> items;

		protected ConfirmDeleteDialog(Shell parentShell, String description, List<IPersist> overridingPersists)
		{
			super(parentShell);
			this.description = description;
			this.overridingPersists = overridingPersists;
		}

		@Override
		protected Control createContents(Composite parent)
		{
			GridLayout gridLayout = new GridLayout();
			parent.setLayout(gridLayout);

			Label l = new Label(parent, SWT.NONE);
			l.setText(description);

			options = new HashMap<String, List<IPersist>>();
			checkboxTableViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION);
			checkboxTableViewer.setContentProvider(new IStructuredContentProvider()
			{

				@Override
				public void dispose()
				{
				}

				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
				{
				}

				public Object[] getElements(Object inputElement)
				{
					if (inputElement instanceof List< ? >)
					{
						List<IPersist> persists = (List<IPersist>)inputElement;
						items = new TreeSet<String>();
						int i = 0;
						for (IPersist p : persists)
						{
							String item = ((Form)p.getAncestor(IRepository.FORMS)).getName();
							items.add(item);
							if (!options.containsKey(item))
							{
								options.put(item, new ArrayList<IPersist>());
							}
							options.get(item).add(p);
						}
						return items.toArray();
					}
					return null;
				}
			});
			checkboxTableViewer.setInput(overridingPersists);
			selected = new HashSet<>(overridingPersists);
			gridLayout.numColumns = 2;
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessVerticalSpace = true;
			gridData.grabExcessHorizontalSpace = true;
			gridData.horizontalSpan = 2;
			checkboxTableViewer.getTable().setLayoutData(gridData);
			checkboxTableViewer.addCheckStateListener(this);
			Composite container = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout(2, true);
			container.setLayout(layout);
			selectAllButtons = new SelectAllButtonsBar(this, container);
			checkboxTableViewer.setAllChecked(true);
			selectAllButtons.disableSelectAll();
			return super.createContents(parent);
		}

		@Override
		public void checkStateChanged(CheckStateChangedEvent event)
		{
			if (event.getChecked())
			{
				selected.addAll(options.get(event.getElement()));
			}
			else
			{
				selected.removeAll(options.get(event.getElement()));
			}
			if (checkboxTableViewer.getCheckedElements().length < checkboxTableViewer.getTable().getItemCount())
			{
				selectAllButtons.enableAll();
			}
			if (checkboxTableViewer.getCheckedElements().length == 0)
			{
				selectAllButtons.disableDeselectAll();
			}
			if (checkboxTableViewer.getCheckedElements().length == checkboxTableViewer.getTable().getItemCount())
			{
				selectAllButtons.disableSelectAll();
			}
		}

		@Override
		public void selectAll()
		{
			checkboxTableViewer.setAllChecked(true);
			selected.addAll(overridingPersists);
		}

		@Override
		public void deselectAll()
		{
			checkboxTableViewer.setAllChecked(false);
			selected.removeAll(overridingPersists);
		}

		@Override
		protected void configureShell(Shell newShell)
		{
			super.configureShell(newShell);
			newShell.setText("Element overriden in subforms");
		}

	}

	@Override
	public void execute()
	{
		String label = "delete element";

		ArrayList<IPersist> confirmedChildren = new ArrayList<IPersist>();
		for (IPersist child : children)
		{
			List<IPersist> overriding = getOverridingPersists(child);
			if (!overriding.isEmpty())
			{
				String name = child instanceof ISupportName ? ((ISupportName)child).getName() : "";
				if (name == null) name = child.toString();

				ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"Delete the overidden element '" + name + "' from the following subforms:", overriding);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Window.OK)
				{
					confirmedChildren.addAll(dialog.selected);
					for (IPersist p : dialog.selected)
					{
						Form subform = (Form)p.getAncestor(IRepository.FORMS);
						if (!subform.isChanged()) subforms.add(subform);
					}
				}
				else
				{
					continue;
				}
			}
			confirmedChildren.add(child);
		}
		children = confirmedChildren.toArray(new IPersist[confirmedChildren.size()]);

		if (children.length > 1) label += 's';
		for (IPersist child : children)
		{
			if (child instanceof ISupportName && ((ISupportName)child).getName() != null)
			{
				label += ' ' + ((ISupportName)child).getName();
			}
		}
		setLabel(label);
		if (children.length > 0) redo();
	}

	@Override
	public void redo()
	{
		parents = new ISupportChilds[children.length];
		for (int i = 0; i < children.length; i++)
		{
			// parent may be the form or a tab panel
			parents[i] = children[i].getParent();
			try
			{
				((IDeveloperRepository)children[i].getRootObject().getRepository()).deleteObject(children[i]);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not delete element", e);
			}
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, Arrays.asList(children));
		try
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().saveEditingSolutionNodes(
				subforms.toArray(new IPersist[subforms.size()]), false);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not save forms", e);
		}
	}

	@Override
	public void undo()
	{
		for (int i = children.length - 1; i >= 0; i--)
		{
			try
			{
				((IDeveloperRepository)parents[i].getRootObject().getRepository()).undeleteObject(parents[i], children[i]);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not restore element", e);
			}
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, Arrays.asList(children));
	}

	public IPersist[] getPersists()
	{
		return children;
	}

	private static List<IPersist> getOverridingPersists(IPersist persist)
	{
		List<IPersist> overriding = new ArrayList<IPersist>();

		List<Form> inheriting = getAllSubforms(ServoyModelManager.getServoyModelManager().getServoyModel(), (Form)persist.getAncestor(IRepository.FORMS));
		Collections.sort(inheriting, NameComparator.INSTANCE);
		for (Form child : inheriting)
		{
			Iterator<IPersist> elementsIte = child.getAllObjects();
			while (elementsIte.hasNext())
			{
				addOverriding(overriding, persist, elementsIte.next());
			}
		}
		return overriding;
	}

	private static List<Form> getAllSubforms(ServoyModel servoyModel, Form form)
	{
		List<Form> subforms = new ArrayList<>();
		List<Form> inheriting = servoyModel.getEditingFlattenedSolution(form).getDirectlyInheritingForms(form);
		for (Form child : inheriting)
		{
			subforms.add(child);
			subforms.addAll(getAllSubforms(servoyModel, child));
		}
		return subforms;
	}

	private static void addOverriding(List<IPersist> overriding, IPersist superPersist, IPersist p)
	{
		if (p instanceof ISupportExtendsID && (((ISupportExtendsID)p).getExtendsID() == superPersist.getID()))
		{
			overriding.add(p);
			if (p instanceof ISupportChilds)
			{
				ISupportChilds parent = (ISupportChilds)p;
				for (IPersist child : Utils.iterate(parent.getAllObjects()))
				{
					if (child instanceof ISupportExtendsID && !PersistHelper.isOverrideElement((ISupportExtendsID)child))
					{ // is is an extra child element compared to its super child elements
						overriding.add(child);
					}
					else if (((AbstractBase)child).hasOverrideProperties())
					{
						overriding.add(child);
					}
				}
			}
			else if (p instanceof ISupportChilds)
			{
				ISupportChilds parent = (ISupportChilds)p;
				Iterator<IPersist> childrenIt = parent.getAllObjects();
				while (childrenIt.hasNext())
				{
					addOverriding(overriding, superPersist, childrenIt.next());
				}
			}
		}
	}
}
