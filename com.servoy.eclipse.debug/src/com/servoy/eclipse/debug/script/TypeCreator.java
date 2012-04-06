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
package com.servoy.eclipse.debug.script;


import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.javascript.scriptdoc.JavaDoc2HTMLTextReader;
import org.eclipse.dltk.javascript.typeinfo.ITypeInfoContext;
import org.eclipse.dltk.javascript.typeinfo.ITypeNames;
import org.eclipse.dltk.javascript.typeinfo.TypeUtil;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Member;
import org.eclipse.dltk.javascript.typeinfo.model.Method;
import org.eclipse.dltk.javascript.typeinfo.model.Parameter;
import org.eclipse.dltk.javascript.typeinfo.model.ParameterKind;
import org.eclipse.dltk.javascript.typeinfo.model.Property;
import org.eclipse.dltk.javascript.typeinfo.model.SimpleType;
import org.eclipse.dltk.javascript.typeinfo.model.Type;
import org.eclipse.dltk.javascript.typeinfo.model.TypeInfoModelFactory;
import org.eclipse.dltk.javascript.typeinfo.model.TypeKind;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.mozilla.javascript.JavaMembers;
import org.mozilla.javascript.JavaMembers.BeanProperty;
import org.mozilla.javascript.MemberBox;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.IconProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerListContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataprocessing.FoundSet;
import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.Record;
import com.servoy.j2db.documentation.IParameter;
import com.servoy.j2db.documentation.ScriptParameter;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.scripting.IConstantsObject;
import com.servoy.j2db.scripting.IDeprecated;
import com.servoy.j2db.scripting.IJavaScriptType;
import com.servoy.j2db.scripting.IPrefixedConstantsObject;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ITypedScriptObject;
import com.servoy.j2db.scripting.InstanceJavaMembers;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.ui.runtime.IRuntimeComponent;
import com.servoy.j2db.util.HtmlUtils;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 *
 */
@SuppressWarnings("nls")
public abstract class TypeCreator
{
	private static final int INSTANCE_METHOD = 1;
	private static final int STATIC_METHOD = 2;
	private static final int INSTANCE_FIELD = 3;
	private static final int STATIC_FIELD = 4;

	public static final String TYPE_PREFIX = "plugins.";

	protected final static ImageDescriptor METHOD = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/function.gif"), null));
	protected final static ImageDescriptor PROPERTY = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/properties_icon.gif"), null));
	protected final static ImageDescriptor CONSTANT = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/constant.gif"), null));

	protected final static ImageDescriptor ELEMENTS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/elements.gif"), null));

	protected final static ImageDescriptor SPECIAL_PROPERTY = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/special_properties_icon.gif"), null));

	protected final static ImageDescriptor GLOBAL_VAR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/global_variable.gif"), null));
	protected final static ImageDescriptor GLOBAL_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/global_method.gif"), null));

	protected final static ImageDescriptor FORM_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/designer.gif"), null));
	protected final static ImageDescriptor FORM_METHOD_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/form_method.gif"), null));
	protected final static ImageDescriptor FORM_VARIABLE_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/form_variable.gif"), null));

	protected final static ImageDescriptor FOUNDSET_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/foundset.gif"), null));
	protected final static ImageDescriptor RELATION_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/relation.gif"), null));

	protected final static ImageDescriptor COLUMN_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/column.gif"), null));
	protected final static ImageDescriptor COLUMN_AGGR_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columnaggr.gif"), null));
	protected final static ImageDescriptor COLUMN_CALC_IMAGE = ImageDescriptor.createFromURL(FileLocator.find(Activator.getDefault().getBundle(), new Path(
		"/icons/columncalc.gif"), null));

	protected final static ImageDescriptor GLOBALS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/globe.gif"), null));
	protected final static ImageDescriptor SCOPES = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/scopes.gif"), null));
	protected final static ImageDescriptor FORMS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/forms.gif"), null));

	protected final static ImageDescriptor PLUGINS = ImageDescriptor.createFromURL(FileLocator.find(com.servoy.eclipse.ui.Activator.getDefault().getBundle(),
		new Path("/icons/plugin.gif"), null));

	protected final static ImageDescriptor PLUGIN_DEFAULT = ImageDescriptor.createFromURL(FileLocator.find(
		com.servoy.eclipse.ui.Activator.getDefault().getBundle(), new Path("/icons/plugin_conn.gif"), null));

	public static final String IMAGE_DESCRIPTOR = "servoy.IMAGEDESCRIPTOR";
	public static final String RESOURCE = "servoy.RESOURCE";
	public static final String VALUECOLLECTION = "servoy.VALUECOLLECTION";
	public static final String LAZY_VALUECOLLECTION = "servoy.LAZY_VALUECOLLECTION";

	public final static Set<String> BASE_TYPES = new HashSet<String>(128);

	static
	{
		BASE_TYPES.add("Object");
		BASE_TYPES.add("Number");
		BASE_TYPES.add("Array");
		BASE_TYPES.add("String");
		BASE_TYPES.add("Date");
		BASE_TYPES.add("Function");
		BASE_TYPES.add("Boolean");
		BASE_TYPES.add("RegExp");
		BASE_TYPES.add("Error");
		BASE_TYPES.add("Math");
	}

	private final ConcurrentMap<String, Class< ? >> classTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, Class< ? >> anonymousClassTypes = new ConcurrentHashMap<String, Class< ? >>();
	private final ConcurrentMap<String, IScopeTypeCreator> scopeTypes = new ConcurrentHashMap<String, IScopeTypeCreator>();
	protected final ConcurrentMap<Class< ? >, Class< ? >[]> linkedTypes = new ConcurrentHashMap<Class< ? >, Class< ? >[]>();
	protected final ConcurrentMap<Class< ? >, String> prefixedTypes = new ConcurrentHashMap<Class< ? >, String>();
	private volatile boolean initialized;
	private final ConcurrentMap<String, Boolean> invariantScopes = new ConcurrentHashMap<String, Boolean>();
	protected static final List<String> objectMethods = Arrays.asList(new String[] { "wait", "toString", "hashCode", "equals", "notify", "notifyAll", "getClass" });

	public TypeCreator()
	{
		super();
	}

	protected void initalize()
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		synchronized (this)
		{
			if (!initialized)
			{
				initialized = true;
				if (servoyModel instanceof ServoyModel)
				{
					((ServoyModel)servoyModel).addPersistChangeListener(true, new IPersistChangeListener()
					{
						public void persistChanges(Collection<IPersist> changes)
						{
							flushCache();
						}
					});
				}
			}
		}
	}

	protected final Class< ? > getTypeClass(String name)
	{
		Class< ? > clz = classTypes.get(name);
		if (clz == null)
		{
			clz = anonymousClassTypes.get(name);
		}
		return clz;
	}


	public final Set<String> getTypeNames(String prefix)
	{
		Set<String> names = new HashSet<String>(classTypes.keySet());
		if (prefix != null && !"".equals(prefix.trim()))
		{
			String lowerCasePrefix = prefix.toLowerCase();
			Iterator<String> iterator = names.iterator();
			while (iterator.hasNext())
			{
				String name = iterator.next();
				if (name.startsWith(TYPE_PREFIX)) name = name.substring(name.lastIndexOf(".") + 1);
				if (!name.toLowerCase().startsWith(lowerCasePrefix)) iterator.remove();
			}
		}
		return names;
	}


	public Type getType(ITypeInfoContext context, String typeName)
	{
		if (BASE_TYPES.contains(typeName) || typeName.startsWith("Array<")) return null;
		if (!initialized) initalize();
		String realTypeName = typeName;
		if (realTypeName.equals("JSFoundset")) realTypeName = FoundSet.JS_FOUNDSET;
		Type type = createType(context, realTypeName, realTypeName);
		if (type != null)
		{
			context.markInvariant(type);
		}
		else
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				type = context.getInvariantType(realTypeName, fs.getSolution().getName());
				if (type == null)
				{
					type = context.getInvariantType(realTypeName, "scope:tables");
					if (type == null)
					{
						type = context.getInvariantType(realTypeName, "scope:qbcolumns");
					}
					if (type == null)
					{
						type = createDynamicType(context, realTypeName, realTypeName);
						if (type != null && realTypeName.indexOf('<') != -1)
						{
							context.markInvariant(type, fs.getSolution().getName());
							invariantScopes.put(fs.getSolution().getName(), Boolean.TRUE);
						}
					}
				}
			}
			else
			{
				type = createDynamicType(context, realTypeName, realTypeName);
			}
		}
		return type;
	}

	protected final void registerConstantsForScriptObject(IReturnedTypesProvider scriptObject, String prefix)
	{
		if (scriptObject == null) return;
		Class< ? >[] allReturnedTypes = scriptObject.getAllReturnedTypes();
		if (allReturnedTypes == null) return;

		for (Class< ? > element : allReturnedTypes)
		{
			boolean constant = false;
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(element, null);
			if (javaMembers != null)
			{
				Object[] members = javaMembers.getIds(false);
				ArrayList<String> al = new ArrayList<String>(members.length);
				for (Object el : members)
				{
					al.add((String)el);
				}
				if (javaMembers instanceof InstanceJavaMembers)
				{
					al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
				}
				else
				{
					al.removeAll(objectMethods);
				}
				// skip constants only classes
				constant = al.size() == 0;
			}

			boolean add = false;
			if (IPrefixedConstantsObject.class.isAssignableFrom(element))
			{
				add = true;
				try
				{
					IPrefixedConstantsObject constants = (IPrefixedConstantsObject)element.newInstance();
					if (constant)
					{
						addAnonymousClassType(constants.getPrefix(), element);
						ElementResolver.registerConstantType(constants.getPrefix(), constants.getPrefix());
						if (prefix != null)
						{
							addAnonymousClassType(prefix + constants.getPrefix(), element);
							ElementResolver.registerConstantType(prefix + constants.getPrefix(), prefix + constants.getPrefix());
						}
					}
					else
					{
						addType(constants.getPrefix(), element);
						if (prefix != null)
						{
							addType(prefix + constants.getPrefix(), element);
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
			else if (IConstantsObject.class.isAssignableFrom(element) || IJavaScriptType.class.isAssignableFrom(element))
			{
				add = true;
				if (constant)
				{
					addAnonymousClassType(element.getSimpleName(), element);
					ElementResolver.registerConstantType(element.getSimpleName(), element.getSimpleName());
					if (prefix != null)
					{
						addAnonymousClassType(prefix + element.getSimpleName(), element);
						ElementResolver.registerConstantType(prefix + element.getSimpleName(), prefix + element.getSimpleName());
					}
				}
				else
				{
					addType(element.getSimpleName(), element);
					if (prefix != null)
					{
						addType(prefix + element.getSimpleName(), element);
					}
				}

			}
			if (prefix != null && add)
			{
				prefixedTypes.put(element, prefix);
			}
		}
	}

	protected Type createDynamicType(ITypeInfoContext context, String typeNameClassName, String fullTypeName)
	{
		IScopeTypeCreator creator = scopeTypes.get(typeNameClassName);
		if (creator != null)
		{
			return creator.createType(context, fullTypeName);
		}
		return null;
	}

	/**
	 * @param typeNameClassName
	 * @return
	 */
	protected final Type createType(ITypeInfoContext context, String typeNameClassName, String fullTypeName)
	{
		Class< ? > cls = getTypeClass(typeNameClassName);
		if (cls != null)
		{
			return createType(context, fullTypeName, cls);
		}
		return null;
	}

	/**
	 * @param context
	 * @param typeName
	 * @param cls
	 * @return
	 */
	protected final Type createType(ITypeInfoContext context, String typeName, Class< ? > cls)
	{
		Type type = TypeInfoModelFactory.eINSTANCE.createType();
		type.setName(typeName);
		type.setKind(TypeKind.JAVA);
		EList<Member> members = type.getMembers();

		fill(context, members, cls, typeName);

		if (cls != ServoyException.class && !IFoundSet.class.isAssignableFrom(cls))
		{
			ImageDescriptor desc = IconProvider.instance().descriptor(cls);
			type.setAttribute(IMAGE_DESCRIPTOR, desc);
		}
		if (IDeprecated.class.isAssignableFrom(cls) || (prefixedTypes.containsKey(cls) && !typeName.equals(prefixedTypes.get(cls) + cls.getSimpleName())))
		{
			type.setDeprecated(true);
			type.setVisible(false);
		}
		if (cls != IRuntimeComponent.class && IRuntimeComponent.class.isAssignableFrom(cls))
		{
			type.setSuperType(context.getType("RuntimeComponent"));
		}
		Class< ? >[] returnTypes = linkedTypes.get(cls);
		if (returnTypes != null)
		{
			int index = typeName.indexOf('<');
			int index2;
			String config = typeName;
			if (index != -1 && (index2 = typeName.indexOf('>', index)) != -1)
			{
				config = typeName.substring(index + 1, index2);
			}
			for (Class< ? > returnTypeClass : returnTypes)
			{
				String name = returnTypeClass.getSimpleName();
				if (IPrefixedConstantsObject.class.isAssignableFrom(returnTypeClass))
				{
					try
					{
						IPrefixedConstantsObject constants = (IPrefixedConstantsObject)returnTypeClass.newInstance();
						name = constants.getPrefix();
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				String prefix = TYPE_PREFIX + config + ".";
				members.add(createProperty(name, true, TypeUtil.classType(context.getType(prefix + name)), null, null));
			}
		}
		return type;
	}

	/**
	 * @param typeName 
	 * @param members
	 * @param class1
	 */
	private final void fill(ITypeInfoContext context, EList<Member> membersList, Class< ? > scriptObjectClass, String typeName)
	{
		ArrayList<String> al = new ArrayList<String>();
		JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(scriptObjectClass, null);
		if (Scriptable.class.isAssignableFrom(scriptObjectClass) && !(javaMembers instanceof InstanceJavaMembers))
		{
			// if the class is a scriptable an the javamembers is not a instance java members, just return nothing.
			return;
		}
		if (javaMembers != null)
		{
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			Object[] members = javaMembers.getIds(false);
			for (Object element : members)
			{
				al.add((String)element);
			}
			if (IConstantsObject.class.isAssignableFrom(scriptObjectClass))
			{
				members = javaMembers.getIds(true);
				for (Object element : members)
				{
					al.add((String)element);
				}
			}
			if (javaMembers instanceof InstanceJavaMembers)
			{
				al.removeAll(((InstanceJavaMembers)javaMembers).getGettersAndSettersToHide());
			}
			else
			{
				al.removeAll(objectMethods);
			}

			for (String name : al)
			{
				int type = 0;
				Object object = javaMembers.getMethod(name, false);
				if (object == null)
				{
					object = javaMembers.getField(name, false);
					if (object == null)
					{
						object = javaMembers.getField(name, true);
						if (object != null)
						{
							type = STATIC_FIELD;
						}
						else
						{
							object = javaMembers.getMethod(name, true);
							type = STATIC_METHOD;
						}
					}
					else type = INSTANCE_FIELD;
				}
				else type = INSTANCE_METHOD;

				if (object != null)
				{
					Class< ? > returnTypeClz = getReturnType(object);
					if (type == INSTANCE_METHOD || type == STATIC_METHOD)
					{
						MemberBox[] memberbox = null;
						if (object instanceof NativeJavaMethod)
						{
							memberbox = ((NativeJavaMethod)object).getMethods();
						}
						int membersSize = memberbox == null ? 1 : memberbox.length;
						for (int i = 0; i < membersSize; i++)
						{
							Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
							method.setName(name);
							Class< ? >[] parameterTypes = memberbox[i].getParameterTypes();

							if (scriptObject instanceof ITypedScriptObject)
							{
								if (((ITypedScriptObject)scriptObject).isDeprecated(name, parameterTypes))
								{
									method.setDeprecated(true);
									method.setVisible(false);
								}
							}
							else if (scriptObject != null && scriptObject.isDeprecated(name))
							{
								method.setDeprecated(true);
								method.setVisible(false);
							}
							method.setDescription(getDoc(name, scriptObjectClass, name, parameterTypes)); // TODO name should be of parent.
							if (returnTypeClz != null)
							{
								method.setType(getMemberTypeName(context, name, returnTypeClz, typeName));
							}
							method.setAttribute(IMAGE_DESCRIPTOR, METHOD);
							method.setStatic(type == STATIC_METHOD);

							IParameter[] scriptParams = getParameters(name, scriptObjectClass, memberbox[i]);
							if (scriptParams != null && scriptParams.length > 0)
							{
								EList<Parameter> parameters = method.getParameters();
								for (IParameter param : scriptParams)
								{
									Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
									parameter.setName(param.getName());
									if (param.getType() != null)
									{
										String paramType = param.getType();
										if (paramType.endsWith("[]"))
										{
											String componentType = paramType.substring(0, paramType.length() - 2);
											if (param.isVarArgs())
											{
												parameter.setType(context.getTypeRef(componentType));
											}
											else if (ITypeNames.OBJECT.equals(componentType))
											{
												parameter.setType(context.getTypeRef(ITypeNames.ARRAY));
											}
											else
											{
												parameter.setType(TypeUtil.arrayOf(context.getTypeRef(SolutionExplorerListContentProvider.TYPES.get(componentType))));
											}
										}
										else
										{
											parameter.setType(context.getTypeRef(SolutionExplorerListContentProvider.TYPES.get(paramType)));
										}
									}
									parameter.setKind(param.isVarArgs() ? ParameterKind.VARARGS : param.isOptional() ? ParameterKind.OPTIONAL
										: ParameterKind.NORMAL);
									parameters.add(parameter);
								}
							}
							else if (parameterTypes != null && parameterTypes.length > 0)
							{
								EList<Parameter> parameters = method.getParameters();
								for (Class< ? > paramClass : parameterTypes)
								{
									Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
									if (paramClass.isArray())
									{
										Class< ? > componentType = paramClass.getComponentType();
										parameter.setName(SolutionExplorerListContentProvider.TYPES.get(componentType.getName()));
										parameter.setType(TypeUtil.arrayOf(context.getTypeRef(SolutionExplorerListContentProvider.TYPES.get(componentType.getName()))));
									}
									else
									{
										parameter.setName(SolutionExplorerListContentProvider.TYPES.get(paramClass.getName()));
										parameter.setType(context.getTypeRef(SolutionExplorerListContentProvider.TYPES.get(paramClass.getName())));
									}
									parameter.setKind(ParameterKind.NORMAL);
									parameters.add(parameter);
								}
							}
							membersList.add(method);
						}
					}
					else
					{
						JSType returnType = null;
						if (returnTypeClz != null)
						{
							returnType = getMemberTypeName(context, name, returnTypeClz, typeName);
						}
						ImageDescriptor descriptor = IconProvider.instance().descriptor(returnTypeClz);
						if (descriptor == null)
						{
							descriptor = type == STATIC_FIELD ? CONSTANT : PROPERTY;
						}
						Property property = createProperty(name, false, returnType, getDoc(name, scriptObjectClass, name, null), descriptor);
						property.setStatic(type == STATIC_FIELD);
						if (scriptObject != null && scriptObject.isDeprecated(name))
						{
							property.setDeprecated(true);
							property.setVisible(false);
						}
						membersList.add(property);
					}
				}
			}
		}
	}

	protected final JSType getMemberTypeName(ITypeInfoContext context, String memberName, Class< ? > memberReturnType, String objectTypeName)
	{
		int index = objectTypeName.indexOf('<');
		int index2;
		if (index != -1 && (index2 = objectTypeName.indexOf('>', index)) != -1)
		{
			String config = objectTypeName.substring(index + 1, index2);

			if (memberReturnType == Record.class)
			{
				return context.getTypeRef(Record.JS_RECORD + '<' + config + '>');
			}
			if (memberReturnType == FoundSet.class)
			{
				if (memberName.equals("unrelated"))
				{
					if (config.indexOf('.') == -1)
					{
						// its really a relation, unrelate it.
						FlattenedSolution fs = TypeCreator.getFlattenedSolution(context);
						if (fs != null)
						{
							Relation relation = fs.getRelation(config);
							if (relation != null)
							{
								return context.getTypeRef(FoundSet.JS_FOUNDSET + '<' + relation.getForeignDataSource() + '>');
							}
						}
						return context.getTypeRef(FoundSet.JS_FOUNDSET);
					}
				}
				return context.getTypeRef(FoundSet.JS_FOUNDSET + '<' + config + '>');
			}
		}
		if (memberReturnType.isArray())
		{
			Class< ? > returnType = getReturnType(memberReturnType.getComponentType());
			if (returnType != null)
			{
				JSType componentJSType = getMemberTypeName(context, memberName, returnType, objectTypeName);
				if (componentJSType != null)
				{
					return TypeUtil.arrayOf(componentJSType);
				}
			}
			return context.getTypeRef(ITypeNames.ARRAY);
		}

		String typeName = null;
		if (prefixedTypes.containsKey(memberReturnType))
		{
			typeName = prefixedTypes.get(memberReturnType) + memberReturnType.getSimpleName();
		}
		else
		{
			typeName = SolutionExplorerListContentProvider.TYPES.get(memberReturnType.getSimpleName());
			addAnonymousClassType(typeName, memberReturnType);
		}
		return context.getTypeRef(typeName);
	}

	public final void addType(String name, Class< ? > cls)
	{
		classTypes.put(name, cls);
	}

	protected void addAnonymousClassType(String name, Class< ? > cls)
	{
		if (!classTypes.containsKey(name) && !scopeTypes.containsKey(name) && !BASE_TYPES.contains(name))
		{
			anonymousClassTypes.put(name, cls);
		}
	}

	public final void addScopeType(String name, IScopeTypeCreator creator)
	{
		scopeTypes.put(name, creator);
	}

	/**
	 * @param context
	 * @param type
	 * @param provider
	 * @return
	 */
	protected static final Type getDataProviderType(ITypeInfoContext context, IDataProvider provider)
	{
		ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, provider, com.servoy.eclipse.core.Activator.getDefault().getDesignClient());
		switch (componentFormat.dpType)
		{
			case IColumnTypes.DATETIME :
				return context.getType("Date");

			case IColumnTypes.INTEGER :
			case IColumnTypes.NUMBER :
				return context.getType("Number");

			case IColumnTypes.TEXT :
				return context.getType("String");

			default :
				// for now don't return a type (so that anything is valid)
				// maybe we should return Array<byte>
				// should be in sync with TypeProvider.DataprovidersScopeCreator
//				return context.getType("Object");
		}
		return null;
	}


	protected interface IScopeTypeCreator
	{
		Type createType(ITypeInfoContext context, String fullTypeName);
	}

	public IParameter[] getParameters(String key, Class< ? > scriptObjectClass, MemberBox member)
	{
		if (scriptObjectClass == null) return null;
		IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
		IParameter[] parameters = null;
		String[] parameterNames = null;
		if (scriptObject instanceof ITypedScriptObject)
		{
			parameters = ((ITypedScriptObject)scriptObject).getParameters(key, member.getParameterTypes());
		}
		else if (scriptObject != null)
		{
			parameterNames = scriptObject.getParameterNames(key);
		}

		if (parameterNames != null && parameters == null)
		{
			int memberParamLength = member.getParameterTypes().length;
			if (memberParamLength < parameterNames.length)
			{
				boolean removeOptional = false;
				// if parameterNames bigger then the members parameter types and it is not a vararg, just get the first names.
				if (memberParamLength == 1 && member.getParameterTypes()[0].isArray())
				{
					parameters = new IParameter[parameterNames.length];
				}
				else
				{
					parameters = new IParameter[memberParamLength];
					removeOptional = true;
				}
				for (int i = 0; i < parameters.length; i++)
				{
					String name = parameterNames[i];
					boolean vararg = false;
					boolean optional = name.startsWith("[") && name.endsWith("]");
					if (optional && removeOptional)
					{
						optional = false;
						name = name.substring(1, name.length() - 1);
						if (name.startsWith("..."))
						{
							vararg = true;
							name = name.substring(3);
						}
					}
					else if (name.startsWith("[..."))
					{
						vararg = true;
					}
					else if (optional)
					{
						name = name.substring(1, name.length() - 1);
					}
					String type = null;
					if (removeOptional && i < member.getParameterTypes().length)
					{
						Class< ? > paramClass = member.getParameterTypes()[i];
						String className = (paramClass.isArray()) ? paramClass.getComponentType().getName() : paramClass.getName();
						if (prefixedTypes.containsKey((paramClass.isArray()) ? paramClass.getComponentType() : paramClass))
						{
							className = prefixedTypes.get((paramClass.isArray()) ? paramClass.getComponentType() : paramClass) + className;
						}
						if (paramClass.isArray())
						{
							type = SolutionExplorerListContentProvider.TYPES.get(className) + "[]";
						}
						else
						{
							type = SolutionExplorerListContentProvider.TYPES.get(className);
						}
					}
					parameters[i] = new ScriptParameter(name, type, i < member.getParameterTypes().length ? member.getParameterTypes()[i] : null, optional,
						vararg);
				}
			}
			else if (memberParamLength == parameterNames.length)
			{
				parameters = new IParameter[memberParamLength];
				for (int i = 0; i < memberParamLength; i++)
				{
					Class< ? > paramClass = member.getParameterTypes()[i];
					String name = null;
					String type = null;
					boolean optional = false;
					if (parameterNames != null)
					{
						name = parameterNames[i];
						if (name.startsWith("[") && name.endsWith("]"))
						{
							name = name.substring(1, name.length() - 1);
							optional = true;
						}
						else if (paramClass.isArray())
						{
							type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass.getComponentType())
								? prefixedTypes.get(paramClass.getComponentType()) : "") + paramClass.getComponentType().getName()) +
								"[]";
						}
						else
						{
							type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass) ? prefixedTypes.get(paramClass) : "") +
								paramClass.getName());
						}

					}
					else if (paramClass.isArray())
					{
						type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass.getComponentType())
							? prefixedTypes.get(paramClass.getComponentType()) : "") + paramClass.getComponentType().getName()) +
							"[]";
						name = type;

					}
					else
					{
						type = SolutionExplorerListContentProvider.TYPES.get((prefixedTypes.containsKey(paramClass) ? prefixedTypes.get(paramClass) : "") +
							paramClass.getName());
						name = type;
					}
					parameters[i] = new ScriptParameter(name, type, paramClass, optional, false);
				}
			}
		}
		return parameters;
	}

	/**
	 * 
	 */
	protected void flushCache()
	{
		//if (changes.contains(tables))
		ITypeInfoContext.INVARIANTS.reset("scope:tables");
		ITypeInfoContext.INVARIANTS.reset("scope:qbcolumns");

		Set<String> keySet = invariantScopes.keySet();
		for (String context : keySet)
		{
			ITypeInfoContext.INVARIANTS.reset(context);
		}
	}

	public static FlattenedSolution getFlattenedSolution(ITypeInfoContext context)
	{
		String name = context.getContext();
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		if (name == null && context.getModelElement() != null)
		{
			IResource resource = context.getModelElement().getResource();
			if (resource != null)
			{
				name = resource.getProject().getName();
			}
		}
		if (name != null && servoyModel.isSolutionActive(name))
		{
			ServoyProject servoyProject = servoyModel.getServoyProject(name);
			if (servoyProject != null)
			{
				return servoyProject.getEditingFlattenedSolution();
			}
		}
		return null;
	}

	public static Form getForm(ITypeInfoContext context)
	{
		String formName = SolutionSerializer.getFormNameForJSFile(context.getModelElement().getResource());
		if (formName != null)
		{
			FlattenedSolution fs = getFlattenedSolution(context);
			if (fs != null)
			{
				return fs.getForm(formName);
			}
		}
		return null;
	}

	protected static Member createMethod(ITypeInfoContext context, ScriptMethod sm, ImageDescriptor image)
	{
		return createMethod(context, sm, image, null);
	}

	/**
	 * @param sm
	 * @return
	 */
	protected static Member createMethod(ITypeInfoContext context, ScriptMethod sm, ImageDescriptor image, String fileName)
	{
		Method method = TypeInfoModelFactory.eINSTANCE.createMethod();
		method.setName(sm.getName());

		MethodArgument[] arguments = sm.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
		if (arguments != null && arguments.length > 0)
		{
			EList<Parameter> parameters = method.getParameters();
			for (MethodArgument argument : arguments)
			{
				Parameter parameter = TypeInfoModelFactory.eINSTANCE.createParameter();
				parameter.setKind(ParameterKind.NORMAL);
				parameter.setName(argument.getName());
				parameter.setType(context.getTypeRef(argument.getType().getName()));
				parameters.add(parameter);
			}
		}


		String type = sm.getSerializableRuntimeProperty(IScriptProvider.TYPE);
		if (type != null)
		{
			method.setType(context.getTypeRef(type));
		}
		String comment = sm.getRuntimeProperty(IScriptProvider.COMMENT);
		if (comment == null)
		{
			String declaration = sm.getDeclaration();
			int commentStart = declaration.indexOf("/**");
			if (commentStart != -1)
			{
				int commentEnd = declaration.indexOf("*/", commentStart);
				comment = declaration.substring(commentStart, commentEnd);
			}
		}
		if (comment != null)
		{
			if (comment.lastIndexOf("@deprecated") != -1)
			{
				method.setDeprecated(true);
			}

			method.setDescription(getParsedComment(comment));
		}
		if (image != null)
		{
			method.setAttribute(IMAGE_DESCRIPTOR, image);
		}
		if (fileName != null)
		{
			method.setAttribute(RESOURCE, fileName);
		}
		return method;
	}

	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String typeName, String description, ImageDescriptor image)
	{
		return createProperty(context, name, readonly, typeName, description, image, null);
	}

	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String typeName, String description, ImageDescriptor image,
		Object resource)
	{
		SimpleType type = null;
		if (typeName != null)
		{
			type = context.getTypeRef(typeName);
		}
		return createProperty(name, readonly, type, description, image, resource);
	}


	public static Property createProperty(ITypeInfoContext context, String name, boolean readonly, String typeName, ImageDescriptor image)
	{
		SimpleType type = null;
		if (typeName != null)
		{
			type = context.getTypeRef(typeName);
		}
		return createProperty(name, readonly, type, null, image);
	}

	public static Property createProperty(String name, boolean readonly, JSType type, String description, ImageDescriptor image)
	{
		return createProperty(name, readonly, type, description, image, null);
	}

	public static Property createProperty(String name, boolean readonly, JSType type, String description, ImageDescriptor image, Object resource)
	{
		Property property = TypeInfoModelFactory.eINSTANCE.createProperty();
		property.setName(name);
		property.setReadOnly(readonly);
		if (description != null)
		{
			property.setDescription(description);
		}
		if (type != null)
		{
			property.setType(type);
		}
		if (image != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, image);
		}
		else if (type instanceof SimpleType && ((SimpleType)type).getTarget().getAttribute(IMAGE_DESCRIPTOR) != null)
		{
			property.setAttribute(IMAGE_DESCRIPTOR, ((SimpleType)type).getTarget().getAttribute(IMAGE_DESCRIPTOR));
		}
		if (resource != null)
		{
			property.setAttribute(RESOURCE, resource);
		}
		return property;
	}

	public static String getParsedComment(String comment)
	{
		int currPos = 0;
		int endPos = comment.length();
		boolean newLine = true;
		StringBuilder sb = new StringBuilder(comment.length());
		outer : while (currPos < endPos)
		{
			char ch;
			if (newLine)
			{
				do
				{
					ch = comment.charAt(currPos++);
					if (currPos >= endPos) break outer;
					if (ch == '\n' || ch == '\r') break;
				}
				while (Character.isWhitespace(ch) || ch == '*' || ch == '/');
			}
			else
			{
				ch = comment.charAt(currPos++);
			}
			newLine = ch == '\n' || ch == '\r';

			if (newLine)
			{
				if (sb.length() != 0) sb.append("<br/>\n");
			}
			else
			{
				sb.append(ch);
			}
		}

		JavaDoc2HTMLTextReader reader = new JavaDoc2HTMLTextReader(new StringReader(sb.toString()));
		try
		{
			return reader.getString();
		}
		catch (IOException e)
		{
			return comment;
		}
	}

	private static final ConcurrentMap<MethodSignature, String> docCache = new ConcurrentHashMap<MethodSignature, String>(64, 0.9f, 16);

	/**
	 * @param key
	 * @param scriptObject
	 * @param name
	 * @return
	 */
	public static String getDoc(String key, Class< ? > scriptObjectClass, String name, Class< ? >[] parameterTypes)
	{
		if (scriptObjectClass == null) return null;

		MethodSignature cacheKey = new MethodSignature(scriptObjectClass, name, parameterTypes);
		String doc = docCache.get(cacheKey);
		if (doc == null)
		{
			doc = key;
			IScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectForClass(scriptObjectClass);
			if (scriptObject != null)
			{
				StringBuilder docBuilder = new StringBuilder(200);
				String sample = null;
				boolean deprecated = false;
				String deprecatedText = null;
				IParameter[] parameters = null;
				String returnText = null;
				if (scriptObject instanceof ITypedScriptObject)
				{
					String toolTip = ((ITypedScriptObject)scriptObject).getToolTip(key, parameterTypes);
					if (toolTip != null) docBuilder.append(toolTip);
					sample = ((ITypedScriptObject)scriptObject).getSample(key, parameterTypes);
					deprecated = ((ITypedScriptObject)scriptObject).isDeprecated(key, parameterTypes);
					if (deprecated)
					{
						deprecatedText = ((ITypedScriptObject)scriptObject).getDeprecatedText(key, parameterTypes);
					}

					if (parameterTypes != null)
					{
						parameters = ((ITypedScriptObject)scriptObject).getParameters(key, parameterTypes);
					}

					Class< ? > returnedType = ((ITypedScriptObject)scriptObject).getReturnedType(key, parameterTypes);
					String returnDescription = ((ITypedScriptObject)scriptObject).getReturnDescription(key, parameterTypes);
					if ((returnedType != Void.class && returnedType != void.class && returnedType != null) || returnDescription != null)
					{
						returnText = "<b>@return</b> ";
						if (returnedType != null) returnText += returnedType.getSimpleName() + ' ';
						if (returnDescription != null) returnText += returnDescription;
					}
				}
				else
				{
					String toolTip = scriptObject.getToolTip(name);
					if (toolTip != null) docBuilder.append(toolTip);
					sample = scriptObject.getSample(key);
					deprecated = scriptObject.isDeprecated(key);
				}
				if (sample != null)
				{
					docBuilder.append("<br/><pre>");
					docBuilder.append(HtmlUtils.escapeMarkup(sample));
					docBuilder.append("</pre>");
				}
				if (docBuilder.length() > 0)
				{
					if (parameters != null)
					{
						StringBuilder sb = new StringBuilder(parameters.length * 30);
						for (IParameter parameter : parameters)
						{
							sb.append("<br/><b>@param</b> ");
							if (parameter.getType() != null)
							{
								sb.append("{");
								sb.append(SolutionExplorerListContentProvider.TYPES.get(parameter.getType()));
								sb.append("} ");
							}
							sb.append(parameter.getName());
							if (parameter.getDescription() != null)
							{
								sb.append(" ");
								sb.append(parameter.getDescription());
							}
						}
						docBuilder.append(sb);
					}
					if (returnText != null)
					{
						docBuilder.append("<br/></br>");
						docBuilder.append(returnText);
					}
					if (deprecatedText != null)
					{
						docBuilder.append("<br/><br/><b>@deprecated</b> ");
						docBuilder.append(deprecatedText);
					}
					else if (deprecated) docBuilder.append("<br/><br/><b>@deprecated</b>");
					doc = Utils.stringReplace(docBuilder.toString(), "\n", "<br/>"); //$NON-NLS-1$ //$NON-NLS-2$
					doc = Utils.stringReplace(doc, "%%prefix%%", ""); //$NON-NLS-1$ //$NON-NLS-2$
					doc = Utils.stringReplace(doc, "%%elementName%%", "elements.elem"); //$NON-NLS-1$
				}
			}
			docCache.putIfAbsent(cacheKey, doc);
		}
		return doc;
	}

	public static Class< ? > getReturnType(Object object)
	{
		Class< ? > returnType = null;
		if (object instanceof NativeJavaMethod)
		{
			NativeJavaMethod method = (NativeJavaMethod)object;
			MemberBox[] methods = method.getMethods();
			if (methods != null && methods.length > 0)
			{
				returnType = methods[0].getReturnType();
			}
		}
		else if (object instanceof BeanProperty)
		{
			returnType = ((BeanProperty)object).getGetter().getReturnType();
		}
		else if (object instanceof Field)
		{
			returnType = ((Field)object).getType();
		}
		return getReturnType(returnType);
	}

	/**
	 * @param returnType
	 */
	private static Class< ? > getReturnType(Class< ? > returnType)
	{
		if (returnType == Object.class || returnType == null) return null;
		if (returnType.isArray()) return returnType;
		if (!returnType.isAssignableFrom(Void.class) && !returnType.isAssignableFrom(void.class))
		{
			if (returnType.isAssignableFrom(Record.class))
			{
				return Record.class;
			}
			else if (returnType.isAssignableFrom(FoundSet.class))
			{
				return FoundSet.class;
			}
			else if (returnType.isPrimitive() || Number.class.isAssignableFrom(returnType))
			{
				if (returnType.isAssignableFrom(boolean.class)) return Boolean.class;
				if (returnType.isAssignableFrom(byte.class))
				{
					return byte.class;
				}
				return Number.class;
			}
			else if (returnType == Object.class || returnType == String.class || Date.class.isAssignableFrom(returnType))
			{
				return returnType;
			}
			JavaMembers javaMembers = ScriptObjectRegistry.getJavaMembers(returnType, null);
			if (javaMembers != null)
			{
				return returnType;
			}
		}
		return null;
	}

	private final static class MethodSignature
	{
		private final Class< ? > scriptObjectClass;
		private final String name;
		private final Class< ? >[] parameterTypes;

		/**
		 * @param scriptObjectClass
		 * @param name
		 * @param parameterTypes
		 */
		public MethodSignature(Class< ? > scriptObjectClass, String name, Class< ? >[] parameterTypes)
		{
			this.scriptObjectClass = scriptObjectClass;
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + Arrays.hashCode(parameterTypes);
			result = prime * result + ((scriptObjectClass == null) ? 0 : scriptObjectClass.hashCode());
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			MethodSignature other = (MethodSignature)obj;
			if (name == null)
			{
				if (other.name != null) return false;
			}
			else if (!name.equals(other.name)) return false;
			if (!Arrays.equals(parameterTypes, other.parameterTypes)) return false;
			if (scriptObjectClass == null)
			{
				if (other.scriptObjectClass != null) return false;
			}
			else if (!scriptObjectClass.equals(other.scriptObjectClass)) return false;
			return true;
		}
	}
}
