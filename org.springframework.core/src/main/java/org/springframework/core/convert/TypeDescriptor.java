/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Context about a type to convert to.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TypeDescriptor {

	/** Constant defining a TypeDescriptor for a <code>null</code> value */
	public static final TypeDescriptor NULL = new TypeDescriptor();

	private static final Map<Class<?>, TypeDescriptor> typeDescriptorCache = new HashMap<Class<?>, TypeDescriptor>();

	private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	static {
		typeDescriptorCache.put(boolean.class, new TypeDescriptor(boolean.class));
		typeDescriptorCache.put(Boolean.class, new TypeDescriptor(Boolean.class));
		typeDescriptorCache.put(byte.class, new TypeDescriptor(byte.class));
		typeDescriptorCache.put(Byte.class, new TypeDescriptor(Byte.class));
		typeDescriptorCache.put(char.class, new TypeDescriptor(char.class));
		typeDescriptorCache.put(Character.class, new TypeDescriptor(Character.class));
		typeDescriptorCache.put(short.class, new TypeDescriptor(short.class));
		typeDescriptorCache.put(Short.class, new TypeDescriptor(Short.class));
		typeDescriptorCache.put(int.class, new TypeDescriptor(int.class));
		typeDescriptorCache.put(Integer.class, new TypeDescriptor(Integer.class));
		typeDescriptorCache.put(long.class, new TypeDescriptor(long.class));
		typeDescriptorCache.put(Long.class, new TypeDescriptor(Long.class));
		typeDescriptorCache.put(float.class, new TypeDescriptor(float.class));
		typeDescriptorCache.put(Float.class, new TypeDescriptor(Float.class));
		typeDescriptorCache.put(double.class, new TypeDescriptor(double.class));
		typeDescriptorCache.put(Double.class, new TypeDescriptor(Double.class));
		typeDescriptorCache.put(String.class, new TypeDescriptor(String.class));
	}


	private Class<?> type;

	private MethodParameter methodParameter;

	private Field field;

	private int fieldNestingLevel = 1;

	private TypeDescriptor elementType;

	private TypeDescriptor mapKeyType;

	private TypeDescriptor mapValueType;

	private Annotation[] annotations;


	/**
	 * Create a new type descriptor from a method or constructor parameter.
	 * <p>Use this constructor when a target conversion point originates from a method parameter,
	 * such as a setter method argument.
	 * @param methodParameter the MethodParameter to wrap
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.type = methodParameter.getParameterType();
		this.methodParameter = methodParameter;
	}

	/**
	 * Create a new type descriptor for a field.
	 * Use this constructor when a target conversion point originates from a field.
	 * @param field the field to wrap
	 */
	public TypeDescriptor(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.type = field.getType();
		this.field = field;
	}

	/**
	 * Create a new type descriptor for the given class.
	 * @param type the class
	 * @return the type descriptor
	 */
	public static TypeDescriptor valueOf(Class<?> type) {
		if (type == null) {
			return NULL;
		}
		TypeDescriptor desc = typeDescriptorCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(type));
	}
	
	/**
	 * Create a new type descriptor for the class of the given object.
	 * @param object the object
	 * @return the type descriptor
	 */
	public static TypeDescriptor forObject(Object object) {
		if (object == null) {
			return NULL;
		}
		if (object instanceof Collection<?>) {
			return new TypeDescriptor(object.getClass(), CollectionUtils.findCommonElementType((Collection<?>) object));
		}
		else if (object instanceof Map<?, ?>) {
			return new TypeDescriptor(object.getClass(), CollectionUtils.findCommonElementType(((Map<?, ?>) object).keySet()), CollectionUtils.findCommonElementType(((Map<?, ?>) object).values()));
		}
		else {
			return valueOf(object.getClass());
		}
	}
	
	/**
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * @return the declared type, or <code>null</code> if this is {@link TypeDescriptor#NULL}
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * Determine the declared type of the wrapped parameter/field.
	 * Returns the Object wrapper type if the underlying type is a primitive.
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * Returns the name of this type: the fully qualified class name.
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * Is this type a primitive type?
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * Obtain the annotations associated with the wrapped parameter/field, if any.
	 */
	public synchronized Annotation[] getAnnotations() {
		if (this.annotations == null) {
			this.annotations = resolveAnnotations();
		}
		return this.annotations;
	}

	/**
	 * Obtain the annotation associated with the wrapped parameter/field, if any.
	 */
	public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
		for (Annotation annotation : getAnnotations()) {
			if (annotation.annotationType().equals(annotationType)) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Returns true if an object of this type can be assigned to a reference of given targetType.
	 * @param targetType the target type
	 * @return true if this type is assignable to the target
	 */
	public boolean isAssignableTo(TypeDescriptor targetType) {
		if (this == TypeDescriptor.NULL || targetType == TypeDescriptor.NULL) {
			return true;
		}
		if (isCollection() && targetType.isCollection() || isArray() && targetType.isArray()) {
			return targetType.getType().isAssignableFrom(getType()) &&
					getElementTypeDescriptor().isAssignableTo(targetType.getElementTypeDescriptor());
		}
		else if (isMap() && targetType.isMap()) {
			return targetType.getType().isAssignableFrom(getType()) &&
					getMapKeyTypeDescriptor().isAssignableTo(targetType.getMapKeyTypeDescriptor()) &&
					getMapValueTypeDescriptor().isAssignableTo(targetType.getMapValueTypeDescriptor());
		}
		else {
			return targetType.getObjectType().isAssignableFrom(getObjectType());
		}
	}

	/**
	 * A textual representation of the type descriptor (eg. Map<String,Foo>) for use in messages.
	 */
	public String asString() {
		return toString();
	}
	
	// indexable type descriptor operations
	
	/**
	 * Is this type an array type?
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * Is this type a {@link Collection} type?
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is an array type or {@link Collection} type, returns the underlying element type.
	 * Returns <code>null</code> if the type is neither an array or collection.
	 */
	public Class<?> getElementType() {
		return getElementTypeDescriptor().getType();
	}

	/**
	 * Return the element type as a type descriptor.
	 */
	public synchronized TypeDescriptor getElementTypeDescriptor() {
		if (!isCollection() && !isArray()) {
			throw new IllegalStateException("Not a collection or array type");			
		}
		if (this.elementType == null) {
			this.elementType = resolveElementTypeDescriptor();
		}
		return this.elementType;
	}

	// map type descriptor operations
	
	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * Determine the generic key type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getMapKeyType() {
		return getMapKeyTypeDescriptor().getType();
	}

	/**
	 * Returns map key type as a type descriptor.
	 */
	public synchronized TypeDescriptor getMapKeyTypeDescriptor() {
		if (!isMap()) {
			throw new IllegalStateException("Not a Map type");
		}
		if (this.mapKeyType == null) {
			this.mapKeyType = resolveMapKeyTypeDescriptor();
		}
		return this.mapKeyType;
	}

	/**
	 * Determine the generic value type of the wrapped Map parameter/field, if any.
	 * @return the generic type, or <code>null</code> if none
	 */
	public Class<?> getMapValueType() {
		return getMapValueTypeDescriptor().getType();
	}

	/**
	 * Returns map value type as a type descriptor.
	 */
	public synchronized TypeDescriptor getMapValueTypeDescriptor() {
		if (this.mapValueType == null) {
			this.mapValueType = resolveMapValueTypeDescriptor();
		}
		return this.mapValueType;
	}

	// special case public operations

	public TypeDescriptor(Class<?> componentType, MethodParameter methodParameter) {
		if (componentType == null) {
			componentType = Object.class;
		}
		this.type = componentType;
		this.methodParameter = methodParameter;
	}
	
	public MethodParameter getMethodParameter() {
		return methodParameter;
	}

	public TypeDescriptor applyType(Object object) {
		if (object == null) {
			return this;
		}
		// TODO preserve binding context with returned copy
		// TODO fall back to generic info if collection is empty
		if (object instanceof Collection<?>) {
			return new TypeDescriptor(object.getClass(), CollectionUtils.findCommonElementType((Collection<?>) object));
		}
		else if (object instanceof Map<?, ?>) {
			return new TypeDescriptor(object.getClass(), CollectionUtils.findCommonElementType(((Map<?, ?>) object).keySet()), CollectionUtils.findCommonElementType(((Map<?, ?>) object).values()));
		}
		else {
			return valueOf(object.getClass());
		}
	}
	
	// extending Object
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeDescriptor) || obj == TypeDescriptor.NULL) {
			return false;
		}
		TypeDescriptor other = (TypeDescriptor) obj;
		boolean annotatedTypeEquals = getType().equals(other.getType()) && ObjectUtils.nullSafeEquals(getAnnotations(), other.getAnnotations());
		if (isCollection()) {
			return annotatedTypeEquals && ObjectUtils.nullSafeEquals(getElementType(), other.getElementType());
		}
		else if (isMap()) {
			return annotatedTypeEquals && ObjectUtils.nullSafeEquals(getMapKeyType(), other.getMapKeyType()) &&
					ObjectUtils.nullSafeEquals(getMapValueType(), other.getMapValueType());
		}
		else {
			return annotatedTypeEquals;
		}
	}

	public int hashCode() {
		return (this == TypeDescriptor.NULL ? 0 : getType().hashCode());
	}

	public String toString() {
		if (this == TypeDescriptor.NULL) {
			return "null";
		}
		else {
			StringBuilder builder = new StringBuilder();
			Annotation[] anns = getAnnotations();
			for (Annotation ann : anns) {
				builder.append("@").append(ann.annotationType().getName()).append(' ');
			}
			builder.append(ClassUtils.getQualifiedName(getType()));
			if (isMap()) {
				builder.append("<").append(getMapKeyTypeDescriptor());
				builder.append(", ").append(getMapValueTypeDescriptor()).append(">");
			}
			else if (isCollection()) {
				builder.append("<").append(getElementTypeDescriptor()).append(">");
			}
			return builder.toString();
		}
	}

	// subclassing hooks
	
	protected Annotation[] resolveAnnotations() {
		if (this.field != null) {
			return this.field.getAnnotations();
		}
		else if (this.methodParameter != null) {
			if (this.methodParameter.getParameterIndex() < 0) {
				return this.methodParameter.getMethodAnnotations();
			}
			else {
				return this.methodParameter.getParameterAnnotations();
			}
		}
		else {
			return EMPTY_ANNOTATION_ARRAY;
		}
	}

	protected TypeDescriptor newComponentTypeDescriptor(Class<?> componentType, MethodParameter nested) {
		return new TypeDescriptor(componentType, nested);
	}
	
	// internal helpers

	private TypeDescriptor resolveElementTypeDescriptor() {
		if (isCollection()) {
			return createComponentTypeDescriptor(resolveCollectionElementType());
		}
		else {
			// TODO: GenericCollectionTypeResolver is not capable of applying nesting levels to array fields;
			// this means generic info of nested lists or maps stored inside array method parameters or fields is not obtainable
			return createComponentTypeDescriptor(getType().getComponentType());
		}
	}

	private TypeDescriptor resolveMapKeyTypeDescriptor() {
		return createComponentTypeDescriptor(resolveMapKeyType());
	}

	private TypeDescriptor resolveMapValueTypeDescriptor() {
		return createComponentTypeDescriptor(resolveMapValueType());
	}

	private TypeDescriptor createComponentTypeDescriptor(Class<?> componentType) {
		if (componentType == null) {
			componentType = Object.class;
		}
		if (this.methodParameter != null) {
			MethodParameter nested = new MethodParameter(this.methodParameter);
			nested.increaseNestingLevel();
			return newComponentTypeDescriptor(componentType, nested);				
		}
		else if (this.field != null) {
			return new TypeDescriptor(componentType, this.field, this.fieldNestingLevel + 1);
		}
		else {
			return TypeDescriptor.valueOf(componentType);
		}
	}

	private Class<?> resolveCollectionElementType() {
		if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getCollectionParameterType(this.methodParameter);
		}
		else if (this.field != null) {
			return GenericCollectionTypeResolver.getCollectionFieldType(this.field, this.fieldNestingLevel);
		}
        else {
			return GenericCollectionTypeResolver.getCollectionType((Class<? extends Collection>) this.type);
		}		
	}
	
	private Class<?> resolveMapKeyType() {
		if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getMapKeyParameterType(this.methodParameter);
		}
		else if (this.field != null) {
			return GenericCollectionTypeResolver.getMapKeyFieldType(this.field, this.fieldNestingLevel);
		}
		else {
			return GenericCollectionTypeResolver.getMapKeyType((Class<? extends Map>) this.type);
		}
	}

	private Class<?> resolveMapValueType() {
		if (this.methodParameter != null) {
			return GenericCollectionTypeResolver.getMapValueParameterType(this.methodParameter);
		}
		else if (this.field != null) {
			return GenericCollectionTypeResolver.getMapValueFieldType(this.field, this.fieldNestingLevel);
		}
		else {
			return GenericCollectionTypeResolver.getMapValueType((Class<? extends Map>) this.type);
		}
	}

	// internal constructors

	private TypeDescriptor(Class<?> componentType, Field field, int nestingLevel) {
		this.type = componentType;
		this.field = field;
		this.fieldNestingLevel = nestingLevel;
	}

	private TypeDescriptor() {
	}

	private TypeDescriptor(Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		this.type = type;
	}

	private TypeDescriptor(Class<?> collectionType, Class<?> elementType) {
		this.type = collectionType;
		if (elementType == null) {
			elementType = Object.class;
		}
		this.elementType = TypeDescriptor.valueOf(elementType);
	}
	
	private TypeDescriptor(Class<?> mapType, Class<?> keyType, Class<?> valueType) {
		this.type = mapType;
		if (keyType == null) {
			keyType = Object.class;
		}
		if (valueType == null) {
			valueType = Object.class;
		}
		this.mapKeyType = TypeDescriptor.valueOf(keyType);
		this.mapValueType = TypeDescriptor.valueOf(valueType);
	}

}