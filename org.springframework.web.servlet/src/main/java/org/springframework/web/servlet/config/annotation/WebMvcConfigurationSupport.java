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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.converter.xml.XmlAwareFormHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * A base class that provides default configuration for Spring MVC applications by registering Spring MVC 
 * infrastructure components to be detected by the {@link DispatcherServlet}. Typically applications should not
 * have to extend this class. A more likely place to start is to annotate an @{@link Configuration}
 * class with @{@link EnableWebMvc} (see @{@link EnableWebMvc} and {@link WebMvcConfigurer} for details).
 * 
 * <p>If using @{@link EnableWebMvc} does not give you all you need, consider extending directly from this 
 * class. Remember to add @{@link Configuration} to your subclass and @{@link Bean} to any superclass 
 * @{@link Bean} methods you choose to override.
 * 
 * <p>This class registers the following {@link HandlerMapping}s:</p>
 * <ul>
 * 	<li>{@link RequestMappingHandlerMapping} ordered at 0 for mapping requests to annotated controller methods.
 * 	<li>{@link HandlerMapping} ordered at 1 to map URL paths directly to view names.
 * 	<li>{@link BeanNameUrlHandlerMapping} ordered at 2 to map URL paths to controller bean names.
 * 	<li>{@link HandlerMapping} ordered at {@code Integer.MAX_VALUE-1} to serve static resource requests.
 * 	<li>{@link HandlerMapping} ordered at {@code Integer.MAX_VALUE} to forward requests to the default servlet.
 * </ul>
 *
 * <p>Registers these {@link HandlerAdapter}s:
 * <ul>
 * 	<li>{@link RequestMappingHandlerAdapter} for processing requests with annotated controller methods.
 * 	<li>{@link HttpRequestHandlerAdapter} for processing requests with {@link HttpRequestHandler}s.
 * 	<li>{@link SimpleControllerHandlerAdapter} for processing requests with interface-based {@link Controller}s.
 * </ul>
 *
 * <p>Registers a {@link HandlerExceptionResolverComposite} with this chain of exception resolvers:
 * <ul>
 * 	<li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through @{@link ExceptionHandler} methods.
 * 	<li>{@link ResponseStatusExceptionResolver} for exceptions annotated with @{@link ResponseStatus}.
 * 	<li>{@link DefaultHandlerExceptionResolver} for resolving known Spring exception types
 * </ul>
 *
 * <p>Registers these other instances:
 * <ul>
 * 	<li>{@link FormattingConversionService} for use with annotated controller methods and the spring:eval JSP tag.
 * 	<li>{@link Validator} for validating model attributes on annotated controller methods.
 * </ul>
 *
 * @see EnableWebMvc
 * @see WebMvcConfigurer
 * @see WebMvcConfigurerAdapter
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class WebMvcConfigurationSupport implements ApplicationContextAware, ServletContextAware {

	private ServletContext servletContext;

	private ApplicationContext applicationContext;

	private List<Object> interceptors;

	private List<HttpMessageConverter<?>> messageConverters;

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * Returns a {@link RequestMappingHandlerMapping} ordered at 0 for mapping requests to annotated controllers.
	 */
	@Bean
	public RequestMappingHandlerMapping requestMappingHandlerMapping() {
		RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
		handlerMapping.setOrder(0);
		handlerMapping.setInterceptors(getInterceptors());
		return handlerMapping;
	}
	
	/**
	 * Provides access to the shared handler interceptors used to configure {@link HandlerMapping} instances with.
	 * This method cannot be overridden, use {@link #addInterceptors(InterceptorRegistry)} instead. 
	 */
	protected final Object[] getInterceptors() {
		if (interceptors == null) {
			InterceptorRegistry registry = new InterceptorRegistry();
			addInterceptors(registry);
			registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService()));
			interceptors = registry.getInterceptors();
		}
		return interceptors.toArray();
	}
	
	/**
	 * Override this method to add Spring MVC interceptors for pre/post-processing of controller invocation.
	 * @see InterceptorRegistry
	 */
	protected void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * Returns a handler mapping ordered at 1 to map URL paths directly to view names.
	 * To configure view controllers, override {@link #addViewControllers(ViewControllerRegistry)}. 
	 */
	@Bean
	public HandlerMapping viewControllerHandlerMapping() {
		ViewControllerRegistry registry = new ViewControllerRegistry();
		addViewControllers(registry);
		
		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		handlerMapping = handlerMapping != null ? handlerMapping : new EmptyHandlerMapping();
		handlerMapping.setInterceptors(getInterceptors());
		return handlerMapping;
	}

	/**
	 * Override this method to add view controllers.
	 * @see ViewControllerRegistry
	 */
	protected void addViewControllers(ViewControllerRegistry registry) {
	}
	
	/**
	 * Returns a {@link BeanNameUrlHandlerMapping} ordered at 2 to map URL paths to controller bean names.
	 */
	@Bean
	public BeanNameUrlHandlerMapping beanNameHandlerMapping() {
		BeanNameUrlHandlerMapping mapping = new BeanNameUrlHandlerMapping();
		mapping.setOrder(2);
		mapping.setInterceptors(getInterceptors());
		return mapping;
	}

	/**
	 * Returns a handler mapping ordered at Integer.MAX_VALUE-1 with mapped resource handlers.
	 * To configure resource handling, override {@link #addResourceHandlers(ResourceHandlerRegistry)}.
	 */
	@Bean
	public HandlerMapping resourceHandlerMapping() {
		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(applicationContext, servletContext);
		addResourceHandlers(registry);
		AbstractHandlerMapping handlerMapping = registry.getHandlerMapping();
		handlerMapping = handlerMapping != null ? handlerMapping : new EmptyHandlerMapping();
		return handlerMapping;
	}

	/**
	 * Override this method to add resource handlers for serving static resources. 
	 * @see ResourceHandlerRegistry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * Returns a handler mapping ordered at Integer.MAX_VALUE with a mapped default servlet handler.
	 * To configure "default" Servlet handling, override 
	 * {@link #configureDefaultServletHandling(DefaultServletHandlerConfigurer)}.  
	 */
	@Bean
	public HandlerMapping defaultServletHandlerMapping() {
		DefaultServletHandlerConfigurer configurer = new DefaultServletHandlerConfigurer(servletContext);
		configureDefaultServletHandling(configurer);
		AbstractHandlerMapping handlerMapping = configurer.getHandlerMapping();
		handlerMapping = handlerMapping != null ? handlerMapping : new EmptyHandlerMapping();
		return handlerMapping;
	}

	/**
	 * Override this method to configure "default" Servlet handling. 
	 * @see DefaultServletHandlerConfigurer
	 */
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * Returns a {@link RequestMappingHandlerAdapter} for processing requests through annotated controller methods.
	 * Consider overriding one of these other more fine-grained methods:
	 * <ul>
	 *  <li>{@link #addArgumentResolvers(List)} for adding custom argument resolvers.
	 * 	<li>{@link #addReturnValueHandlers(List)} for adding custom return value handlers.
	 * 	<li>{@link #configureMessageConverters(List)} for adding custom message converters.
	 * </ul>
	 */
	@Bean
	public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
		ConfigurableWebBindingInitializer webBindingInitializer = new ConfigurableWebBindingInitializer();
		webBindingInitializer.setConversionService(mvcConversionService());
		webBindingInitializer.setValidator(mvcValidator());
		
		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();
		addArgumentResolvers(argumentResolvers);

		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<HandlerMethodReturnValueHandler>();
		addReturnValueHandlers(returnValueHandlers);
		
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
		adapter.setMessageConverters(getMessageConverters());
		adapter.setWebBindingInitializer(webBindingInitializer);
		adapter.setCustomArgumentResolvers(argumentResolvers);
		adapter.setCustomReturnValueHandlers(returnValueHandlers);
		return adapter;
	}

	/**
	 * Add custom {@link HandlerMethodArgumentResolver}s to use in addition to the ones registered by default.
	 * <p>Custom argument resolvers are invoked before built-in resolvers except for those that rely on the presence 
	 * of annotations (e.g. {@code @RequestParameter}, {@code @PathVariable}, etc.). The latter can be customized 
	 * by configuring the {@link RequestMappingHandlerAdapter} directly. 
	 * @param argumentResolvers the list of custom converters; initially an empty list.
	 */
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	/**
	 * Add custom {@link HandlerMethodReturnValueHandler}s in addition to the ones registered by default.
	 * <p>Custom return value handlers are invoked before built-in ones except for those that rely on the presence 
	 * of annotations (e.g. {@code @ResponseBody}, {@code @ModelAttribute}, etc.). The latter can be customized
	 * by configuring the {@link RequestMappingHandlerAdapter} directly.
	 * @param returnValueHandlers the list of custom handlers; initially an empty list.
	 */
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	/**
	 * Provides access to the shared {@link HttpMessageConverter}s used by the 
	 * {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}. 
	 * This method cannot be overridden. Use {@link #configureMessageConverters(List)} instead.
	 * Also see {@link #addDefaultHttpMessageConverters(List)} that can be used to add default message converters.
	 */
	protected final List<HttpMessageConverter<?>> getMessageConverters() {
		if (messageConverters == null) {
			messageConverters = new ArrayList<HttpMessageConverter<?>>();
			configureMessageConverters(messageConverters);
			if (messageConverters.isEmpty()) {
				addDefaultHttpMessageConverters(messageConverters);
			}
		}
		return messageConverters;
	}

	/**
	 * Override this method to add custom {@link HttpMessageConverter}s to use with 
	 * the {@link RequestMappingHandlerAdapter} and the {@link ExceptionHandlerExceptionResolver}.
	 * Adding converters to the list turns off the default converters that would otherwise be registered by default.
	 * Also see {@link #addDefaultHttpMessageConverters(List)} that can be used to add default message converters.
	 * @param converters a list to add message converters to; initially an empty list.
	 */
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * A method available to subclasses to add default {@link HttpMessageConverter}s. 
	 * @param messageConverters the list to add the default message converters to
	 */
	protected final void addDefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
		stringConverter.setWriteAcceptCharset(false);

		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(stringConverter);
		messageConverters.add(new ResourceHttpMessageConverter());
		messageConverters.add(new SourceHttpMessageConverter<Source>());
		messageConverters.add(new XmlAwareFormHttpMessageConverter());

		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent("javax.xml.bind.Binder", classLoader)) {
			messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
		}
		if (ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", classLoader)) {
			messageConverters.add(new MappingJacksonHttpMessageConverter());
		}
		if (ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", classLoader)) {
			messageConverters.add(new AtomFeedHttpMessageConverter());
			messageConverters.add(new RssChannelHttpMessageConverter());
		}
	}
	
	/**
	 * Returns a {@link FormattingConversionService} for use with annotated controller methods and the 
	 * {@code spring:eval} JSP tag. Also see {@link #addFormatters(FormatterRegistry)} as an alternative
	 * to overriding this method.
	 */
	@Bean
	public FormattingConversionService mvcConversionService() {
		FormattingConversionService conversionService = new DefaultFormattingConversionService();
		addFormatters(conversionService);
		return conversionService;
	}

	/**
	 * Override this method to add custom {@link Converter}s and {@link Formatter}s.
	 */
	protected void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * Returns {@link Validator} for validating {@code @ModelAttribute} and {@code @RequestBody} arguments of 
	 * annotated controller methods. To configure a custom validation, override {@link #getValidator()}.
	 */
	@Bean
	Validator mvcValidator() {
		Validator validator = getValidator();
		if (validator != null) {
			return validator;
		}
		else if (ClassUtils.isPresent("javax.validation.Validator", getClass().getClassLoader())) {
			Class<?> clazz;
			try {
				String className = "org.springframework.validation.beanvalidation.LocalValidatorFactoryBean";
				clazz = ClassUtils.forName(className, WebMvcConfigurationSupport.class.getClassLoader());
			} catch (ClassNotFoundException e) {
				throw new BeanInitializationException("Could not find default validator");
			} catch (LinkageError e) {
				throw new BeanInitializationException("Could not find default validator");
			}
			return (Validator) BeanUtils.instantiate(clazz);
		}
		else {
			return new Validator() {
				public boolean supports(Class<?> clazz) {
					return false;
				}
				public void validate(Object target, Errors errors) {
				}
			};
		}
	}

	/**
	 * Override this method to provide a custom {@link Validator}.
	 */
	protected Validator getValidator() {
		return null;
	}

	/**
	 * Returns a {@link HttpRequestHandlerAdapter} for processing requests with {@link HttpRequestHandler}s.
	 */
	@Bean
	public HttpRequestHandlerAdapter httpRequestHandlerAdapter() {
		return new HttpRequestHandlerAdapter();
	}

	/**
	 * Returns a {@link SimpleControllerHandlerAdapter} for processing requests with interface-based controllers.
	 */
	@Bean
	public SimpleControllerHandlerAdapter simpleControllerHandlerAdapter() {
		return new SimpleControllerHandlerAdapter();
	}

	/**
	 * Returns a {@link HandlerExceptionResolverComposite} that contains a list of exception resolvers.
	 * To customize the list of exception resolvers, override {@link #configureHandlerExceptionResolvers(List)}.
	 */
	@Bean
	HandlerExceptionResolver handlerExceptionResolver() throws Exception {
		List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<HandlerExceptionResolver>();
		configureHandlerExceptionResolvers(exceptionResolvers);
		
		if (exceptionResolvers.isEmpty()) {
			addDefaultHandlerExceptionResolvers(exceptionResolvers);
		}
		
		HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();
		composite.setOrder(0);
		composite.setExceptionResolvers(exceptionResolvers);
		return composite;
	}

	/**
	 * Override this method to configure the list of {@link HandlerExceptionResolver}s to use.
	 * Adding resolvers to the list turns off the default resolvers that would otherwise be registered by default.
	 * Also see {@link #addDefaultHandlerExceptionResolvers(List)} that can be used to add the default exception resolvers.
	 * @param exceptionResolvers a list to add exception resolvers to; initially an empty list.
	 */
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
	}

	/**
	 * A method available to subclasses for adding default {@link HandlerExceptionResolver}s.
	 * <p>Adds the following exception resolvers:
	 * <ul>
	 * 	<li>{@link ExceptionHandlerExceptionResolver} for handling exceptions through @{@link ExceptionHandler} methods.
	 * 	<li>{@link ResponseStatusExceptionResolver} for exceptions annotated with @{@link ResponseStatus}.
	 * 	<li>{@link DefaultHandlerExceptionResolver} for resolving known Spring exception types
	 * </ul>
	 */
	protected final void addDefaultHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver = new ExceptionHandlerExceptionResolver();
		exceptionHandlerExceptionResolver.setMessageConverters(getMessageConverters());
		exceptionHandlerExceptionResolver.afterPropertiesSet();

		exceptionResolvers.add(exceptionHandlerExceptionResolver);
		exceptionResolvers.add(new ResponseStatusExceptionResolver());
		exceptionResolvers.add(new DefaultHandlerExceptionResolver());
	}

	private final static class EmptyHandlerMapping extends AbstractHandlerMapping {
		
		@Override
		protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
			return null;
		}
	}
	
}
