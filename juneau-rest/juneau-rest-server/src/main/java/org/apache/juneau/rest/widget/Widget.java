// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.widget;

import java.io.*;
import java.util.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.utils.*;

/**
 * Defines an interface for resolvers of <js>"$W{...}"</js> string variables.
 *
 * <p>
 * Widgets must provide one of the following public constructors:
 * <ul>
 * 	<li><code><jk>public</jk> Widget();</code>
 * 	<li><code><jk>public</jk> Widget(PropertyStore);</code>
 * </ul>
 *
 * <p>
 * Widgets can be defined as inner classes of REST resource classes.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.HtmlDocAnnotation.Widgets}
 * </ul>
 */
public abstract class Widget {

	private final ClasspathResourceManager rm = new ClasspathResourceManager(getClass(), ClasspathResourceFinderRecursive.INSTANCE, false);

	/**
	 * The widget key.
	 *
	 * <p>
	 * (i.e. The variable name inside the <js>"$W{...}"</js> variable).
	 *
	 * <p>
	 * The returned value must not be <jk>null</jk>.
	 *
	 * <p>
	 * If not overridden, the default value is the class simple name.
	 *
	 * @return The widget key.
	 */
	public String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * Resolves the HTML content for this widget.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @return The HTML content of this widget.
	 * @throws Exception
	 */
	public String getHtml(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Resolves any Javascript that should be added to the <xt>&lt;head&gt;/&lt;script&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @return The Javascript needed by this widget.
	 * @throws Exception
	 */
	public String getScript(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Resolves any CSS styles that should be added to the <xt>&lt;head&gt;/&lt;style&gt;</xt> element.
	 *
	 * <p>
	 * A returned value of <jk>null</jk> will cause nothing to be added to the page.
	 *
	 * @param req The HTTP request object.
	 * @return The CSS styles needed by this widget.
	 * @throws Exception
	 */
	public String getStyle(RestRequest req) throws Exception {
		return null;
	}

	/**
	 * Retrieves the specified classpath resource and returns the contents as a string.
	 *
	 * <p>
	 * Same as {@link Class#getResourceAsStream(String)} except if it doesn't find the resource on this class, searches
	 * up the parent hierarchy chain.
	 *
	 * <p>
	 * If the resource cannot be found in the classpath, then an attempt is made to look relative to the JVM working directory.
	 * <br>Path traversals outside the working directory are not allowed for security reasons.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String getClasspathResourceAsString(String name) throws IOException {
		return rm.getString(name);
	}

	/**
	 * Same as {@link #getClasspathResourceAsString(String)} except also looks for localized-versions of the file.
	 *
	 * <p>
	 * If the <code>locale</code> is specified, then we look for resources whose name matches that locale.
	 * <br>For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
	 * files in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 *
	 * @param name Name of the desired resource.
	 * @param locale The locale.  Can be <jk>null</jk>.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String getClasspathResourceAsString(String name, Locale locale) throws IOException {
		return rm.getString(name, locale);
	}

	/**
	 * Convenience method for calling {@link #getClasspathResourceAsString(String)} except also strips Javascript comments from
	 * the file.
	 *
	 * <p>
	 * Comments are assumed to be Java-style block comments: <js>"/*"</js>.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadScript(String name) throws IOException {
		String s = getClasspathResourceAsString(name);
		if (s != null)
			s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
		return s;
	}

	/**
	 * Same as {@link #loadScript(String)} but replaces request-time SVL variables.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'>{@doc juneau-rest-server.SvlVariables}
	 * </ul>
	 *
	 * @param req The current HTTP request.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadScriptWithVars(RestRequest req, String name) throws IOException {
		return req.getVarResolverSession().resolve(loadScript(name));
	}

	/**
	 * Convenience method for calling {@link #getClasspathResourceAsString(String)} except also strips CSS comments from
	 * the file.
	 *
	 * <p>
	 * Comments are assumed to be Java-style block comments: <js>"/*"</js>.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadStyle(String name) throws IOException {
		String s = getClasspathResourceAsString(name);
		if (s != null)
			s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
		return s;
	}

	/**
	 * Same as {@link #loadStyle(String)} but replaces request-time SVL variables.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'>{@doc juneau-rest-server.SvlVariables}
	 * </ul>
	 *
	 * @param req The current HTTP request.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadStyleWithVars(RestRequest req, String name) throws IOException {
		return req.getVarResolverSession().resolve(loadStyle(name));
	}

	/**
	 * Convenience method for calling {@link #getClasspathResourceAsString(String)} except also strips HTML comments from the
	 * file.
	 *
	 * <p>
	 * Comment are assumed to be <js>"<!-- -->"</js> code blocks.
	 *
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadHtml(String name) throws IOException {
		String s = getClasspathResourceAsString(name);
		if (s != null)
			s = s.replaceAll("(?s)<!--(.*?)-->\\s*", "");
		return s;
	}

	/**
	 * Same as {@link #loadHtml(String)} but replaces request-time SVL variables.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'>{@doc juneau-rest-server.SvlVariables}
	 * </ul>
	 *
	 * @param req The current HTTP request.
	 * @param name Name of the desired resource.
	 * @return The resource converted to a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected String loadHtmlWithVars(RestRequest req, String name) throws IOException {
		return req.getVarResolverSession().resolve(loadHtml(name));
	}
}
