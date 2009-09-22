/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.core.convert.support;

import static org.springframework.core.convert.support.ConversionUtils.invokeConverter;

import java.lang.reflect.Array;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

final class ArrayToArrayGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public ArrayToArrayGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(targetType)) {
			return source;
		}
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
		Object target = Array.newInstance(targetElementType.getType(), Array.getLength(source));
		GenericConverter converter = this.conversionService.getConverter(sourceElementType, targetElementType);
		if (converter == null) {
			throw new ConverterNotFoundException(sourceElementType, targetElementType);
		}
		for (int i = 0; i < Array.getLength(target); i++) {
			Object sourceElement = Array.get(source, i);
			Object targetElement = invokeConverter(converter, sourceElement, sourceElementType, targetElementType);
			Array.set(target, i, targetElement);
		}
		return target;
	}

}
