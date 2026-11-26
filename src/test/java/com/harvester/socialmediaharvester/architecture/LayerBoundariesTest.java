package com.harvester.socialmediaharvester.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.WebExceptionHandler;

import com.harvester.socialmediaharvester.SocialMediaHarvesterApplication;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The Packages section of CODING_STANDARDS.md, checked on main code. Fix a failure by
 * putting the class where its responsibility belongs, never by renaming it to fit a rule.
 */
@AnalyzeClasses(packagesOf = SocialMediaHarvesterApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class LayerBoundariesTest {

	private static final String ROOT = "com.harvester.socialmediaharvester";

	@ArchTest
	static final ArchRule packagesDependOnlyAsTheTableAllows = layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.layer("config").definedBy(ROOT + ".config..")
			.layer("controller").definedBy(ROOT + ".controller..")
			.layer("service").definedBy(ROOT + ".service..")
			.layer("repository").definedBy(ROOT + ".repository..")
			.layer("model").definedBy(ROOT + ".model..")
			.layer("dto").definedBy(ROOT + ".dto..")
			.optionalLayer("mapper").definedBy(ROOT + ".mapper..")
			.optionalLayer("exception").definedBy(ROOT + ".exception..")
			.optionalLayer("client").definedBy(ROOT + ".client..")
			.optionalLayer("security").definedBy(ROOT + ".security..")
			.whereLayer("controller").mayNotBeAccessedByAnyLayer()
			.whereLayer("controller").mayOnlyAccessLayers("service", "dto", "mapper", "model", "exception")
			.whereLayer("service").mayOnlyAccessLayers("repository", "model", "exception", "client", "security", "config")
			.whereLayer("repository").mayOnlyAccessLayers("model")
			.whereLayer("mapper").mayOnlyAccessLayers("model", "dto")
			.whereLayer("model").mayOnlyAccessLayers("model")
			.whereLayer("dto").mayOnlyAccessLayers("dto", "model")
			.whereLayer("exception").mayOnlyAccessLayers("dto", "model")
			.whereLayer("client").mayOnlyAccessLayers("model", "exception", "config")
			.whereLayer("security").mayOnlyAccessLayers("model", "exception", "config");

	@ArchTest
	static final ArchRule everyClassSitsInAKnownPackage = classes()
			.that().doNotHaveFullyQualifiedName(SocialMediaHarvesterApplication.class.getName())
			.should().resideInAnyPackage(ROOT + ".config..", ROOT + ".controller..", ROOT + ".service..",
					ROOT + ".repository..", ROOT + ".model..", ROOT + ".dto..", ROOT + ".mapper..",
					ROOT + ".exception..", ROOT + ".client..", ROOT + ".security..")
			.because("a new package needs its row in the CODING_STANDARDS.md dependency table and its layer here");

	@ArchTest
	static final ArchRule onlyConfigurationPropertiesLeaveConfig = noClasses()
			.that().resideInAnyPackage(ROOT + ".service..", ROOT + ".client..", ROOT + ".security..")
			.should().dependOnClassesThat(resideInAPackage(ROOT + ".config..")
					.and(not(annotatedWith(ConfigurationProperties.class))))
			.because("outside config, only @ConfigurationProperties records are read; wiring stays in config");

	@ArchTest
	static final ArchRule dtosUseOnlyEnumsFromModel = noClasses()
			.that().resideInAPackage(ROOT + ".dto..")
			.should().dependOnClassesThat(resideInAPackage(ROOT + ".model..").and(not(JavaClass.Predicates.ENUMS)))
			.because("a DTO is mapped from a model by a controller or mapper, never built from one itself");

	@ArchTest
	static final ArchRule nothingReferencesTheExceptionHandler = noClasses()
			// Only the application's own handlers: @RestControllerAdvice is itself annotated
			// with @ControllerAdvice, so using the annotation must not count as a reference.
			.should().dependOnClassesThat(resideInAPackage(ROOT + "..")
					.and(JavaClass.Predicates.assignableTo(WebExceptionHandler.class)
							.or(annotatedWith(RestControllerAdvice.class))
							.or(annotatedWith(ControllerAdvice.class))))
			.because("error handlers are HTTP boundary components: Spring invokes them, application code does not");

	@ArchTest
	static final ArchRule businessFailuresAreNotHttpTypes = noClasses()
			.that().resideInAnyPackage(ROOT + ".service..", ROOT + ".repository..", ROOT + ".model..")
			.should().dependOnClassesThat().resideInAPackage("org.springframework.http..")
			.orShould().dependOnClassesThat().areAssignableTo(ResponseStatusException.class)
			.orShould().dependOnClassesThat().areAnnotatedWith(ResponseStatus.class)
			.orShould().beAnnotatedWith(ResponseStatus.class)
			.because("business failures are application exceptions; GlobalExceptionHandler picks the HTTP status");

	@ArchTest
	static final ArchRule httpEndpointsLiveInController = classes()
			.that().areAnnotatedWith(RestController.class).or().areAnnotatedWith(Controller.class)
			.should().resideInAPackage(ROOT + ".controller..");

	@ArchTest
	static final ArchRule exceptionsAndTheirHandlerLiveInException = classes()
			.that().areAssignableTo(Throwable.class)
			.or().areAnnotatedWith(RestControllerAdvice.class).or().areAnnotatedWith(ControllerAdvice.class)
			.or().areAssignableTo(WebExceptionHandler.class)
			.should().resideInAPackage(ROOT + ".exception..");

	@ArchTest
	static final ArchRule configurationLivesInConfig = classes()
			.that().areAnnotatedWith(Configuration.class).or().areAnnotatedWith(ConfigurationProperties.class)
			.should().resideInAPackage(ROOT + ".config..");

	@ArchTest
	static final ArchRule tablesLiveInModel = classes()
			.that().areAnnotatedWith(Table.class)
			.should().resideInAPackage(ROOT + ".model..");

	@ArchTest
	static final ArchRule springDataRepositoriesLiveInRepository = classes()
			.that().areAssignableTo(Repository.class)
			.should().resideInAPackage(ROOT + ".repository..");
}
