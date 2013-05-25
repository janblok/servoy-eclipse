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

package com.servoy.eclipse.exporter.mobile.launch;

/**
 * @author obuligan
 *
 */
public interface IMobileLaunchConstants
{

	public static final String LAUNCH_CONFIGURATION_TYPE_ID = "com.servoy.eclipse.mobile.launch";

	public static final String WAR_LOCATION = "war_location";
	public static final String SOLUTION_NAME = "solution_name";
	public static final String SERVER_URL = "server_url";
	public static final String APPLICATION_URL = "application_url";
	public static final String NODEBUG = "nodebug";
	public static final String BROWSER_ID = "browserID";
	public static final String WAR_DEPLOYMENT_TIME = "war_deploy_time";
	public static final String TIMEOUT = "timeout";

	public final static String DEFAULT_SERVICE_URL = "http://localhost:8080";
	public final static String DEFAULT_WAR_DEPLOYMENT_TIME = "10";

}