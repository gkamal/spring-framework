/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context;

/**
 * TODO SPR-7194: document (clean up)
 *
 * Interface for executing a populated configuration {@link Specification}. Provides
 * a generic mechanism for handling container configuration metadata regardless of
 * origin in XML, annotations, or other source format.
 *
 * @author Chris Beams
 * @since 3.1
 * @see AbstractSpecificationExecutor
 * @see org.springframework.beans.factory.xml.BeanDefinitionParser
 * @see org.springframework.context.annotation.AnnotationSpecificationCreator
 * @see org.springframework.context.annotation.ComponentScanSpecificationExecutor
 */
public interface SpecificationExecutor<S extends Specification> {

	/**
	 * Execute the given specification, usually resulting in registration
	 * of bean definitions against a bean factory.
	 */
	void execute(S spec);

}