/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.jsunit.runner;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * Specifies which tests of an active solution's subtree should run.
 * If Test target is null that means the whole active solution.
 * If Test target's module to test is the same as active solution that  means the whole active solution again.
 * @author acostescu
 */
public class TestTarget
{

	private static final int SOLUTION = 1;
	private static final int FORM = 2;
	private static final int GLOBAL_SCOPE = 3;
	private static final int FORM_METHOD = 4;
	private static final int GLOBAL_METHOD = 5;

	private final static String DELIM = "|";


	private Pair<Solution, String> globalScopeToTest; // if a global scope should be tested 
	private Solution moduleToTest; // if a module is to be tested
	private Form formToTest; // if a form scope should be tested
	private ScriptMethod testMethodToTest; // if only one test method should be tested

	private Solution activeSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution();
	private Form testMethodsForm;
	private String testMethodsScope;
	private int type; //value of SOLUTION ,FORM, GLOBAL_SCOPE...

	private TestTarget()
	{
	}

	public Pair<Solution, String> getGlobalScopeToTest()
	{
		return globalScopeToTest;
	}

	public Solution getModuleToTest()
	{
		return moduleToTest;
	}

	public Form getFormToTest()
	{
		return formToTest;
	}

	public ScriptMethod getTestMethodToTest()
	{
		return testMethodToTest;
	}

	public Solution getActiveSolution()
	{
		return activeSolution;
	}

	public TestTarget(Pair<Solution, String> globalScopeToTest)
	{
		this.type = GLOBAL_SCOPE;
		this.globalScopeToTest = globalScopeToTest;
		this.moduleToTest = null;
		this.formToTest = null;
		this.testMethodToTest = null;

	}

	public TestTarget(Solution moduleToTest)
	{
		this.type = SOLUTION;
		this.globalScopeToTest = null;
		this.moduleToTest = moduleToTest;
		this.formToTest = null;
		this.testMethodToTest = null;

	}

	public TestTarget(Form formToTest)
	{
		this.type = FORM;
		this.globalScopeToTest = null;
		this.moduleToTest = null;
		this.formToTest = formToTest;
		this.testMethodToTest = null;

	}

	public TestTarget(Form form, ScriptMethod testMethodToTest)
	{
		this.type = FORM_METHOD;
		this.globalScopeToTest = null;
		this.moduleToTest = null;
		this.formToTest = null;
		this.testMethodToTest = testMethodToTest;
		this.testMethodsForm = form;
		Assert.isNotNull(form);
	}

	public TestTarget(String scope, ScriptMethod testMethodToTest)
	{
		this.type = GLOBAL_METHOD;
		this.globalScopeToTest = null;
		this.moduleToTest = null;
		this.formToTest = null;
		this.testMethodToTest = testMethodToTest;
		this.testMethodsScope = scope;
		Assert.isNotNull(scope);
	}

	@Override
	public String toString()
	{
		return convertToString();
	}

	public String convertToString()
	{
		// careful - this is not for easy debugging only - it's parsed & used
		switch (type)
		{
			case SOLUTION :
			{
				return type + DELIM + activeSolution.getName() + DELIM + moduleToTest.getName();
			}
			case FORM :
			{
				return type + DELIM + activeSolution.getName() + DELIM + formToTest.getName();
			}
			case GLOBAL_SCOPE :
			{
				return type + DELIM + activeSolution.getName() + DELIM + globalScopeToTest.getLeft().getName() + DELIM + globalScopeToTest.getRight();
			}
			case FORM_METHOD :
			{
				return type + DELIM + activeSolution.getName() + DELIM + testMethodsForm.getName() + DELIM + testMethodToTest.getName();
			}
			case GLOBAL_METHOD :
			{
				return type + DELIM + activeSolution.getName() + DELIM + testMethodsScope + DELIM + testMethodToTest.getName();
			}
		}
		return null;
	}

	public static TestTarget convertFromString(String str)
	{
		TestTarget target = new TestTarget();
		FlattenedSolution fl = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingFlattenedSolution();
		StringTokenizer st = new StringTokenizer(str, DELIM);
		int type = Integer.valueOf(st.nextToken());
		String activeSolution = st.nextToken();
		target.activeSolution = findSolution(activeSolution);
		Solution actualActiveSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution();
		if (target.activeSolution.getName() != actualActiveSolution.getName())
		{
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			UIUtils.showInformation(shell, "Cannot run target", "The currently launched configuration is expects the active solution to be : " +
				activeSolution + " instead of " + actualActiveSolution.getName() + "\n Running current active solution");
			target.activeSolution = actualActiveSolution;
			return target;
		}
		target.type = type;
		switch (type)
		{
			case SOLUTION :
			{
				String solutionName = st.nextToken();
				target.moduleToTest = findSolution(solutionName); //Assert not null
				break;
			}
			case FORM :
			{
				String formName = st.nextToken();
				target.formToTest = fl.getForm(formName); //Assert not null
				break;
			}
			case GLOBAL_SCOPE :
			{
				String solName = st.nextToken();
				String scopeName = st.nextToken();
				Solution s = findSolution(solName); //Assert not null
				Pair<Solution, String> pair = new Pair<Solution, String>(s, scopeName);
				target.globalScopeToTest = pair;
				break;
			}
			case FORM_METHOD :
			{
				String formName = st.nextToken();
				String methodName = st.nextToken();
				Form form = fl.getForm(formName); //Assert not null
				ScriptMethod method = form.getScriptMethod(methodName); //Assert not null
				target.testMethodToTest = method;
				break;
			}
			case GLOBAL_METHOD :
			{
				String scopeName = st.nextToken();
				String methodName = st.nextToken();
				target.testMethodToTest = fl.getScriptMethod(scopeName, methodName);
				break;
			}
		}
		return target;
	}

	public static Solution findSolution(String name)
	{
		FlattenedSolution fl = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingFlattenedSolution();
		if (fl.getSolution().getName().equals(name))
		{
			return fl.getSolution();
		}
		for (Solution module : fl.getModules())
		{
			if (module.getName().equals(name))
			{
				return module;
			}
		}
		return null;
	}

	public static TestTarget activeProjectTarget()
	{
		return new TestTarget(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution());
	}
}