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

import java.sql.SQLException;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Creates a new delete table action. It can be used to delete the table given by the selected SimpleUserNode.
 *
 * @author acostescu
 */
public class DeleteInMemTableAction extends AbstractInMemTableAction implements ISelectionChangedListener
{

	/**
	 * Creates a new delete table action.
	 */
	public DeleteInMemTableAction(Shell shell)
	{
		super(shell, "delete", "Deleting");
		setText("Delete in memory datasource");
		setToolTipText("Delete in memory datasource");
	}

	@Override
	protected void doAction(final IServer server, final ITable table) throws SQLException, RepositoryException
	{
		((IServerInternal)server).removeTable(table);
	}

	@Override
	protected void doAction(final IServer server, final ITable table, final ArrayList<String> userSelection) throws RepositoryException
	{
		((MemServer)server).removeTable(table, userSelection);
	}

	@Override
	protected boolean confirm()
	{
		return MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(), "Are you sure you want to delete?");
	}

	@Override
	protected void updateReferencesIfNeeded()
	{
	}
}
