package com.mangofactory.swagger.mixins
import com.fasterxml.classmate.TypeResolver
import com.fasterxml.jackson.databind.ObjectMapper
import com.mangofactory.schema.DefaultGenericTypeNamingStrategy
import com.mangofactory.schema.DefaultTypeNameProvider
import com.mangofactory.schema.DefaultModelProvider
import com.mangofactory.schema.ModelDependencyProvider
import com.mangofactory.schema.ModelProvider
import com.mangofactory.schema.TypeNameExtractor
import com.mangofactory.schema.alternates.AlternateTypeProvider
import com.mangofactory.schema.alternates.AlternateTypeRule
import com.mangofactory.schema.configuration.SwaggerModelsConfiguration
import com.mangofactory.schema.plugins.DocumentationType
import com.mangofactory.schema.plugins.ModelBuilderPlugin
import com.mangofactory.schema.plugins.TypeNameProviderPlugin
import com.mangofactory.schema.plugins.ModelPropertyBuilderPlugin
import com.mangofactory.schema.plugins.SchemaPluginsManager
import com.mangofactory.schema.property.ObjectMapperBeanPropertyNamingStrategy
import com.mangofactory.schema.property.bean.AccessorsProvider
import com.mangofactory.schema.property.bean.BeanModelPropertyProvider
import com.mangofactory.schema.property.constructor.ConstructorModelPropertyProvider
import com.mangofactory.schema.property.field.FieldModelPropertyProvider
import com.mangofactory.schema.property.field.FieldProvider
import com.mangofactory.schema.property.provider.DefaultModelPropertiesProvider
import com.mangofactory.swagger.plugins.ApiModelBuilderPlugin
import com.mangofactory.swagger.plugins.ApiModelPropertyPropertyBuilderPlugin
import org.joda.time.LocalDate
import org.springframework.http.ResponseEntity
import org.springframework.plugin.core.OrderAwarePluginRegistry
import org.springframework.plugin.core.PluginRegistry

import static com.google.common.collect.Lists.*

@SuppressWarnings("GrMethodMayBeStatic")
class ModelProviderSupport {

  def documentationType() {
    new DocumentationType("swagger", "1.2")
  }
  SchemaPluginsManager pluginsManager() {
    PluginRegistry<ModelPropertyBuilderPlugin, DocumentationType> propRegistry =
            OrderAwarePluginRegistry.create(newArrayList(new ApiModelPropertyPropertyBuilderPlugin()))

    PluginRegistry<ModelBuilderPlugin, DocumentationType> modelRegistry =
            OrderAwarePluginRegistry.create(newArrayList(new ApiModelBuilderPlugin(new TypeResolver())))

    PluginRegistry<TypeNameProviderPlugin, DocumentationType> modelNameRegistry =
            OrderAwarePluginRegistry.create(newArrayList(new DefaultTypeNameProvider()))

    new SchemaPluginsManager(propRegistry, modelRegistry, modelNameRegistry)
  }

  ModelProvider providerThatSubstitutesLocalDateWithString() {
    TypeResolver typeResolver = new TypeResolver()
    AlternateTypeProvider alternateTypeProvider = new AlternateTypeProvider()
    alternateTypeProvider.addRule(new AlternateTypeRule(typeResolver.resolve(LocalDate), typeResolver.resolve(String)))
    defaultModelProvider(new ObjectMapper(), typeResolver, alternateTypeProvider)
  }

  ModelProvider providerThatSubstitutesResponseEntityOfVoid() {
    def resolver = new TypeResolver()
    def alternateTypeProvider = new AlternateTypeProvider()
    alternateTypeProvider.addRule(new AlternateTypeRule(resolver.resolve(ResponseEntity, Void),
            resolver.resolve(Void)))
    defaultModelProvider(new ObjectMapper(), resolver, alternateTypeProvider)
  }

  ModelProvider defaultModelProvider(ObjectMapper objectMapper = new ObjectMapper(),
                                     TypeResolver typeResolver = new  TypeResolver(),
                                     AlternateTypeProvider alternateTypeProvider = defaultAlternateTypesProvider()) {

    def fields = new FieldProvider(typeResolver)

    def pluginsManager = pluginsManager()
    TypeNameExtractor typeNameExtractor = new TypeNameExtractor(typeResolver, new DefaultGenericTypeNamingStrategy(),
            pluginsManager)
    def namingStrategy = new ObjectMapperBeanPropertyNamingStrategy(objectMapper)

    def beanModelPropertyProvider = new BeanModelPropertyProvider(new AccessorsProvider(typeResolver), typeResolver,
            alternateTypeProvider, namingStrategy, pluginsManager, typeNameExtractor)
    def fieldModelPropertyProvider = new FieldModelPropertyProvider(fields, alternateTypeProvider, namingStrategy,
            pluginsManager, typeNameExtractor)
    def constructorModelPropertyProvider =
            new ConstructorModelPropertyProvider(fields, alternateTypeProvider, namingStrategy, pluginsManager, typeNameExtractor)

    def modelPropertiesProvider = new DefaultModelPropertiesProvider(beanModelPropertyProvider,
            fieldModelPropertyProvider, constructorModelPropertyProvider)
    modelPropertiesProvider.objectMapper = objectMapper
    def modelDependenciesProvider = modelDependencyProvider(typeResolver, alternateTypeProvider,
            modelPropertiesProvider, typeNameExtractor)
    new DefaultModelProvider(typeResolver, alternateTypeProvider, modelPropertiesProvider, modelDependenciesProvider,
            pluginsManager, typeNameExtractor)
  }

  def defaultAlternateTypesProvider() {
    return new SwaggerModelsConfiguration().alternateTypeProvider(new TypeResolver())
  }

  ModelDependencyProvider modelDependencyProvider(TypeResolver resolver,
      AlternateTypeProvider alternateTypeProvider, DefaultModelPropertiesProvider modelPropertiesProvider,
      TypeNameExtractor typeNameExtractor) {
    new ModelDependencyProvider(resolver, alternateTypeProvider, modelPropertiesProvider, typeNameExtractor)
  }

  ModelDependencyProvider defaultModelDependencyProvider() {
    def typeResolver = new TypeResolver()
    def fields = new FieldProvider(typeResolver)
    def alternateTypeProvider = new AlternateTypeProvider()

    def pluginsManager = pluginsManager()
    TypeNameExtractor typeNameExtractor = new TypeNameExtractor(typeResolver, new DefaultGenericTypeNamingStrategy(),
            pluginsManager)
    def objectMapper = new ObjectMapper()
    def namingStrategy = new ObjectMapperBeanPropertyNamingStrategy(objectMapper);

    def beanModelPropertyProvider = new BeanModelPropertyProvider(new AccessorsProvider(typeResolver), typeResolver,
            alternateTypeProvider, namingStrategy, pluginsManager, typeNameExtractor)
    def fieldModelPropertyProvider = new FieldModelPropertyProvider(fields, alternateTypeProvider, namingStrategy,
            pluginsManager, typeNameExtractor)
    def constructorModelPropertyProvider =
            new ConstructorModelPropertyProvider(fields, alternateTypeProvider, namingStrategy, pluginsManager, typeNameExtractor)

    def modelPropertiesProvider = new DefaultModelPropertiesProvider(beanModelPropertyProvider,
            fieldModelPropertyProvider, constructorModelPropertyProvider)
    modelPropertiesProvider.objectMapper = objectMapper
    modelDependencyProvider(typeResolver, alternateTypeProvider, modelPropertiesProvider, typeNameExtractor)
  }

}
