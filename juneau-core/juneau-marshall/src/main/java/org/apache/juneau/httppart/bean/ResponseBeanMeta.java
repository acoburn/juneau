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
package org.apache.juneau.httppart.bean;

import static org.apache.juneau.internal.ClassFlags.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link Response}.
 */
public class ResponseBeanMeta {

	/**
	 * Represents a non-existent meta object.
	 */
	public static ResponseBeanMeta NULL = new ResponseBeanMeta(new Builder(PropertyStore.DEFAULT));

	/**
	 * Create metadata from specified class.
	 *
	 * @param c The class annotated with {@link Response}.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Response}.
	 */
	public static ResponseBeanMeta create(Class<?> c, PropertyStore ps) {
		if (! hasAnnotation(Response.class, c))
			return null;
		return new ResponseBeanMeta.Builder(ps).apply(c).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ClassMeta<?> cm;
	private final int code;
	private final Map<String,ResponseBeanPropertyMeta> headerMethods;
	private final Method statusMethod;
	private final ResponseBeanPropertyMeta bodyMethod;
	private final HttpPartSerializer partSerializer;
	private final HttpPartSchema schema;
	private final boolean usePartSerializer;

	ResponseBeanMeta(Builder b) {
		this.cm = b.cm;
		this.code = b.code;
		this.partSerializer = ClassUtils.newInstance(HttpPartSerializer.class, b.partSerializer, true, b.ps);
		this.schema = b.schema.build();
		this.usePartSerializer = b.usePartSerializer || partSerializer != null;

		Map<String,ResponseBeanPropertyMeta> hm = new LinkedHashMap<>();
		for (Map.Entry<String,ResponseBeanPropertyMeta.Builder> e : b.headerMethods.entrySet()) {
			ResponseBeanPropertyMeta pm = e.getValue().build(partSerializer);
			hm.put(e.getKey(), pm);

		}
		this.headerMethods = Collections.unmodifiableMap(hm);

		this.bodyMethod = b.bodyMethod == null ? null : b.bodyMethod.build(partSerializer);
		this.statusMethod = b.statusMethod;
	}

	static class Builder {
		ClassMeta<?> cm;
		int code;
		PropertyStore ps;
		boolean usePartSerializer;
		Class<? extends HttpPartSerializer> partSerializer;
		HttpPartSchemaBuilder schema = HttpPartSchema.create();

		Map<String,ResponseBeanPropertyMeta.Builder> headerMethods = new LinkedHashMap<>();
		ResponseBeanPropertyMeta.Builder bodyMethod;
		Method statusMethod;

		Builder(PropertyStore ps) {
			this.ps = ps;
		}

		Builder apply(Class<?> c) {
			apply(getAnnotation(Response.class, c));
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			for (Method m : ClassUtils.getAllMethods(c, false)) {
				if (isAll(m, PUBLIC, HAS_NO_ARGS)) {
					if (hasAnnotation(ResponseHeader.class, m)) {
						if (m.getParameterTypes().length != 0)
							throw new InvalidAnnotationException("@ResponseHeader annotation on method cannot have arguments.  Method=''{0}''", m);
						Class<?> rt = m.getReturnType();
						if (rt == void.class)
							throw new InvalidAnnotationException("Invalid return type for @ResponseHeader annotation on method.  Method=''{0}''", m);
						ResponseHeader a = getAnnotation(ResponseHeader.class, m);
						String n = a.name();
						if (n.isEmpty())
							n = m.getName();
						HttpPartSchemaBuilder s = HttpPartSchema.create().apply(a);
						headerMethods.put(n, ResponseBeanPropertyMeta.create().name(n).partType(HttpPartType.HEADER).apply(s).getter(m));
					}
					if (hasAnnotation(ResponseStatus.class, m)) {
						if (m.getParameterTypes().length != 0)
							throw new InvalidAnnotationException("@ResponseStatus annotation on method cannot have arguments.  Method=''{0}''", m);
						Class<?> rt = m.getReturnType();
						if (rt != int.class || rt != Integer.class)
							throw new InvalidAnnotationException("Invalid return type for @ResponseStatus annotation on method.  Method=''{0}''", m);
						statusMethod = m;
					}
					if (hasAnnotation(ResponseBody.class, m)) {
						if (m.getParameterTypes().length != 0)
							throw new InvalidAnnotationException("@ResponseBody annotation on method cannot have arguments.  Method=''{0}''", m);
						Class<?> rt = m.getReturnType();
						if (rt == void.class)
							throw new InvalidAnnotationException("Invalid return type for @ResponseBody annotation on method.  Method=''{0}''", m);
						ResponseBody a = getAnnotation(ResponseBody.class, m);
						HttpPartSchemaBuilder s = HttpPartSchema.create().apply(a);
						bodyMethod = ResponseBeanPropertyMeta.create().partType(HttpPartType.BODY).apply(s).getter(m);
					}
					if (hasAnnotation(Body.class, m))
						throw new InvalidAnnotationException("@Body annotation cannot be used in a @Response bean.  Use @ResponseBody instead.  Method=''{0}''", m);
					if (hasAnnotation(Header.class, m))
						throw new InvalidAnnotationException("@Header annotation cannot be used in a @Response bean.  Use @ResponseHeader instead.  Method=''{0}''", m);
				}
			}
			return this;
		}

		Builder apply(Response rb) {
			if (rb != null) {
				if (rb.partSerializer() != HttpPartSerializer.Null.class)
					partSerializer = rb.partSerializer();
				if (rb.usePartSerializer())
					usePartSerializer = true;
				if (rb.value().length > 0)
					code = rb.value()[0];
				if (rb.code().length > 0)
					code = rb.code()[0];
				schema.apply(rb.schema());
			}
			return this;
		}

		ResponseBeanMeta build() {
			return new ResponseBeanMeta(this);
		}
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The HTTP status code.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the schema information about the response object.
	 *
	 * @return The schema information about the response object.
	 */
	public HttpPartSchema getSchema() {
		return schema;
	}

	/**
	 * Returns metadata about the <ja>@ResponseHeader</ja>-annotated methods.
	 *
	 * @return Metadata about the <ja>@ResponseHeader</ja>-annotated methods, or an empty collection if none exist.
	 */
	public Collection<ResponseBeanPropertyMeta> getHeaderMethods() {
		return headerMethods.values();
	}

	/**
	 * Returns metadata about the <ja>@ResponseBody</ja>-annotated method.
	 *
	 * @return Metadata about the <ja>@ResponseBody</ja>-annotated method, or <jk>null</jk> if it doesn't exist.
	 */
	public ResponseBeanPropertyMeta getBodyMethod() {
		return bodyMethod;
	}

	/**
	 * Returns metadata about the <ja>@ResponseBody</ja>-annotated method.
	 *
	 * @return Metadata about the <ja>@ResponseBody</ja>-annotated method, or <jk>null</jk> if it doesn't exist.
	 */
	public Method getStatusMethod() {
		return statusMethod;
	}

	/**
	 * Returns the flag for whether the part serializer should be used to serialize this response.
	 *
	 * @return The flag for whether the part serializer should be used to serialize this response.
	 */
	public boolean isUsePartSerializer() {
		return usePartSerializer;
	}

	/**
	 * Returns the part serializer to use to serialize this response.
	 *
	 * @return The part serializer to use to serialize this response.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns metadata about the class.
	 *
	 * @return Metadata about the class.
	 */
	public ClassMeta<?> getClassMeta() {
		return cm;
	}
}
