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

package com.servoy.eclipse.exporter.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.servoy.j2db.dataprocessing.IDataServerInternal;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;

/**
 * Stores and provides the export-relevant arguments the product was started with.
 * @author acostescu
 */
public class ArgumentChest implements IXMLExportUserChannel
{

	private static final String FILE_EXTENSION = ".servoy"; //$NON-NLS-1$

	private boolean invalidArguments = false;
	private boolean mustShowHelp = false;
	private String solutionName = null;
	private String exportFileName = null;

	private boolean exportSampleData = false;
	private int sampleDataCount = 10000;
	private boolean exportI18N = false;
	private boolean exportUsers = false;
	private boolean exportModules = false;
	private List<String> moduleList = null;
	private boolean verbose = false;
	private boolean exportAllTablesFromReferencedServers = false;
	private String protectionPassword = null;
	private String settingsFile = null;
	private String appServerDir = "../../application_server"; //$NON-NLS-1$

	@SuppressWarnings("nls")
	public ArgumentChest(String[] args)
	{
		if (args.length == 0) mustShowHelp = true;
		else
		{
			int i = 0;
			String exportFilePath = null;
			while (i < args.length)
			{
				if ("-help".equalsIgnoreCase(args[i]) || "-?".equals(args[i]) || "/?".equals(args[i]))
				{
					mustShowHelp = true;
					i = args.length - 1;
				}
				else if ("-s".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						solutionName = args[++i];
					}
					else
					{
						info("Solution name was not specified after '-s' argument.", ILogLevel.ERROR);
						invalidArguments = true;
					}
				}
				else if ("-o".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						exportFilePath = args[++i];
					}
					else
					{
						info("Export file path was not specified after '-o' argument.", ILogLevel.ERROR);
						invalidArguments = true;
					}
				}
				else if ("-verbose".equalsIgnoreCase(args[i]))
				{
					verbose = true;
				}
				else if ("-p".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						settingsFile = args[++i];
					}
					else
					{
						info("Properties file was not specified after '-p' argument.", ILogLevel.ERROR);
						invalidArguments = true;
					}
				}
				else if ("-as".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						appServerDir = args[++i];
					}
					else
					{
						info("Application server directory was not specified after '-as' argument.", ILogLevel.ERROR);
						invalidArguments = true;
					}
				}
				else if ("-sd".equalsIgnoreCase(args[i]))
				{
					exportSampleData = true;
				}
				else if ("-sdcount".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						try
						{
							sampleDataCount = Integer.parseInt(args[++i]);
							if (sampleDataCount < 1)
							{
								sampleDataCount = 1;
								info("Number of rows to export per table cannot be < 1. Corrected to 1.", ILogLevel.ERROR);
							}
							else if (sampleDataCount > IDataServerInternal.MAX_ROWS_TO_RETRIEVE)
							{
								sampleDataCount = IDataServerInternal.MAX_ROWS_TO_RETRIEVE;
								info("Number of rows to export per table cannot be > " + IDataServerInternal.MAX_ROWS_TO_RETRIEVE + ". Corrected.",
									ILogLevel.ERROR);
							}
						}
						catch (NumberFormatException e)
						{
							info("Number of rows to export per table specified after '-sdcount' argument is not an integer value.", ILogLevel.ERROR);
							invalidArguments = true;
						}
					}
					else
					{
						info("Number of rows to export per table was not specified after '-sdcount' argument.", ILogLevel.ERROR);
						invalidArguments = true;
					}
				}
				else if ("-i18n".equalsIgnoreCase(args[i]))
				{
					exportI18N = true;
				}
				else if ("-users".equalsIgnoreCase(args[i]))
				{
					exportUsers = true;
				}
				else if ("-tables".equalsIgnoreCase(args[i]))
				{
					exportAllTablesFromReferencedServers = true;
				}
				else if ("-pwd".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						protectionPassword = args[++i];
					}
					else
					{
						info("Protection password was not specified after '-pwd' argument.", ILogLevel.ERROR);
						invalidArguments = true;
					}
				}
				else if ("-modules".equalsIgnoreCase(args[i]))
				{
					exportModules = true;
					if (i < (args.length - 1))
					{
						moduleList = new ArrayList<String>();
						while ((++i) < args.length)
						{
							moduleList.add(args[i]);
						}
					}
				}
				i++;
			}

			// check that the required arguments are provided
			if (!mustShowHelp && !invalidArguments)
			{
				if (solutionName == null || exportFilePath == null)
				{
					info("Required arguments are missing. Please provide both '-s'and '-o' arguments.", ILogLevel.ERROR);
					invalidArguments = true;
				}
				else
				{
					// transform the path into a path+filename
					File f = new File(exportFilePath, solutionName + FILE_EXTENSION);
					exportFileName = f.getAbsolutePath();
				}
			}
		}
	}

	@SuppressWarnings("nls")
	public String getHelpMessage()
	{
		return "USAGE:\n\n"
			+ "    -help or -? or /? or no arguments ... shows current help message.\n\n"
			+ "                  OR\n\n"
			+ "    -s <name_of_solution_to_export> -o <output_dir> -data <workspace_location_that_contains_solution> [optional_arguments]\n\n"
			+ "        Optional arguments:\n"
			+ "        -verbose ... prints more info to console\n"
			+ "        -p <properties_file> ... path and name of properties file. By default the 'servoy.properties' file from 'application_server' will be used\n"
			+ "        -as <app_server_dir> ... specifies where to find the 'application_server' directory. Default: '../../application_server'\n\n"
			+ "        -sd ... exports sample data. IMPORTANT: when exporting sample data all needed DB servers must already be started\n"
			+ "        -sdcount <count> ... number of rows to export per table. Only makes sense when -sd is also present. Can be 'all' (without the '). Default: 10000\n"
			+ "        -i18n ... exports i18n data\n"
			+ "        -users ... exports users\n"
			+ "        -tables ... export all table information about tables from referenced servers. IMPORTANT: when exporting table information all needed DB servers must already be started\n"
			+ "        -pwd <protection_password> ... protect the exported solution with given password.\n"
			+ "        -modules [<module1_name> <module2_name> ... <moduleN_name>] ... MUST be last argument specified in command line.\n"
			+ "                                                                        Includes all or part of referenced modules in export. If only '-modules' is used, it will export all referenced modules.\n"
			+ "                                                                        If a list of modules is also included, it will export only modules from this list, provided they are referenced by exported solution.\n\n"
			+ "EXIT codes: 0 - normal, 1 - export stopped by user, 2 - export failed, 3 - invalid arguments.";
	}

	public String getAppServerDir()
	{
		return appServerDir;
	}

	public String getSettingsFileName()
	{
		return settingsFile;
	}

	public boolean isInvalid()
	{
		return invalidArguments;
	}

	public boolean mustShowHelp()
	{
		return mustShowHelp;
	}

	public String getExportFileName()
	{
		return exportFileName;
	}

	public boolean shouldExportSampleData()
	{
		return exportSampleData;
	}

	public int getNumberOfSampleDataExported()
	{
		return sampleDataCount;
	}

	public boolean shouldExportI18NData()
	{
		return exportI18N;
	}

	public boolean shouldExportUsers()
	{
		return exportUsers;
	}

	public boolean shouldExportModules()
	{
		return exportModules;
	}

	public boolean shouldProtectWithPassword()
	{
		return protectionPassword != null;
	}

	public boolean isVerbose()
	{
		return verbose;
	}

	public String getSolutionName()
	{
		return solutionName;
	}

	// IXMLExportUserChannel methods: 

	public boolean getExportAllTablesFromReferencedServers()
	{
		return exportAllTablesFromReferencedServers;
	}

	public SortedList<String> getModuleIncludeList(SortedList<String> allModules)
	{
		if (moduleList != null)
		{
			allModules.retainAll(moduleList);
			if (allModules.size() != moduleList.size())
			{
				info("Some of the modules specified for export were not actually modules of exported solution.", ILogLevel.ERROR); //$NON-NLS-1$
			}
		}
		return allModules;
	}

	public String getProtectionPassword()
	{
		return protectionPassword;
	}

	public void info(String message, int priority)
	{
		if (priority > ILogLevel.WARNING || verbose)
		{
			System.out.println(message);
		}
	}

}