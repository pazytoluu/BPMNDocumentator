package net.atos.BPMNDocumentator

import groovy.util.logging.Slf4j

@Slf4j
class Main {
    static void main(String[] args) {
        if (args.length == 0) {
            println "Usage: groovy net.atos.BPMNDocumentator.Main <chemin_vers_fichier_bpmn> [chemin_vers_fichier_sortie]"
            System.exit(1)
        }

        String bpmnFilePath = args[0]
        String outputFilePath = args.length > 1 ? args[1] : null

        BpmnDocumenter documenter = new BpmnDocumenter()
        String documentation = documenter.generateDocumentation(bpmnFilePath)

        if (outputFilePath) {
            try {
                new File(outputFilePath).write(documentation, 'UTF-8')
                println "Documentation générée et sauvegardée dans : ${outputFilePath}"
            } catch (IOException e) {
                System.err.println "Erreur lors de l'écriture du fichier de sortie : ${e.getMessage()}"
                // Fallback to console output
                println "\nDocumentation:\n${documentation}"
            }
        } else {
            println documentation
        }
    }
}
