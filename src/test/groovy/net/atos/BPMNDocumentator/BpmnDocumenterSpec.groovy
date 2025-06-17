package net.atos.BPMNDocumentator

import spock.lang.Specification
import java.nio.file.Paths

class BpmnDocumenterSpec extends Specification {

    def "should generate correct French Markdown documentation for a simple BPMN process"() {
        given: "A BpmnDocumenter and a path to a test BPMN file"
        BpmnDocumenter documenter = new BpmnDocumenter()
        // Assuming the test runs from the project root directory
        String testBpmnPath = Paths.get("src", "test", "resources", "bpmn", "test-diagram.bpmn").toAbsolutePath().toString()
        File testFile = new File(testBpmnPath)
        assert testFile.exists() : "Test BPMN file not found at: ${testBpmnPath}"


        when: "Documentation is generated"
        String documentation = documenter.generateDocumentation(testBpmnPath)
        println "Generated Documentation:\n${documentation}" // For debugging during test development

        then: "The documentation should contain expected elements in French and Markdown"
        documentation != null
        documentation.startsWith("# Documentation du Modèle BPMN : test-diagram.bpmn")

        // Process information
        documentation.contains("## Processus : Processus de Test")
        documentation.contains("- **ID** : `Process_Test`")
        documentation.contains("- **Exécutable** : Oui")
        documentation.contains("- **Documentation du processus** : Ceci est la documentation du processus de test.")

        // Start Event
        documentation.contains("#### Événement de démarrage : Début ") // Trailing space from 'elementName ?: '''
        documentation.contains("- **ID** : `StartEvent_1`")

        // User Task
        documentation.contains("#### Tâche utilisateur : Tâche Utilisateur 1 ")
        documentation.contains("- **ID** : `UserTask_1`")
        documentation.contains("- **Documentation** : Documentation pour la tâche utilisateur 1.")
        documentation.contains("- **Assigné (Camunda)** : `testUser`")

        // End Event
        documentation.contains("#### Événement de fin : Fin ")
        documentation.contains("- **ID** : `EndEvent_1`")

        // Sequence Flows
        documentation.contains("#### Flux de séquence :  ") // Name is often empty for sequence flows
        documentation.contains("- **ID** : `Flow_1`")
        documentation.contains("- **De** : `StartEvent_1` (Début)")
        documentation.contains("- **À** : `UserTask_1` (Tâche Utilisateur 1)")

        documentation.contains("- **ID** : `Flow_2`")
        documentation.contains("- **De** : `UserTask_1` (Tâche Utilisateur 1)")
        documentation.contains("- **À** : `EndEvent_1` (Fin)")
    }

    def "should handle non-existent BPMN file gracefully"() {
        given: "A BpmnDocumenter and a path to a non-existent BPMN file"
        BpmnDocumenter documenter = new BpmnDocumenter()
        String nonExistentPath = "path/to/non/existent/file.bpmn"

        when: "Documentation generation is attempted"
        String documentation = documenter.generateDocumentation(nonExistentPath)

        then: "An error message should be returned"
        documentation == "Erreur : Fichier BPMN non trouvé à l'emplacement : ${nonExistentPath}"
    }

    def "should handle invalid BPMN file gracefully"() {
        given: "A BpmnDocumenter and a path to an invalid (e.g., empty or corrupted) BPMN file"
        BpmnDocumenter documenter = new BpmnDocumenter()
        File invalidFile = Paths.get("src", "test", "resources", "bpmn", "invalid-diagram.bpmn").toFile()
        invalidFile.parentFile.mkdirs()
        invalidFile.write("This is not valid XML for BPMN.")
        String invalidBpmnPath = invalidFile.absolutePath


        when: "Documentation generation is attempted"
        String documentation = documenter.generateDocumentation(invalidBpmnPath)
        println "Generated Documentation for invalid file:\n${documentation}"


        then: "An error message indicating parsing error should be returned"
        documentation.startsWith("Erreur lors de la lecture du fichier BPMN :")

        cleanup:
        invalidFile.delete()
    }
}
