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

package com.servoy.eclipse.ui.search;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Solution;

/**
 * Base persist search implementation of {@link ISearchQuery} 
 * 
 * @author jcompagner
 * @since 6.0
 */
public abstract class AbstractPersistSearch implements ISearchQuery
{

	private FileSearchResult fResult;

	/**
	 * 
	 */
	public AbstractPersistSearch()
	{
		super();
	}

	/**
	 * @return
	 */
	protected TextSearchResultCollector getResultCollector()
	{
		AbstractTextSearchResult searchResult = (AbstractTextSearchResult)getSearchResult();
		searchResult.removeAll();
		return createTextSearchCollector(searchResult);
	}

	/**
	 * @param searchResult
	 * @return
	 */
	protected TextSearchResultCollector createTextSearchCollector(AbstractTextSearchResult searchResult)
	{
		return new TextSearchResultCollector(searchResult);
	}

	/**
	 * @return
	 */
	protected IResource[] getScopes(Solution sol)
	{
		List<IResource> scopes = new ArrayList<IResource>();
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		scopes.add(servoyModel.getServoyProject(sol.getName()).getProject());
		ServoyProject currentProject = servoyModel.getActiveProject();

		searchForReferencedProjects(currentProject, scopes, sol, new HashSet<Solution>());
		return scopes.toArray(new IResource[scopes.size()]);
	}


	/**
	 * @param project
	 * @param scopes
	 * @param hashSet
	 */
	private void searchForReferencedProjects(ServoyProject project, List<IResource> scopes, Solution referencedSolution, HashSet<Solution> processed)
	{
		if (processed.contains(project.getSolution())) return;
		processed.add(project.getSolution());

		Solution[] modules = project.getModules();

		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		for (Solution module : modules)
		{
			if (processed.contains(module)) continue;
			if (module.equals(referencedSolution))
			{
				scopes.add(project.getProject());
			}
			else
			{
				searchForReferencedProjects(servoyModel.getServoyProject(module.getName()), scopes, referencedSolution, processed);
			}
		}
	}

	public boolean canRerun()
	{
		return true;
	}

	public boolean canRunInBackground()
	{
		return true;
	}

	public ISearchResult getSearchResult()
	{
		if (fResult == null)
		{
			fResult = new FileSearchResult(this);
			new SearchResultUpdater(fResult);
		}
		return fResult;
	}

}