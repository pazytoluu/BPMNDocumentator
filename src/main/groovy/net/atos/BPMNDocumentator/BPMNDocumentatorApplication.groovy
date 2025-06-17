package net.atos.BPMNDocumentator

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class BPMNDocumentatorApplication implements CommandLineRunner {

	static void main(String[] args) {
		SpringApplication.run(BPMNDocumentatorApplication.class, args)
	}

	@Override
	void run(String... args) throws Exception {
		if (args.length == 0) {
			System.err.println("ERREUR : Veuillez fournir le chemin vers le fichier BPMN en argument.")
			System.err.println("Usage: java -jar target/BPMNDocumentator-0.0.1-SNAPSHOT.jar <chemin_vers_fichier_bpmn> [chemin_vers_fichier_sortie]")
			System.exit(1)
		}
		// Call the main method of our script-like Main class
		Main.main(args)
	}
}
