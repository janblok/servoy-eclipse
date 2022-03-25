/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.StyleClassPropertyType;

import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;

/**
 * @author jcompagner
 * @since 2022.06
 *
 */
public class PropertyWizardDialog extends Dialog
{
	private final Shell parentShell;
	private final PersistContext persistContext;
	private final FlattenedSolution flattenedSolution;
	private final ITable table;
	private final DataProviderOptions dataproviderOptions;
	private final IDialogSettings settings;
	private final Collection<PropertyDescription> wizardProperties;
	private final PropertyDescription property;
	private DataproviderComposite dataproviderComposite;

	/**
	 * @param parentShell
	 */
	public PropertyWizardDialog(Shell parentShell, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions, final IDialogSettings settings, PropertyDescription property, Collection<PropertyDescription> wizardProperties)
	{
		super(parentShell);
		this.parentShell = parentShell;
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.dataproviderOptions = dataproviderOptions;
		this.settings = settings;
		this.property = property;
		this.wizardProperties = wizardProperties;
	}

	@Override
	protected boolean isResizable()
	{
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Property configurator for " + property.getName());

		Composite area = (Composite)super.createDialogArea(parent);
		// first check what the main thing must be (dataprovders, forms, relations?)
		List<PropertyDescription> dataproviderProperties = wizardProperties.stream()
			.filter(prop -> FoundsetLinkedPropertyType.class.isAssignableFrom(prop.getType().getClass()) ||
				DataproviderPropertyType.class.isAssignableFrom(prop.getType().getClass()))
			.sorted((desc1, desc2) -> Integer.parseInt((String)desc1.getTag("wizard")) - Integer.parseInt((String)desc2.getTag("wizard")))
			.collect(Collectors.toList());
		List<PropertyDescription> styleProperties = wizardProperties.stream()
			.filter(prop -> StyleClassPropertyType.class.isAssignableFrom(prop.getType().getClass()))
			.sorted((desc1, desc2) -> Integer.parseInt((String)desc1.getTag("wizard")) - Integer.parseInt((String)desc2.getTag("wizard")))
			.collect(Collectors.toList());
		if (dataproviderProperties.size() > 0 || styleProperties.size() > 0)
		{
			dataproviderComposite = new DataproviderComposite(area, persistContext, flattenedSolution, table, dataproviderOptions, settings,
				dataproviderProperties, styleProperties);
		}
		return area;
	}

	public List<Map<String, Object>> getResult()
	{
		if (dataproviderComposite != null)
		{
			return dataproviderComposite.getResult();
		}
		return Collections.emptyList();
	}

}
