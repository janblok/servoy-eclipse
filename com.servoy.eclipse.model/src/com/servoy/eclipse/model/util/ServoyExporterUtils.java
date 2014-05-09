/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.model.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.IMetadataDefManager;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsManager;
import com.servoy.j2db.util.xmlxport.MetadataDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * @author acostache
 *
 */
public class ServoyExporterUtils
{
	private static ServoyExporterUtils svyExportUtlsInstance = null;

	private ServoyExporterUtils()
	{

	}

	public static ServoyExporterUtils getInstance()
	{
		if (svyExportUtlsInstance == null)
		{
			svyExportUtlsInstance = new ServoyExporterUtils();
		}
		return svyExportUtlsInstance;
	}

	/**
	 * This method takes care of minimal db info to be used in export in the case in which the db is down.
	 * It will create and initialize the table def manager and the metadata manager, which will contain server and table and metadata info
	 * to be used at solution export.
	 * 
	 * If the database is not down, it will export all needed (non-minimal) db info, based on dbi files (only). That is, if needed,
	 * it will export all tables from referenced servers, based on dbi files.
	 * 
	 * NOTE: if there are no dbi files created, export info will be empty
	 * 
	 * @param true if the database is down, false otherwise 
	 * 
	 * @throws CoreException
	 * @throws JSONException 
	 * @throws IOException 
	 */
	public Pair<ITableDefinitionsManager, IMetadataDefManager> prepareDbiFilesBasedExportData(Solution activeSolution, boolean exportReferencedModules,
		boolean exportI18NData, boolean exportAllTablesFromReferencedServers, boolean exportMetaData) throws CoreException, JSONException, IOException
	{
		// A. get only the needed servers (and tables) 
		final Map<String, List<String>> neededServersTables = getNeededServerTables(activeSolution, exportReferencedModules, exportI18NData);
		final boolean exportAll = exportAllTablesFromReferencedServers;
		DataModelManager dmm = ServoyModelFinder.getServoyModel().getDataModelManager();

		// B. for needed tables, get dbi files (db is down)
		Map<String, List<IFile>> server_tableDbiFiles = new HashMap<String, List<IFile>>();
		for (Entry<String, List<String>> neededServersTableEntry : neededServersTables.entrySet())
		{
			final String serverName = neededServersTableEntry.getKey();
			final List<String> tables = neededServersTableEntry.getValue();
			IFolder serverInformationFolder = dmm.getDBIFileContainer(serverName);
			final List<IFile> dbiz = new ArrayList<IFile>();
			if (serverInformationFolder.exists())
			{
				serverInformationFolder.accept(new IResourceVisitor()
				{
					public boolean visit(IResource resource) throws CoreException
					{
						String extension = resource.getFileExtension();
						if (extension != null && extension.equalsIgnoreCase(DataModelManager.COLUMN_INFO_FILE_EXTENSION))
						{
							//we found a dbi file
							String tableName = resource.getName().substring(0,
								resource.getName().length() - DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length());
							if (tables.contains(tableName) || exportAll)
							{
								dbiz.add((IFile)resource);
							}
						}
						return true;
					}

				}, IResource.DEPTH_ONE, false);
			}

			// minimum requirement for dbi files based export: all needed dbi files must be found
			if (!allNeededDbiFilesExist(tables, dbiz))
			{
				throw new FileNotFoundException(
					"Could not locate all needed dbi files for server '" + serverName + "'.\nPlease make sure the necessary files exist."); //$NON-NLS-1$ //$NON-NLS-2$
			}

			server_tableDbiFiles.put(serverName, dbiz);
		}

		// C. deserialize table dbis to get tabledefs and metadata info
		Map<String, List<TableDef>> serverTableDefs = new HashMap<String, List<TableDef>>();
		List<MetadataDef> metadataDefs = new ArrayList<MetadataDef>();
		for (Entry<String, List<IFile>> server_tableDbiFile : server_tableDbiFiles.entrySet())
		{
			String serverName = server_tableDbiFile.getKey();
			List<TableDef> tableDefs = new ArrayList<TableDef>();
			for (IFile file : server_tableDbiFile.getValue())
			{
				if (file.exists())
				{
					InputStream is = file.getContents(true);
					String dbiFileContent = null;
					try
					{
						dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF-8"));
					}
					finally
					{
						is = Utils.closeInputStream(is);
					}
					if (dbiFileContent != null)
					{
						TableDef tableInfo = dmm.deserializeTableInfo(dbiFileContent);
						tableDefs.add(tableInfo);
						if (exportMetaData && tableInfo.isMetaData != null && tableInfo.isMetaData.booleanValue())
						{
							String ds = DataSourceUtils.createDBTableDataSource(serverName, tableInfo.name);
							IFile mdf = dmm.getMetaDataFile(ds);
							if (mdf != null && mdf.exists())
							{
								String wscontents = null;
								wscontents = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()).getUTF8Contents(mdf.getFullPath().toString());
								if (wscontents != null)
								{
									MetadataDef mdd = new MetadataDef(ds, wscontents);
									if (!metadataDefs.contains(mdd)) metadataDefs.add(mdd);
								}
							}
						}
					}
				}
			}
			serverTableDefs.put(serverName, tableDefs);
		}

		ITableDefinitionsManager tableDefManager = null;
		IMetadataDefManager metadataDefManager = null;

		// D. make use of tabledef info and metadata for the managers
		tableDefManager = new ITableDefinitionsManager()
		{
			private Map<String, List<TableDef>> serverTableDefsMap = new HashMap<String, List<TableDef>>();

			public void setServerTableDefs(Map<String, List<TableDef>> serverTableDefsMap)
			{
				this.serverTableDefsMap = serverTableDefsMap;
			}

			public Map<String, List<TableDef>> getServerTableDefs()
			{
				return this.serverTableDefsMap;
			}
		};
		tableDefManager.setServerTableDefs(serverTableDefs);

		metadataDefManager = new IMetadataDefManager()
		{
			private List<MetadataDef> metadataDefList = new ArrayList<MetadataDef>();

			public void setMetadataDefsList(List<MetadataDef> metadataDefList)
			{
				this.metadataDefList = metadataDefList;
			}

			public List<MetadataDef> getMetadataDefsList()
			{
				return this.metadataDefList;
			}
		};
		metadataDefManager.setMetadataDefsList(metadataDefs);

		return new Pair<ITableDefinitionsManager, IMetadataDefManager>(tableDefManager, metadataDefManager);
	}

	private boolean allNeededDbiFilesExist(List<String> neededTableNames, List<IFile> existingDbiFiles)
	{
		if (neededTableNames != null && existingDbiFiles != null && neededTableNames.size() > 0 && existingDbiFiles.size() == 0) return false;

		boolean allFound = true;
		List<String> dbiFileNames = new ArrayList<String>(existingDbiFiles.size());
		for (IFile f : existingDbiFiles)
		{
			if (f != null)
			{
				int i = f.getName().lastIndexOf(".dbi"); //$NON-NLS-1$
				dbiFileNames.add(f.getName().substring(0, i));
			}
		}

		for (String tableName : neededTableNames)
		{
			if (!dbiFileNames.contains(tableName))
			{
				allFound = false;
				break;
			}
		}

		return allFound;
	}

	private void addServerTable(Map<String, List<String>> srvTbl, String serverName, String tableName)
	{
		List<String> tablesForServer = srvTbl.get(serverName);
		if (tablesForServer == null)
		{
			tablesForServer = new ArrayList<String>();
		}
		if (!tablesForServer.contains(tableName)) tablesForServer.add(tableName);
		srvTbl.put(serverName, tablesForServer);
	}

	private Map<String, List<String>> getNeededServerTables(Solution mainActiveSolution, boolean includeModules, boolean includeI18NData)
	{
		DataSourceCollectorVisitor collector = new DataSourceCollectorVisitor();
		//get modules to export if needed, or just the active project
		if (includeModules)
		{
			ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProjectWithImportHooks(); // TODO shouldn't this be done selectively/after the developer chooses what modules he wants exported?
			for (ServoyProject module : modules)
			{
				module.getEditingSolution().acceptVisitor(collector);
			}
		}
		else mainActiveSolution.acceptVisitor(collector);

		Map<String, List<String>> neededServersTablesMap = new HashMap<String, List<String>>();
		Set<String> dataSources = collector.getDataSources();
		Set<String> serverNames = DataSourceUtils.getServerNames(dataSources);

		for (String serverName : serverNames)
		{
			for (String tableName : DataSourceUtils.getServerTablenames(dataSources, serverName))
			{
				addServerTable(neededServersTablesMap, serverName, tableName);
			}
		}

		// check if i18n info is needed
		if (mainActiveSolution.getI18nDataSource() != null && includeI18NData) addServerTable(neededServersTablesMap, mainActiveSolution.getI18nServerName(),
			mainActiveSolution.getI18nTableName());

		return neededServersTablesMap;
	}

	/** 
	 * 
	 * @return true if the database is down (servers or tables are inaccessible)
	 */
	public boolean hasDbDownErrorMarkers(String[] projects)
	{
		if (projects != null && projects.length > 0)
		{
			try
			{
				for (String moduleName : projects)
				{
					ServoyProject module = ServoyModelFinder.getServoyModel().getServoyProject(moduleName);
					if (module != null)
					{
						IMarker[] markers = module.getProject().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
						for (IMarker marker : markers)
						{
							// db down errors = missing server (what other cases?)
							if (marker.getAttribute(IMarker.SEVERITY) != null && marker.getAttribute(IMarker.SEVERITY).equals(IMarker.SEVERITY_ERROR) &&
								ServoyBuilder.MISSING_SERVER.equals(marker.getType()))
							{
								return true;
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return false;
	}
}