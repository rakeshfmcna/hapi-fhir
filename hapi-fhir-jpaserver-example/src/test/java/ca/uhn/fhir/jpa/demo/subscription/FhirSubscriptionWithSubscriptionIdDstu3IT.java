/*
 *  Copyright 2016 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ca.uhn.fhir.jpa.demo.subscription;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.EncodingEnum;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.URI;

/**
 * Adds a FHIR subscription with criteria through the rest interface. Then creates a websocket with the id of the
 * subscription
 *
 * Note: This test only returns a ping with the subscription id, Check FhirSubscriptionWithSubscriptionIdDstu3IT for
 * a test that returns the xml of the observation
 *
 * To execute the following test, execute it the following way:
 * 1. Execute the 'createPatient' test
 * 2. Update the patient id in the 'createSubscriptionWithoutWebsocket' and the 'sendObservation' tests
 * 3. Execute the 'createSubscription' test
 * 4. Update the subscription id in the 'attachWebSocket' test
 * 5. Execute the 'attachWebSocket' test
 * 6. Execute the 'sendObservation' test
 * 7. Look in the 'attachWebSocket' terminal execution and wait for your ping with the subscription id
 */
public class FhirSubscriptionWithSubscriptionIdDstu3IT {

    private static final Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirSubscriptionWithSubscriptionIdDstu3IT.class);

    public static final String LPI_CODESYSTEM = "http://cognitivemedicine.com/lpi";
    public static final String LPI_CODE = "LPI-FHIR";
    public static final String FHIR_URL = "http://localhost:8080/baseDstu3";

    @Test
    public void createPatient() throws Exception{
        FhirContext ctx = FhirContext.forDstu3();
        IGenericClient client = ctx.newRestfulGenericClient(FHIR_URL);
        Patient patient = getPatient();
        MethodOutcome methodOutcome = client.create().resource(patient).execute();
        String id = methodOutcome.getId().getIdPart();
        patient.setId(id);
        System.out.println("Patient id generated by server is: " + id);
    }

    @Test
    public void createSubscription() {
        String patientId = "1";
        FhirContext ctx = FhirContext.forDstu3();
        IGenericClient client = ctx.newRestfulGenericClient(FHIR_URL);

        Subscription subscription = new Subscription();
        subscription.setReason("Monitor new neonatal function (note, age will be determined by the monitor)");
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        //subscription.setCriteria("Observation?subject=Patient/" + patientId +"&_format=xml");
        subscription.setCriteria("Observation?subject=Patient/" + patientId);
        Subscription.SubscriptionChannelComponent channel = new Subscription.SubscriptionChannelComponent();
        channel.setType(Subscription.SubscriptionChannelType.WEBSOCKET);
        channel.setPayload("application/json");
        subscription.setChannel(channel);

        MethodOutcome methodOutcome = client.create().resource(subscription).execute();
        String id = methodOutcome.getId().getIdPart();

        System.out.println("Subscription id generated by server is: " + id);
    }

    @Test
    public void attachWebSocket() throws Exception{
        String subscriptionId = "5";
        subscriptionId = subscriptionId + "";

        String target = "ws://localhost:8080/websocket/dstu3";

        WebSocketClient webSocketClient = new WebSocketClient();
        SocketImplementation socket = new SocketImplementation(subscriptionId, EncodingEnum.JSON);

        try {
            webSocketClient.start();
            URI echoUri = new URI(target);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            ourLog.info("Connecting to : {}", echoUri);
            webSocketClient.connect(socket, echoUri, request);

            while (true) {
                Thread.sleep(500L);
            }

        } finally {
            try {
                ourLog.info("Shutting down websocket client");
                webSocketClient.stop();
            } catch (Exception e) {
                ourLog.error("Failure", e);
            }
        }
    }

    @Test
    public void createObservation() throws Exception {
        Observation observation = new Observation();
        CodeableConcept codeableConcept = new CodeableConcept();
        observation.setCode(codeableConcept);
        Coding coding = codeableConcept.addCoding();
        coding.setCode("82313006");
        coding.setSystem("SNOMED-CT");

        observation.setStatus(Observation.ObservationStatus.FINAL);

        FhirContext ctx = FhirContext.forDstu3();
        IGenericClient client = ctx.newRestfulGenericClient(FHIR_URL);

        MethodOutcome methodOutcome2 = client.create().resource(observation).execute();
        String observationId = methodOutcome2.getId().getIdPart();
        observation.setId(observationId);

        System.out.println("Observation id generated by server is: " + observationId);
    }

    @Test
    public void createObservationThatDoesNotMatch() throws Exception {
        String patientId = "4";
        Observation observation = new Observation();
        IdDt idDt = new IdDt();
        idDt.setValue("Patient/"+ patientId);
        CodeableConcept codeableConcept = new CodeableConcept();
        observation.setCode(codeableConcept);
        Coding coding = codeableConcept.addCoding();
        coding.setCode("8231");
        coding.setSystem("SNOMED-CT");
        observation.setStatus(Observation.ObservationStatus.FINAL);

        FhirContext ctx = FhirContext.forDstu3();
        IGenericClient client = ctx.newRestfulGenericClient(FHIR_URL);

        MethodOutcome methodOutcome2 = client.create().resource(observation).execute();
        String observationId = methodOutcome2.getId().getIdPart();
        observation.setId(observationId);

        System.out.println("Observation id generated by server is: " + observationId);

    }

    public Patient getPatient(){
        String patientId = "1";

        Patient patient = new Patient();
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        Identifier identifier = patient.addIdentifier();
        identifier.setValue(patientId);
        identifier.setSystem(LPI_CODESYSTEM);

        IBaseMetaType meta = patient.getMeta();
        IBaseCoding tag = meta.addTag();
        tag.setCode(LPI_CODE);
        tag.setSystem(LPI_CODESYSTEM);

        setTag(patient);

        return patient;
    }

    public static void setTag(IBaseResource resource){
        IBaseMetaType meta = resource.getMeta();
        IBaseCoding tag = meta.addTag();
        tag.setCode(LPI_CODE);
        tag.setSystem(LPI_CODESYSTEM);
    }
}
