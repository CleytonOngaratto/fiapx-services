package br.com.fiap.fiapx.video.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.com.fiap.fiapx");

    @Test
    void domain_is_framework_free() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta..", "io.quarkus..", "org.hibernate..", "org.mapstruct..")
                .check(CLASSES);
    }

    @Test
    void usecase_does_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("..usecase..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(CLASSES);
    }

    @Test
    void usecase_does_not_depend_on_adapter() {
        noClasses()
                .that().resideInAPackage("..usecase..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .check(CLASSES);
    }

    @Test
    void domain_and_usecase_do_not_know_external_providers() {
        noClasses()
                .that().resideInAnyPackage("..domain..", "..usecase..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("software.amazon..", "com.rabbitmq..", "io.vertx..", "redis.clients..")
                .check(CLASSES);
    }

    @Test
    void interactors_carry_no_injection_annotation() {
        noClasses()
                .that().resideInAPackage("..usecase.interactor..")
                .should().beAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                .orShould().beAnnotatedWith("jakarta.inject.Singleton")
                .check(CLASSES);
    }
}
