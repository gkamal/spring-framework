/*
 * Copyright 2004-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ui.model.config;

import org.springframework.ui.format.Formatter;
import org.springframework.ui.model.FieldModel;

/**
 * A fluent interface for configuring a {@link FieldModel}.
 * @author Keith Donald
 * @since 3.0
 */
public interface FieldModelConfiguration {
	
	/**
	 * Set the Formatter to use to format bound property values.
	 */
	FieldModelConfiguration formatWith(Formatter<?> formatter);

	/**
	 * If a Map field, set the Formatter to use to format map keys.
	 */
	FieldModelConfiguration formatKeysWith(Formatter<?> formatter);

	/**
	 * If a List, array, or Map, set the Formatter to use to format indexed elements.
	 */
	FieldModelConfiguration formatElementsWith(Formatter<?> formatter);

}