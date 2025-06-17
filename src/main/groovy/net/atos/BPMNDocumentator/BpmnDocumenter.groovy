package net.atos.BPMNDocumentator

import org.camunda.bpm.model.bpmn.Bpmn
import org.camunda.bpm.model.bpmn.BpmnModelInstance
import org.camunda.bpm.model.bpmn.instance.*
import org.camunda.bpm.model.xml.instance.ModelElementInstance
import java.nio.file.Files
import java.nio.file.Paths

class BpmnDocumenter {

    private static final Map<String, String> TYPE_TRANSLATIONS = [
        "process": "Processus",
        "userTask": "Tâche utilisateur",
        "serviceTask": "Tâche de service",
        "scriptTask": "Tâche de script",
        "businessRuleTask": "Tâche de règle métier",
        "manualTask": "Tâche manuelle",
        "sendTask": "Tâche d'envoi",
        "receiveTask": "Tâche de réception",
        "startEvent": "Événement de démarrage",
        "endEvent": "Événement de fin",
        "intermediateThrowEvent": "Événement intermédiaire de lancement",
        "intermediateCatchEvent": "Événement intermédiaire de capture",
        "boundaryEvent": "Événement de frontière",
        "sequenceFlow": "Flux de séquence",
        "exclusiveGateway": "Passerelle exclusive",
        "parallelGateway": "Passerelle parallèle",
        "inclusiveGateway": "Passerelle inclusive",
        "eventBasedGateway": "Passerelle basée sur les événements",
        "complexGateway": "Passerelle complexe",
        "participant": "Participant",
        "collaboration": "Collaboration",
        "lane": "Couloir",
        "callActivity": "Activité d'appel",
        "subProcess": "Sous-Processus",
        // Camunda specific types often have 'camunda' prefix in properties, not element type itself
        // Standard BPMN types are generally sufficient here.
    ]

    String generateDocumentation(String bpmnFilePath) {
        File bpmnFile = new File(bpmnFilePath)
        if (!bpmnFile.exists()) {
            return "Erreur : Fichier BPMN non trouvé à l'emplacement : ${bpmnFilePath}"
        }

        BpmnModelInstance modelInstance
        try {
            modelInstance = Bpmn.readModelFromFile(bpmnFile)
        } catch (Exception e) {
            return "Erreur lors de la lecture du fichier BPMN : ${e.getMessage()}"
        }

        StringBuilder documentation = new StringBuilder()
        documentation.append("# Documentation du Modèle BPMN : ${bpmnFile.getName()}\n\n")

        // Document Collaborations and Participants first if they exist
        def collaborations = modelInstance.getModelElementsByType(Collaboration.class)
        collaborations.each { collaboration ->
            documentation.append("## Collaboration : ${collaboration.getId()}\n")
            collaboration.getParticipants().each { participant ->
                documentation.append("### Participant : ${participant.getName() ?: participant.getId()}\n")
                documentation.append("- **ID** : `${participant.getId()}`\n")
                if (participant.getProcess() != null) {
                    documentation.append("- **Processus Référencé** : `${participant.getProcess().getId()}`\n")
                }
                documentation.append("\n")
            }
        }

        def processDefinitions = modelInstance.getModelElementsByType(Process.class)
        if (processDefinitions.isEmpty() && collaborations.isEmpty()) {
             return "Aucun processus ou collaboration trouvé dans le fichier BPMN."
        }

        processDefinitions.each { process ->
            String processName = process.getName() ?: process.getId()
            String translatedProcessType = TYPE_TRANSLATIONS.getOrDefault("process", "Processus")
            documentation.append("## ${translatedProcessType} : ${processName}\n")
            documentation.append("- **ID** : `${process.getId()}`\n")
            documentation.append("- **Exécutable** : ${process.isExecutable() ? 'Oui' : 'Non'}\n")
            String processDocumentation = getElementDocumentation(process)
            if (processDocumentation) {
                documentation.append("- **Documentation du processus** : ${processDocumentation}\n")
            }
            documentation.append("\n")

            if (!process.getLanes().isEmpty()){
                 documentation.append("### Couloirs (Lanes) : \n")
                 process.getLanes().each { lane ->
                    documentation.append("#### ${TYPE_TRANSLATIONS.getOrDefault('lane','Couloir')} : ${lane.getName() ?: lane.getId()}\n")
                    documentation.append("- **ID** : `${lane.getId()}`\n")
                    String laneDocumentation = getElementDocumentation(lane)
                    if (laneDocumentation){
                        documentation.append("- **Documentation du couloir** : ${laneDocumentation}\n")
                    }
                    documentation.append("##### Éléments dans ce couloir:\n")
                     lane.getFlowNodeRefs().each{ flowNode ->
                        extractElementDetails(flowNode, documentation, modelInstance)
                     }
                    documentation.append("\n")
                 }
            } else {
                // Elements not in lanes (or if no lanes defined)
                process.getFlowElements().each { element ->
                    // Avoid duplicating elements already processed via lanes
                    if (element.getParentElement() instanceof Process) {
                         extractElementDetails(element, documentation, modelInstance)
                    }
                }
            }
            documentation.append("\n")
        }

        return documentation.toString()
    }

    private void extractElementDetails(FlowElement element, StringBuilder documentation, BpmnModelInstance modelInstance) {
        String originalElementType = element.getElementType().getTypeName()
        String translatedElementType = TYPE_TRANSLATIONS.getOrDefault(originalElementType, originalElementType)
        String elementName = element.getName()
        String elementId = element.getId()
        String elementDocs = getElementDocumentation(element)

        documentation.append("#### ${translatedElementType} : ${elementName ?: ''} \n")
        documentation.append("- **ID** : `${elementId}`\n")
        // No need to print "Type" again if it's in the title, unless it's a fallback
        if (!TYPE_TRANSLATIONS.containsKey(originalElementType)){
            documentation.append("- **Type BPMN Original** : `${originalElementType}`\n")
        }

        if (elementName) {
            // documentation.append("- **Nom** : ${elementName}\n") // Already in title
        }
        if (elementDocs) {
            documentation.append("- **Documentation** : ${elementDocs.replaceAll("\n"," ")}\n") // Basic formatting for multiline docs
        }

        if (element instanceof UserTask) {
            UserTask task = (UserTask) element
            if (task.getCamundaAssignee()) documentation.append("- **Assigné (Camunda)** : `${task.getCamundaAssignee()}`\n")
            if (task.getCamundaCandidateUsers()) documentation.append("- **Utilisateurs Candidats (Camunda)** : `${task.getCamundaCandidateUsers()}`\n")
            if (task.getCamundaCandidateGroups()) documentation.append("- **Groupes Candidats (Camunda)** : `${task.getCamundaCandidateGroups()}`\n")
            if (task.getCamundaDueDate()) documentation.append("- **Date d'Échéance (Camunda)** : `${task.getCamundaDueDate()}`\n")
            if (task.getCamundaFollowUpDate()) documentation.append("- **Date de Suivi (Camunda)** : `${task.getCamundaFollowUpDate()}`\n")
            if (task.getCamundaPriority()) documentation.append("- **Priorité (Camunda)** : `${task.getCamundaPriority()}`\n")
        } else if (element instanceof ServiceTask) {
            ServiceTask task = (ServiceTask) element
            if (task.getCamundaExpression()) documentation.append("- **Expression (Camunda)** : `${task.getCamundaExpression()}`\n")
            else if (task.getCamundaDelegateExpression()) documentation.append("- **Expression Déléguée (Camunda)** : `${task.getCamundaDelegateExpression()}`\n")
            else if (task.getCamundaClass()) documentation.append("- **Classe Java (Camunda)** : `${task.getCamundaClass()}`\n")
            if (task.getCamundaResultVariable()) documentation.append("- **Variable de Résultat (Camunda)** : `${task.getCamundaResultVariable()}`\n")
            // Consider adding topic for external tasks
        } else if (element instanceof ScriptTask) {
            ScriptTask task = (ScriptTask) element
            if (task.getScriptFormat()) documentation.append("- **Format de Script** : `${task.getScriptFormat()}`\n")
            if (task.getScript() != null && task.getScript().getTextContent()) documentation.append("- **Script** : ```${task.getScript().getTextContent()}```\n")
            if (task.getCamundaResultVariable()) documentation.append("- **Variable de Résultat (Camunda)** : `${task.getCamundaResultVariable()}`\n")
        } else if (element instanceof BusinessRuleTask) {
            BusinessRuleTask task = (BusinessRuleTask) element
            if (task.getCamundaDecisionRef()) documentation.append("- **Référence de Décision DMN (Camunda)** : `${task.getCamundaDecisionRef()}`\n")
            // Other DMN related attributes
        } else if (element instanceof SendTask) {
            SendTask task = (SendTask) element
            if (task.getCamundaExpression()) documentation.append("- **Expression (Camunda)** : `${task.getCamundaExpression()}`\n")
            // Other Camunda attributes for send task
        } else if (element instanceof ReceiveTask) {
            ReceiveTask task = (ReceiveTask) element
            if (task.getMessage() != null) documentation.append("- **Message Référencé** : `${task.getMessage().getName()}`\n")
            // Other attributes
        } else if (element instanceof CallActivity) {
            CallActivity call = (CallActivity) element
            if (call.getCalledElement()) documentation.append("- **Processus Appelé** : `${call.getCalledElement()}`\n")
            if (call.getCamundaCalledElementBinding()) documentation.append("- **Liaison (Camunda)** : `${call.getCamundaCalledElementBinding()}`\n")
            if (call.getCamundaCalledElementVersion()) documentation.append("- **Version (Camunda)** : `${call.getCamundaCalledElementVersion()}`\n")
        } else if (element instanceof SequenceFlow) {
            SequenceFlow sequenceFlow = (SequenceFlow) element
            documentation.append("- **De** : `${sequenceFlow.getSource().getId()}` (${sequenceFlow.getSource().getName() ?: 'ID Source'})\n")
            documentation.append("- **À** : `${sequenceFlow.getTarget().getId()}` (${sequenceFlow.getTarget().getName() ?: 'ID Cible'})\n")
            if (sequenceFlow.getConditionExpression() != null) {
                documentation.append("- **Condition** : `${sequenceFlow.getConditionExpression().getTextContent()}`\n")
            }
        } else if (element instanceof StartEvent) {
            StartEvent event = (StartEvent) element
            if (event.getCamundaFormKey()) documentation.append("- **Clé de Formulaire (Camunda)** : `${event.getCamundaFormKey()}`\n")
            if (event.getCamundaInitiator()) documentation.append("- **Initiateur (Camunda)** : `${event.getCamundaInitiator()}`\n")
        } else if (element instanceof EndEvent) {
            // EndEvent specific (e.g. error code for error end event)
        } else if (element instanceof IntermediateCatchEvent || element instanceof BoundaryEvent) {
            // Common for catch events (e.g. timer definition, message definition)
            if (element.getEventDefinitions().size() > 0) {
                 EventDefinition eventDef = element.getEventDefinitions().iterator().next();
                 if (eventDef instanceof TimerEventDefinition) {
                     TimerEventDefinition ted = (TimerEventDefinition) eventDef;
                     if (ted.getTimeDate() != null) documentation.append("- **Date du minuteur** : `${ted.getTimeDate().getTextContent()}`\n");
                     if (ted.getTimeDuration() != null) documentation.append("- **Durée du minuteur** : `${ted.getTimeDuration().getTextContent()}`\n");
                     if (ted.getTimeCycle() != null) documentation.append("- **Cycle du minuteur** : `${ted.getTimeCycle().getTextContent()}`\n");
                 } else if (eventDef instanceof MessageEventDefinition) {
                     MessageEventDefinition med = (MessageEventDefinition) eventDef;
                     if (med.getMessage() != null) documentation.append("- **Message Réf.** : `${med.getMessage().getName()}` (ID: `${med.getMessage().getId()}`)\n");
                 } else if (eventDef instanceof SignalEventDefinition) {
                     SignalEventDefinition sed = (SignalEventDefinition) eventDef;
                     if (sed.getSignal() != null) documentation.append("- **Signal Réf.** : `${sed.getSignal().getName()}` (ID: `${sed.getSignal().getId()}`)\n");
                 }
                 // Add other event definition types (Conditional, Escalation, etc.)
            }
            if (element instanceof BoundaryEvent) {
                BoundaryEvent boundaryEvent = (BoundaryEvent) element;
                documentation.append("- **Attaché à** : `${boundaryEvent.getAttachedTo().getId()}`\n");
                documentation.append("- **Annuler l'activité (Cancel Activity)** : ${boundaryEvent.cancelActivity() ? 'Oui' : 'Non'}\n");
            }
        }
        // Add more specific details for other element types (Gateways, other Events, etc.)

        documentation.append("\n") // Add a blank line for spacing in Markdown
    }

    private String getElementDocumentation(ModelElementInstance element) {
        // For Process and Lane, getDocumentation is not directly available, need to access Documentation sub-element
        if (element.getDomElement().getChildElementsByNameNs("http://www.omg.org/spec/BPMN/20100524/MODEL", "documentation").isEmpty()){
            return null
        }
        return element.getDomElement().getChildElementsByNameNs("http://www.omg.org/spec/BPMN/20100524/MODEL", "documentation")
                .collect { it.getTextContent() }.join("\n").trim()
    }
}
