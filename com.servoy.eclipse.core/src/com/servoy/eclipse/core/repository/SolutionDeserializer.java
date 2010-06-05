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
package com.servoy.eclipse.core.repository;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.parser.ISourceParser;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.internal.javascript.parser.JavaScriptSourceParser;
import org.eclipse.dltk.javascript.ast.Argument;
import org.eclipse.dltk.javascript.ast.ArrayInitializer;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.DecimalLiteral;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.NewExpression;
import org.eclipse.dltk.javascript.ast.NullExpression;
import org.eclipse.dltk.javascript.ast.ObjectInitializer;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.Statement;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.ast.Type;
import org.eclipse.dltk.javascript.ast.UnaryOperation;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.javascript.ast.VariableStatement;
import org.eclipse.dltk.javascript.ast.VoidExpression;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;
import org.eclipse.ui.part.FileEditorInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.builder.ErrorKeeper;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RuntimeProperty;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ServerProxy;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.MethodArgument.ArgumentType;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Reads repository (solution) objects from file system directories. (since we don't want to store the parent uuid in each element, we assume a certain aspects
 * from serializer. (such as directory structure and file locations)
 * 
 * @author jblok
 */
public class SolutionDeserializer
{
	private static final String VARIABLE_TYPE_JSON_ATTRIBUTE = "variableType"; //$NON-NLS-1$
	private static final String JS_TYPE_JSON_ATTRIBUTE = "jsType"; //$NON-NLS-1$
	private static final String ARGUMENTS_JSON_ATTRIBUTE = "arguments"; //$NON-NLS-1$
	static final String LINE_NUMBER_OFFSET_JSON_ATTRIBUTE = "lineNumberOffset"; //$NON-NLS-1$
	private static final String COMMENT_JSON_ATTRIBUTE = "comment"; //$NON-NLS-1$
	private static final String CHANGED_JSON_ATTRIBUTE = "changed"; //$NON-NLS-1$

	public static final RuntimeProperty<Boolean> POSSIBLE_DUPLICATE_UUID = new RuntimeProperty<Boolean>()
	{

	};
	private final IDeveloperRepository repository;
	private final ErrorKeeper<File, Exception> errorKeeper;
	private static final Map<UUID, HashSet<UUID>> alreadyUsedUUID = new HashMap<UUID, HashSet<UUID>>();

	public SolutionDeserializer(IDeveloperRepository repository, ErrorKeeper<File, Exception> errorKeeper)
	{
		this.repository = repository;
		this.errorKeeper = errorKeeper;
	}

	public static JSONObject getJSONObject(String content)
	{
		try
		{
			return new ServoyJSONObject(content, true);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error created json object of: " + content, e); //$NON-NLS-1$
		}
		return null;
	}

	public static JSONArray getJSONArray(String content)
	{
		try
		{
			return new ServoyJSONArray(content);
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error created json object of: " + content, e); //$NON-NLS-1$
		}
		return null;
	}

	private static HashSet<UUID> getAlreadyUsedUUIDsForSolution(UUID solutionUUID)
	{
		HashSet<UUID> solutionUUIDs = alreadyUsedUUID.get(solutionUUID);
		if (solutionUUIDs == null)
		{
			solutionUUIDs = new HashSet<UUID>();
			alreadyUsedUUID.put(solutionUUID, solutionUUIDs);
		}

		return solutionUUIDs;
	}

	public Solution readSolution(File workspace_dir, SolutionMetaData smd, List<File> changedFiles, boolean useFilesForDitryMark) throws RepositoryException
	{
		if (smd == null) return null;

		Solution solution = (Solution)repository.createRootObject(smd);
		solution.setChangeHandler(new EclipseChangeHandler(repository));

		HashSet<UUID> solutionUUIDs = getAlreadyUsedUUIDsForSolution(solution.getUUID());
		solutionUUIDs.clear();

		updateSolution(new File(workspace_dir, smd.getName()), solution, changedFiles, null, true, useFilesForDitryMark);

		if (!useFilesForDitryMark)
		{
			// clear all changed flags
			solution.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					o.clearChanged();
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
		}

		return solution;
	}

	/**
	 * Update the solution
	 * 
	 * @param solutionDir
	 * @param solution
	 * @param changedFiles flag objects listed in these files as changed
	 * @param readAll when false, only read object listed in changedFileNames
	 * @return list of files in changedFileNames that have not been visited
	 * @throws RepositoryException
	 */
	public List<File> updateSolution(File solutionDir, final Solution solution, List<File> changedFiles, List<IPersist> strayCats, boolean readAll,
		boolean useFilesForDirtyMark) throws RepositoryException
	{
		if (solution == null) return null;
		try
		{
			if (errorKeeper != null)
			{
				errorKeeper.removeError(solutionDir);
			}
			List<File> changedFilesCopy = null;
			if (changedFiles != null)
			{
				if (errorKeeper != null)
				{
					for (File cf : changedFiles)
					{
						errorKeeper.removeError(cf);
					}
				}
				changedFilesCopy = new ArrayList<File>(changedFiles.size());
				changedFilesCopy.addAll(changedFiles);
			}
			Map<IPersist, JSONObject> persist_json_map = new HashMap<IPersist, JSONObject>();
			readObjectFilesFromSolutionDir(solutionDir, solutionDir, solution, persist_json_map, changedFilesCopy, strayCats, readAll, useFilesForDirtyMark);
			readMediasFromSolutionDir(solutionDir, solution, persist_json_map, changedFilesCopy, strayCats, readAll, useFilesForDirtyMark);
			completePersist(persist_json_map, useFilesForDirtyMark);
			solution.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					if (o.isChanged())
					{
						((EclipseChangeHandler)solution.getChangeHandler()).fireIPersistChanged(o);
					}
					return CONTINUE_TRAVERSAL;
				}

			});
			DataSourceCollectorVisitor datasourceCollector = new DataSourceCollectorVisitor();
			solution.acceptVisitor(datasourceCollector);

			Map<String, IServer> serverProxies = new HashMap<String, IServer>();
			for (String serverName : DataSourceUtils.getServerNames(datasourceCollector.getDataSources()))
			{
				try
				{
					IServer s = repository.getServer(serverName);
					if (s != null)
					{
						serverProxies.put(serverName, new ServerProxy(s));
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}

			solution.setServerProxies(serverProxies);

			return changedFilesCopy; // what remains of the day
		}
		catch (Exception e)
		{
			if (errorKeeper != null)
			{
				// get the innermost exception - the most relevant one for the user
				errorKeeper.addError(solutionDir, new Exception("Please check the .log file for more info.")); //$NON-NLS-1$
			}
			if (e instanceof RepositoryException)
			{
				throw (RepositoryException)e;
			}
			else
			{
				throw new RepositoryException(e);
			}
		}
		finally
		{
			// clear the UUID string/filename cache after loading of the solution.
			persistFileNameCache.clear();
		}
	}

	private void readObjectFilesFromSolutionDir(File solutionDir, File dir, ISupportChilds parent, Map<IPersist, JSONObject> persist_json_map,
		List<File> changedFiles, List<IPersist> strayCats, boolean readAll, boolean useFilesForDirtyMark) throws RepositoryException, JSONException
	{
		if (dir != null && dir.exists())
		{
			List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
			Map<JSONObject, File> fileMap = new HashMap<JSONObject, File>();
			List<File> scriptFiles = new ArrayList<File>();
			List<File> subdirs = new ArrayList<File>();
			String[] files = dir.list();
			Arrays.sort(files, new ObjBeforeJSExtensionComparator());
			Map<File, List<JSONObject>> childrenJSObjectMap = new HashMap<File, List<JSONObject>>(); // js objects from Form & TableNode
			Map<File, ISupportChilds> jsParentFileMap = new HashMap<File, ISupportChilds>(); // keep which js files belong to which parent

			for (final String file : files)
			{
				File f = null;
				try
				{
					// root metadata and medias are read elsewhere
					if (dir.equals(solutionDir) &&
						(file.equals(SolutionSerializer.MEDIAS_DIR) ||
							file.equals(SolutionSerializer.MEDIAS_DIR + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION) || file.equals(SolutionSerializer.ROOT_METADATA)))
					{
						continue;
					}

					f = new File(dir, file);
					if (f.isDirectory())
					{
						subdirs.add(f);
					}
					else
					{
						boolean changed = isChangedFile(solutionDir, f, changedFiles);
						if (readAll || changed || hasSubEntries(f, changedFiles) || hasRelatedEntries(f, changedFiles))
						{
							boolean recognized = false;
							if (SolutionSerializer.isJSONFile(file))
							{
								JSONObject json_obj = new ServoyJSONObject(Utils.getTXTFileContent(f, Charset.forName("UTF8")), true); //$NON-NLS-1$
								if (json_obj.length() == 0)
								{
									// empty file just skip this one.
									continue;
								}
								json_obj.put(CHANGED_JSON_ATTRIBUTE, changed);
								jsonObjects.add(json_obj);
								fileMap.put(json_obj, f);
								recognized = true;
							}
							else if (file.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
							{
								List<JSONObject> scriptObjects = parseJSFile(f, changed);
								File parentFile = f.getParentFile();
								if (parentFile.getName().equals(SolutionSerializer.FORMS_DIR) ||
									(parentFile.getParentFile() != null && parentFile.getParentFile().getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME)))
								{
									childrenJSObjectMap.put(f, scriptObjects);
								}
								else
								{
									// old structure parsing									
									testDuplicates(parent, scriptObjects);
									if (scriptObjects != null)
									{
										jsonObjects.addAll(scriptObjects);
										scriptFiles.add(f);
										jsParentFileMap.put(f, parent);
									}
									if (scriptObjects != null)
									{
										for (JSONObject object : scriptObjects)
										{
											fileMap.put(object, f);
										}
									}
								}
								recognized = true;
							}
							if (changedFiles != null && recognized)
							{
								changedFiles.remove(f);
							}
						}
					}
					// skip all other files
				}
				catch (JSONException e)
				{
					// skip this file
					if (f != null && errorKeeper != null) errorKeeper.addError(f, e);
					ServoyLog.logError("Invalid JSON syntax in file " + f, e); //$NON-NLS-1$
				}
			}

			Set<UUID> saved = new HashSet<UUID>();
			Map<File, IPersist> persistFileMap = new HashMap<File, IPersist>();
			for (JSONObject object : jsonObjects)
			{
				setMissingTypeOnScriptObject(object, parent);
				File file = fileMap.get(object);
				IPersist persist = null;
				try
				{
					persist = deserializePersist(repository, parent, persist_json_map, object, strayCats, file, saved, useFilesForDirtyMark);
				}
				catch (JSONException e)
				{
					ServoyLog.logError("Could not read json object from file " + file + " -- skipping", e); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Could not read json object from file " + file + " -- skipping", e); //$NON-NLS-1$//$NON-NLS-2$
				}
				if (persist != null)
				{
					saved.add(persist.getUUID());
					if (file != null)
					{
						persistFileMap.put(file, persist);
					}
				}
			}

			// parse the forms/tablenodes js objects
			Iterator<Entry<File, List<JSONObject>>> childrenJSObjectMapIte = childrenJSObjectMap.entrySet().iterator();
			Entry<File, List<JSONObject>> childrenJSObjectMapEntry;
			File jsonFile, jsFile;
			while (childrenJSObjectMapIte.hasNext())
			{
				childrenJSObjectMapEntry = childrenJSObjectMapIte.next();
				jsFile = childrenJSObjectMapEntry.getKey();
				String jsFileName = jsFile.getName();
				if (jsFileName.endsWith(SolutionSerializer.JS_FILE_EXTENSION))
				{
					if (jsFile.getParentFile().getName().equals(SolutionSerializer.FORMS_DIR))
					{
						jsonFile = new File(jsFile.getParent(), jsFileName.substring(0, jsFileName.length() - SolutionSerializer.JS_FILE_EXTENSION.length()) +
							SolutionSerializer.FORM_FILE_EXTENSION);
					}
					else
					{
						// tablenode
						jsonFile = new File(jsFile.getParent(), jsFileName.substring(0, jsFileName.length() -
							SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT.length()) +
							SolutionSerializer.TABLENODE_FILE_EXTENSION);
					}

					if (jsonFile.exists())
					{
						ISupportChilds parentForm = (ISupportChilds)persistFileMap.get(jsonFile);
						List<JSONObject> childrenJSObjects = childrenJSObjectMapEntry.getValue();
						testDuplicates(parentForm, childrenJSObjects);

						if (childrenJSObjects != null)
						{
							scriptFiles.add(jsFile);
							for (JSONObject object : childrenJSObjects)
							{
								setMissingTypeOnScriptObject(object, parentForm);
								IPersist persist = null;
								try
								{
									persist = deserializePersist(repository, parentForm, persist_json_map, object, strayCats, jsFile, saved,
										useFilesForDirtyMark);
								}
								catch (JSONException e)
								{
									ServoyLog.logError("Could not read json object from file " + jsFile + " -- skipping", e); //$NON-NLS-1$ //$NON-NLS-2$
								}
								catch (RepositoryException e)
								{
									ServoyLog.logError("Could not read json object from file " + jsFile + " -- skipping", e); //$NON-NLS-1$//$NON-NLS-2$
								}
								if (persist != null)
								{
									saved.add(persist.getUUID());
								}
							}
							if (jsFile != null)
							{
								jsParentFileMap.put(jsFile, parentForm);
							}
						}
					}
				}
			}

			// check for lost children (stray cats blues)
			for (File scriptFile : scriptFiles)
			{
				if (jsParentFileMap.containsKey(scriptFile))
				{
					ISupportChilds jsParent = jsParentFileMap.get(scriptFile);
					for (IPersist child : Utils.asArray(jsParent.getAllObjects(), IPersist.class))
					{
						if (scriptFile.getName().equals(getFileName(child)) && !saved.contains(child.getUUID()))
						{
							jsParent.removeChild(child);
							if (strayCats != null)
							{
								strayCats.add(child);
							}
						}
					}
				}
			}

			// a parent that has children in a subdirectory will always have the same name as the directory,
			// example: forms/orders.obj describes the form and forms/orders/*.obj describe the elements.
			for (File subdir : subdirs)
			{
				// check for new parent
				File subdirPersistFile = new File(dir, subdir.getName() + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION);
				IPersist subdirPersist = persistFileMap.get(subdirPersistFile);
				if (subdirPersist != null || isContainerDir(parent, solutionDir, subdir))
				{
					ISupportChilds newParent = (subdirPersist instanceof ISupportChilds) ? (ISupportChilds)subdirPersist : parent;
					readObjectFilesFromSolutionDir(solutionDir, subdir, newParent, persist_json_map, changedFiles, strayCats, readAll, useFilesForDirtyMark);
				}
			}
		}
	}

	/**
	 * @param parent
	 * @param subdir
	 * @return
	 */
	private boolean isContainerDir(ISupportChilds parent, File solutionDir, File subdir)
	{
		if (parent instanceof Solution)
		{
			File parentDir = subdir.getParentFile();
			if (parentDir.equals(solutionDir))
			{
				// main solution directory
				return SolutionSerializer.FORMS_DIR.equals(subdir.getName()) || SolutionSerializer.RELATIONS_DIR.equals(subdir.getName()) ||
					SolutionSerializer.VALUELISTS_DIR.equals(subdir.getName()) || SolutionSerializer.DATASOURCES_DIR_NAME.equals(subdir.getName());
			}
			if (parentDir.getParentFile().equals(solutionDir) && SolutionSerializer.DATASOURCES_DIR_NAME.equals(parentDir.getName()))
			{
				// a subdirectory of the datasources directory (mysol/datasources/myserver) where table node files are stored
				return true;
			}
		}
		return false;
	}

	/**
	 * @param parent
	 * @param scriptObjects
	 */
	private void testDuplicates(ISupportChilds parent, List<JSONObject> scriptObjects)
	{
		if (scriptObjects != null)
		{
			HashMap<String, JSONObject> uuidToJson = new HashMap<String, JSONObject>(scriptObjects.size());
			for (JSONObject object : scriptObjects)
			{
				if (object.has(SolutionSerializer.PROP_UUID) && object.has(SolutionSerializer.PROP_NAME))
				{
					String uuid = object.optString(SolutionSerializer.PROP_UUID);
					JSONObject duplicate = uuidToJson.put(uuid, object);
					if (duplicate != null)
					{
						try
						{
							IPersist persist = parent.getChild(UUID.fromString(uuid));
							if (persist instanceof ISupportName)
							{
								String name = ((ISupportName)persist).getName();
								if (duplicate.optString(SolutionSerializer.PROP_NAME).equals(name))
								{
									object.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
									uuidToJson.put(uuid, duplicate);
								}
								else
								{
									duplicate.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
								}
							}
							else
							{
								// if the uuid is a copy from a complete other place.
								// then just put 2 new uuid in it.
								object.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
								duplicate.put(SolutionSerializer.PROP_UUID, UUID.randomUUID().toString());
							}
						}
						catch (JSONException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Check if file is a subdir-file and child elements are in the files list.
	 * <p>
	 * For example, file mysolution/forms/orders/button.obj is a sub-entry of mysolution/forms/orders.obj
	 * 
	 * @param file
	 * @param files
	 * @return
	 */
	private boolean hasSubEntries(File file, List<File> files)
	{
		if (!file.getName().endsWith(SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION)) return false;

		if (file.getName().equals(SolutionSerializer.SOLUTION_SETTINGS)) return true;

		String dirPath = file.getPath().substring(0, file.getPath().length() - SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION.length()) + File.separatorChar;
		for (File f : files)
		{
			if (f.getPath().startsWith(dirPath))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if file is a form or tablenode file and child elements are in the files list.
	 * <p>
	 * For example, file mysolution/forms/orders.js is a sub-entry of mysolution/forms/orders.obj
	 * 
	 * @param file
	 * @param files
	 * @return
	 */
	private boolean hasRelatedEntries(File file, List<File> files)
	{
		String dirPath = null;

		if (file.getName().endsWith(SolutionSerializer.FORM_FILE_EXTENSION))
		{
			dirPath = file.getPath().substring(0, file.getPath().length() - SolutionSerializer.FORM_FILE_EXTENSION.length()) +
				SolutionSerializer.JS_FILE_EXTENSION;
		}
		else if (file.getName().endsWith(SolutionSerializer.TABLENODE_FILE_EXTENSION))
		{
			dirPath = file.getPath().substring(0, file.getPath().length() - SolutionSerializer.TABLENODE_FILE_EXTENSION.length()) +
				SolutionSerializer.CALCULATIONS_POSTFIX_WITH_EXT;
		}
		else
		{
			return false;
		}

		String filePath;
		for (File f : files)
		{
			filePath = f.getPath();
			if (filePath.equals(dirPath))
			{
				return true;
			}
		}
		return false;
	}


	private void setMissingTypeOnScriptObject(JSONObject object, ISupportChilds parent) throws JSONException
	{
		if (!object.has(SolutionSerializer.PROP_TYPEID))
		{
			if (parent instanceof Form)
			{
				if (object.has("declaration")) //$NON-NLS-1$
				{
					object.put(SolutionSerializer.PROP_TYPEID, IRepository.METHODS);
				}
				else
				{
					object.put(SolutionSerializer.PROP_TYPEID, IRepository.SCRIPTVARIABLES);
				}
			}
			else if (parent instanceof Solution)
			{
				if (object.has("declaration")) //$NON-NLS-1$
				{
					object.put(SolutionSerializer.PROP_TYPEID, IRepository.METHODS);
				}
				else
				{
					object.put(SolutionSerializer.PROP_TYPEID, IRepository.SCRIPTVARIABLES);
				}
			}
			else if (parent instanceof TableNode)
			{
				object.put(SolutionSerializer.PROP_TYPEID, IRepository.SCRIPTCALCULATIONS);
			}
		}
	}

	private void readMediasFromSolutionDir(File dir, Solution parent, Map<IPersist, JSONObject> persist_json_map, List<File> changedFiles,
		List<IPersist> strayCats, boolean readAll, boolean useFilesForDirtyMark) throws JSONException, RepositoryException
	{
		if (dir.exists())
		{
			File fmediasobjects = new File(dir, SolutionSerializer.MEDIAS_DIR + SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION);
			if (!readAll && (changedFiles == null /* || !changedFiles.contains(fmediasobjects) */)) // it is possible that only the media content has been updated
			{
				// no changes in medias
				return;
			}
			if (changedFiles != null)
			{
				changedFiles.remove(fmediasobjects);
			}

			Set<UUID> mediaUUIDS = new HashSet<UUID>();

			File mediasDir = new File(dir, SolutionSerializer.MEDIAS_DIR);
			String mediasobjects = Utils.getTXTFileContent(fmediasobjects);
			if (mediasobjects != null)
			{
				JSONArray array = new JSONArray(mediasobjects);
				for (int i = 0; i < array.length(); i++)
				{
					if (!array.isNull(i))
					{
						JSONObject obj = array.getJSONObject(i);
						String name = obj.has(SolutionSerializer.PROP_NAME) ? obj.getString(SolutionSerializer.PROP_NAME) : null;
						if (name != null)
						{
							boolean newMedia = parent.getMedia(name) == null;
							IPersist persist = deserializePersist(repository, parent, persist_json_map, obj, strayCats, mediasDir, new HashSet<UUID>(0),
								useFilesForDirtyMark);
							if (persist instanceof Media)
							{
								File mf = new File(mediasDir, name);
								if (mf.exists())
								{
									mediaUUIDS.add(persist.getUUID());
									boolean changed = newMedia || isChangedFile(dir, mf, changedFiles);
									if (readAll || changed)
									{
										((Media)persist).setPermMediaData(Utils.getFileContent(mf));
										if (obj.has(SolutionSerializer.PROP_MIME_TYPE)) ((Media)persist).setMimeType(obj.getString(SolutionSerializer.PROP_MIME_TYPE));
										obj.put(CHANGED_JSON_ATTRIBUTE, changed);
										if (changed)
										{
											persist.flagChanged();
											if (changedFiles != null)
											{
												changedFiles.remove(mf);
											}
										}
									}
								}
							}
						}
					}
				}
			}

			// find all media persists that are no longer listed in the medias.obj file
			for (IPersist media : Utils.asArray(parent.getMedias(false), Media.class))
			{
				if (!mediaUUIDS.contains(media.getUUID()))
				{
					parent.removeChild(media);
					if (strayCats != null)
					{
						strayCats.add(media);
					}
				}
			}

		}
	}

	@SuppressWarnings("nls")
	private List<JSONObject> parseJSFile(File file, boolean markAsChanged) throws JSONException
	{
		String fileContent = Utils.getTXTFileContent(file, Charset.forName("UTF8")); //$NON-NLS-1$
		if (fileContent == null) return Collections.<JSONObject> emptyList();

		StringBuilder sbfileContent = null;
		int lastIndex = 0;
		for (int i = 0; i < fileContent.length(); i++)
		{
			if (fileContent.charAt(i) == '\r')
			{
				if (sbfileContent == null)
				{
					sbfileContent = new StringBuilder(fileContent.length());
				}
				sbfileContent.append(fileContent.substring(lastIndex, i));
				lastIndex = i + 1;
			}
		}
		if (sbfileContent != null)
		{
			sbfileContent.append(fileContent.substring(lastIndex));
			fileContent = sbfileContent.toString();
		}
		try
		{
			List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
			JavaScriptParser parser = new JavaScriptParser();
			parser.setTypeInformationEnabled(true);
			final ArrayList<IProblem> problems = new ArrayList<IProblem>();
			IProblemReporter reporter = new IProblemReporter()
			{

				public Object getAdapter(Class adapter)
				{
					return null;
				}

				public void reportProblem(IProblem problem)
				{
					if (problem.isError())
					{
						problems.add(problem);
					}
				}
			};

			Script script = parser.parse(fileContent, reporter);
			if (problems.size() > 0)
			{
				// if there are problems with this parser, try the rhino parser
				IProblem[] problemsArray = new IProblem[problems.size()];
				problems.toArray(problemsArray);
				problems.clear();
				ISourceParser p = new JavaScriptSourceParser();
				final String content = fileContent;
				p.parse(new IModuleSource()
				{

					public String getFileName()
					{
						return null;
					}

					public String getSourceContents()
					{
						return content;
					}

					public IModelElement getModelElement()
					{
						return null;
					}

					public char[] getContentsAsCharArray()
					{
						return content.toCharArray();
					}
				}, reporter);
				if (problems.size() == 0)
				{
					// rhino didn't have problems, report this!
					StringBuilder sb = new StringBuilder();
					sb.append("AST Parser found problems in the file: " + file.getAbsolutePath()); //$NON-NLS-1$
					for (IProblem problem : problemsArray)
					{
						sb.append(", message: "); //$NON-NLS-1$
						sb.append(problem.getMessage());
						sb.append(", linenumber: "); //$NON-NLS-1$
						sb.append(problem.getSourceLineNumber());
						sb.append(" position( "); //$NON-NLS-1$
						sb.append(problem.getSourceStart());
						sb.append(","); //$NON-NLS-1$
						sb.append(problem.getSourceEnd());
						sb.append(")"); //$NON-NLS-1$
					}

					Debug.error(sb.toString());
				}
				return null;
			}
			if (script == null)
			{
				Debug.error("No script returned when parsing " + file.getAbsolutePath()); //$NON-NLS-1$
				return null;
			}


			ArrayList<VariableDeclaration> variables = new ArrayList<VariableDeclaration>();
			ArrayList<FunctionStatement> functionss = new ArrayList<FunctionStatement>();
			List<Statement> statements = script.getStatements();
			for (ASTNode node : statements)
			{
				if (node instanceof VoidExpression)
				{
					Expression exp = ((VoidExpression)node).getExpression();
					if (exp instanceof VariableStatement)
					{
						variables.addAll(((VariableStatement)exp).getVariables());
					}
					else if (exp instanceof FunctionStatement)
					{
						functionss.add((FunctionStatement)exp);
					}

				}
			}

			List<Comment> comments = script.getComments();

			ArrayList<Line> lines = new ArrayList<Line>();
			int counter = 0;
			Line currentLine = new Line(0, 0);
			lines.add(currentLine);
			while (counter < fileContent.length())
			{
				if (fileContent.charAt(counter) == '\n')
				{
					currentLine = new Line(currentLine.line + 1, counter + 1);
					lines.add(currentLine);
				}
				counter++;
			}
			//add an extra last line.
			lines.add(new Line(currentLine.line + 1, counter + 1));
			for (VariableDeclaration field : variables)
			{
				String comment = null;
				int start = (field.getParent() instanceof VariableStatement) ? field.getParent().sourceStart() - 1 : field.sourceStart() - 1;
				for (int i = 0; i < comments.size(); i++)
				{
					Comment cmt = comments.get(i);
					if (cmt.sourceEnd() == start)
					{
						comment = cmt.getText();
						comments.remove(i);
						break;
					}
				}
				boolean newField = true;
				JSONObject json = null;
				if (comment != null)
				{
					int prop_idx = comment.indexOf(SolutionSerializer.PROPERTIESKEY);
					if (prop_idx != -1)
					{
						int prop_newline_idx = comment.indexOf('}', prop_idx);
						if (prop_newline_idx < comment.length() && prop_newline_idx >= prop_idx + SolutionSerializer.PROPERTIESKEY.length())
						{
							String sobj = comment.substring(prop_idx + SolutionSerializer.PROPERTIESKEY.length(), prop_newline_idx + 1);
							json = new ServoyJSONObject(sobj, false);
							newField = false;
						}
						else
						{
							ServoyLog.logError("Invalid properties comment, ignoring:\n" + comment, null); //$NON-NLS-1$
						}
					}
				}
				if (json == null)
				{
					json = new ServoyJSONObject();
				}

				Identifier ident = field.getIdentifier();
				json.put(SolutionSerializer.PROP_NAME, ident.getName());
				Expression code = field.getInitializer();
				Type type = field.getType();
				if (type != null)
				{
					json.putOpt(JS_TYPE_JSON_ATTRIBUTE, type.getName());
				}

				if (code != null)
				{
					String value_part = fileContent.substring(code.sourceStart(), code.sourceEnd());
					if (value_part.endsWith(";")) value_part = value_part.substring(0, value_part.length() - 1); //$NON-NLS-1$
					if (code instanceof UnaryOperation)
					{
						code = ((UnaryOperation)code).getExpression();
					}
					if (code instanceof DecimalLiteral)
					{
						int variableType = json.optInt(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.TEXT);
						try
						{
							Integer.parseInt(value_part);
							if (variableType != IColumnTypes.NUMBER) json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.INTEGER);
						}
						catch (NumberFormatException e)
						{
							try
							{
								Double.parseDouble(value_part);
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.NUMBER);
							}
							catch (NumberFormatException e2)
							{
								// ignore shouldnt happen
								if (json.has(VARIABLE_TYPE_JSON_ATTRIBUTE))
								{
									if (variableType == IColumnTypes.INTEGER || variableType == IColumnTypes.NUMBER)
									{
										json.remove(VARIABLE_TYPE_JSON_ATTRIBUTE);
									}
								}
							}
						}
					}
					else if (code instanceof StringLiteral)
					{
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.TEXT);
					}
					else if (code instanceof NullExpression)
					{
						if (type != null)
						{
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, getServoyType(type.getName()));
						}
						else if (newField)
						{
							json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
						}
					}
					else if (code instanceof CallExpression)
					{
						ASTNode callExpression = ((CallExpression)code).getExpression();
						if (callExpression instanceof NewExpression)
						{
							String objectclass = null;
							Expression objectClassExpression = ((NewExpression)callExpression).getObjectClass();
							if (objectClassExpression instanceof Identifier)
							{
								objectclass = ((Identifier)objectClassExpression).getName();
							}
							if ("String".equals(objectclass)) //$NON-NLS-1$
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.TEXT);
							}
							else if ("Date".equals(objectclass)) //$NON-NLS-1$
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.DATETIME);
							}
							else
							{
								json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
							}
						}
					}
					else if (code instanceof FunctionStatement || code instanceof ObjectInitializer || code instanceof ArrayInitializer)
					{
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
					}
					else
					{
						Debug.log("Unknow expression falling back to media: " + code.getClass()); //$NON-NLS-1$
						json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, IColumnTypes.MEDIA);
					}
					json.put("defaultValue", value_part); //$NON-NLS-1$
				}
				else if (type != null)
				{
					json.put(VARIABLE_TYPE_JSON_ATTRIBUTE, getServoyType(type.getName()));
				}

				int linenr = 1;
				int fieldLineIndex = field.sourceStart();
				for (Line line : lines)
				{
					if (line.start > fieldLineIndex)
					{
						linenr = line.line;
						break;
					}
				}

				json.put(LINE_NUMBER_OFFSET_JSON_ATTRIBUTE, linenr);
				json.put(COMMENT_JSON_ATTRIBUTE, comment);
				json.put(CHANGED_JSON_ATTRIBUTE, markAsChanged);
				jsonObjects.add(json);
			}

			for (FunctionStatement function : functionss)
			{
				String comment = null;
				int start = function.sourceStart() - 1;
				for (int i = 0; i < comments.size(); i++)
				{
					Comment cmt = comments.get(i);
					if (cmt.sourceEnd() == start)
					{
						comment = cmt.getText();
						comments.remove(i);
						break;
					}
				}
				JSONObject json = null;
				if (comment != null)
				{
					if (!comment.startsWith("/**") && comment.startsWith("/*")) //$NON-NLS-1$ //$NON-NLS-2$
					{
						comment = "/*" + comment.substring(1); //$NON-NLS-1$
					}
					int prop_idx = comment.indexOf(SolutionSerializer.PROPERTIESKEY);
					if (prop_idx != -1)
					{
						int prop_newline_idx = comment.indexOf('}', prop_idx);
						String sobj = comment.substring(prop_idx + SolutionSerializer.PROPERTIESKEY.length(), prop_newline_idx + 1);
						json = new ServoyJSONObject(sobj, false);
					}
				}
				else
				{
					comment = ""; //$NON-NLS-1$
				}
				if (json == null)
				{
					json = new ServoyJSONObject();
				}

				json.put(SolutionSerializer.PROP_NAME, function.getName().getName());

				if ("".equals(comment)) //$NON-NLS-1$
				{
					json.put("declaration", fileContent.substring(function.sourceStart(), function.sourceEnd()) + '\n'); //$NON-NLS-1$
				}
				else
				{
					json.put("declaration", comment.trim() + '\n' + fileContent.substring(function.sourceStart(), function.sourceEnd()) + '\n'); //$NON-NLS-1$
				}
//				json.put("filename", file.getAbsolutePath()); //$NON-NLS-1$


				int linenr = 1;
				int functionLineIndex = function.sourceStart();
				for (Line line : lines)
				{
					if (line.start > functionLineIndex)
					{
						linenr = line.line;
						break;
					}
				}
				Type type = function.getReturnType();
				if (type != null)
				{
					json.putOpt(JS_TYPE_JSON_ATTRIBUTE, type.getName());
				}


				json.put(ARGUMENTS_JSON_ATTRIBUTE, (Object)function.getArguments());
				json.put(LINE_NUMBER_OFFSET_JSON_ATTRIBUTE, linenr);
				json.put(COMMENT_JSON_ATTRIBUTE, comment);
				json.put(CHANGED_JSON_ATTRIBUTE, markAsChanged);
				jsonObjects.add(json);
			}
			return jsonObjects;
		}
		catch (RuntimeException e)
		{
			// if there is a runtime exception throw then something in the parsing did go wrong.
			// then this js file will be skipped.
			ServoyLog.logWarning("Javascript file '" + file + "' had a parsing error ", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	/**
	 * @param json
	 * @param name
	 * @throws JSONException
	 */
	@SuppressWarnings("nls")
	private int getServoyType(String name)
	{
		if ("String".equals(name))
		{
			return IColumnTypes.TEXT;
		}
		else if ("Date".equals(name))
		{
			return IColumnTypes.DATETIME;
		}
		else if ("Number".equals(name))
		{
			return IColumnTypes.NUMBER;
		}
		else if ("Integer".equals(name))
		{
			return IColumnTypes.INTEGER;
		}
		else
		{
			return IColumnTypes.MEDIA;
		}
	}

	// cache for expensive UUID->string creation.
	private static final Map<IPersist, String> persistFileNameCache = new HashMap<IPersist, String>(512);

	private static String getFileName(IPersist persist)
	{
		String filename = persistFileNameCache.get(persist);
		if (filename == null)
		{
			filename = SolutionSerializer.getFileName(persist, false);
			persistFileNameCache.put(persist, filename);
		}
		return filename;
	}

	@SuppressWarnings("nls")
	public static IPersist deserializePersist(IDeveloperRepository repository, final ISupportChilds parent, Map<IPersist, JSONObject> persist_json_map,
		JSONObject obj, final List<IPersist> strayCats, File file, Set<UUID> saved, boolean useFilesForDirtyMark) throws RepositoryException, JSONException
	{
		if (!obj.has(SolutionSerializer.PROP_TYPEID))
		{
			ServoyLog.logError("The json object couldnt be deserialized into a persist: " + obj + " on parent: " + parent, null); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}

		HashSet<UUID> solutionUUIDs = getAlreadyUsedUUIDsForSolution(parent.getRootObject().getUUID());

		IPersist existingNode = null;
		UUID uuid;
		boolean scriptUUIDNotFound = false;
		if (obj.has(SolutionSerializer.PROP_UUID))
		{
			try
			{
				uuid = UUID.fromString(obj.getString(SolutionSerializer.PROP_UUID));
			}
			catch (Exception e)
			{
				// object has corrupt uuid, generate a new one so that the object can at least be saved
				ServoyLog.logError("Could not parse UUID -- generating new uuid", e);
				uuid = UUID.randomUUID();
			}
			existingNode = AbstractRepository.searchPersist(parent, uuid);

			if (existingNode == null && file != null && !file.getPath().endsWith(SolutionSerializer.JS_FILE_EXTENSION) &&
				!SolutionSerializer.isCompositeWithItems(parent))
			{
				// check if another persists exists linked to the same file, this can happen when the uuid has been updated
				// Note that this is only applicable if the persist has its own file
				final String fileName = file.getName();
				final String parentDirName = file.getParentFile().getName();
				final String parentRelativePath = SolutionSerializer.getRelativePath(parent, false);
				IPersist persistInSameFile = (IPersist)parent.acceptVisitor(new IPersistVisitor()
				{
					public Object visit(IPersist o)
					{
						if (o == parent)
						{
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
						if (fileName.equals(getFileName(o)))
						{
							String relativePath = SolutionSerializer.getRelativePath(o, false);
							if (relativePath.replace(parentRelativePath, "").startsWith(parentDirName)) //$NON-NLS-1$
							{
								// must make sure also the same parent dir
								// updated persist in same file
								return o;
							}
						}
						// just check the immediate children only
						return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
				});
				if (persistInSameFile != null)
				{
					// updated persist in same file (uuid changed), add to strayCats and remove
					persistInSameFile.getParent().removeChild(persistInSameFile);
					if (strayCats != null) strayCats.add(persistInSameFile);
				}
			}
			else if (existingNode == null && file.getPath().endsWith(SolutionSerializer.JS_FILE_EXTENSION))
			{
				scriptUUIDNotFound = true;
			}
		}
		else
		{
			uuid = UUID.randomUUID();
		}

		IPersist retval = null;
		if (existingNode == null)
		{
			// check if there is already a child with that name and not with that uuid (then it is an incoming uuid change)
			// so that child should be used. Else we will have 2 childs with the same name but different uuids.
			if (obj.has(SolutionSerializer.PROP_NAME))
			{
				int objectTypeId = obj.getInt(SolutionSerializer.PROP_TYPEID);
				String name = obj.getString(SolutionSerializer.PROP_NAME);
				Iterator<IPersist> allObjects = parent.getAllObjects();
				while (allObjects.hasNext())
				{
					IPersist persist = allObjects.next();
					if (persist.getTypeID() == objectTypeId && persist instanceof ISupportName && name.equals(((ISupportName)persist).getName()))
					{
						retval = persist;
						if (scriptUUIDNotFound)
						{
							// scriptUUID wasnt found previously. let this persist that maps with its name use the uuid from the file
							// so that overwrite and update from a team provider really overwrites it
							((AbstractBase)persist).resetUUID(uuid);
							((AbstractBase)persist).setRuntimeProperty(POSSIBLE_DUPLICATE_UUID, Boolean.TRUE);
						}
						break;
					}
				}
			}
			if (retval == null)
			{
				retval = createPersistInParent(parent, repository, obj, uuid);
				if (scriptUUIDNotFound && solutionUUIDs.contains(uuid))
				{
					((AbstractBase)retval).setRuntimeProperty(POSSIBLE_DUPLICATE_UUID, Boolean.TRUE);
				}
			}
		}
		else
		{
			retval = existingNode;
			String fileName = getFileName(retval);
			if (file != null && !fileName.equals(file.getName()) && SolutionSerializer.isJSONFile(fileName))
			{
				((AbstractBase)retval).setRuntimeProperty(POSSIBLE_DUPLICATE_UUID, Boolean.TRUE);
			}
		}

		solutionUUIDs.add(uuid);

		if (file != null)
		{
			((AbstractBase)retval).setSerializableRuntimeProperty(IScriptProvider.FILENAME, file.getAbsolutePath());
		}
		persist_json_map.put(retval, obj);

		if (retval instanceof ISupportChilds && SolutionSerializer.isCompositeWithItems(retval))
		{
			Set<UUID> newChildUUIDs = new HashSet<UUID>();
			if (obj.has(SolutionSerializer.PROP_ITEMS))
			{
				JSONArray items = obj.getJSONArray(SolutionSerializer.PROP_ITEMS);
				for (int i = 0; i < items.length(); i++)
				{
					JSONObject child_obj = items.getJSONObject(i);
					if (SolutionSerializer.isCompositeWithItems(retval) && obj.has(CHANGED_JSON_ATTRIBUTE) &&
						Utils.getAsBoolean(obj.get(CHANGED_JSON_ATTRIBUTE))) child_obj.put(CHANGED_JSON_ATTRIBUTE, true);

					IPersist newChild = deserializePersist(repository, (ISupportChilds)retval, persist_json_map, child_obj, strayCats, file, saved,
						useFilesForDirtyMark);
					if (newChild != null) newChildUUIDs.add(newChild.getUUID());
				}
			}

			List<IPersist> itemsToRemove = new ArrayList<IPersist>();
			// check for lost children
			Iterator<IPersist> it = ((ISupportChilds)retval).getAllObjects();
			while (it.hasNext())
			{
				IPersist ch = it.next();
				if (!newChildUUIDs.contains(ch.getUUID()) && SolutionSerializer.isCompositeItem(ch))
				{
					if (strayCats != null)
					{
						strayCats.add(ch);
					}
					itemsToRemove.add(ch);
					//it.remove() cannot remove on unmodifiable list, should use removeChild later on
				}
			}
			Iterator<IPersist> removalIterator = itemsToRemove.iterator();
			while (removalIterator.hasNext())
			{
				IPersist persist = removalIterator.next();
				persist.getParent().removeChild(persist);
			}
		}

		if (useFilesForDirtyMark) handleChanged(obj, retval);
		return retval;
	}

	private static IPersist createPersistInParent(ISupportChilds parent, IDeveloperRepository repository, JSONObject obj, UUID uuid)
		throws RepositoryException, JSONException
	{
		int objectTypeId = obj.getInt(SolutionSerializer.PROP_TYPEID);
		IPersist retval = null;
		if (objectTypeId == IRepository.SOLUTIONS)
		{
			retval = parent.getRootObject();
		}
		else
		{
			int element_id = repository.getElementIdForUUID(uuid);
			retval = repository.createObject(parent, objectTypeId, element_id, uuid);
			parent.addChild(retval);
		}
		return retval;
	}

	private static void handleChanged(JSONObject obj, IPersist retval) throws JSONException
	{
		retval.setRevisionNumber(-1);
		if (obj.has(CHANGED_JSON_ATTRIBUTE) && Utils.getAsBoolean(obj.get(CHANGED_JSON_ATTRIBUTE)))
		{
			retval.flagChanged();
			if (SolutionSerializer.isCompositeWithItems(retval))
			{
				// also flag items that are stored in the same file; otherwise they will never be flagged
				Iterator<IPersist> iterator = ((ISupportChilds)retval).getAllObjects();
				IPersist p;
				while (iterator.hasNext())
				{
					p = iterator.next();
					if (!(p instanceof IScriptProvider || p instanceof IVariable)) p.flagChanged();
				}
			}
		}
	}

	//for reference tracking we need to have 2 stage deserialize, this is the last part
	private void completePersist(Map<IPersist, JSONObject> persist_json_map, boolean useFilesForDirtyMark) throws RepositoryException, JSONException
	{
		Iterator<Map.Entry<IPersist, JSONObject>> it = persist_json_map.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<IPersist, JSONObject> entry = it.next();
			IPersist retval = entry.getKey();
			JSONObject obj = entry.getValue();
			if (retval instanceof ScriptVariable)
			{
				if (obj.has(LINE_NUMBER_OFFSET_JSON_ATTRIBUTE))
				{
					int linenr = obj.getInt(LINE_NUMBER_OFFSET_JSON_ATTRIBUTE);
					((ScriptVariable)retval).setSerializableRuntimeProperty(IScriptProvider.LINENUMBER, new Integer(linenr));
				}
				if (obj.has(COMMENT_JSON_ATTRIBUTE))
				{
					String comment = obj.getString(COMMENT_JSON_ATTRIBUTE);
					((ScriptVariable)retval).setComment(comment);
				}
				if (obj.has(JS_TYPE_JSON_ATTRIBUTE))
				{
					String type = obj.getString(JS_TYPE_JSON_ATTRIBUTE);
					((ScriptVariable)retval).setSerializableRuntimeProperty(IScriptProvider.TYPE, type);
				}
			}
			else if (retval instanceof AbstractScriptProvider)
			{
				MethodArgument[] methodArguments = NULL;
				if (obj.has(ARGUMENTS_JSON_ATTRIBUTE))
				{
					List<Argument> arguments = (List)obj.remove(ARGUMENTS_JSON_ATTRIBUTE);
					if (arguments.size() > 0)
					{
						methodArguments = new MethodArgument[arguments.size()];
//						String comment = obj.optString(COMMENT_JSON_ATTRIBUTE);
//						MethodArgument[] jsDocArguments = parseJSDocArguments(comment);
						for (int i = 0; i < arguments.size(); i++)
						{
							Argument argument = arguments.get(i);
							String name = argument.getArgumentName();
//							for (int j = 0; j < jsDocArguments.length; j++)
//							{
//								if (jsDocArguments[j].getName().equals(name))
//								{
//									methodArguments[i] = jsDocArguments[j];
//									continue outer;
//								}
//							}
							ArgumentType argumentType = ArgumentType.valueOf(argument.getType() != null ? argument.getType().getName() : null);
							methodArguments[i] = new MethodArgument(name, argumentType, null); // TODO: parse description
						}
					}
				}
				((AbstractScriptProvider)retval).setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, methodArguments);

				if (obj.has(COMMENT_JSON_ATTRIBUTE))
				{
					String comment = obj.getString(COMMENT_JSON_ATTRIBUTE);
					((AbstractScriptProvider)retval).setRuntimeProperty(IScriptProvider.COMMENT, comment);
				}
				if (obj.has(JS_TYPE_JSON_ATTRIBUTE))
				{
					String type = obj.getString(JS_TYPE_JSON_ATTRIBUTE);
					((AbstractScriptProvider)retval).setSerializableRuntimeProperty(IScriptProvider.TYPE, type);
				}

			}

			updatePersistWithValues(repository, retval, obj);
			if (useFilesForDirtyMark) handleChanged(obj, retval);
		}
	}

	private static MethodArgument[] NULL = new MethodArgument[0];

//	private static MethodArgument[] parseJSDocArguments(String doc)
//	{
//		MethodArgument[] arguments = NULL;
//
//
//		return arguments;
//	}

	public static void updatePersistWithValues(IDeveloperRepository repository, IPersist retval, JSONObject obj) throws RepositoryException, JSONException
	{
		LinkedHashMap<String, Object> propertyValues = new LinkedHashMap<String, Object>(); //  use linked hashmap to preserve ordening
		ContentSpec cs = repository.getContentSpec();

		Iterator<ContentSpec.Element> iterator = cs.getPropertiesForObjectType(retval.getTypeID());
		// Note that elements are sorted by contentid desc.
		// This is needed because otherwise deprecated properties (with lower content id) may get overwritten with the default value of their replacement.
		while (iterator.hasNext())
		{
			ContentSpec.Element element = iterator.next();

			if (element.isMetaData()) continue;

			String propertyName = element.getName();
			if (SolutionSerializer.PROP_UUID.equals(propertyName) || SolutionSerializer.PROP_TYPEID.equals(propertyName)) continue;

			if (obj.has(propertyName))
			{
				Object propertyObjectValue = obj.getString(propertyName);
				if (element.getTypeID() == IRepository.ELEMENTS)
				{
					String id = propertyObjectValue.toString();
					UUID uuid = null;
					if (id.indexOf('-') > 0)
					{
						uuid = UUID.fromString(id);
						propertyObjectValue = new Integer(repository.getElementIdForUUID(uuid));
					}
					else
					{
						propertyObjectValue = new Integer(Utils.getAsInteger(id));
					}

					//filling this in case the obj is sent to team repository (with other ids)
					HashMap<UUID, Integer> map = ((AbstractBase)retval).getSerializableRuntimeProperty(AbstractBase.UUIDToIDMapProperty);
					if (map == null)
					{
						map = new HashMap<UUID, Integer>();
						((AbstractBase)retval).setSerializableRuntimeProperty(AbstractBase.UUIDToIDMapProperty, map);
					}
					if (uuid != null)
					{
						map.put(uuid, (Integer)propertyObjectValue);
					}
				}
				else
				{
					propertyObjectValue = repository.convertArgumentStringToObject(element.getTypeID(), (String)propertyObjectValue);
				}
				propertyValues.put(propertyName, propertyObjectValue);
				obj.remove(propertyName);
			}
			else if (!element.isDeprecated())
			{
				// Overwrite with default value, when property has been reset to default it is not written to the json-file
				propertyValues.put(propertyName, repository.convertArgumentStringToObject(element.getTypeID(), element.getDefaultTextualClassValue()));
			}
		}
		repository.updatePersistWithValueMap(retval, propertyValues);
	}

	public static UUID getUUID(File file)
	{
		if (!SolutionSerializer.isJSONFile(file.getName()))
		{
			return null;
		}
		UUID uuid = null;
		try
		{
			JSONObject obj = new ServoyJSONObject(Utils.getTXTFileContent(file), true);
			if (obj.has(SolutionSerializer.PROP_UUID))
			{
				uuid = UUID.fromString(obj.getString(SolutionSerializer.PROP_UUID));
			}
			else
			{
				uuid = UUID.randomUUID();
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logWarning("Cannot get uuid from file " + file, e); //$NON-NLS-1$
		}
		return uuid;
	}

	public static String getUUID(File file, int position)
	{
		if (!SolutionSerializer.isJSONFile(file.getName()) || position < 0)
		{
			return null;
		}
		String uuid = null;
		String text = Utils.getTXTFileContent(file);
		if (text != null && text.length() > position)
		{
			String[] lLines = text.substring(0, position).split("\n");
			String[] rLines = text.substring(position).split("\n");
			for (int i = lLines.length - 1; i >= 0; i--)
			{
				if (lLines[i].trim().startsWith("{") || lLines[i].trim().startsWith("}")) break;
				if (lLines[i].trim().startsWith(SolutionSerializer.PROP_UUID))
				{
					try
					{
						JSONObject obj = new JSONObject("{" + lLines[i] + ((i == lLines.length - 1) ? rLines[0] : "") + "}");
						return obj.getString(SolutionSerializer.PROP_UUID);
					}
					catch (JSONException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			for (int i = 0; i <= rLines.length; i++)
			{
				if (rLines[i].trim().startsWith("{") || rLines[i].trim().startsWith("}")) break;
				if (rLines[i].trim().startsWith(SolutionSerializer.PROP_UUID))
				{
					try
					{
						JSONObject obj = new JSONObject("{" + rLines[i] + ((i == 0) ? lLines[lLines.length - 1] : "") + "}");
						return obj.getString(SolutionSerializer.PROP_UUID);
					}
					catch (JSONException e)
					{
						ServoyLog.logError(e);
					}
				}
			}

		}
		return uuid;
	}

	public static IPersist findPersistFromFile(FileEditorInput fileEditorInput)
	{
		try
		{
			IProjectNature nature = fileEditorInput.getFile().getProject().getNature(ServoyProject.NATURE_ID);
			if (nature instanceof ServoyProject)
			{
				if (!fileEditorInput.getFile().getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT) &&
					!SolutionSerializer.isJSONFile(fileEditorInput.getFile().getName()))
				{
					String[] segments = fileEditorInput.getFile().getProjectRelativePath().segments();
					if (segments.length == 2 && SolutionSerializer.MEDIAS_DIR.equals(segments[0]))
					{
						return ((ServoyProject)nature).getEditingSolution().getMedia(segments[1]);
					}
				}

				File file = fileEditorInput.getFile().getLocation().toFile();
				UUID uuid = getUUID(file);
				return AbstractRepository.searchPersist(((ServoyProject)nature).getSolution(), uuid);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}


	public static SolutionMetaData deserializeRootMetaData(IDeveloperRepository repository, File wsd, String name) throws RepositoryException
	{
		try
		{
			File frmd = new File(wsd, name + '/' + SolutionSerializer.ROOT_METADATA);
			String solutionmetadata = Utils.getTXTFileContent(frmd);
			if (solutionmetadata == null) return null;

			int fileVersion;
			JSONObject obj = new ServoyJSONObject(solutionmetadata, true);
			if (!obj.has(SolutionSerializer.PROP_FILE_VERSION))
			{
				throw new RepositoryException("Cannot handle files with unknown version"); //$NON-NLS-1$
			}
			else
			{
				fileVersion = obj.getInt(SolutionSerializer.PROP_FILE_VERSION);
				if (fileVersion <= 0)
				{
					throw new RepositoryException("Cannot handle files with invalid version (<= 0)"); //$NON-NLS-1$
				}
				if (fileVersion > AbstractRepository.repository_version)
				{
					throw new RepositoryException("Cannot handle file versions greater than " + AbstractRepository.repository_version); //$NON-NLS-1$
				}
			}
			UUID rootObjectUuid = UUID.fromString(obj.getString(SolutionSerializer.PROP_UUID));
			int objectTypeId = obj.getInt(SolutionSerializer.PROP_TYPEID);
//			String name = obj.getString(SolutionSerializer.PROP_NAME);
			int solutionType = obj.getInt("solutionType"); //$NON-NLS-1$
			String protectionPassword = obj.getString("protectionPassword"); //$NON-NLS-1$
			boolean mustAuthenticate = obj.getBoolean("mustAuthenticate"); //$NON-NLS-1$
			//int id = repository.getNewElementID(rootObjectUuid);
			int id = repository.getElementIdForUUID(rootObjectUuid);
			SolutionMetaData metadata = (SolutionMetaData)repository.createRootObjectMetaData(id, rootObjectUuid, name, objectTypeId, 1, 1);
			metadata.setProtectionPassword(protectionPassword);
			metadata.setMustAuthenticate(mustAuthenticate);
			metadata.setSolutionType(solutionType);
			metadata.setFileVersion(fileVersion);
			if (AbstractRepository.repository_version != fileVersion) metadata.flagChanged();
			else metadata.clearChanged();

			return metadata;
		}
		catch (JSONException e)
		{
			throw new RepositoryException("Cannot get root meta data from file " + wsd, e); //$NON-NLS-1$
		}
	}

	public static class ObjBeforeJSExtensionComparator implements Comparator<String>
	{
		public int compare(String fname1, String fname2)
		{
			if (fname1 != null && fname2 != null)
			{
				if ((SolutionSerializer.isJSONFile(fname1) && SolutionSerializer.isJSONFile(fname2)) ||
					(!SolutionSerializer.isJSONFile(fname1) && !SolutionSerializer.isJSONFile(fname2)))
				{
					// 2 json files or 2 other files
					return fname1.compareTo(fname2);
				}
				else if (SolutionSerializer.isJSONFile(fname1))
				{
					// json and other
					return -1;
				}
				else
				{
					// other and orders.obj
					return 1;
				}
			}
			return 0;
		}
	}

	// as solutionDir can be also be a temp folder, ignore it when searching in changedFiles
	private boolean isChangedFile(File solutionDir, File file, List<File> changedFiles)
	{
		if (changedFiles != null)
		{
			String filePath = file.getPath().substring(solutionDir.getPath().length());

			for (File changedFile : changedFiles)
			{
				if (changedFile.getPath().endsWith(filePath))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * @author jcompagner
	 *
	 */
	private static class Line
	{
		int line;
		int start;

		/**
		 * @param i
		 * @param j
		 */
		public Line(int line, int start)
		{
			this.line = line;
			this.start = start;
		}
	}

}
